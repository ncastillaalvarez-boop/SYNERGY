package com.proaula.spring.synergy.Security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

/**
 * Tokens JWT de la API REST (app móvil).
 *
 *  - Token de acceso: dura poco (app.jwtExpirationMs) y va en "Authorization: Bearer ...".
 *  - Token de renovación (refresh): dura días (app.jwtRefreshExpirationMs) y SOLO sirve para
 *    pedir un token de acceso nuevo en POST /api/auth/refresh. No abre ninguna otra ruta.
 *
 * La clave se lee de la variable de entorno JWT_SECRET (o de application-secrets.properties).
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    static final String CLAIM_TIPO = "tipo";
    static final String TIPO_ACCESO = "acceso";
    static final String TIPO_RENOVACION = "renovacion";
    /** Texto de la clave de desarrollo que trae application.properties. */
    private static final String MARCA_CLAVE_DESARROLLO = "clave_de_desarrollo";

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs:780000}")
    private long jwtExpirationMs;

    @Value("${app.jwtRefreshExpirationMs:604800000}") // 7 días
    private long jwtRefreshExpirationMs;

    private SecretKey clave;

    public JwtTokenProvider() {
    }

    /** Constructor para pruebas unitarias. */
    public JwtTokenProvider(String jwtSecret, long jwtExpirationMs, long jwtRefreshExpirationMs) {
        this.jwtSecret = jwtSecret;
        this.jwtExpirationMs = jwtExpirationMs;
        this.jwtRefreshExpirationMs = jwtRefreshExpirationMs;
        iniciar();
    }

    @PostConstruct
    void iniciar() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("Falta la clave JWT: define app.jwtSecret o la variable JWT_SECRET.");
        }
        byte[] bytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 64) {
            // HS512 necesita al menos 512 bits. Antes una clave corta fallaba al primer login;
            // ahora se deriva una de 512 bits con SHA-512 y se avisa en la consola.
            log.warn("La clave JWT tiene menos de 64 caracteres; usa una más larga en JWT_SECRET.");
            try {
                bytes = java.security.MessageDigest.getInstance("SHA-512").digest(bytes);
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
        if (jwtSecret.contains(MARCA_CLAVE_DESARROLLO)) {
            log.warn("Se está usando la clave JWT de desarrollo. En producción define la variable de entorno JWT_SECRET.");
        }
        clave = Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(String correo, String rol) {
        return construir(correo, rol, TIPO_ACCESO, jwtExpirationMs);
    }

    public String generateRefreshToken(String correo) {
        return construir(correo, null, TIPO_RENOVACION, jwtRefreshExpirationMs);
    }

    private String construir(String correo, String rol, String tipo, long duracionMs) {
        Date ahora = new Date();
        var builder = Jwts.builder()
                .subject(correo)
                .claim(CLAIM_TIPO, tipo)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + duracionMs));
        if (rol != null) {
            builder.claim("rol", rol);
        }
        return builder.signWith(clave, Jwts.SIG.HS512).compact();
    }

    private Claims leer(String token) {
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
    }

    public String getCorreoFromToken(String token) {
        return leer(token).getSubject();
    }

    public String getRolFromToken(String token) {
        return (String) leer(token).get("rol");
    }

    /** true solo para tokens de ACCESO válidos (un token de renovación no abre la API). */
    public boolean validateToken(String token) {
        return esDelTipo(token, TIPO_ACCESO);
    }

    /** true solo para tokens de RENOVACIÓN válidos. */
    public boolean validateRefreshToken(String token) {
        return esDelTipo(token, TIPO_RENOVACION);
    }

    private boolean esDelTipo(String token, String tipo) {
        try {
            Object t = leer(token).get(CLAIM_TIPO);
            // Los tokens emitidos antes de esta versión no tienen "tipo": se tratan como de acceso
            return tipo.equals(t == null ? TIPO_ACCESO : t);
        } catch (Exception e) {
            return false;
        }
    }

    public Long getExpirationTime() {
        return jwtExpirationMs;
    }

    public Long getRefreshExpirationTime() {
        return jwtRefreshExpirationMs;
    }
}
