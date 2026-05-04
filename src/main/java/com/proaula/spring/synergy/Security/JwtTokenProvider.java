package com.proaula.spring.synergy.Security;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private long jwtExpirationMs;

    @Value("${app.jwtRefreshExpirationMs}")
    private long jwtRefreshExpirationMs;

    // ============================================================
    // ACCESS TOKEN
    // ============================================================
    public String generateToken(String correo, String rol) {
        return buildToken(correo, rol, jwtExpirationMs, "access");
    }

    // ============================================================
    // REFRESH TOKEN
    // ============================================================
    public String generateRefreshToken(String correo) {
        return buildToken(correo, null, jwtRefreshExpirationMs, "refresh");
    }

    // ============================================================
    // CONSTRUCCIÓN INTERNA DE TOKENS
    // ============================================================
    private String buildToken(String correo, String rol, long expirationMs, String tipo) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .setSubject(correo)
                .claim("tipo", tipo)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512);

        if (rol != null) {
            builder.claim("rol", rol);
        }

        return builder.compact();
    }

    // ============================================================
    // EXTRAER DATOS DEL TOKEN
    // ============================================================
    public String getCorreoFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getRolFromToken(String token) {
        return (String) getClaims(token).get("rol");
    }

    public String getTipoFromToken(String token) {
        return (String) getClaims(token).get("tipo");
    }

    private Claims getClaims(String token) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ============================================================
    // VALIDACIÓN
    // ============================================================
    public boolean validateToken(String token) {
        try {
            Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) key)
                    .build()
                    .parseSignedClaims(token);
            return true;
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
