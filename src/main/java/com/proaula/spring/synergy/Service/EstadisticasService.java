package com.proaula.spring.synergy.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.Catalogos;
import com.proaula.spring.synergy.Model.EntregaTarea;
import com.proaula.spring.synergy.Model.Proyecto;

/**
 * Estadísticas del panel del administrador con agregaciones de MongoDB ($group): una consulta
 * por gráfica en lugar de una por cada barra (antes eran 16 consultas). Los resultados se
 * guardan 2 minutos en memoria para que recargar el panel no repita todo el trabajo.
 */
@Service
public class EstadisticasService {

    private static final Duration VIGENCIA = Duration.ofMinutes(2);

    private final MongoTemplate mongoTemplate;
    private final Map<String, Object[]> cache = new ConcurrentHashMap<>(); // clave -> [instante, valor]

    public EstadisticasService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /** Borra la caché (por ejemplo, después de recalcular el riesgo o aprobar proyectos). */
    public void invalidar() {
        cache.clear();
    }

    @SuppressWarnings("unchecked")
    private <T> T enCache(String clave, Supplier<T> calcular) {
        Object[] guardado = cache.get(clave);
        long ahora = System.currentTimeMillis();
        if (guardado != null && ahora - (long) guardado[0] < VIGENCIA.toMillis()) {
            return (T) guardado[1];
        }
        T valor = calcular.get();
        cache.put(clave, new Object[] {ahora, valor});
        return valor;
    }

    /** Filtro de fechas de creación (null = sin límite). */
    private static Criteria porFechas(LocalDate desde, LocalDate hasta) {
        Criteria c = new Criteria();
        if (desde != null && hasta != null) {
            c = Criteria.where("fechaCreacion").gte(desde.atStartOfDay()).lt(hasta.plusDays(1).atStartOfDay());
        } else if (desde != null) {
            c = Criteria.where("fechaCreacion").gte(desde.atStartOfDay());
        } else if (hasta != null) {
            c = Criteria.where("fechaCreacion").lt(hasta.plusDays(1).atStartOfDay());
        }
        return c;
    }

    /** {valor del campo -> cantidad} con un solo $group. */
    private Map<String, Long> agrupar(String coleccion, Criteria filtro, String campo) {
        Aggregation ag = Aggregation.newAggregation(
                Aggregation.match(filtro),
                Aggregation.group(campo).count().as("n"));
        AggregationResults<Document> r = mongoTemplate.aggregate(ag, coleccion, Document.class);
        Map<String, Long> m = new LinkedHashMap<>();
        for (Document d : r.getMappedResults()) {
            Object id = d.get("_id");
            m.put(id == null ? null : id.toString(), ((Number) d.get("n")).longValue());
        }
        return m;
    }

