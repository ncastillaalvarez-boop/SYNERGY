package com.proaula.spring.synergy.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.proaula.spring.synergy.Service.RiesgoCalculadora.ActividadInfo;
import com.proaula.spring.synergy.Service.RiesgoCalculadora.Indicadores;
import com.proaula.spring.synergy.Service.RiesgoCalculadora.TareaInfo;

/** Variables del modelo predictivo, reglas de la propuesta, estimación de fin y simulador. */
class RiesgoCalculadoraTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 23);

    private static Indicadores indicadores(String categoria, List<TareaInfo> tareas, List<ActividadInfo> entregas) {
        return RiesgoCalculadora.calcular(HOY.minusDays(40), HOY.plusDays(40), categoria, List.of("v1", "v2"),
                tareas, entregas, List.of(), HOY);
    }

    @Test
    void proyectoSinCategoriaNoFalla() {
        // Antes: List.of(...).indexOf(null) lanzaba NullPointerException y dejaba todo en "Sin datos"
        Indicadores i = indicadores(null, List.of(new TareaInfo("PENDIENTE", HOY.minusDays(30), HOY.plusDays(5), null)), List.of());
        double[] v = i.vector();
        assertEquals(RiesgoCalculadora.VARIABLES_NUMERICAS.size() + com.proaula.spring.synergy.Model.Catalogos.CATEGORIAS.size(), v.length);
        assertNotNull(RiesgoProyectoService.predecir(i).nivel());
    }

    @Test
    void cuentaTareasVencidasYRetraso() {
        Indicadores i = indicadores("Reciclaje y residuos", List.of(
                new TareaInfo("PENDIENTE", HOY.minusDays(30), HOY.minusDays(20), null),
                new TareaInfo("PENDIENTE", HOY.minusDays(30), HOY.minusDays(3), null),
                new TareaInfo("COMPLETADA", HOY.minusDays(30), HOY.minusDays(10), HOY.minusDays(12)),
                new TareaInfo("CANCELADA", HOY.minusDays(30), HOY.minusDays(30), null)), List.of());
        assertEquals(3, i.totalTareas(), "las canceladas no cuentan");
        assertEquals(2, i.tareasVencidas());
        assertEquals(20, i.diasRetraso());
        assertEquals(0.667, i.pctTareasPendientes(), 0.001);
    }

    @Test
    void participacionSoloCuentaVoluntariosDelEquipo() {
        Indicadores i = indicadores("Reciclaje y residuos", List.of(),
                List.of(new ActividadInfo("v1", HOY.minusDays(1)), new ActividadInfo("externo", HOY.minusDays(1))));
        assertEquals(0.5, i.tasaParticipacion(), 0.001);
        assertEquals(2, i.actividades());
    }

    @Test
    void reglasDeLaPropuesta() {
        Indicadores alto = new Indicadores(20, 0.1, 5, 0.9, 60, 0.9, 0.5, 0.4, 10, 10, 1, null);
        Indicadores medio = new Indicadores(7, 0.1, 5, 0.9, 60, 0.9, 0.5, 0.4, 10, 10, 1, null);
        Indicadores bajo = new Indicadores(0, 0.1, 5, 0.9, 60, 0.9, 0.5, 0.4, 10, 10, 0, null);
        assertEquals("Alto", RiesgoCalculadora.nivelPorReglas(alto));
        assertEquals("Medio", RiesgoCalculadora.nivelPorReglas(medio));
        assertEquals("Bajo", RiesgoCalculadora.nivelPorReglas(bajo));
    }

    @Test
    void estimaLaFechaDeFinAlRitmoActual() {
        // 2 tareas hechas en 20 días -> 0,1 por día; quedan 2 -> 20 días más
        List<TareaInfo> tareas = List.of(
                new TareaInfo("COMPLETADA", HOY.minusDays(20), HOY, HOY.minusDays(10)),
                new TareaInfo("COMPLETADA", HOY.minusDays(20), HOY, HOY.minusDays(2)),
                new TareaInfo("PENDIENTE", HOY.minusDays(20), HOY.plusDays(5), null),
                new TareaInfo("PENDIENTE", HOY.minusDays(20), HOY.plusDays(9), null));
        assertEquals(HOY.plusDays(20), RiesgoCalculadora.estimarFin(HOY.minusDays(20), tareas, HOY));
        assertNull(RiesgoCalculadora.estimarFin(HOY.minusDays(3), tareas, HOY), "muy pronto para estimar");
    }

    @Test
    void elSimuladorMejoraLosIndicadores() {
        Indicadores actual = new Indicadores(18, 0.6, 4, 0.25, 80, 0.4, 0.5, -0.1, 3, 10, 3, "Reciclaje y residuos");
        Indicadores despues = RiesgoCalculadora.simular(actual, 6, 3, 0);
        assertEquals(0, despues.tareasVencidas());
        assertEquals(0, despues.diasRetraso());
        assertTrue(despues.pctTareasPendientes() < actual.pctTareasPendientes());
        assertEquals(1.0, despues.tasaParticipacion(), 0.001);
    }
}
