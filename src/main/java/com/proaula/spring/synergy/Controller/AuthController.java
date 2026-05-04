package com.proaula.spring.synergy.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Security.AuthResponse;
import com.proaula.spring.synergy.Security.CookieUtil;
import com.proaula.spring.synergy.Security.JwtTokenProvider;
import com.proaula.spring.synergy.Security.LoginRequest;
import com.proaula.spring.synergy.Service.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CookieUtil cookieUtil;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, 
                                    HttpServletResponse response) {
        try {
            AuthResponse authResponse = authService.login(loginRequest);

            // Generar refresh token
            String refreshToken = jwtTokenProvider.generateRefreshToken(loginRequest.getCorreo());

            // Setear cookies HttpOnly
            cookieUtil.createAccessTokenCookie(response, authResponse.getToken());
            cookieUtil.createRefreshTokenCookie(response, refreshToken);

            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Error de autenticación: " + e.getMessage());
        }
    }

    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@RequestBody Usuarios usuario) {
        try {
            AuthResponse response = authService.registrar(usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error al registrar: " + e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = extractRefreshTokenFromCookie(request);

            if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Refresh token inválido o expirado");
            }

            // Verificar que sea un refresh token
            String tipo = jwtTokenProvider.getTipoFromToken(refreshToken);
            if (!"refresh".equals(tipo)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Tipo de token incorrecto");
            }

            // Generar nuevo access token
            String correo = jwtTokenProvider.getCorreoFromToken(refreshToken);
            AuthResponse newAuth = authService.refreshToken(correo);

            cookieUtil.createAccessTokenCookie(response, newAuth.getToken());

            return ResponseEntity.ok(newAuth);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Error al refrescar token: " + e.getMessage());
        }
    }

    @PostMapping("/cambiar-contrasena")
    public ResponseEntity<?> cambiarContrasena(@RequestBody Map<String, String> request) {
        try {
            String usuarioId = request.get("usuarioId");
            String contrasenaActual = request.get("contrasenaActual");
            String nuevaContrasena = request.get("nuevaContrasena");

            authService.cambiarContrasena(usuarioId, contrasenaActual, nuevaContrasena);
            
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Contraseña cambiada exitosamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        cookieUtil.clearCookies(response);
        Map<String, String> resp = new HashMap<>();
        resp.put("mensaje", "Sesión cerrada exitosamente");
        return ResponseEntity.ok(resp);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("SYNERGY_REFRESH_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}