package com.proaula.spring.synergy.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.Comentario;
import com.proaula.spring.synergy.Model.EntregaTarea;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;

/**
 * Datos del panel del voluntario: próximas tareas, participación, insignias, historial de
 * entregas con la retroalimentación del líder y proyectos recomendados.
 */
@Service
public class PanelVoluntarioService {

    private final MongoTemplate mongoTemplate;

    public PanelVoluntarioService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /** Insignia del panel (obtenida o por obtener, con lo que falta). */
    public record Insignia(String nombre, String descripcion, String icono, boolean obtenida, String progreso) {
    }

    /** Entrega con el título de su tarea, para el historial. */
    public record EntregaVista(EntregaTarea entrega, String tarea, String proyecto, boolean aTiempo) {
    }

    /** Todo lo que necesita la vista. */
    public record Panel(List<Tarea> proximas, List<Tarea> vencidas, Map<String, String> nombresProyecto,
                        int totalTareas, int tareasCompletadas, int entregas, int entregasAprobadas, int entregasATiempo,
                        int comentarios, int proyectos, int proyectosActivosConAporte, int participacion,
                        List<Insignia> insignias, List<EntregaVista> historial, List<Proyecto> completados,
                        List<Proyecto> recomendados, List<Tarea> todasAbiertas) {
        public long getInsigniasObtenidas() {
            return insignias.stream().filter(Insignia::obtenida).count();
        }
        public int getPuntualidad() {
            return entregas == 0 ? 0 : Math.round(entregasATiempo * 100f / entregas);
        }
    }

