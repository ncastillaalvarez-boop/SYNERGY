package com.proaula.spring.synergy.Model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Listas fijas que usan los formularios, los filtros de búsqueda y el generador
 * de datos. Tenerlas en un solo sitio evita que un filtro busque un valor que
 * ningún formulario puede guardar.
 */
public final class Catalogos {

    private Catalogos() {
    }

    /** Estados de un proyecto (en orden de ciclo de vida) con su etiqueta visible. */
    public static final Map<String, String> ESTADOS_PROYECTO = new LinkedHashMap<>();
    static {
        ESTADOS_PROYECTO.put("PENDIENTE", "Por aprobar");
        ESTADOS_PROYECTO.put("ACTIVO", "Activo");
        ESTADOS_PROYECTO.put("COMPLETADO", "Completado");
        ESTADOS_PROYECTO.put("CANCELADO", "Cancelado");
    }

    /** Estados que puede ver cualquier visitante (los pendientes y cancelados no son públicos). */
    public static final List<String> ESTADOS_PUBLICOS = List.of("ACTIVO", "COMPLETADO");

    public static final List<String> CATEGORIAS = List.of(
        "Limpieza de playas",
        "Reforestación",
        "Manglares y ciénagas",
        "Reciclaje y residuos",
        "Huertas comunitarias",
        "Educación ambiental",
        "Fauna y aves",
        "Agua y saneamiento",
        "Energía limpia",
        "Espacios verdes",
        "Calidad del aire",
        "Canales y caños"
    );

    public static final List<String> ORGANIZACIONES = List.of(
        "Comunidad",
        "ONG",
        "Institución educativa",
        "Proyecto estudiantil",
        "Entidad gubernamental",
        "Empresa",
        "Otro"
    );

    /** Barrios y sectores de Cartagena de Indias. */
    public static final List<String> BARRIOS = List.of(
        "Blas de Lezo", "Bocagrande", "Boston", "Canapote", "Castillogrande",
        "Centro Histórico", "Chambacú", "Ciudadela 2000", "Crespo", "El Cabrero",
        "El Country", "El Educador", "El Laguito", "El Pozón", "El Socorro",
        "Getsemaní", "La Boquilla", "La Consolata", "La Esperanza", "La María",
        "La Matuna", "Las Gaviotas", "Los Alpes", "Manga", "Marbella",
        "Nelson Mandela", "Nuevo Bosque", "Olaya Herrera", "Pie de la Popa",
        "San Diego", "San Fernando", "San José de los Campanos", "Ternera",
        "Torices", "Villa Estrella", "Zaragocilla"
    );

    public static final List<String> AUDIENCIAS = List.of(
        "Habitantes del barrio",
        "Jóvenes voluntarios",
        "Familias y niños",
        "Estudiantes",
        "Adultos mayores",
        "Comerciantes locales",
        "Público general"
    );

    /** Opciones de orden para los listados de proyectos. */
    public static final Map<String, String> ORDENES_PROYECTO = new LinkedHashMap<>();
    static {
        ORDENES_PROYECTO.put("recientes", "Más recientes");
        ORDENES_PROYECTO.put("inicio", "Próximos a iniciar");
        ORDENES_PROYECTO.put("nombre", "Nombre (A-Z)");
        ORDENES_PROYECTO.put("fin", "Próximos a finalizar");
    }

    public static String etiquetaEstado(String estado) {
        return estado == null ? "" : ESTADOS_PROYECTO.getOrDefault(estado.toUpperCase(), estado);
    }
}
