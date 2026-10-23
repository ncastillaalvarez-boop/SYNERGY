package com.proaula.spring.synergy.Security;

import java.util.Locale;

import com.proaula.spring.synergy.Model.Usuarios;

import jakarta.servlet.http.HttpSession;

/**
 * Utilidades para la sesión web (Thymeleaf).
 * La web guarda el usuario autenticado en la sesión con la clave "usuario".
 */
public final class SesionUtil {

    public static final String ATTR_USUARIO = "usuario";
    public static final String ATTR_USUARIO_ID = "usuarioId";
    /** Página a la que se vuelve después de registrarse o iniciar sesión (p. ej. la lista de proyectos). */
    public static final String ATTR_VOLVER = "volverTrasLogin";

    /** Rutas internas a las que se permite volver (evita redirecciones a otros sitios). */
    private static final String[] RUTAS_VOLVER = {"/publico/proyectos", "/proyectos/participar", "/proyectos/publico"};

    private SesionUtil() {
    }

    public static Usuarios usuario(HttpSession session) {
        Object valor = session.getAttribute(ATTR_USUARIO);
        return valor instanceof Usuarios u ? u : null;
    }

    public static void iniciar(HttpSession session, Usuarios usuario) {
        session.setAttribute(ATTR_USUARIO, usuario);
        session.setAttribute(ATTR_USUARIO_ID, usuario.getId());
    }

    /** Rol normalizado en mayúsculas (ADMINISTRADOR, LIDER, PARTICIPANTE). */
    public static String rol(Usuarios usuario) {
        if (usuario == null || usuario.getRol() == null) {
            return "";
        }
        String rol = usuario.getRol().trim().toUpperCase(Locale.ROOT);
        return switch (rol) {
            case "ADMIN", "ADMINISTRADOR_ROL" -> "ADMINISTRADOR";
            case "LIDER_ROL" -> "LIDER";
            case "USUARIO", "PARTICIPANTE_ROL", "VOLUNTARIO" -> "PARTICIPANTE";
            default -> rol;
        };
    }

    public static boolean esAdmin(Usuarios usuario) {
        return "ADMINISTRADOR".equals(rol(usuario));
    }

    public static boolean esLider(Usuarios usuario) {
        return "LIDER".equals(rol(usuario));
    }

    /** Panel de inicio según el rol del usuario. */
    public static String panelPorRol(Usuarios usuario) {
        return switch (rol(usuario)) {
            case "ADMINISTRADOR" -> "/admin/proyectos";
            case "LIDER" -> "/lider/dashboard";
            case "PARTICIPANTE" -> "/dashboard";
            default -> "/login";
        };
    }

    /**
     * Devuelve la ruta solo si es una página interna permitida (lista de proyectos con sus filtros);
     * cualquier otra cosa (URL externa, "//", "\\") devuelve null.
     */
    public static String rutaVolverSegura(String ruta) {
        if (ruta == null || ruta.isBlank() || ruta.length() > 500) {
            return null;
        }
        String r = ruta.trim();
        if (!r.startsWith("/") || r.startsWith("//") || r.contains("\\") || r.contains("\r") || r.contains("\n")) {
            return null;
        }
        for (String permitida : RUTAS_VOLVER) {
            if (r.equals(permitida) || r.startsWith(permitida + "?")) {
                return r;
            }
        }
        return null;
    }

    /** Destino después de entrar: la página guardada en la sesión (si es segura) o el panel del rol. */
    public static String destinoTrasEntrar(HttpSession session, Usuarios usuario, String volver) {
        String destino = rutaVolverSegura(volver);
        if (destino == null) {
            Object guardado = session.getAttribute(ATTR_VOLVER);
            destino = rutaVolverSegura(guardado == null ? null : guardado.toString());
        }
        session.removeAttribute(ATTR_VOLVER);
        return destino != null ? destino : panelPorRol(usuario);
    }
}
