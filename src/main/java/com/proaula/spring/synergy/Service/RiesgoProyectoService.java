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
import com.proaula.spring.synergy.Model.HistorialRiesgo;
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
    private final NotificacionService notificaciones;

    public RiesgoProyectoService(MongoTemplate mongoTemplate, NotificacionService notificaciones) {
        this.mongoTemplate = mongoTemplate;
        this.notificaciones = notificaciones;
    }

    /** Resultado para un proyecto. nivel == null: todavía no hay datos para predecir. */
    public record Evaluacion(String nivel, double probabilidad, double puntaje, double[] probabilidades,
                             Indicadores indicadores, List<String> motivos, String sugerencia,
                             String factor, LocalDate fechaEstimadaFin) {

        Evaluacion conFechaEstimada(LocalDate fecha) {
            return new Evaluacion(nivel, probabilidad, puntaje, probabilidades, indicadores, motivos, sugerencia, factor, fecha);
        }
    }

    /** Historial para la mini gráfica: puntos (fecha, nivel, puntaje) y el último cambio en palabras. */
    public record ResumenHistorial(List<HistorialRiesgo> puntos, String cambio, String polilinea) {
        public boolean isVacio() {
            return puntos == null || puntos.size() < 2;
        }
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
                    "Crear y asignar las primeras tareas del proyecto.", null, null);
        }
        double[] prob = ModeloRiesgo.score(i.vector());
        int mejor = 0;
        for (int k = 1; k < prob.length; k++) {
            if (prob[k] > prob[mejor]) {
                mejor = k;
            }
        }
        String nivel = RiesgoCalculadora.NIVELES.get(mejor);
        double puntaje = puntaje(prob);
        return new Evaluacion(nivel, prob[mejor], puntaje, prob, i, RiesgoCalculadora.motivos(i),
                RiesgoCalculadora.sugerencia(nivel, i), "Bajo".equals(nivel) ? null : factorPrincipal(i, puntaje), null);
    }

    /** P(Alto) + 0,5·P(Medio): un solo número de 0 a 1 para ordenar por prioridad. */
    static double puntaje(double[] prob) {
        return prob[2] + 0.5 * prob[1];
    }

    private static double puntaje(Indicadores i) {
        return puntaje(ModeloRiesgo.score(i.vector()));
    }

    /**
     * Explicación de la predicción: se "arregla" una cosa a la vez (el atraso, el avance, la
     * participación) y se mide cuánto baja el riesgo según el MISMO modelo. Lo que más lo baja es
     * lo que más está influyendo. Devuelve null si ningún cambio lo baja de forma apreciable.
     */
    public static String factorPrincipal(Indicadores i, double puntajeBase) {
        String mejor = null;
        double mayorBaja = 0.05;

        if (i.diasRetraso() > 0 || i.tareasVencidas() > 0) {
            Indicadores sin = new Indicadores(0, i.pctTareasPendientes(), i.participantes(), i.tasaParticipacion(),
                    i.duracionDias(), i.avanceReal(), i.avanceEsperado(), i.brechaAvance(), i.actividades(),
                    i.totalTareas(), 0, i.categoria());
            double baja = puntajeBase - puntaje(sin);
            if (baja > mayorBaja) {
                mayorBaja = baja;
                mejor = i.tareasVencidas() == 1
                        ? "una tarea vencida hace " + i.diasRetraso() + " días"
                        : i.tareasVencidas() + " tareas vencidas (la más atrasada, " + i.diasRetraso() + " días)";
            }
        }
        if (i.brechaAvance() < 0 && i.totalTareas() > 0) {
            double pct = Math.max(0, Math.min(1, 1 - i.avanceEsperado()));
            Indicadores alDia = new Indicadores(i.diasRetraso(), pct, i.participantes(), i.tasaParticipacion(),
                    i.duracionDias(), 1 - pct, i.avanceEsperado(), 0, i.actividades(), i.totalTareas(),
                    i.tareasVencidas(), i.categoria());
            double baja = puntajeBase - puntaje(alDia);
            if (baja > mayorBaja) {
                mayorBaja = baja;
                mejor = "el avance: va " + Math.round(-i.brechaAvance() * 100) + " puntos por debajo de lo esperado";
            }
        }
        if (i.participantes() > 0 && i.tasaParticipacion() < 1.0) {
            Indicadores todos = new Indicadores(i.diasRetraso(), i.pctTareasPendientes(), i.participantes(), 1.0,
                    i.duracionDias(), i.avanceReal(), i.avanceEsperado(), i.brechaAvance(),
                    Math.max(i.actividades(), i.participantes() * 2), i.totalTareas(), i.tareasVencidas(), i.categoria());
            double baja = puntajeBase - puntaje(todos);
            if (baja > mayorBaja) {
                mayorBaja = baja;
                mejor = "la participación: solo " + Math.round(i.tasaParticipacion() * 100) + " % de los voluntarios ha participado";
            }
        }
        if (i.participantes() == 0) {
            Indicadores conEquipo = new Indicadores(i.diasRetraso(), i.pctTareasPendientes(), 5, 1.0, i.duracionDias(),
                    i.avanceReal(), i.avanceEsperado(), i.brechaAvance(), Math.max(i.actividades(), 10), i.totalTareas(),
                    i.tareasVencidas(), i.categoria());
            double baja = puntajeBase - puntaje(conEquipo);
            if (baja > mayorBaja) {
                mejor = "que no tiene voluntarios inscritos";
            }
        }
        return mejor;
    }

    public Evaluacion evaluar(Proyecto p) {
        Datos d = cargar(List.of(p.getId()));
        return evaluar(p, d);
    }

    private static Evaluacion evaluar(Proyecto p, Datos d) {
        LocalDate corte = corteActual(p);
        return predecir(indicadores(p, d, corte))
                .conFechaEstimada(RiesgoCalculadora.estimarFin(p.getFechaInicio(), d.tareasDe(p.getId()), corte));
    }

    /** Simulador para el líder: cómo cambiaría el riesgo con algunas acciones. */
    public Map<String, Object> simular(Proyecto p, int completar, int activar, int nuevos) {
        Datos d = cargar(List.of(p.getId()));
        Indicadores actual = indicadores(p, d, corteActual(p));
        Evaluacion antes = predecir(actual);
        Evaluacion despues = predecir(RiesgoCalculadora.simular(actual, completar, activar, nuevos));
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("antes", resumen(antes));
        r.put("despues", resumen(despues));
        int pendientes = (int) Math.round(actual.pctTareasPendientes() * actual.totalTareas());
        int activos = (int) Math.round(actual.tasaParticipacion() * actual.participantes());
        r.put("maxCompletar", pendientes);
        r.put("maxActivar", Math.max(0, actual.participantes() - activos));
        return r;
    }

    private static Map<String, Object> resumen(Evaluacion e) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("nivel", e.nivel());
        m.put("puntaje", Math.round(e.puntaje() * 1000) / 1000.0);
        double[] p = e.probabilidades();
        m.put("probabilidades", Map.of("Bajo", Math.round(p[0] * 1000) / 1000.0, "Medio", Math.round(p[1] * 1000) / 1000.0,
                "Alto", Math.round(p[2] * 1000) / 1000.0));
        m.put("factor", e.factor());
        return m;
    }

    private static LocalDate corteActual(Proyecto p) {
        LocalDate hoy = LocalDate.now();
        return p.getFechaFin() != null && p.getFechaFin().isBefore(hoy) ? p.getFechaFin() : hoy;
    }

    // =====================================================================
    // HISTORIAL Y AVISOS
    // =====================================================================

    /** Semanas hacia atrás que se reconstruyen la primera vez que se calcula un proyecto. */
    private static final int SEMANAS_RECONSTRUIR = 8;

    /** Riesgo de los últimos {@code dias} días de un proyecto (del más antiguo al más reciente). */
    public ResumenHistorial historial(String idProyecto, int dias) {
        Query q = Query.query(Criteria.where("idProyecto").is(idProyecto).and("fecha").gte(LocalDate.now().minusDays(dias)))
                .with(org.springframework.data.domain.Sort.by("fecha"));
        List<HistorialRiesgo> puntos = mongoTemplate.find(q, HistorialRiesgo.class);
        return resumirHistorial(puntos, LocalDate.now());
    }

    /** Texto del último cambio de nivel y puntos de la mini gráfica (160 x 40). Sin base de datos. */
    public static ResumenHistorial resumirHistorial(List<HistorialRiesgo> puntos, LocalDate hoy) {
        if (puntos == null || puntos.isEmpty()) {
            return new ResumenHistorial(List.of(), null, "");
        }
        String cambio = null;
        HistorialRiesgo ultimo = puntos.get(puntos.size() - 1);
        for (int k = puntos.size() - 2; k >= 0; k--) {
            HistorialRiesgo anterior = puntos.get(k);
            if (anterior.getNivel() != null && !anterior.getNivel().equals(ultimo.getNivel())) {
                // el cambio ocurrió en el punto siguiente al último con otro nivel
                LocalDate fechaCambio = puntos.get(k + 1).getFecha();
                long dias = java.time.temporal.ChronoUnit.DAYS.between(fechaCambio, hoy);
                boolean sube = RiesgoCalculadora.NIVELES.indexOf(ultimo.getNivel()) > RiesgoCalculadora.NIVELES.indexOf(anterior.getNivel());
                cambio = (sube ? "Subió" : "Bajó") + " de " + anterior.getNivel() + " a " + ultimo.getNivel() + " "
                        + (dias <= 0 ? "hoy" : dias == 1 ? "ayer" : "hace " + dias + " días");
                break;
            }
        }
        if (cambio == null && puntos.size() > 1) {
            long dias = java.time.temporal.ChronoUnit.DAYS.between(puntos.get(0).getFecha(), hoy);
            cambio = "Se mantiene en " + ultimo.getNivel() + " desde hace " + dias + " días";
        }
        StringBuilder poli = new StringBuilder();
        int n = puntos.size();
        for (int k = 0; k < n; k++) {
            double x = n == 1 ? 80 : 4 + k * (152.0 / (n - 1));
            double y = 36 - Math.max(0, Math.min(1, puntos.get(k).getPuntaje())) * 32;
            poli.append(String.format(Locale.ROOT, "%.1f,%.1f ", x, y));
        }
        return new ResumenHistorial(puntos, cambio, poli.toString().trim());
    }

    private static boolean pasoAAlto(String antes, String ahora) {
        return "Alto".equals(ahora) && ("Bajo".equals(antes) || "Medio".equals(antes));
    }

    private void avisarRiesgoAlto(Proyecto p, Evaluacion e) {
        String detalle = e.factor() != null ? "Lo que más influye: " + e.factor() + ". " : "";
        notificaciones.notificar(p.getIdLider(), "RIESGO", "Riesgo alto: " + p.getNombre(),
                detalle + (e.sugerencia() != null ? e.sugerencia() : ""), "/lider/proyecto/" + p.getId() + "/detalle");
    }

    /** Guarda el punto de hoy y, si el proyecto no tenía historial, reconstruye las semanas anteriores. */
    private void guardarHistorial(List<HistorialRiesgo> hoy, List<Proyecto> sinHistorial, Datos d) {
        if (!hoy.isEmpty()) {
            BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, HistorialRiesgo.class);
            for (HistorialRiesgo h : hoy) {
                bulk.upsert(Query.query(Criteria.where("idProyecto").is(h.getIdProyecto()).and("fecha").is(h.getFecha())),
                        new Update().set("nivel", h.getNivel()).set("puntaje", h.getPuntaje()));
            }
            bulk.execute();
        }
        List<HistorialRiesgo> pasado = new ArrayList<>();
        LocalDate fecha = LocalDate.now();
        for (Proyecto p : sinHistorial) {
            for (int semana = 1; semana <= SEMANAS_RECONSTRUIR; semana++) {
                LocalDate corte = fecha.minusWeeks(semana);
                if (corte.isBefore(p.getFechaInicio())) {
                    break;
                }
                try {
                    Evaluacion e = predecir(indicadores(p, d, corte));
                    if (e.nivel() != null) {
                        pasado.add(new HistorialRiesgo(p.getId(), corte, e.nivel(), redondear(e.puntaje())));
                    }
                } catch (RuntimeException ex) {
                    break;
                }
            }
        }
        for (int desde = 0; desde < pasado.size(); desde += LOTE) {
            mongoTemplate.insert(pasado.subList(desde, Math.min(desde + LOTE, pasado.size())), HistorialRiesgo.class);
        }
    }

    private static double redondear(double v) {
        return Math.round(v * 1000) / 1000.0;
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
            LocalDate hoy = LocalDate.now();
            List<HistorialRiesgo> puntosHoy = new ArrayList<>();
            java.util.Set<String> conHistorial = new java.util.HashSet<>(
                    mongoTemplate.findDistinct(new Query(), "idProyecto", HistorialRiesgo.class, String.class));
            List<Proyecto> sinHistorial = new ArrayList<>();
            List<Proyecto> nuevosAltos = new ArrayList<>();
            for (Proyecto p : activos) {
                if (p.getId() == null || p.getFechaInicio() == null || p.getFechaFin() == null) {
                    sinFechas++;
                    continue;
                }
                Update u;
                try {
                    Evaluacion e = evaluar(p, d);
                    u = actualizacion(e);
                    if (e.nivel() != null) {
                        puntosHoy.add(new HistorialRiesgo(p.getId(), hoy, e.nivel(), redondear(e.puntaje())));
                    }
                    if (!conHistorial.contains(p.getId())) {
                        sinHistorial.add(p);
                    }
                    if (pasoAAlto(p.getNivelRiesgo(), e.nivel())) {
                        avisarRiesgoAlto(p, e);
                        nuevosAltos.add(p);
                    }
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
            try {
                guardarHistorial(puntosHoy, sinHistorial, d);
            } catch (RuntimeException e) {
                log.warn("No se pudo guardar el historial de riesgo: {}", e.toString());
            }
            if (!nuevosAltos.isEmpty()) {
                notificaciones.notificarAdministradores("RIESGO",
                        nuevosAltos.size() == 1 ? "Un proyecto pasó a riesgo alto" : nuevosAltos.size() + " proyectos pasaron a riesgo alto",
                        nuevosAltos.size() == 1 ? nuevosAltos.get(0).getNombre()
                                : "Revisa la lista ordenada por riesgo para priorizar el seguimiento.",
                        "/admin/proyectos?estado=ACTIVO&riesgo=Alto&orden=riesgo");
            }
            mongoTemplate.updateMulti(Query.query(Criteria.where("estado").ne("ACTIVO").and("nivelRiesgo").exists(true)),
                    new Update().unset("nivelRiesgo").unset("probabilidadRiesgo").unset("puntajeRiesgo")
                            .unset("riesgoMotivos").unset("riesgoSugerencia").unset("riesgoActualizado")
                            .unset("riesgoFactor").unset("fechaEstimadaFin"), Proyecto.class);
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
            Evaluacion e = evaluar(p);
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(p.getId())), actualizacion(e), Proyecto.class);
            if (e.nivel() != null) {
                mongoTemplate.upsert(Query.query(Criteria.where("idProyecto").is(p.getId()).and("fecha").is(LocalDate.now())),
                        new Update().set("nivel", e.nivel()).set("puntaje", redondear(e.puntaje())), HistorialRiesgo.class);
            }
            if (pasoAAlto(p.getNivelRiesgo(), e.nivel())) {
                avisarRiesgoAlto(p, e);
                notificaciones.notificarAdministradores("RIESGO", "Un proyecto pasó a riesgo alto", p.getNombre(),
                        "/admin/proyecto/" + p.getId());
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo recalcular el riesgo del proyecto {}: {}", idProyecto, e.getMessage());
        }
    }

    private static Update actualizacion(Evaluacion e) {
        Update u = new Update()
                .set("riesgoMotivos", e.motivos())
                .set("riesgoSugerencia", e.sugerencia())
                .set("riesgoActualizado", LocalDateTime.now())
                .set("modeloVersion", ModeloInfo.VERSION);
        if (e.factor() != null) {
            u.set("riesgoFactor", e.factor());
        } else {
            u.unset("riesgoFactor");
        }
        if (e.fechaEstimadaFin() != null) {
            u.set("fechaEstimadaFin", e.fechaEstimadaFin());
        } else {
            u.unset("fechaEstimadaFin");
        }
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
