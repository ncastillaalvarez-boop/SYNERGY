package com.proaula.spring.synergy.Service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.UsuarioRepository;
import com.proaula.spring.synergy.Security.AuthResponse;
import com.proaula.spring.synergy.Security.JwtTokenProvider;
import com.proaula.spring.synergy.Security.LoginRequest;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getCorreo(),
                    loginRequest.getContrasena()
                )
            );

            Usuarios usuario = (Usuarios) authentication.getPrincipal();
            
            // Actualizar último acceso
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);

            // Generar token JWT
            String token = jwtTokenProvider.generateToken(usuario.getCorreo(), usuario.getRol());

            return new AuthResponse(
                token,
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.getPrivilegios(),
                jwtTokenProvider.getExpirationTime()
            );

        } catch (Exception e) {
            throw new RuntimeException("Error en autenticación: " + e.getMessage());
        }
    }

    public AuthResponse registrar(Usuarios usuario) {
        // Verificar si el usuario ya existe
        if (usuarioRepository.findByCorreo(usuario.getCorreo()) != null) {
            throw new RuntimeException("El correo ya está registrado");
        }

        // Encriptar contraseña
        String passwordEncriptada = passwordEncoder.encode(usuario.getContrasena());
        usuario.setContrasena(passwordEncriptada);
        usuario.setActivo(true);
        usuario.setEstado("ACTIVO");
        usuario.setFechaCreacion(LocalDateTime.now());
        usuario.setUltimaCambioContrasena(LocalDateTime.now());
        
        // Guardar usuario
        Usuarios usuarioGuardado = usuarioRepository.save(usuario);

        // Generar token
        String token = jwtTokenProvider.generateToken(usuarioGuardado.getCorreo(), usuarioGuardado.getRol());

        return new AuthResponse(
            token,
            usuarioGuardado.getId(),
            usuarioGuardado.getNombre(),
            usuarioGuardado.getCorreo(),
            usuarioGuardado.getRol(),
            usuarioGuardado.getPrivilegios(),
            jwtTokenProvider.getExpirationTime()
        );
    }

    public void cambiarContrasena(String usuarioId, String contrasenaActual, String nuevaContrasena) {
        Usuarios usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar contraseña actual
        if (!passwordEncoder.matches(contrasenaActual, usuario.getContrasena())) {
            throw new RuntimeException("Contraseña actual incorrecta");
        }

        // Actualizar contraseña
        usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));
        usuario.setUltimaCambioContrasena(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }
    public AuthResponse refreshToken(String correo) {
        Usuarios usuario = usuarioRepository.findByCorreo(correo);
    
        if (usuario == null) {
            throw new RuntimeException("Usuario no encontrado");
        }

        if (!usuario.isEnabled()) {
            throw new RuntimeException("Usuario inactivo");
        }

        // Generar nuevo access token
        String nuevoToken = jwtTokenProvider.generateToken(usuario.getCorreo(), usuario.getRol());

        return new AuthResponse(
            nuevoToken,
            usuario.getId(),
            usuario.getNombre(),
            usuario.getCorreo(),
            usuario.getRol(),
            usuario.getPrivilegios(),
            jwtTokenProvider.getExpirationTime()
        );
    }

    public void logout(String usuarioId) {
        // En JWT no es necesario hacer logout en el servidor
        // El token se invalida en el cliente
    }
}
