package com.proaula.spring.synergy.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.proaula.spring.synergy.Model.Proyecto;

/** Validaciones de proyectos, tareas, archivos y exportación a CSV. */
class ValidacionServiceTest {

    private static Proyecto valido() {
        Proyecto p = new Proyecto("Limpieza de la playa", "Recoger residuos con la comunidad.",
                LocalDate.now(), LocalDate.now().plusMonths(2), "lider");
        p.setCategoria("Limpieza de playas");
        return p;
    }

    @Test
    void proyectoCompletoEsValido() {
        assertNull(ValidacionService.validarCampos(valido()));
    }

    @Test
    void categoriaFechasYNombreSonObligatorios() {
        Proyecto sinCategoria = valido();
        sinCategoria.setCategoria(null);
        assertNotNull(ValidacionService.validarCampos(sinCategoria));

        Proyecto alReves = valido();
        alReves.setFechaFin(alReves.getFechaInicio().minusDays(1));
        assertNotNull(ValidacionService.validarCampos(alReves));

        Proyecto corto = valido();
        corto.setNombre("Eco");
        assertNotNull(ValidacionService.validarCampos(corto));
    }

    @Test
    void tareaDentroDeLasFechasDelProyecto() {
        Proyecto p = valido();
        assertNull(ValidacionService.validarTarea("Jornada de limpieza", LocalDate.now().plusDays(3), p, true));
        assertNotNull(ValidacionService.validarTarea("Jornada de limpieza", p.getFechaFin().plusDays(1), p, true));
        assertNotNull(ValidacionService.validarTarea("Jornada", LocalDate.now().minusDays(1), p, true));
        assertNotNull(ValidacionService.validarTarea("ab", LocalDate.now().plusDays(1), p, true));
    }

    @Test
    void nombresIgualesSinImportarTildesNiMayusculas() {
        assertEquals(ValidacionService.normalizar("Limpieza en Getsemaní"), ValidacionService.normalizar("  LIMPIEZA EN GETSEMANI "));
    }

    @Test
    void archivosConContenidoQueNoCorrespondeSeRechazan() {
        byte[] texto = "hola".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> ValidadorArchivos.validar("foto.png", texto));
        assertThrows(IllegalArgumentException.class, () -> ValidadorArchivos.validar("virus.exe", texto));
        assertThrows(IllegalArgumentException.class, () -> ValidadorArchivos.validar("vacio.pdf", new byte[0]));
        ValidadorArchivos.validar("notas.txt", texto);
        ValidadorArchivos.validar("informe.pdf", "%PDF-1.7 ...".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void csvParaExcelEscapaYEvitaFormulas() {
        assertEquals("\"a;b\"", ExportadorCsv.celda("a;b"));
        assertEquals("'=1+1", ExportadorCsv.celda("=1+1"));
        assertEquals("", ExportadorCsv.celda(null));
        assertEquals("x;2\r\n", ExportadorCsv.fila("x", 2));
    }

    @Test
    void insigniasDelVoluntario() {
        var nuevas = PanelVoluntarioService.insignias(0, 0, 0, 0, 0);
        assertEquals(0, nuevas.stream().filter(PanelVoluntarioService.Insignia::obtenida).count());
        var veterano = PanelVoluntarioService.insignias(12, 12, 6, 3, 1);
        assertEquals(nuevas.size(), veterano.stream().filter(PanelVoluntarioService.Insignia::obtenida).count());
    }
}
