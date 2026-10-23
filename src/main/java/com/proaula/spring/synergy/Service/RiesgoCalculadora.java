package com.proaula.spring.synergy.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.proaula.spring.synergy.Model.Catalogos;

/**
 * Cálculo de las variables del modelo predictivo de riesgo (propuesta del proyecto de aula).
 *
 * Es código puro (sin base de datos) para que las MISMAS fórmulas se usen al exportar el
 * dataset que se entrena en Google Colab y al predecir el riesgo de los proyectos activos.
 *
 * Variable objetivo nivel_riesgo (Bajo / Medio / Alto), reglas de la propuesta:
 *   Alto : retraso > 15 días, o más del 50 % de tareas pendientes, o participación < 40 %
 *   Medio: retraso de 5 a 15 días, o 25-50 % de tareas pendientes, o participación de 40-70 %
 *   Bajo : retraso < 5 días, menos del 25 % pendientes y participación > 70 %
 *
 * Para que el modelo PREDIGA (y no copie la regla), en el dataset las variables se miden a la
 * mitad del proyecto y el nivel de riesgo se calcula con cómo terminó (ver RiesgoProyectoService).
 */
public final class RiesgoCalculadora {

    /** Orden de las clases en el modelo: 0 = Bajo, 1 = Medio, 2 = Alto. */
    public static final List<String> NIVELES = List.of("Bajo", "Medio", "Alto");

    /** Variables numéricas, en el orden exacto que espera ModeloRiesgo (después van las categorías). */
    public static final List<String> VARIABLES_NUMERICAS = List.of(
            "dias_retraso", "pct_tareas_pendientes", "participantes", "tasa_participacion",
            "duracion_dias", "avance_real", "avance_esperado", "brecha_avance", "actividades");

    /** Tope para que un dato atípico no domine el modelo. */
    private static final int MAX_DIAS_RETRASO = 120;

    private RiesgoCalculadora() {
    }

    /** Tarea vista por el modelo: completada = fecha en que se completó (null si no). */
    public record TareaInfo(String estado, LocalDate creada, LocalDate vence, LocalDate completada) {
        boolean cancelada() {
            return "CANCELADA".equalsIgnoreCase(estado);
        }
    }

    /** Una entrega o un comentario de un voluntario en el proyecto. */
    public record ActividadInfo(String idUsuario, LocalDate fecha) {
    }

    /** Indicadores de un proyecto en una fecha de corte. */
    public record Indicadores(
            int diasRetraso,
            double pctTareasPendientes,
            int participantes,
            double tasaParticipacion,
            int duracionDias,
            double avanceReal,
            double avanceEsperado,
            double brechaAvance,
            int actividades,
            int totalTareas,
            int tareasVencidas,
            String categoria) {

        /** Vector de entrada del modelo: variables numéricas + una columna 0/1 por categoría. */
        public double[] vector() {
            double[] v = new double[VARIABLES_NUMERICAS.size() + Catalogos.CATEGORIAS.size()];
            v[0] = diasRetraso;
            v[1] = pctTareasPendientes;
            v[2] = participantes;
            v[3] = tasaParticipacion;
            v[4] = duracionDias;
            v[5] = avanceReal;
            v[6] = avanceEsperado;
            v[7] = brechaAvance;
            v[8] = actividades;
            // List.of(...).indexOf(null) lanza NullPointerException: hay proyectos sin categoría
            int i = categoria == null ? -1 : Catalogos.CATEGORIAS.indexOf(categoria);
            if (i >= 0) {
                v[VARIABLES_NUMERICAS.size() + i] = 1;
            }
            return v;
        }
    }

