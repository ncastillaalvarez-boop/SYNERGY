package com.proaula.spring.synergy.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.Catalogos;
import com.proaula.spring.synergy.Model.Comentario;
import com.proaula.spring.synergy.Model.EntregaTarea;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Service.RiesgoCalculadora.ActividadInfo;
import com.proaula.spring.synergy.Service.RiesgoCalculadora.Indicadores;
import com.proaula.spring.synergy.Service.RiesgoCalculadora.TareaInfo;

/**
 * Modelo predictivo de riesgo de los proyectos.
 *
 *  1. construirDataset(): proyectos ya terminados -> variables a la MITAD del proyecto +
 *     nivel_riesgo según cómo TERMINÓ. Se descarga en CSV/ARFF (panel del administrador) y se
 *     entrena en Google Colab (notebook en docs/modelo-predictivo).
 *  2. El modelo elegido se exporta desde Colab a código Java (m2cgen) -> ModeloRiesgo.java.
 *  3. recalcularTodos(): predice el riesgo de los proyectos ACTIVOS con ModeloRiesgo y guarda
 *     nivel, probabilidad, motivos y sugerencia en cada proyecto. Corre al arrancar, cada noche
 *     y cuando cambian las tareas de un proyecto.
 */
@Service
@Order(Ordered.LOWEST_PRECEDENCE)
public class RiesgoProyectoService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RiesgoProyectoService.class);

    /** Mínimo de tareas para que un proyecto terminado entre al dataset. */
    private static final int MIN_TAREAS_DATASET = 4;

    private final MongoTemplate mongoTemplate;

    public RiesgoProyectoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /** Resultado para un proyecto. nivel == null: todavía no hay datos para predecir. */
    public record Evaluacion(String nivel, double probabilidad, double puntaje, double[] probabilidades,
                             Indicadores indicadores, List<String> motivos, String sugerencia) {
    }

    /** Fila del dataset de entrenamiento. */
    public record FilaDataset(String codigo, Indicadores variables, String nivelRiesgo) {
    }

    // =====================================================================
    // DATOS (una consulta por colección, solo los campos necesarios)
    // =====================================================================

    private record Datos(Map<String, List<TareaInfo>> tareas, Map<String, List<ActividadInfo>> entregas,
                         Map<String, List<ActividadInfo>> comentarios) {
        List<TareaInfo> tareasDe(String id) { return tareas.getOrDefault(id, List.of()); }
        List<ActividadInfo> entregasDe(String id) { return entregas.getOrDefault(id, List.of()); }
        List<ActividadInfo> comentariosDe(String id) { return comentarios.getOrDefault(id, List.of()); }
    }

    private Datos cargar(Collection<String> idsProyecto) {
        Query qt = new Query();
        if (idsProyecto != null) {
            qt.addCriteria(Criteria.where("idProyecto").in(idsProyecto));
        }
        qt.fields().include("idProyecto", "estado", "fechaCreacion", "fechaEntrega", "fechaActualizacion");
        Map<String, List<TareaInfo>> tareas = new HashMap<>();
        Map<String, String> proyectoDeTarea = new HashMap<>();
        for (Tarea t : mongoTemplate.find(qt, Tarea.class)) {
            if (t.getIdProyecto() == null) {
                continue;
            }
            proyectoDeTarea.put(t.getId(), t.getIdProyecto());
            tareas.computeIfAbsent(t.getIdProyecto(), k -> new ArrayList<>()).add(info(t));
        }

        Query qe = new Query(Criteria.where("idTarea").in(proyectoDeTarea.keySet()));
        qe.fields().include("idTarea", "idUsuario", "fechaEntrega"); // sin el archivo (campo "datos")
        Map<String, List<ActividadInfo>> entregas = new HashMap<>();
        for (EntregaTarea e : mongoTemplate.find(qe, EntregaTarea.class)) {
            String idProyecto = proyectoDeTarea.get(e.getIdTarea());
            if (idProyecto != null) {
                entregas.computeIfAbsent(idProyecto, k -> new ArrayList<>())
                        .add(new ActividadInfo(e.getIdUsuario(), fecha(e.getFechaEntrega())));
            }
        }

        Query qc = new Query();
        if (idsProyecto != null) {
            qc.addCriteria(Criteria.where("idProyecto").in(idsProyecto));
        }
        qc.fields().include("idProyecto", "idUsuario", "fecha");
        Map<String, List<ActividadInfo>> comentarios = new HashMap<>();
        for (Comentario c : mongoTemplate.find(qc, Comentario.class)) {
            if (c.getIdProyecto() != null) {
                comentarios.computeIfAbsent(c.getIdProyecto(), k -> new ArrayList<>())
                        .add(new ActividadInfo(c.getIdUsuario(), fecha(c.getFecha())));
            }
        }
        return new Datos(tareas, entregas, comentarios);
    }

    /** Una tarea completada se toma como cumplida en su última actualización. */
    public static TareaInfo info(Tarea t) {
        LocalDate creada = fecha(t.getFechaCreacion());
        LocalDate completada = "COMPLETADA".equalsIgnoreCase(t.getEstado())
                ? (t.getFechaActualizacion() != null ? fecha(t.getFechaActualizacion()) : creada)
                : null;
        return new TareaInfo(t.getEstado(), creada, t.getFechaEntrega(), completada);
    }

    private static LocalDate fecha(LocalDateTime f) {
        return f == null ? null : f.toLocalDate();
    }

    private static List<String> voluntarios(Proyecto p) {
        return p.getParticipantes() == null ? List.of()
                : p.getParticipantes().stream().filter(id -> id != null && !id.equals(p.getIdLider())).toList();
    }

    private static Indicadores indicadores(Proyecto p, Datos d, LocalDate corte) {
        return RiesgoCalculadora.calcular(p.getFechaInicio(), p.getFechaFin(), p.getCategoria(), voluntarios(p),
                d.tareasDe(p.getId()), d.entregasDe(p.getId()), d.comentariosDe(p.getId()), corte);
    }

    // =====================================================================
    // DATASET PARA COLAB
    // =====================================================================

    public List<FilaDataset> construirDataset() {
        Query q = new Query(Criteria.where("estado").in("COMPLETADO", "CANCELADO")
                .and("fechaFin").lt(LocalDate.now()).and("fechaInicio").ne(null));
        List<Proyecto> terminados = mongoTemplate.find(q, Proyecto.class);
        Datos d = cargar(terminados.stream().map(Proyecto::getId).toList());
        return construirDataset(terminados, d.tareas(), d.entregas(), d.comentarios());
    }

    /** Versión sin base de datos (se usa también en las pruebas). */
    public static List<FilaDataset> construirDataset(List<Proyecto> terminados, Map<String, List<TareaInfo>> tareas,
                                                     Map<String, List<ActividadInfo>> entregas,
                                                     Map<String, List<ActividadInfo>> comentarios) {
        Datos d = new Datos(tareas, entregas, comentarios);
        List<FilaDataset> filas = new ArrayList<>();
        for (Proyecto p : terminados) {
            if (p.getFechaInicio() == null || p.getFechaFin() == null || !p.getFechaFin().isAfter(p.getFechaInicio())) {
                continue;
            }
            long validas = d.tareasDe(p.getId()).stream().filter(t -> !"CANCELADA".equalsIgnoreCase(t.estado())).count();
            if (validas < MIN_TAREAS_DATASET) {
                continue;
            }
            long dias = ChronoUnit.DAYS.between(p.getFechaInicio(), p.getFechaFin());
            LocalDate mitad = p.getFechaInicio().plusDays(dias / 2);
            Indicadores enLaMitad = indicadores(p, d, mitad);
            Indicadores alFinal = indicadores(p, d, p.getFechaFin());
            filas.add(new FilaDataset(p.getCodigo(), enLaMitad, RiesgoCalculadora.nivelPorReglas(alFinal)));
        }
        return filas;
    }

    public static String aCsv(List<FilaDataset> filas) {
        StringBuilder sb = new StringBuilder("codigo,categoria,");
        sb.append(String.join(",", RiesgoCalculadora.VARIABLES_NUMERICAS)).append(",nivel_riesgo\n");
        for (FilaDataset f : filas) {
            Indicadores v = f.variables();
            sb.append(csv(f.codigo())).append(',').append(csv(v.categoria())).append(',')
                    .append(numeros(v)).append(',').append(f.nivelRiesgo()).append('\n');
        }
        return sb.toString();
    }

    /** Formato ARFF para abrir el mismo dataset en Weka (como dice la propuesta). */
    public static String aArff(List<FilaDataset> filas) {
        StringBuilder sb = new StringBuilder("% Synergy - dataset del modelo predictivo de riesgo\n");
        sb.append("% Variables medidas a la mitad del proyecto; nivel_riesgo segun como termino.\n");
        sb.append("@RELATION riesgo_proyectos_synergy\n\n");
        sb.append("@ATTRIBUTE categoria {");
        sb.append(String.join(",", Catalogos.CATEGORIAS.stream().map(c -> "'" + c.replace("'", "") + "'").toList()));
        sb.append("}\n");
        for (String v : RiesgoCalculadora.VARIABLES_NUMERICAS) {
            sb.append("@ATTRIBUTE ").append(v).append(" NUMERIC\n");
        }
        sb.append("@ATTRIBUTE nivel_riesgo {Bajo,Medio,Alto}\n\n@DATA\n");
        for (FilaDataset f : filas) {
            String cat = f.variables().categoria() == null || !Catalogos.CATEGORIAS.contains(f.variables().categoria())
                    ? "?" : "'" + f.variables().categoria().replace("'", "") + "'";
            sb.append(cat).append(',').append(numeros(f.variables())).append(',').append(f.nivelRiesgo()).append('\n');
        }
        return sb.toString();
    }

    private static String numeros(Indicadores v) {
        return String.format(Locale.ROOT, "%d,%.3f,%d,%.3f,%d,%.3f,%.3f,%.3f,%d", v.diasRetraso(),
                v.pctTareasPendientes(), v.participantes(), v.tasaParticipacion(), v.duracionDias(),
                v.avanceReal(), v.avanceEsperado(), v.brechaAvance(), v.actividades());
    }

    private static String csv(String s) {
        if (s == null) {
            return "";
        }
        return s.contains(",") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    // =====================================================================
    // PREDICCIÓN
    // =====================================================================

    /** Predice con el modelo exportado desde Colab (ModeloRiesgo.java). */
    public static Evaluacion predecir(Indicadores i) {
        // Proyecto que apenas empieza y aún no tiene tareas: no hay con qué predecir
        if (i.totalTareas() == 0 && i.avanceEsperado() < 0.10) {
            return new Evaluacion(null, 0, 0, new double[3], i, List.of("Proyecto recién iniciado, aún sin tareas"),
                    "Crear y asignar las primeras tareas del proyecto.");
        }
        double[] prob = ModeloRiesgo.score(i.vector());
        int mejor = 0;
        for (int k = 1; k < prob.length; k++) {
            if (prob[k] > prob[mejor]) {
                mejor = k;
            }
        }
        String nivel = RiesgoCalculadora.NIVELES.get(mejor);
        double puntaje = prob[2] + 0.5 * prob[1];
        return new Evaluacion(nivel, prob[mejor], puntaje, prob, i, RiesgoCalculadora.motivos(i),
                RiesgoCalculadora.sugerencia(nivel, i));
    }

    public Evaluacion evaluar(Proyecto p) {
        Datos d = cargar(List.of(p.getId()));
        return predecir(indicadores(p, d, corteActual(p)));
    }

    private static LocalDate corteActual(Proyecto p) {
        LocalDate hoy = LocalDate.now();
        return p.getFechaFin() != null && p.getFechaFin().isBefore(hoy) ? p.getFechaFin() : hoy;
    }

    /** Tamaño de cada lote de escritura en MongoDB. */
    private static final int LOTE = 500;

    /** Último recálculo completo y último error (se muestran en el panel del administrador). */
    private volatile LocalDateTime ultimoCalculo;
    private volatile String ultimoError;

    public LocalDateTime getUltimoCalculo() { return ultimoCalculo; }
    public String getUltimoError() { return ultimoError; }

    /**
     * Recalcula el riesgo de todos los proyectos activos (y limpia el de los demás).
     * Un proyecto con datos raros no detiene a los demás: se omite y se registra en el log.
     */
    @Scheduled(cron = "${synergy.riesgo.cron:0 30 2 * * *}")
    public synchronized int recalcularTodos() {
        long inicio = System.currentTimeMillis();
        ultimoCalculo = LocalDateTime.now();
        try {
            List<Proyecto> activos = mongoTemplate.find(Query.query(Criteria.where("estado").is("ACTIVO")), Proyecto.class);
            Datos d = cargar(activos.stream().map(Proyecto::getId).filter(id -> id != null).toList());
            BulkOperations bulk = null;
            int enLote = 0;
            int n = 0;
            int sinFechas = 0;
            int fallidos = 0;
            for (Proyecto p : activos) {
                if (p.getId() == null || p.getFechaInicio() == null || p.getFechaFin() == null) {
                    sinFechas++;
                    continue;
                }
                Update u;
                try {
                    u = actualizacion(predecir(indicadores(p, d, corteActual(p))));
                } catch (RuntimeException e) {
                    if (fallidos++ == 0) {
                        log.error("Error calculando el riesgo del proyecto {} ({})", p.getId(), p.getNombre(), e);
                    }
                    continue;
                }
                if (bulk == null) {
                    bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Proyecto.class);
                }
                bulk.updateOne(Query.query(Criteria.where("_id").is(p.getId())), u);
                n++;
                if (++enLote == LOTE) {
                    bulk.execute();
                    bulk = null;
                    enLote = 0;
                }
            }
            if (bulk != null) {
                bulk.execute();
            }
            mongoTemplate.updateMulti(Query.query(Criteria.where("estado").ne("ACTIVO").and("nivelRiesgo").exists(true)),
                    new Update().unset("nivelRiesgo").unset("probabilidadRiesgo").unset("puntajeRiesgo")
                            .unset("riesgoMotivos").unset("riesgoSugerencia").unset("riesgoActualizado"), Proyecto.class);
            log.info("Riesgo recalculado para {} de {} proyectos activos en {} ms (sin fechas: {}, con error: {})",
                    n, activos.size(), System.currentTimeMillis() - inicio, sinFechas, fallidos);
            ultimoError = fallidos > 0 ? fallidos + " proyectos no se pudieron calcular (ver la consola)." : null;
            return n;
        } catch (RuntimeException e) {
            ultimoError = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.error("No se pudo recalcular el riesgo de los proyectos", e);
            throw e;
        }
    }

    /** Se llama cuando cambian las tareas o entregas de un proyecto. Nunca lanza errores. */
    public void recalcularProyecto(String idProyecto) {
        try {
            Proyecto p = idProyecto == null ? null : mongoTemplate.findById(idProyecto, Proyecto.class);
            if (p == null || !"ACTIVO".equals(p.getEstado()) || p.getFechaInicio() == null || p.getFechaFin() == null) {
                return;
            }
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(p.getId())), actualizacion(evaluar(p)), Proyecto.class);
        } catch (RuntimeException e) {
            log.warn("No se pudo recalcular el riesgo del proyecto {}: {}", idProyecto, e.getMessage());
        }
    }

    private static Update actualizacion(Evaluacion e) {
        Update u = new Update()
                .set("riesgoMotivos", e.motivos())
                .set("riesgoSugerencia", e.sugerencia())
                .set("riesgoActualizado", LocalDateTime.now());
        if (e.nivel() == null) {
            u.unset("nivelRiesgo").unset("probabilidadRiesgo").set("puntajeRiesgo", 0.0);
        } else {
            u.set("nivelRiesgo", e.nivel())
                    .set("probabilidadRiesgo", Math.round(e.probabilidad() * 1000) / 1000.0)
                    .set("puntajeRiesgo", Math.round(e.puntaje() * 1000) / 1000.0);
        }
        return u;
    }

    /** Al arrancar la aplicación (después de preparar los datos). */
    @Override
    public void run(ApplicationArguments args) {
        try {
            recalcularTodos();
        } catch (RuntimeException e) {
            // el detalle ya quedó en el log; la aplicación arranca igual
            log.warn("No se pudo calcular el riesgo al arrancar: {}", e.toString(), e);
        }
    }
}
