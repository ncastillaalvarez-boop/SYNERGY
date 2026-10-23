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

    @PostMapping("/cambiar-contrasena")
    public ResponseEntity<?> cambiarContrasena(
            @RequestBody Map<String, String> request) {
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
    public ResponseEntity<?> logout() {
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Sesión cerrada exitosamente");
        return ResponseEntity.ok(response);
    }
}
