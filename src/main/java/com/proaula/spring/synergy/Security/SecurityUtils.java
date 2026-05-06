package com.proaula.spring.synergy.Security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.proaula.spring.synergy.Model.Usuarios;

public class SecurityUtils {

    /**
     * Obtiene el usuario autenticado actual desde el SecurityContext (JWT).
     * Reemplaza el viejo session.getAttribute("usuario").
     * 
     * @return Usuarios autenticado, o null si no hay nadie autenticado
     */
    public static Usuarios getUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        // El principal puede ser "anonymousUser" si no hay JWT
        Object principal = auth.getPrincipal();
        if (principal instanceof Usuarios) {
            return (Usuarios) principal;
        }

        return null;
    }

    /**
     * Verifica si hay un usuario autenticado.
     */
    public static boolean estaAutenticado() {
        return getUsuarioActual() != null;
    }
}
