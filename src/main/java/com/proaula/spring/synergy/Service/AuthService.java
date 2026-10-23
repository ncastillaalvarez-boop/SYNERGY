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

    @Autowired
    private com.proaula.spring.synergy.Security.IntentosLoginService intentosLogin;

    public AuthResponse login(LoginRequest loginRequest) {
        String correo = loginRequest.getCorreo() == null ? "" : loginRequest.getCorreo().trim().toLowerCase();
        if (correo.isEmpty() || loginRequest.getContrasena() == null || loginRequest.getContrasena().isEmpty()) {
            throw new IllegalArgumentException("Escribe tu correo y tu contraseña.");
        }
        if (intentosLogin.estaBloqueado(correo)) {
            throw new com.proaula.spring.synergy.Security.LoginBloqueadoException(intentosLogin.mensajeBloqueo(correo));
        }
        // Lanza BadCredentialsException / DisabledException; el controlador las traduce a un mensaje claro
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(correo, loginRequest.getContrasena()));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            intentosLogin.registrarFallo(correo);
            throw e;
        }
        intentosLogin.registrarExito(correo);

        Usuarios usuario = (Usuarios) authentication.getPrincipal();
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);
        return respuesta(usuario);
    }

    /** Token de acceso + token de renovación para la app móvil. */
    private AuthResponse respuesta(Usuarios usuario) {
        String token = jwtTokenProvider.generateToken(usuario.getCorreo(), usuario.getRol());
        return new AuthResponse(token, usuario.getId(), usuario.getNombre(), usuario.getCorreo(),
                usuario.getRol(), usuario.getPrivilegios(), jwtTokenProvider.getExpirationTime())
                .conRefreshToken(jwtTokenProvider.generateRefreshToken(usuario.getCorreo()));
    }

    /**
     * Da un token de acceso nuevo a partir del token de renovación, sin pedir la contraseña.
     * Si la cuenta fue desactivada, ya no se renueva.
     */
    public AuthResponse renovar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new org.springframework.security.authentication.BadCredentialsException("Tu sesión expiró. Inicia sesión de nuevo.");
        }
        Usuarios usuario = usuarioRepository.findByCorreo(jwtTokenProvider.getCorreoFromToken(refreshToken));
        if (usuario == null) {
            throw new org.springframework.security.authentication.BadCredentialsException("Tu sesión expiró. Inicia sesión de nuevo.");
        }
        if (!usuario.isEnabled()) {
            throw new org.springframework.security.authentication.DisabledException("Cuenta inactiva");
        }
        return respuesta(usuario);
    }

    public AuthResponse registrar(Usuarios usuario) {
        // Mismas reglas que el registro web
        String nombre = usuario.getNombre() == null ? "" : usuario.getNombre().trim();
        String correo = usuario.getCorreo() == null ? "" : usuario.getCorreo().trim().toLowerCase();
        String contrasena = usuario.getContrasena() == null ? "" : usuario.getContrasena();
        if (nombre.isEmpty() || !correo.contains("@")) {
            throw new IllegalArgumentException("Escribe tu nombre y un correo válido.");
        }
        String problema = com.proaula.spring.synergy.Security.PoliticaContrasena.validar(contrasena);
        if (problema != null) {
            throw new IllegalArgumentException(problema);
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

        return respuesta(usuarioGuardado);
    }

    public void cambiarContrasena(String usuarioId, String contrasenaActual, String nuevaContrasena) {
        Usuarios usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Verificar contraseña actual
        if (contrasenaActual == null || !passwordEncoder.matches(contrasenaActual, usuario.getContrasena())) {
            throw new IllegalArgumentException("Contraseña actual incorrecta");
        }
        String problema = com.proaula.spring.synergy.Security.PoliticaContrasena.validar(nuevaContrasena);
        if (problema != null) {
            throw new IllegalArgumentException(problema);
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
