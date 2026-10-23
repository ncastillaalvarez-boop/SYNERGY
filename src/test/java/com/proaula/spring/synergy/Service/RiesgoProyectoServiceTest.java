package com.proaula.spring.synergy.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.proaula.spring.synergy.Model.HistorialRiesgo;
import com.proaula.spring.synergy.Service.RiesgoCalculadora.Indicadores;

/** Predicción con el modelo exportado de Colab y textos del historial (sin base de datos). */
class RiesgoProyectoServiceTest {

    @Test
    void proyectoRecienIniciadoSinTareasQuedaSinDatos() {
        Indicadores i = new Indicadores(0, 1.0, 3, 0, 90, 0, 0.05, -0.05, 0, 0, 0, "Reciclaje y residuos");
        assertNull(RiesgoProyectoService.predecir(i).nivel());
    }

    @Test
    void proyectoMuyAtrasadoEsAltoYExplicaPorQue() {
        Indicadores i = new Indicadores(40, 0.8, 5, 0.2, 90, 0.2, 0.8, -0.6, 2, 10, 6, "Reciclaje y residuos");
        RiesgoProyectoService.Evaluacion e = RiesgoProyectoService.predecir(i);
        assertEquals("Alto", e.nivel());
        assertTrue(e.puntaje() > 0.5);
        assertEquals(1.0, e.probabilidades()[0] + e.probabilidades()[1] + e.probabilidades()[2], 0.001);
    }

    @Test
    void proyectoAlDiaEsBajo() {
        Indicadores i = new Indicadores(0, 0.1, 5, 1.0, 90, 0.9, 0.5, 0.4, 20, 10, 0, "Reciclaje y residuos");
        assertEquals("Bajo", RiesgoProyectoService.predecir(i).nivel());
    }

    @Test
    void historialDiceCuandoSubioDeNivel() {
        LocalDate hoy = LocalDate.of(2026, 10, 23);
        List<HistorialRiesgo> puntos = new ArrayList<>();
        puntos.add(new HistorialRiesgo("p", hoy.minusDays(10), "Medio", 0.4));
        puntos.add(new HistorialRiesgo("p", hoy.minusDays(3), "Alto", 0.8));
        puntos.add(new HistorialRiesgo("p", hoy, "Alto", 0.85));
        RiesgoProyectoService.ResumenHistorial r = RiesgoProyectoService.resumirHistorial(puntos, hoy);
        assertEquals("Subió de Medio a Alto hace 3 días", r.cambio());
        assertEquals(3, r.polilinea().split(" ").length);
    }

    @Test
    void historialVacioNoSeDibuja() {
        assertTrue(RiesgoProyectoService.resumirHistorial(List.of(), LocalDate.now()).isVacio());
    }
}
