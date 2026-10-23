package com.proaula.spring.synergy.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Dto.FiltroProyectos;
import com.proaula.spring.synergy.Model.Catalogos;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;

/**
 * Búsquedas paginadas directamente en MongoDB (no se cargan todos los
 * documentos en memoria), pensadas para miles de proyectos y usuarios.
 */
@Service
public class BusquedaService {

    private final MongoTemplate mongoTemplate;

    public BusquedaService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    // =====================================================================
    // PROYECTOS
    // =====================================================================

    /**
     * @param estadosPermitidos estados que este usuario puede ver (p. ej. un invitado
     *                          no ve PENDIENTE ni CANCELADO). null = todos.
     */
    public Page<Proyecto> buscarProyectos(FiltroProyectos f, Collection<String> estadosPermitidos, int tamanio) {
        List<Criteria> condiciones = new ArrayList<>();

        String estado = normalizar(f.getEstado());
        if (estado != null) {
            if (estadosPermitidos != null && !estadosPermitidos.contains(estado)) {
                // Estado no permitido para este usuario: no devuelve nada
                condiciones.add(Criteria.where("estado").is("__ninguno__"));
            } else {
                condiciones.add(Criteria.where("estado").is(estado));
            }
        } else if (estadosPermitidos != null) {
            condiciones.add(Criteria.where("estado").in(estadosPermitidos));
        }

        if (tiene(f.getCategoria())) {
            condiciones.add(Criteria.where("categoria").is(f.getCategoria()));
        }
        if (tiene(f.getOrganizacion())) {
            condiciones.add(Criteria.where("tipoOrganizacion").is(f.getOrganizacion()));
        }
        if (tiene(f.getUbicacion())) {
            condiciones.add(Criteria.where("ubicacion").is(f.getUbicacion()));
        }
        if (tiene(f.getRiesgo()) && RiesgoCalculadora.NIVELES.contains(f.getRiesgo())) {
            condiciones.add(Criteria.where("nivelRiesgo").is(f.getRiesgo()));
        }
        if (tiene(f.getQ())) {
            Pattern p = patron(f.getQ());
            condiciones.add(new Criteria().orOperator(
                Criteria.where("nombre").regex(p),
                Criteria.where("descripcion").regex(p),
                Criteria.where("codigo").regex(p),
                Criteria.where("ubicacion").regex(p),
                Criteria.where("categoria").regex(p)));
        }

        Query query = new Query();
        if (!condiciones.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(condiciones.toArray(new Criteria[0])));
        }
        return paginar(query, ordenProyectos(f.getOrden()), f.getPage(), tamanio, Proyecto.class);
    }

    /** Proyectos de un líder concreto, con los mismos filtros. */
    public Page<Proyecto> buscarProyectosDeLider(String idLider, FiltroProyectos f, int tamanio) {
        List<Criteria> condiciones = new ArrayList<>();
        condiciones.add(Criteria.where("idLider").is(idLider));
        String estado = normalizar(f.getEstado());
        if (estado != null) {
            condiciones.add(Criteria.where("estado").is(estado));
        }
        if (tiene(f.getQ())) {
            Pattern p = patron(f.getQ());
            condiciones.add(new Criteria().orOperator(
                Criteria.where("nombre").regex(p),
                Criteria.where("codigo").regex(p)));
        }
        Query query = new Query(new Criteria().andOperator(condiciones.toArray(new Criteria[0])));
        return paginar(query, ordenProyectos(f.getOrden()), f.getPage(), tamanio, Proyecto.class);
    }

    public long contarProyectos(String estado) {
        Query q = estado == null ? new Query() : Query.query(Criteria.where("estado").is(estado));
        return mongoTemplate.count(q, Proyecto.class);
    }

    /** Conteo de proyectos por estado, en el orden de Catalogos.ESTADOS_PROYECTO. */
    public Map<String, Long> proyectosPorEstado() {
        Map<String, Long> conteo = new java.util.LinkedHashMap<>();
        for (String estado : Catalogos.ESTADOS_PROYECTO.keySet()) {
            conteo.put(estado, contarProyectos(estado));
        }
        return conteo;
    }

    /** Proyectos activos por nivel de riesgo (Alto, Medio, Bajo y "Sin datos"). */
    public Map<String, Long> activosPorRiesgo() {
        Map<String, Long> conteo = new java.util.LinkedHashMap<>();
        for (String nivel : List.of("Alto", "Medio", "Bajo")) {
            conteo.put(nivel, mongoTemplate.count(Query.query(Criteria.where("estado").is("ACTIVO").and("nivelRiesgo").is(nivel)), Proyecto.class));
        }
        conteo.put("Sin datos", mongoTemplate.count(Query.query(Criteria.where("estado").is("ACTIVO").and("nivelRiesgo").exists(false)), Proyecto.class));
        return conteo;
    }

    /** Número de proyectos por categoría (todas las categorías del catálogo, en orden). */
    public Map<String, Long> proyectosPorCategoria() {
        Map<String, Long> conteo = new java.util.LinkedHashMap<>();
        for (String c : Catalogos.CATEGORIAS) {
            conteo.put(c, mongoTemplate.count(Query.query(Criteria.where("categoria").is(c)), Proyecto.class));
        }
        return conteo;
    }

    private static Sort ordenProyectos(String orden) {
        if (orden == null) {
            orden = "recientes";
        }
        return switch (orden) {
            // Prioridad del modelo predictivo: primero los de mayor riesgo
            case "riesgo" -> Sort.by(Sort.Order.desc("puntajeRiesgo"), Sort.Order.asc("fechaFin"));
            case "nombre" -> Sort.by(Sort.Direction.ASC, "nombre");
            case "inicio" -> Sort.by(Sort.Direction.ASC, "fechaInicio");
            case "fin" -> Sort.by(Sort.Direction.ASC, "fechaFin");
            default -> Sort.by(Sort.Direction.DESC, "fechaCreacion").and(Sort.by(Sort.Direction.DESC, "_id"));
        };
    }

    // =====================================================================
    // USUARIOS
    // =====================================================================

    public Page<Usuarios> buscarUsuarios(String q, String rol, String estado, int pagina, int tamanio) {
        List<Criteria> condiciones = new ArrayList<>();
        if (tiene(rol)) {
            condiciones.add(Criteria.where("rol").is(rol.trim().toUpperCase()));
        }
        if (tiene(estado)) {
            condiciones.add(Criteria.where("estado").is(estado.trim().toUpperCase()));
        }
        if (tiene(q)) {
            Pattern p = patron(q);
            condiciones.add(new Criteria().orOperator(
                Criteria.where("nombre").regex(p),
                Criteria.where("correo").regex(p)));
        }
        Query query = new Query();
        if (!condiciones.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(condiciones.toArray(new Criteria[0])));
        }
        return paginar(query, Sort.by(Sort.Direction.ASC, "nombre"), pagina, tamanio, Usuarios.class);
    }

    public long contarUsuarios(String rol) {
        Query q = rol == null ? new Query() : Query.query(Criteria.where("rol").is(rol));
        return mongoTemplate.count(q, Usuarios.class);
    }

    /** Nombres de usuario por id, solo para los ids pedidos (evita cargar a todos). */
    public Map<String, String> nombresDe(Collection<String> ids) {
        Map<String, String> nombres = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return nombres;
        }
        Query q = Query.query(Criteria.where("_id").in(ids));
        q.fields().include("nombre");
        for (Usuarios u : mongoTemplate.find(q, Usuarios.class)) {
            nombres.put(u.getId(), u.getNombre());
        }
        return nombres;
    }

    // =====================================================================
    // UTILIDADES
    // =====================================================================

    private <T> Page<T> paginar(Query query, Sort orden, int pagina, int tamanio, Class<T> tipo) {
        long total = mongoTemplate.count(query, tipo);
        int totalPaginas = (int) Math.max(1, (total + tamanio - 1) / tamanio);
        int paginaReal = Math.min(Math.max(1, pagina), totalPaginas); // si piden la 999 se muestra la última
        Pageable pageable = PageRequest.of(paginaReal - 1, tamanio, orden);
        List<T> contenido = mongoTemplate.find(Query.of(query).with(pageable), tipo);
        return new PageImpl<>(contenido, pageable, total);
    }

    /**
     * Búsqueda "contiene" que ignora mayúsculas y tildes: "reforestacion" encuentra
     * "Reforestación". Los caracteres especiales se escapan (no se inyecta regex).
     */
    static Pattern patron(String texto) {
        String t = texto.trim();
        if (t.length() > 60) {
            t = t.substring(0, 60);
        }
        String limpio = java.text.Normalizer.normalize(t.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        StringBuilder regex = new StringBuilder();
        for (char c : limpio.toCharArray()) {
            switch (c) {
                case 'a' -> regex.append("[aáàä]");
                case 'e' -> regex.append("[eéèë]");
                case 'i' -> regex.append("[iíìï]");
                case 'o' -> regex.append("[oóòö]");
                case 'u' -> regex.append("[uúùü]");
                case 'n' -> regex.append("[nñ]");
                default -> {
                    if (!Character.isLetterOrDigit(c) && c != ' ') {
                        regex.append('\\'); // escapa . * + ? ( ) [ ] etc. (válido en Java y en MongoDB)
                    }
                    regex.append(c);
                }
            }
        }
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
    }

    /** "TODOS" o vacío = sin filtro de estado. */
    private static String normalizar(String estado) {
        if (!tiene(estado) || "TODOS".equalsIgnoreCase(estado.trim())) {
            return null;
        }
        return estado.trim().toUpperCase();
    }

    private static boolean tiene(String v) {
        return v != null && !v.isBlank();
    }
}
