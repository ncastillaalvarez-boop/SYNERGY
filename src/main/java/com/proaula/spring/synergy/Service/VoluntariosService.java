package com.proaula.spring.synergy.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.Comentario;
import com.proaula.spring.synergy.Model.EntregaTarea;
import com.proaula.spring.synergy.Model.Participacion;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;

/**
 * Análisis de los voluntarios de un proyecto:
 *  - inactivos: inscritos hace más de una semana que nunca entregaron ni comentaron;
 *  - recomendados: voluntarios que ya participaron en proyectos de la misma categoría y que
 *    todavía no están en este (para que el líder los invite).
 */
@Service
public class VoluntariosService {

    /** Días de gracia antes de considerar inactivo a un voluntario recién inscrito. */
    public static final int DIAS_GRACIA = 7;

    private final MongoTemplate mongoTemplate;

    public VoluntariosService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /** Ids de los voluntarios (sin el líder) con al menos una entrega o un comentario en el proyecto. */
    public Set<String> activos(Proyecto p) {
        Set<String> activos = new HashSet<>();
        Query qt = Query.query(Criteria.where("idProyecto").is(p.getId()));
        qt.fields().include("_id");
        List<String> idsTareas = mongoTemplate.find(qt, Tarea.class).stream().map(Tarea::getId).toList();
        if (!idsTareas.isEmpty()) {
            Query qe = Query.query(Criteria.where("idTarea").in(idsTareas));
            qe.fields().include("idUsuario");
            mongoTemplate.find(qe, EntregaTarea.class).forEach(e -> activos.add(e.getIdUsuario()));
        }
        Query qc = Query.query(Criteria.where("idProyecto").is(p.getId()));
        qc.fields().include("idUsuario");
        mongoTemplate.find(qc, Comentario.class).forEach(c -> activos.add(c.getIdUsuario()));
        activos.remove(null);
        return activos;
    }

    public List<Usuarios> inactivos(Proyecto p) {
        Set<String> activos = activos(p);
        Map<String, LocalDateTime> fechaUnion = new HashMap<>();
        mongoTemplate.find(Query.query(Criteria.where("idProyecto").is(p.getId())), Participacion.class)
                .forEach(x -> fechaUnion.put(x.getIdUsuario(), x.getFechaUnion()));
        LocalDateTime limite = LocalDateTime.now().minusDays(DIAS_GRACIA);
        List<String> ids = new ArrayList<>();
        for (String uid : p.getParticipantes()) {
            if (uid == null || uid.equals(p.getIdLider()) || activos.contains(uid)) {
                continue;
            }
            LocalDateTime union = fechaUnion.get(uid);
            if (union == null || union.isBefore(limite)) {
                ids.add(uid);
            }
        }
        List<Usuarios> resultado = new ArrayList<>();
        mongoTemplate.find(Query.query(Criteria.where("_id").in(ids)), Usuarios.class).forEach(resultado::add);
        resultado.sort(Comparator.comparing(u -> u.getNombre() == null ? "" : u.getNombre()));
        return resultado;
    }

    /** Recomendación con su motivo (cuántos proyectos de la categoría hizo). */
    public record Recomendado(Usuarios usuario, int proyectosCategoria) {
    }

    public List<Recomendado> recomendados(Proyecto p, int cantidad) {
        if (p.getCategoria() == null) {
            return List.of();
        }
        Query q = Query.query(Criteria.where("categoria").is(p.getCategoria()).and("_id").ne(p.getId())
                .and("estado").in("ACTIVO", "COMPLETADO"));
        q.fields().include("participantes", "idLider");
        Map<String, Integer> veces = new LinkedHashMap<>();
        for (Proyecto otro : mongoTemplate.find(q, Proyecto.class)) {
            for (String uid : otro.getParticipantes()) {
                if (uid != null && !uid.equals(otro.getIdLider())) {
                    veces.merge(uid, 1, Integer::sum);
                }
            }
        }
        Set<String> yaEstan = new HashSet<>(p.getParticipantes());
        yaEstan.add(p.getIdLider());
        List<String> candidatos = veces.entrySet().stream()
                .filter(e -> !yaEstan.contains(e.getKey()))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(cantidad * 4L)
                .map(Map.Entry::getKey).toList();
        if (candidatos.isEmpty()) {
            return List.of();
        }
        Query qu = Query.query(Criteria.where("_id").in(candidatos).and("rol").is("PARTICIPANTE").and("activo").is(true));
        qu.fields().include("nombre", "correo", "rol", "activo", "fotoUrl");
        List<Recomendado> lista = new ArrayList<>();
        for (Usuarios u : mongoTemplate.find(qu, Usuarios.class)) {
            lista.add(new Recomendado(u, veces.getOrDefault(u.getId(), 0)));
        }
        lista.sort(Comparator.comparingInt(Recomendado::proyectosCategoria).reversed()
                .thenComparing(r -> r.usuario().getNombre() == null ? "" : r.usuario().getNombre()));
        return lista.size() > cantidad ? lista.subList(0, cantidad) : lista;
    }
}
