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

    /**
     * Coordenadas APROXIMADAS (centro del barrio) para el mapa del panel del administrador.
     * Sirven para ubicar los proyectos a escala de ciudad, no como dirección exacta.
     */
    public static final Map<String, double[]> COORDENADAS_BARRIOS = new LinkedHashMap<>();
    static {
        COORDENADAS_BARRIOS.put("Blas de Lezo", new double[] {10.3935, -75.4985});
        COORDENADAS_BARRIOS.put("Bocagrande", new double[] {10.3990, -75.5550});
        COORDENADAS_BARRIOS.put("Boston", new double[] {10.4105, -75.5005});
        COORDENADAS_BARRIOS.put("Canapote", new double[] {10.4385, -75.5185});
        COORDENADAS_BARRIOS.put("Castillogrande", new double[] {10.3915, -75.5530});
        COORDENADAS_BARRIOS.put("Centro Histórico", new double[] {10.4236, -75.5500});
        COORDENADAS_BARRIOS.put("Chambacú", new double[] {10.4270, -75.5410});
        COORDENADAS_BARRIOS.put("Ciudadela 2000", new double[] {10.3875, -75.4620});
        COORDENADAS_BARRIOS.put("Crespo", new double[] {10.4455, -75.5150});
        COORDENADAS_BARRIOS.put("El Cabrero", new double[] {10.4345, -75.5400});
        COORDENADAS_BARRIOS.put("El Country", new double[] {10.4020, -75.4930});
        COORDENADAS_BARRIOS.put("El Educador", new double[] {10.3900, -75.4800});
        COORDENADAS_BARRIOS.put("El Laguito", new double[] {10.3960, -75.5610});
        COORDENADAS_BARRIOS.put("El Pozón", new double[] {10.4080, -75.4520});
        COORDENADAS_BARRIOS.put("El Socorro", new double[] {10.3790, -75.4950});
        COORDENADAS_BARRIOS.put("Getsemaní", new double[] {10.4210, -75.5450});
        COORDENADAS_BARRIOS.put("La Boquilla", new double[] {10.4800, -75.4900});
        COORDENADAS_BARRIOS.put("La Consolata", new double[] {10.3800, -75.4700});
        COORDENADAS_BARRIOS.put("La Esperanza", new double[] {10.4150, -75.5100});
        COORDENADAS_BARRIOS.put("La María", new double[] {10.4180, -75.5050});
        COORDENADAS_BARRIOS.put("La Matuna", new double[] {10.4230, -75.5470});
        COORDENADAS_BARRIOS.put("Las Gaviotas", new double[] {10.3970, -75.4860});
        COORDENADAS_BARRIOS.put("Los Alpes", new double[] {10.4000, -75.4870});
        COORDENADAS_BARRIOS.put("Manga", new double[] {10.4130, -75.5370});
        COORDENADAS_BARRIOS.put("Marbella", new double[] {10.4370, -75.5300});
        COORDENADAS_BARRIOS.put("Nelson Mandela", new double[] {10.3640, -75.4740});
        COORDENADAS_BARRIOS.put("Nuevo Bosque", new double[] {10.3980, -75.5040});
        COORDENADAS_BARRIOS.put("Olaya Herrera", new double[] {10.4180, -75.4900});
        COORDENADAS_BARRIOS.put("Pie de la Popa", new double[] {10.4170, -75.5290});
        COORDENADAS_BARRIOS.put("San Diego", new double[] {10.4270, -75.5470});
        COORDENADAS_BARRIOS.put("San Fernando", new double[] {10.3800, -75.4850});
        COORDENADAS_BARRIOS.put("San José de los Campanos", new double[] {10.3780, -75.4620});
        COORDENADAS_BARRIOS.put("Ternera", new double[] {10.3910, -75.4720});
        COORDENADAS_BARRIOS.put("Torices", new double[] {10.4330, -75.5330});
        COORDENADAS_BARRIOS.put("Villa Estrella", new double[] {10.3870, -75.4550});
        COORDENADAS_BARRIOS.put("Zaragocilla", new double[] {10.4060, -75.5110});
    }

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
