package com.proaula.spring.synergy.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.proaula.spring.synergy.Model.Catalogos;
import com.proaula.spring.synergy.Model.Proyecto;

/**
 * Jerarquía de proyectos como ÁRBOL construido con los datos reales de MongoDB:
 *
 *   Synergy (raíz)
 *    └── Categoría            (n proyectos)
 *         └── Barrio          (n proyectos)      ← solo de la categoría abierta
 *              └── Proyecto   (código, estado, tareas)
 *
 * Antes el árbol se llenaba a mano con IDs numéricos inventados, vivía solo en memoria
 * (se perdía al reiniciar) y no tenía relación con los proyectos de la base de datos.
 * Se conserva la estructura de datos (nodos padre/hijo, recorrido en preorden, ruta
 * desde la raíz y conteo recursivo) que pedía el curso.
 */
@Service
public class JerarquiaProyectoService {

    /** Nodo del árbol. */
    public static class NodoProyecto {
        private final String id;
        private final String nombre;
        private final String tipo; // ORGANIZACION, CATEGORIA, BARRIO, PROYECTO
        private final String detalle;
        private final String url;
        private final int tareas;
        private NodoProyecto padre;
        private final List<NodoProyecto> hijos = new ArrayList<>();
        private int nivel;

        public NodoProyecto(String id, String nombre, String tipo, String detalle, String url, int tareas) {
            this.id = id;
            this.nombre = nombre;
            this.tipo = tipo;
            this.detalle = detalle;
            this.url = url;
            this.tareas = tareas;
        }

        public void agregarHijo(NodoProyecto hijo) {
            hijo.padre = this;
            hijo.nivel = this.nivel + 1;
            hijos.add(hijo);
        }

        public String getId() { return id; }
        public String getNombre() { return nombre; }
        public String getTipo() { return tipo; }
        public String getDetalle() { return detalle; }
        public String getUrl() { return url; }
        public NodoProyecto getPadre() { return padre; }
        public List<NodoProyecto> getHijos() { return hijos; }
        public int getNivel() { return nivel; }
        public boolean isAbierto() { return !hijos.isEmpty(); }
    }

    private final MongoTemplate mongoTemplate;

    public JerarquiaProyectoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Construye el árbol. Si se indica una categoría, se despliegan sus barrios y proyectos.
     */
    public NodoProyecto construir(String categoriaAbierta) {
        long total = mongoTemplate.count(new Query(), Proyecto.class);
        NodoProyecto raiz = new NodoProyecto("raiz", "Synergy", "ORGANIZACION",
                total + " proyectos", "/jerarquia", 0);

        Map<String, Long> porCategoria = contarPor("categoria", null);
        for (String categoria : Catalogos.CATEGORIAS) {
            long n = porCategoria.getOrDefault(categoria, 0L);
            boolean abierta = categoria.equals(categoriaAbierta);
            String url = abierta ? "/jerarquia"
                    : UriComponentsBuilder.fromPath("/jerarquia").queryParam("categoria", categoria).encode().build().toUriString();
            NodoProyecto nodoCategoria = new NodoProyecto("cat:" + categoria, categoria, "CATEGORIA", n + " proyectos", url, 0);
            raiz.agregarHijo(nodoCategoria);
            if (abierta) {
                desplegarCategoria(nodoCategoria, categoria);
            }
        }
        long sinCategoria = porCategoria.getOrDefault(null, 0L);
        if (sinCategoria > 0) {
            raiz.agregarHijo(new NodoProyecto("cat:none", "Sin categoría", "CATEGORIA", sinCategoria + " proyectos", null, 0));
        }
        return raiz;
    }

    private void desplegarCategoria(NodoProyecto nodoCategoria, String categoria) {
        Query q = Query.query(Criteria.where("categoria").is(categoria)).with(Sort.by("ubicacion", "nombre"));
        q.fields().include("nombre", "codigo", "estado", "ubicacion", "tareas");
        Map<String, List<Proyecto>> porBarrio = new TreeMap<>();
        for (Proyecto p : mongoTemplate.find(q, Proyecto.class)) {
            String barrio = p.getUbicacion() == null ? "Sin barrio" : p.getUbicacion();
            porBarrio.computeIfAbsent(barrio, k -> new ArrayList<>()).add(p);
        }
        porBarrio.forEach((barrio, proyectos) -> {
            NodoProyecto nodoBarrio = new NodoProyecto("bar:" + barrio, barrio, "BARRIO", proyectos.size() + " proyectos", null, 0);
            nodoCategoria.agregarHijo(nodoBarrio);
            for (Proyecto p : proyectos) {
                int tareas = p.getTareas() == null ? 0 : p.getTareas().size();
                nodoBarrio.agregarHijo(new NodoProyecto(p.getId(), p.getNombre(), "PROYECTO",
                        (p.getCodigo() == null ? "" : p.getCodigo() + " · ") + p.getEstadoEtiqueta(),
                        "/admin/proyecto/" + p.getId(), tareas));
            }
        });
    }

    /** Conteo agrupado por un campo (con filtro opcional). */
    private Map<String, Long> contarPor(String campo, Criteria filtro) {
        List<org.springframework.data.mongodb.core.aggregation.AggregationOperation> ops = new ArrayList<>();
        if (filtro != null) {
            ops.add(Aggregation.match(filtro));
        }
        ops.add(Aggregation.group(campo).count().as("total"));
        Map<String, Long> conteo = new LinkedHashMap<>();
        for (Document d : mongoTemplate.aggregate(Aggregation.newAggregation(ops), "proyectos", Document.class)) {
            conteo.put(d.getString("_id"), ((Number) d.get("total")).longValue());
        }
        return conteo;
    }

    // =====================================================================
    // OPERACIONES DE ÁRBOL
    // =====================================================================

    /** Recorrido en preorden: lista plana (nivel + nodo) para pintar el árbol. */
    public List<NodoProyecto> preorden(NodoProyecto raiz) {
        List<NodoProyecto> resultado = new ArrayList<>();
        recorrer(raiz, resultado);
        return resultado;
    }

    private void recorrer(NodoProyecto nodo, List<NodoProyecto> resultado) {
        resultado.add(nodo);
        for (NodoProyecto hijo : nodo.getHijos()) {
            recorrer(hijo, resultado);
        }
    }

    /** Búsqueda en profundidad (DFS) de un nodo por id. */
    public NodoProyecto buscar(NodoProyecto nodo, String id) {
        if (nodo.getId().equals(id)) {
            return nodo;
        }
        for (NodoProyecto hijo : nodo.getHijos()) {
            NodoProyecto encontrado = buscar(hijo, id);
            if (encontrado != null) {
                return encontrado;
            }
        }
        return null;
    }

    /** Ruta desde la raíz hasta el nodo (migas de pan). */
    public List<String> ruta(NodoProyecto nodo) {
        List<String> ruta = new ArrayList<>();
        while (nodo != null) {
            ruta.add(0, nodo.getNombre());
            nodo = nodo.getPadre();
        }
        return ruta;
    }

    /** Suma recursiva de las tareas de un subárbol. */
    public int contarTareas(NodoProyecto nodo) {
        int total = nodo.tareas;
        for (NodoProyecto hijo : nodo.getHijos()) {
            total += contarTareas(hijo);
        }
        return total;
    }
}
