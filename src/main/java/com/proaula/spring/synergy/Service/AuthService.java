package com.proaula.spring.synergy.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.RolRepository;
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
    private RolRepository rolRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Carga el nombre del rol en el campo transient del usuario
    private void cargarRolNombre(Usuarios usuario) {
        if (usuario != null && usuario.getIdRol() != null) {
            rolRepository.findById(usuario.getIdRol())
                    .ifPresent(rol -> usuario.setRol(rol.getNombre()));
        }
    }

    public AuthResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getCorreo(),
                    loginRequest.getContrasena()
                )
            );

            // El principal ya tiene rolNombre cargado por UserDetailsServiceImpl
            Usuarios usuario = (Usuarios) authentication.getPrincipal();

            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);

            String token = jwtTokenProvider.generateToken(usuario.getCorreo(), usuario.getRol());

            return new AuthResponse(
                token,
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol(),
                new ArrayList<>(),
                jwtTokenProvider.getExpirationTime()
            );

        } catch (Exception e) {
            throw new RuntimeException("Error en autenticación: " + e.getMessage());
        }
    }

    public AuthResponse registrar(Usuarios usuario) {
        if (usuarioRepository.findByCorreo(usuario.getCorreo()) != null) {
            throw new RuntimeException("El correo ya está registrado");
        }

        String passwordEncriptada = passwordEncoder.encode(usuario.getContrasena());
        usuario.setContrasena(passwordEncriptada);
        usuario.setEstado("ACTIVO");
        usuario.setFechaCreacion(LocalDateTime.now());
        usuario.setUltimoCambioContrasena(LocalDateTime.now());

        Usuarios usuarioGuardado = usuarioRepository.save(usuario);

        // Cargar nombre del rol para el JWT
        cargarRolNombre(usuarioGuardado);

        String token = jwtTokenProvider.generateToken(usuarioGuardado.getCorreo(), usuarioGuardado.getRol());

        return new AuthResponse(
            token,
            usuarioGuardado.getId(),
            usuarioGuardado.getNombre(),
            usuarioGuardado.getCorreo(),
            usuarioGuardado.getRol(),
            new ArrayList<>(),
            jwtTokenProvider.getExpirationTime()
        );
    }

    public void cambiarContrasena(String usuarioId, String contrasenaActual, String nuevaContrasena) {
        Usuarios usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(contrasenaActual, usuario.getContrasena())) {
            throw new RuntimeException("Contraseña actual incorrecta");
        }

        usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));
        usuario.setUltimoCambioContrasena(LocalDateTime.now());
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

        cargarRolNombre(usuario);

        String nuevoToken = jwtTokenProvider.generateToken(usuario.getCorreo(), usuario.getRol());

        return new AuthResponse(
            nuevoToken,
            usuario.getId(),
            usuario.getNombre(),
            usuario.getCorreo(),
            usuario.getRol(),
            new ArrayList<>(),
            jwtTokenProvider.getExpirationTime()
        );
    }

    public void logout(String usuarioId) {
        // En JWT el logout se maneja en el cliente invalidando el token
    }
}