    /** Conteo en el orden de la lista de claves (las que no aparecen quedan en 0). */
    private static Map<String, Long> ordenar(Map<String, Long> crudo, List<String> claves) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (String k : claves) {
            m.put(k, crudo.getOrDefault(k, 0L));
        }
        return m;
    }

    public Map<String, Long> proyectosPorEstado(LocalDate desde, LocalDate hasta) {
        return enCache("estado" + desde + hasta, () -> ordenar(
                agrupar("proyectos", porFechas(desde, hasta), "estado"),
                new ArrayList<>(Catalogos.ESTADOS_PROYECTO.keySet())));
    }

    public Map<String, Long> proyectosPorCategoria(LocalDate desde, LocalDate hasta) {
        return enCache("categoria" + desde + hasta, () -> ordenar(
                agrupar("proyectos", porFechas(desde, hasta), "categoria"), Catalogos.CATEGORIAS));
    }

    /** Activos por nivel de riesgo: Alto, Medio, Bajo y "Sin datos" (sin nivel calculado). */
    public Map<String, Long> activosPorRiesgo() {
        return enCache("riesgo", () -> {
            Map<String, Long> crudo = agrupar("proyectos", Criteria.where("estado").is("ACTIVO"), "nivelRiesgo");
            Map<String, Long> m = ordenar(crudo, List.of("Alto", "Medio", "Bajo"));
            m.put("Sin datos", crudo.getOrDefault(null, 0L));
            return m;
        });
    }

    /** Proyectos activos por barrio (para el mapa), solo barrios con al menos uno. */
    public Map<String, Long> activosPorBarrio() {
        return enCache("barrio", () -> {
            Map<String, Long> crudo = agrupar("proyectos", Criteria.where("estado").is("ACTIVO"), "ubicacion");
            Map<String, Long> m = new LinkedHashMap<>();
            for (String b : Catalogos.BARRIOS) {
                if (crudo.getOrDefault(b, 0L) > 0) {
                    m.put(b, crudo.get(b));
                }
            }
            return m;
        });
    }

    /** Fila de la tendencia mensual. */
    public record Mes(String clave, String etiqueta, long creados, long completados, long cancelados) {
    }

    /**
     * Últimos 12 meses: proyectos creados (por fecha de creación), completados (por fecha de fin)
     * y cancelados (por última actualización).
     */
    public List<Mes> tendenciaMensual() {
        return enCache("tendencia", () -> {
            YearMonth actual = YearMonth.now();
            YearMonth primero = actual.minusMonths(11);
            LocalDateTime desde = primero.atDay(1).atStartOfDay();
            Map<String, Long> creados = porMes(Criteria.where("fechaCreacion").gte(desde), "fechaCreacion");
            Map<String, Long> completados = porMes(Criteria.where("estado").is("COMPLETADO").and("fechaFin").gte(primero.atDay(1)), "fechaFin");
            Map<String, Long> cancelados = porMes(Criteria.where("estado").is("CANCELADO").and("fechaActualizacion").gte(desde), "fechaActualizacion");
            List<Mes> meses = new ArrayList<>();
            String[] nombres = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"};
            for (YearMonth m = primero; !m.isAfter(actual); m = m.plusMonths(1)) {
                String k = m.toString();
                meses.add(new Mes(k, nombres[m.getMonthValue() - 1] + " " + String.valueOf(m.getYear()).substring(2),
                        creados.getOrDefault(k, 0L), completados.getOrDefault(k, 0L), cancelados.getOrDefault(k, 0L)));
            }
            return meses;
        });
    }

    private Map<String, Long> porMes(Criteria filtro, String campoFecha) {
        Aggregation ag = Aggregation.newAggregation(
                Aggregation.match(filtro),
                Aggregation.project().and(DateOperators.dateOf(campoFecha).toString("%Y-%m")).as("mes"),
                Aggregation.group("mes").count().as("n"));
        Map<String, Long> m = new LinkedHashMap<>();
        for (Document d : mongoTemplate.aggregate(ag, "proyectos", Document.class).getMappedResults()) {
            if (d.get("_id") != null) {
                m.put(d.get("_id").toString(), ((Number) d.get("n")).longValue());
            }
        }
        return m;
    }

    /** Propuestas esperando aprobación hace más de {@code dias} días (las más antiguas primero). */
    public List<Proyecto> pendientesAntiguos(int dias, int cantidad) {
        Query q = Query.query(Criteria.where("estado").is("PENDIENTE")
                .and("fechaCreacion").lt(LocalDateTime.now().minusDays(dias)))
                .with(Sort.by("fechaCreacion")).limit(cantidad);
        q.fields().include("nombre", "codigo", "fechaCreacion", "idLider", "estado");
        return mongoTemplate.find(q, Proyecto.class);
    }

    public long contarPendientesAntiguos(int dias) {
        return mongoTemplate.count(Query.query(Criteria.where("estado").is("PENDIENTE")
                .and("fechaCreacion").lt(LocalDateTime.now().minusDays(dias))), Proyecto.class);
    }

    /** Días promedio entre la propuesta y la aprobación (solo proyectos aprobados desde esta versión). */
    public Double diasPromedioAprobacion() {
        return enCache("aprobacion", () -> {
            Query q = Query.query(Criteria.where("fechaAprobacion").exists(true));
            q.fields().include("fechaCreacion", "fechaAprobacion");
            List<Proyecto> aprobados = mongoTemplate.find(q, Proyecto.class);
            double suma = 0;
            int n = 0;
            for (Proyecto p : aprobados) {
                if (p.getFechaCreacion() != null && p.getFechaAprobacion() != null) {
                    suma += Duration.between(p.getFechaCreacion(), p.getFechaAprobacion()).toHours() / 24.0;
                    n++;
                }
            }
            return n == 0 ? null : Math.round(suma / n * 10) / 10.0;
        });
    }

    /** Posición en un ranking: id del usuario y cantidad. */
    public record Puesto(String idUsuario, long cantidad) {
    }

    /** Voluntarios con más entregas. */
    public List<Puesto> topVoluntarios(int cantidad) {
        return enCache("topVoluntarios", () -> top("entregas_tareas", Criteria.where("idUsuario").ne(null), "idUsuario", cantidad));
    }

    /** Líderes con más proyectos completados. */
    public List<Puesto> topLideres(int cantidad) {
        return enCache("topLideres", () -> top("proyectos", Criteria.where("estado").is("COMPLETADO"), "idLider", cantidad));
    }

    private List<Puesto> top(String coleccion, Criteria filtro, String campo, int cantidad) {
        Aggregation ag = Aggregation.newAggregation(
                Aggregation.match(filtro),
                Aggregation.group(campo).count().as("n"),
                Aggregation.sort(Sort.Direction.DESC, "n"),
                Aggregation.limit(cantidad));
        List<Puesto> lista = new ArrayList<>();
        for (Document d : mongoTemplate.aggregate(ag, coleccion, Document.class).getMappedResults()) {
            if (d.get("_id") != null) {
                lista.add(new Puesto(d.get("_id").toString(), ((Number) d.get("n")).longValue()));
            }
        }
        return lista;
    }

    /** Usado en las pruebas para comprobar que la colección de entregas existe con ese nombre. */
    static String coleccionEntregas() {
        return EntregaTarea.class.getAnnotation(org.springframework.data.mongodb.core.mapping.Document.class).collection();
    }
}