    public Panel preparar(Usuarios usuario) {
        String uid = usuario.getId();
        LocalDate hoy = LocalDate.now();

        // Tareas asignadas (sin descripción larga ni listas)
        Query qt = Query.query(Criteria.where("usuariosAsignados").is(uid));
        qt.fields().include("titulo", "estado", "fechaEntrega", "idProyecto");
        List<Tarea> tareas = mongoTemplate.find(qt, Tarea.class);
        List<Tarea> abiertas = tareas.stream()
                .filter(t -> !"COMPLETADA".equalsIgnoreCase(t.getEstado()) && !"CANCELADA".equalsIgnoreCase(t.getEstado()))
                .sorted(Comparator.comparing(Tarea::getFechaEntrega, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<Tarea> vencidas = abiertas.stream().filter(t -> t.getFechaEntrega() != null && t.getFechaEntrega().isBefore(hoy)).toList();
        List<Tarea> proximas = abiertas.stream().filter(t -> t.getFechaEntrega() == null || !t.getFechaEntrega().isBefore(hoy))
                .limit(6).toList();
        int completadas = (int) tareas.stream().filter(t -> "COMPLETADA".equalsIgnoreCase(t.getEstado())).count();

        // Entregas propias SIN el archivo
        Query qe = Query.query(Criteria.where("idUsuario").is(uid)).with(Sort.by(Sort.Direction.DESC, "fechaEntrega"));
        qe.fields().exclude("datos");
        List<EntregaTarea> entregas = mongoTemplate.find(qe, EntregaTarea.class);
        Map<String, Tarea> tareaPorId = new HashMap<>();
        Set<String> idsTareasEntregas = new HashSet<>();
        entregas.forEach(e -> idsTareasEntregas.add(e.getIdTarea()));
        if (!idsTareasEntregas.isEmpty()) {
            Query q = Query.query(Criteria.where("_id").in(idsTareasEntregas));
            q.fields().include("titulo", "fechaEntrega", "idProyecto");
            mongoTemplate.find(q, Tarea.class).forEach(t -> tareaPorId.put(t.getId(), t));
        }
        int aTiempo = 0;
        int aprobadas = 0;
        for (EntregaTarea e : entregas) {
            Tarea t = tareaPorId.get(e.getIdTarea());
            if (puntual(e, t)) {
                aTiempo++;
            }
            if ("REVISADO".equals(e.getEstado())) {
                aprobadas++;
            }
        }

        long comentarios = mongoTemplate.count(Query.query(Criteria.where("idUsuario").is(uid)), Comentario.class);

        // Proyectos donde participa
        Query qp = Query.query(Criteria.where("participantes").is(uid));
        qp.fields().include("nombre", "codigo", "estado", "categoria", "ubicacion", "fechaInicio", "fechaFin", "idLider");
        List<Proyecto> proyectos = mongoTemplate.find(qp, Proyecto.class);
        Map<String, String> nombres = new HashMap<>();
        proyectos.forEach(p -> nombres.put(p.getId(), p.getNombre()));
        // Nombres de proyectos de tareas cuyos proyectos ya no lo tienen como participante
        Set<String> faltan = new HashSet<>();
        abiertas.forEach(t -> { if (t.getIdProyecto() != null && nombres.get(t.getIdProyecto()) == null) faltan.add(t.getIdProyecto()); });
        tareaPorId.values().forEach(t -> { if (t.getIdProyecto() != null && nombres.get(t.getIdProyecto()) == null) faltan.add(t.getIdProyecto()); });
        if (!faltan.isEmpty()) {
            Query q = Query.query(Criteria.where("_id").in(faltan));
            q.fields().include("nombre");
            mongoTemplate.find(q, Proyecto.class).forEach(p -> nombres.put(p.getId(), p.getNombre()));
        }

        // Participación: proyectos activos en los que ya aportó (entrega o comentario)
        Set<String> conAporte = new HashSet<>();
        tareaPorId.values().forEach(t -> conAporte.add(t.getIdProyecto()));
        Query qc = Query.query(Criteria.where("idUsuario").is(uid));
        qc.fields().include("idProyecto");
        mongoTemplate.find(qc, Comentario.class).forEach(c -> conAporte.add(c.getIdProyecto()));
        List<Proyecto> activos = proyectos.stream().filter(p -> "ACTIVO".equals(p.getEstado())).toList();
        int activosConAporte = (int) activos.stream().filter(p -> conAporte.contains(p.getId())).count();
        int participacion = activos.isEmpty() ? 0 : Math.round(activosConAporte * 100f / activos.size());

        List<Proyecto> completados = proyectos.stream().filter(p -> "COMPLETADO".equals(p.getEstado())).toList();

        List<EntregaVista> historial = new ArrayList<>();
        for (EntregaTarea e : entregas.subList(0, Math.min(6, entregas.size()))) {
            Tarea t = tareaPorId.get(e.getIdTarea());
            historial.add(new EntregaVista(e, t == null ? "Tarea eliminada" : t.getTitulo(),
                    t == null ? "" : nombres.getOrDefault(t.getIdProyecto(), ""), puntual(e, t)));
        }

        List<Insignia> insignias = insignias(entregas.size(), aTiempo, (int) comentarios, proyectos.size(), completados.size());

        return new Panel(proximas, vencidas, nombres, tareas.size(), completadas, entregas.size(), aprobadas, aTiempo,
                (int) comentarios, proyectos.size(), activosConAporte, participacion, insignias, historial, completados,
                recomendados(uid, proyectos, 4), abiertas);
    }

    /** La entrega llegó el día de la fecha límite o antes. */
    static boolean puntual(EntregaTarea e, Tarea t) {
        return t != null && t.getFechaEntrega() != null && e.getFechaEntrega() != null
                && !e.getFechaEntrega().toLocalDate().isAfter(t.getFechaEntrega());
    }

    /** Reglas de las insignias (código puro, se prueba sin base de datos). */
    public static List<Insignia> insignias(int entregas, int aTiempo, int comentarios, int proyectos, int completados) {
        List<Insignia> l = new ArrayList<>();
        l.add(new Insignia("Primera entrega", "Entregaste tu primera tarea", "fa-flag-checkered",
                entregas >= 1, entregas >= 1 ? null : "Entrega una tarea"));
        l.add(new Insignia("Constante", "10 entregas realizadas", "fa-fire",
                entregas >= 10, entregas >= 10 ? null : entregas + " de 10 entregas"));
        l.add(new Insignia("Puntual", "5 o más entregas, todas a tiempo", "fa-clock",
                entregas >= 5 && aTiempo == entregas, entregas >= 5 && aTiempo == entregas ? null
                        : entregas < 5 ? entregas + " de 5 entregas" : (entregas - aTiempo) + " entregas tarde"));
        l.add(new Insignia("En equipo", "Participas en 3 o más proyectos", "fa-people-group",
                proyectos >= 3, proyectos >= 3 ? null : proyectos + " de 3 proyectos"));
        l.add(new Insignia("Buena conversación", "5 comentarios en los proyectos", "fa-comments",
                comentarios >= 5, comentarios >= 5 ? null : comentarios + " de 5 comentarios"));
        l.add(new Insignia("Misión cumplida", "Fuiste parte de un proyecto completado", "fa-trophy",
                completados >= 1, completados >= 1 ? null : "Termina un proyecto con tu equipo"));
        return l;
    }

    /**
     * Proyectos activos recomendados: de las categorías y barrios donde ya participa (o los que
     * tienen más cupo de voluntarios si aún no participa en ninguno).
     */
    List<Proyecto> recomendados(String uid, List<Proyecto> suyos, int cantidad) {
        Map<String, Integer> pesoCategoria = new HashMap<>();
        Set<String> barrios = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (Proyecto p : suyos) {
            ids.add(p.getId());
            if (p.getCategoria() != null) {
                pesoCategoria.merge(p.getCategoria(), 1, Integer::sum);
            }
            if (p.getUbicacion() != null) {
                barrios.add(p.getUbicacion());
            }
        }
        Criteria c = Criteria.where("estado").is("ACTIVO").and("participantes").ne(uid).and("idLider").ne(uid);
        if (!pesoCategoria.isEmpty() || !barrios.isEmpty()) {
            c = c.orOperator(Criteria.where("categoria").in(pesoCategoria.keySet()), Criteria.where("ubicacion").in(barrios));
        }
        Query q = Query.query(c).with(Sort.by(Sort.Direction.DESC, "fechaCreacion")).limit(40);
        q.fields().include("nombre", "codigo", "categoria", "ubicacion", "fechaFin", "participantes", "estado", "descripcion");
        List<Proyecto> candidatos = new ArrayList<>(mongoTemplate.find(q, Proyecto.class));
        candidatos.removeIf(p -> ids.contains(p.getId()));
        candidatos.sort(Comparator.comparingInt((Proyecto p) ->
                pesoCategoria.getOrDefault(p.getCategoria(), 0) * 2 + (barrios.contains(p.getUbicacion()) ? 1 : 0)).reversed()
                .thenComparingInt(p -> p.getParticipantes() == null ? 0 : p.getParticipantes().size()));
        return candidatos.subList(0, Math.min(cantidad, candidatos.size()));
    }
}