    /**
     * Calcula los indicadores del proyecto tal como estaban en la fecha "corte".
     *
     * @param voluntarios ids de los participantes (sin el líder)
     * @param entregas    entregas de tareas del proyecto (autor y fecha)
     * @param comentarios comentarios del proyecto (autor y fecha)
     */
    public static Indicadores calcular(LocalDate inicio, LocalDate fin, String categoria,
                                       Collection<String> voluntarios, List<TareaInfo> tareas,
                                       List<ActividadInfo> entregas, List<ActividadInfo> comentarios,
                                       LocalDate corte) {
        int total = 0;
        int pendientes = 0;
        int vencidas = 0;
        long retraso = 0;
        for (TareaInfo t : tareas) {
            if (t.cancelada() || t.creada() == null || t.creada().isAfter(corte)) {
                continue;
            }
            total++;
            boolean hecha = t.completada() != null && !t.completada().isAfter(corte);
            if (!hecha) {
                pendientes++;
                if (t.vence() != null && t.vence().isBefore(corte)) {
                    vencidas++;
                    retraso = Math.max(retraso, ChronoUnit.DAYS.between(t.vence(), corte));
                }
            }
        }
        double pctPendientes = total == 0 ? 1.0 : pendientes / (double) total;

        // Participación activa: voluntarios con al menos una entrega o un comentario hasta el corte
        Set<String> equipo = new HashSet<>(voluntarios);
        Set<String> activos = new HashSet<>();
        int actividades = 0;
        for (List<ActividadInfo> lista : List.of(entregas, comentarios)) {
            for (ActividadInfo a : lista) {
                if (a.fecha() != null && !a.fecha().isAfter(corte)) {
                    actividades++;
                    if (a.idUsuario() != null && equipo.contains(a.idUsuario())) {
                        activos.add(a.idUsuario());
                    }
                }
            }
        }
        double tasa = equipo.isEmpty() ? 0.0 : activos.size() / (double) equipo.size();

        long duracion = Math.max(1, ChronoUnit.DAYS.between(inicio, fin));
        double esperado = Math.min(1.0, Math.max(0.0, ChronoUnit.DAYS.between(inicio, corte) / (double) duracion));
        double real = 1.0 - pctPendientes;

        return new Indicadores((int) Math.min(MAX_DIAS_RETRASO, retraso), redondear(pctPendientes), equipo.size(),
                redondear(tasa), (int) duracion, redondear(real), redondear(esperado), redondear(real - esperado),
                actividades, total, vencidas, categoria);
    }

    /** Nivel de riesgo según las reglas de la propuesta (se usa para etiquetar el dataset). */
    public static String nivelPorReglas(Indicadores i) {
        if (i.diasRetraso() > 15 || i.pctTareasPendientes() > 0.50 || i.tasaParticipacion() < 0.40) {
            return "Alto";
        }
        if (i.diasRetraso() >= 5 || i.pctTareasPendientes() >= 0.25 || i.tasaParticipacion() <= 0.70) {
            return "Medio";
        }
        return "Bajo";
    }

    /** Motivos legibles para mostrar junto al semáforo. */
    public static List<String> motivos(Indicadores i) {
        List<String> m = new ArrayList<>();
        if (i.tareasVencidas() > 0) {
            m.add(i.tareasVencidas() + (i.tareasVencidas() == 1 ? " tarea vencida" : " tareas vencidas")
                    + " (la más atrasada lleva " + i.diasRetraso() + " días)");
        }
        if (i.totalTareas() > 0 && i.pctTareasPendientes() >= 0.25) {
            m.add(porcentaje(i.pctTareasPendientes()) + " de las tareas siguen pendientes");
        }
        if (i.participantes() == 0) {
            m.add("No tiene voluntarios inscritos");
        } else if (i.tasaParticipacion() <= 0.70) {
            m.add("Solo " + porcentaje(i.tasaParticipacion()) + " de los voluntarios ha participado");
        }
        if (i.brechaAvance() <= -0.15) {
            m.add("Avance " + Math.round(-i.brechaAvance() * 100) + " puntos por debajo de lo esperado");
        }
        if (i.totalTareas() == 0) {
            m.add("Aún no tiene tareas asignadas");
        }
        if (m.isEmpty()) {
            m.add("Avanza según lo planeado");
        }
        return m;
    }

    /** Acción sugerida para el líder o el administrador. */
    public static String sugerencia(String nivel, Indicadores i) {
        if ("Bajo".equals(nivel)) {
            return "Mantener el seguimiento habitual.";
        }
        if (i.participantes() == 0 || i.tasaParticipacion() < 0.40) {
            return "Convocar más voluntarios y contactar a los inscritos que no han participado.";
        }
        if (i.tareasVencidas() > 0) {
            return "Reasignar o reprogramar las tareas vencidas.";
        }
        if (i.totalTareas() == 0) {
            return "Crear y asignar las primeras tareas del proyecto.";
        }
        return "Revisar el cronograma con el equipo.";
    }

    private static double redondear(double valor) {
        return Math.round(valor * 1000) / 1000.0;
    }

    private static String porcentaje(double valor) {
        return String.format(Locale.ROOT, "%d %%", Math.round(valor * 100));
    }
}
