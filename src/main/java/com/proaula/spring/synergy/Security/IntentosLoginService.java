package com.proaula.spring.synergy.Security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Límite de intentos de inicio de sesión (web y app móvil).
 *
 * Tras {@code maxIntentos} contraseñas incorrectas seguidas para el mismo correo dentro de la
 * ventana, ese correo queda bloqueado {@code bloqueo} minutos. Así un ataque de fuerza bruta no
 * puede probar miles de contraseñas. Un inicio de sesión correcto reinicia el contador.
 *
 * Se guarda en memoria: si se reinicia el servidor, los contadores vuelven a cero.
 */
@Service
public class IntentosLoginService {

    private final int maxIntentos;
    private final Duration ventana;
    private final Duration bloqueo;
    private final Clock reloj;

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    private static final class Registro {
        int fallos;
        Instant primerFallo;
        Instant bloqueadoHasta;
    }

    @Autowired
    public IntentosLoginService(@Value("${synergy.login.max-intentos:5}") int maxIntentos,
                                @Value("${synergy.login.bloqueo-minutos:5}") int bloqueoMinutos) {
        this(maxIntentos, Duration.ofMinutes(15), Duration.ofMinutes(bloqueoMinutos), Clock.systemUTC());
    }

    /** Constructor para pruebas (permite controlar el reloj). */
    public IntentosLoginService(int maxIntentos, Duration ventana, Duration bloqueo, Clock reloj) {
        this.maxIntentos = maxIntentos;
        this.ventana = ventana;
        this.bloqueo = bloqueo;
        this.reloj = reloj;
    }

    private static String clave(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    /** Minutos que faltan para poder intentarlo de nuevo (0 si no está bloqueado). */
    public long minutosBloqueado(String correo) {
        Registro r = registros.get(clave(correo));
        if (r == null || r.bloqueadoHasta == null) {
            return 0;
        }
        Duration resta = Duration.between(Instant.now(reloj), r.bloqueadoHasta);
        if (resta.isNegative() || resta.isZero()) {
            return 0;
        }
        return Math.max(1, (resta.getSeconds() + 59) / 60);
    }

    public boolean estaBloqueado(String correo) {
        return minutosBloqueado(correo) > 0;
    }

    /** Registra una contraseña incorrecta. Devuelve true si con este fallo quedó bloqueado. */
    public boolean registrarFallo(String correo) {
        Instant ahora = Instant.now(reloj);
        Registro r = registros.computeIfAbsent(clave(correo), k -> new Registro());
        synchronized (r) {
            if (r.bloqueadoHasta != null && !ahora.isBefore(r.bloqueadoHasta)) {
                // terminó un bloqueo anterior: empieza de cero
                r.fallos = 0;
                r.bloqueadoHasta = null;
            }
            if (r.primerFallo == null || Duration.between(r.primerFallo, ahora).compareTo(ventana) > 0) {
                r.fallos = 0;
                r.primerFallo = ahora;
            }
            r.fallos++;
            if (r.fallos >= maxIntentos) {
                r.bloqueadoHasta = ahora.plus(bloqueo);
                return true;
            }
            return false;
        }
    }

    public void registrarExito(String correo) {
        registros.remove(clave(correo));
    }

    public String mensajeBloqueo(String correo) {
        long min = minutosBloqueado(correo);
        return "Demasiados intentos fallidos. Espera " + min + (min == 1 ? " minuto" : " minutos")
                + " antes de volver a intentarlo.";
    }

    /** Limpia los registros viejos cada 30 minutos para que el mapa no crezca sin fin. */
    @Scheduled(fixedDelay = 30 * 60 * 1000L)
    public void limpiar() {
        Instant ahora = Instant.now(reloj);
        registros.entrySet().removeIf(e -> {
            Registro r = e.getValue();
            boolean bloqueoTerminado = r.bloqueadoHasta == null || ahora.isAfter(r.bloqueadoHasta);
            boolean ventanaVencida = r.primerFallo == null || Duration.between(r.primerFallo, ahora).compareTo(ventana) > 0;
            return bloqueoTerminado && ventanaVencida;
        });
    }
}
