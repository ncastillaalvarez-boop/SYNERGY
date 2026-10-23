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
import com.proaula.spring.synergy.Security.LoginRequest;
import com.proaula.spring.synergy.Service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            return ResponseEntity.ok(authService.login(loginRequest));
        } catch (com.proaula.spring.synergy.Security.LoginBloqueadoException e) {
            return error(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        } catch (org.springframework.security.authentication.DisabledException
                 | org.springframework.security.authentication.LockedException e) {
            return error(HttpStatus.FORBIDDEN, "Tu cuenta está inactiva. Contacta al administrador.");
        } catch (org.springframework.security.core.AuthenticationException e) {
            return error(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@RequestBody Usuarios usuario) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(usuario));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** Respuesta de error en JSON: {"message": "..."} (la app Flutter muestra "message"). */
    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("message", mensaje));
    }

    /** La app pide un token de acceso nuevo con el token de renovación: {"refreshToken": "..."}. */
    @PostMapping("/refresh")
    public ResponseEntity<?> renovar(@RequestBody(required = false) Map<String, String> body) {
        try {
            return ResponseEntity.ok(authService.renovar(body == null ? null : body.get("refreshToken")));
        } catch (org.springframework.security.authentication.DisabledException e) {
            return error(HttpStatus.FORBIDDEN, "Tu cuenta está inactiva. Contacta al administrador.");
        } catch (org.springframework.security.core.AuthenticationException e) {
            return error(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    /**
     * Cambia la contraseña del usuario AUTENTICADO (token JWT). Antes se tomaba el "usuarioId"
     * del cuerpo de la petición, así que se podía intentar con cualquier cuenta.
     */
    @PostMapping("/cambiar-contrasena")
    public ResponseEntity<?> cambiarContrasena(@RequestBody Map<String, String> request) {
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null
                ? null : org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Usuarios usuario)) {
            return error(HttpStatus.UNAUTHORIZED, "Inicia sesión para cambiar tu contraseña.");
        }
        try {
            authService.cambiarContrasena(usuario.getId(), request.get("contrasenaActual"), request.get("nuevaContrasena"));
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña cambiada exitosamente",
                    "message", "Contraseña cambiada exitosamente"));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Sesión cerrada exitosamente");
        return ResponseEntity.ok(response);
    }
}
