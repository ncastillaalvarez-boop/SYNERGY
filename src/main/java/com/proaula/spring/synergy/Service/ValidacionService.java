package com.proaula.spring.synergy.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.Catalogos;
import com.proaula.spring.synergy.Model.Proyecto;

/**
 * Validaciones de proyectos y tareas, iguales en la web y en la API de la app móvil.
 * Cada método devuelve el motivo del error para mostrarlo al usuario, o null si todo está bien.
 */
@Service
public class ValidacionService {

    public static final int NOMBRE_MIN = 5;
    public static final int NOMBRE_MAX = 120;
    public static final int DESCRIPCION_MAX = 2000;
    public static final int TITULO_MIN = 3;
    public static final int TITULO_MAX = 120;
    /** Un proyecto no puede durar más de 3 años. */
    public static final int DURACION_MAX_DIAS = 3 * 365;

    private final MongoTemplate mongoTemplate;

    public ValidacionService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * @param idActual id del proyecto que se está editando (null si es nuevo); se excluye al buscar duplicados
     */
    public String validarProyecto(Proyecto p, String idActual) {
        String error = validarCampos(p);
        if (error != null) {
            return error;
        }
        Proyecto igual = buscarDuplicado(p.getNombre(), idActual);
        if (igual != null) {
            return "Ya existe un proyecto " + (igual.getEstado() == null ? "" : igual.getEstadoEtiqueta().toLowerCase(Locale.ROOT) + " ")
                    + "con el nombre \"" + igual.getNombre() + "\""
                    + (igual.getCodigo() != null ? " (" + igual.getCodigo() + ")" : "")
                    + ". Agrega el barrio o el año al nombre para distinguirlo.";
        }
        return null;
    }

    /** Reglas que no necesitan la base de datos (también se usan en las pruebas unitarias). */
    public static String validarCampos(Proyecto p) {
        String nombre = p.getNombre() == null ? "" : p.getNombre().trim();
        if (nombre.length() < NOMBRE_MIN) {
            return "El nombre del proyecto debe tener al menos " + NOMBRE_MIN + " caracteres.";
        }
        if (nombre.length() > NOMBRE_MAX) {
            return "El nombre del proyecto no puede pasar de " + NOMBRE_MAX + " caracteres.";
        }
        String descripcion = p.getDescripcion() == null ? "" : p.getDescripcion().trim();
        if (descripcion.isEmpty()) {
            return "Escribe una descripción del proyecto.";
        }
        if (descripcion.length() > DESCRIPCION_MAX) {
            return "La descripción no puede pasar de " + DESCRIPCION_MAX + " caracteres.";
        }
        if (p.getCategoria() == null || !Catalogos.CATEGORIAS.contains(p.getCategoria())) {
            return "Elige la categoría del proyecto.";
        }
        if (p.getFechaInicio() == null || p.getFechaFin() == null) {
            return "Indica la fecha de inicio y la fecha de fin.";
        }
        if (p.getFechaFin().isBefore(p.getFechaInicio())) {
            return "La fecha de fin no puede ser anterior a la de inicio.";
        }
        if (p.getFechaInicio().plusDays(DURACION_MAX_DIAS).isBefore(p.getFechaFin())) {
            return "Un proyecto no puede durar más de 3 años.";
        }
        return null;
    }

    /** Proyecto PENDIENTE o ACTIVO con el mismo nombre (sin importar mayúsculas ni tildes). */
    public Proyecto buscarDuplicado(String nombre, String idActual) {
        String clave = normalizar(nombre);
        if (clave.isEmpty()) {
            return null;
        }
        // Primero un filtro amplio en MongoDB (mismo nombre sin distinguir mayúsculas)...
        String patron = "^" + Pattern.quote(nombre.trim()) + "$";
        Query q = new Query(Criteria.where("estado").in("PENDIENTE", "ACTIVO"));
        q.addCriteria(new Criteria().orOperator(
                Criteria.where("nombre").regex(patron, "i"),
                Criteria.where("nombre").regex("^" + Pattern.quote(sinTildes(nombre.trim())) + "$", "i")));
        q.fields().include("nombre", "estado", "codigo");
        List<Proyecto> candidatos = mongoTemplate.find(q, Proyecto.class);
        // ...y la comparación exacta sin tildes en Java
        return candidatos.stream()
                .filter(c -> !Objects.equals(c.getId(), idActual))
                .filter(c -> normalizar(c.getNombre()).equals(clave))
                .findFirst().orElse(null);
    }

    /**
     * @param nueva true al crear (la fecha no puede estar en el pasado); al editar se permite
     */
    public static String validarTarea(String titulo, LocalDate fechaEntrega, Proyecto proyecto, boolean nueva) {
        String t = titulo == null ? "" : titulo.trim();
        if (t.length() < TITULO_MIN) {
            return "El título de la tarea debe tener al menos " + TITULO_MIN + " caracteres.";
        }
        if (t.length() > TITULO_MAX) {
            return "El título de la tarea no puede pasar de " + TITULO_MAX + " caracteres.";
        }
        if (fechaEntrega == null) {
            return "Indica la fecha de entrega de la tarea.";
        }
        if (nueva && fechaEntrega.isBefore(LocalDate.now())) {
            return "La fecha de entrega no puede estar en el pasado.";
        }
        if (proyecto != null && proyecto.getFechaInicio() != null && fechaEntrega.isBefore(proyecto.getFechaInicio())) {
            return "La tarea debe vencer dentro de las fechas del proyecto (desde el " + proyecto.getFechaInicio() + ").";
        }
        if (proyecto != null && proyecto.getFechaFin() != null && fechaEntrega.isAfter(proyecto.getFechaFin())) {
            return "La tarea debe vencer dentro de las fechas del proyecto (hasta el " + proyecto.getFechaFin() + ").";
        }
        return null;
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return sinTildes(texto.trim().toLowerCase(Locale.ROOT)).replaceAll("\\s+", " ");
    }

    private static String sinTildes(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
