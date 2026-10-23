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
        String correo = loginRequest.getCorreo() == null ? "" : loginRequest.getCorreo().trim().toLowerCase();
        if (correo.isEmpty() || loginRequest.getContrasena() == null || loginRequest.getContrasena().isEmpty()) {
            throw new IllegalArgumentException("Escribe tu correo y tu contraseña.");
        }
        // Lanza BadCredentialsException / DisabledException; el controlador las traduce a un mensaje claro
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(correo, loginRequest.getContrasena()));

        Usuarios usuario = (Usuarios) authentication.getPrincipal();
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String token = jwtTokenProvider.generateToken(usuario.getCorreo(), usuario.getRol());
        return new AuthResponse(token, usuario.getId(), usuario.getNombre(), usuario.getCorreo(),
                usuario.getRol(), usuario.getPrivilegios(), jwtTokenProvider.getExpirationTime());
    }

    public AuthResponse registrar(Usuarios usuario) {
        // Mismas reglas que el registro web
        String nombre = usuario.getNombre() == null ? "" : usuario.getNombre().trim();
        String correo = usuario.getCorreo() == null ? "" : usuario.getCorreo().trim().toLowerCase();
        String contrasena = usuario.getContrasena() == null ? "" : usuario.getContrasena();
        if (nombre.isEmpty() || !correo.contains("@")) {
            throw new IllegalArgumentException("Escribe tu nombre y un correo válido.");
        }
        if (contrasena.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
        }
        if (usuarioRepository.findByCorreo(correo) != null) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo. Inicia sesión.");
        }
        usuario.setId(null);
        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        usuario.setProveedor("LOCAL");
        usuario.setProyectos(new java.util.ArrayList<>());
        usuario.setTareasAsignadas(new java.util.ArrayList<>());

        // Un registro público nunca puede autootorgarse privilegios de administrador.
        // Se conserva la posibilidad de registrar líderes, usada por las vistas web.
        if (!"LIDER".equalsIgnoreCase(usuario.getRol())) {
            usuario.setRol("PARTICIPANTE");
        } else {
            usuario.setRol("LIDER");
        }
        usuario.setPrivilegios(new java.util.ArrayList<>());

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

    public void logout(String usuarioId) {
        // En JWT no es necesario hacer logout en el servidor
        // El token se invalida en el cliente
    }
}
