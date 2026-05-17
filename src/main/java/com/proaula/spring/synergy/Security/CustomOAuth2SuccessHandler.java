package com.proaula.spring.synergy.Security;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Service.UsuarioService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtCookieService jwtCookieService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String correo = oAuth2User.getAttribute("email");
        String nombre = oAuth2User.getAttribute("name");

        Usuarios usuario = usuarioService.buscarPorCorreo(correo);

        if (usuario == null) {
            // Register new volunteer (PARTICIPANTE)
            usuario = new Usuarios();
            usuario.setNombre(nombre);
            usuario.setCorreo(correo);
            // Assign a random secure password since they login via Google
            usuario.setContrasena(passwordEncoder.encode(UUID.randomUUID().toString()));
            usuario.setRol("PARTICIPANTE");
            usuario.setEstado("ACTIVO");

            try {
                String idRol = usuarioService.obtenerIdRolPorNombre("PARTICIPANTE");
                usuario.setIdRol(idRol);
            } catch (Exception ignored) { }

            usuarioService.guardarUsuario(usuario);
        }

        // Generate JWT and set Cookie
        String token = jwtTokenProvider.generateToken(usuario.getCorreo(), usuario.getRol());
        jwtCookieService.addJwtCookie(response, token);

        // Redirect based on role
        String rol = usuario.getRol() != null ? usuario.getRol().toUpperCase() : "PARTICIPANTE";
        String redirectUrl = switch (rol) {
            case "ADMINISTRADOR", "ADMINISTRADOR_ROL", "ADMIN" -> "/admin/proyectos";
            case "LIDER", "LIDER_ROL" -> "/lider/dashboard";
            case "PARTICIPANTE", "PARTICIPANTE_ROL", "USUARIO" -> "/dashboard";
            default -> "/login?error=RoleInvalido";
        };

        response.sendRedirect(redirectUrl);
    }
}
