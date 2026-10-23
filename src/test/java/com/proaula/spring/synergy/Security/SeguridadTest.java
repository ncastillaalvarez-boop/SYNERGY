package com.proaula.spring.synergy.Security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/** Contraseñas, bloqueo por intentos fallidos y redirecciones seguras después del login. */
class SeguridadTest {

    @Test
    void politicaDeContrasenas() {
        assertNull(PoliticaContrasena.validar("Synergy123*"));
        assertNotNull(PoliticaContrasena.validar("corta1A"));
        assertNotNull(PoliticaContrasena.validar("sinmayusculas1"));
        assertNotNull(PoliticaContrasena.validar("SINMINUSCULAS1"));
        assertNotNull(PoliticaContrasena.validar("SinNumeros"));
    }

    /** Reloj que se puede adelantar a mano. */
    static final class RelojFijo extends Clock {
        Instant ahora = Instant.parse("2026-10-23T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    @Test
    void bloqueaTrasCincoFallosYSeDesbloquea() {
        RelojFijo reloj = new RelojFijo();
        IntentosLoginService intentos = new IntentosLoginService(5, Duration.ofMinutes(15), Duration.ofMinutes(5), reloj);
        for (int i = 0; i < 4; i++) {
            intentos.registrarFallo("ana@correo.com");
        }
        assertFalse(intentos.estaBloqueado("ana@correo.com"));
        intentos.registrarFallo("ANA@correo.com ");
        assertTrue(intentos.estaBloqueado("ana@correo.com"), "mismo correo aunque cambien mayúsculas o espacios");
        assertFalse(intentos.estaBloqueado("otro@correo.com"));
        reloj.ahora = reloj.ahora.plus(Duration.ofMinutes(6));
        assertFalse(intentos.estaBloqueado("ana@correo.com"));
    }

    @Test
    void unLoginCorrectoReiniciaElContador() {
        IntentosLoginService intentos = new IntentosLoginService(5, Duration.ofMinutes(15), Duration.ofMinutes(5), new RelojFijo());
        for (int i = 0; i < 4; i++) {
            intentos.registrarFallo("luis@correo.com");
        }
        intentos.registrarExito("luis@correo.com");
        intentos.registrarFallo("luis@correo.com");
        assertFalse(intentos.estaBloqueado("luis@correo.com"));
    }

    @Test
    void soloSeVuelveARutasInternasPermitidas() {
        assertEquals("/publico/proyectos?q=playa", SesionUtil.rutaVolverSegura("/publico/proyectos?q=playa"));
        assertNull(SesionUtil.rutaVolverSegura("https://sitio-malo.com"));
        assertNull(SesionUtil.rutaVolverSegura("//sitio-malo.com"));
        assertNull(SesionUtil.rutaVolverSegura("/admin/usuarios"));
    }

    @Test
    void elTokenDeRenovacionNoAbreLaApi() {
        JwtTokenProvider jwt = new JwtTokenProvider("clave_de_pruebas_unitarias_con_mas_de_sesenta_y_cuatro_caracteres_para_hs512_ok", 60_000, 600_000);
        String acceso = jwt.generateToken("ana@correo.com", "PARTICIPANTE");
        String renovacion = jwt.generateRefreshToken("ana@correo.com");
        assertTrue(jwt.validateToken(acceso));
        assertFalse(jwt.validateToken(renovacion));
        assertTrue(jwt.validateRefreshToken(renovacion));
        assertFalse(jwt.validateRefreshToken(acceso));
        assertEquals("ana@correo.com", jwt.getCorreoFromToken(renovacion));
        assertFalse(jwt.validateToken(acceso + "x"), "un token alterado no sirve");
    }
}
