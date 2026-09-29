package com.proaula.spring.synergy.Security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.UsuarioRepository;
import com.proaula.spring.synergy.Service.HistorialService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Se ejecuta cuando Google confirma la identidad del usuario.
 *
 * - Si el correo ya existe en Synergy, inicia sesión con esa cuenta (conserva su rol).
 * - Si no existe, crea una cuenta nueva con rol PARTICIPANTE (voluntario).
 *   Si el usuario venía desde "Registrarme como líder", se crea con rol LIDER.
 *
 * Después guarda el usuario en la sesión web igual que el login normal.
 */
@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {

    /** Clave de sesión que marca que el registro con Google es para un líder. */
    public static final String ATTR_ROL_GOOGLE = "rolRegistroGoogle";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final HistorialService historialService;

    public GoogleLoginSuccessHandler(UsuarioRepository usuarioRepository,
                                     PasswordEncoder passwordEncoder,
                                     HistorialService historialService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.historialService = historialService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User googleUser = (OAuth2User) authentication.getPrincipal();
        String correo = googleUser.getAttribute("email");
        String nombre = googleUser.getAttribute("name");
        String foto = googleUser.getAttribute("picture");
        Boolean verificado = googleUser.getAttribute("email_verified");

        if (correo == null || Boolean.FALSE.equals(verificado)) {
            response.sendRedirect(request.getContextPath() + "/login?error=google");
            return;
        }
        correo = correo.trim().toLowerCase();

        HttpSession session = request.getSession();
        Usuarios usuario = usuarioRepository.findByCorreo(correo);

        if (usuario == null) {
            Object rolPedido = session.getAttribute(ATTR_ROL_GOOGLE);
            String rol = "LIDER".equals(rolPedido) ? "LIDER" : "PARTICIPANTE";

            usuario = new Usuarios();
            usuario.setNombre(nombre != null ? nombre : correo);
            usuario.setCorreo(correo);
            // Contraseña aleatoria: la cuenta solo se usa con Google hasta que el usuario
            // defina una contraseña propia en "Mis credenciales".
            usuario.setContrasena(passwordEncoder.encode(UUID.randomUUID().toString()));
            usuario.setRol(rol);
            usuario.setActivo(true);
            usuario.setEstado("ACTIVO");
            usuario.setProveedor("GOOGLE");
            usuario.setFechaCreacion(LocalDateTime.now());
            historialService.registrarAccion("CREAR", "USUARIO",
                    "Registro con Google: " + correo + " (" + rol + ")", correo);
        } else if (!usuario.isEnabled()) {
            response.sendRedirect(request.getContextPath() + "/login?error=inactivo");
            return;
        }

        session.removeAttribute(ATTR_ROL_GOOGLE);
        if (foto != null) {
            usuario.setFotoUrl(foto);
        }
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuario = usuarioRepository.save(usuario);

        SesionUtil.iniciar(session, usuario);
        response.sendRedirect(request.getContextPath() + SesionUtil.panelPorRol(usuario));
    }
}
