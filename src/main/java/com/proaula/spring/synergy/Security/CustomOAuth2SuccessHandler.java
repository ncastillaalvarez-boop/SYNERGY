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

    private static final String[] ROLES_BLOQUEADOS = { "LIDER", "LIDER_ROL", "ADMINISTRADOR", "ADMINISTRADOR_ROL", "ADMIN" };

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
        String googleId = oAuth2User.getAttribute("sub");

        Usuarios usuario = usuarioService.buscarPorCorreo(correo);

        if (usuario == null) {
            // Case A: email not in DB — register as new PARTICIPANTE
            usuario = new Usuarios();
            usuario.setNombre(nombre);
            usuario.setCorreo(correo);
            usuario.setContrasena(passwordEncoder.encode(UUID.randomUUID().toString()));
            usuario.setRol("PARTICIPANTE");
            usuario.setEstado("ACTIVO");
            usuario.setGoogleId(googleId);
            usuario.setAuthProvider("google");

            try {
                String idRol = usuarioService.obtenerIdRolPorNombre("PARTICIPANTE");
                usuario.setIdRol(idRol);
            } catch (Exception ignored) { }

            usuarioService.guardarUsuario(usuario);

        } else {
            // Existing user — reject if LIDER or ADMIN
            String rol = usuario.getRol() != null ? usuario.getRol().toUpperCase() : "";
            for (String bloqueado : ROLES_BLOQUEADOS) {
                if (bloqueado.equals(rol)) {
                    response.sendRedirect("/login?oauthError=true");
                    return;
                }
            }

            if (usuario.getGoogleId() == null) {
                // Case B: email exists with local account, no googleId yet — link accounts
                usuario.setGoogleId(googleId);
                usuario.setAuthProvider("both");
                usuarioService.guardarUsuario(usuario);
            }
            // Case C: googleId already set — just log in (no changes needed)
        }

        String token = jwtTokenProvider.generateToken(usuario.getCorreo(), usuario.getRol());
        jwtCookieService.addJwtCookie(response, token);

        response.sendRedirect("/dashboard");
    }
}
