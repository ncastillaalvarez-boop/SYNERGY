package com.proaula.spring.synergy.Security;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.proaula.spring.synergy.Model.Catalogos;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;

/**
 * Reglas de acceso de la API REST en un solo lugar (antes cada controlador las repetía y
 * algunos endpoints no revisaban nada: cualquier usuario veía las tareas o los proyectos
 * pendientes de otros cambiando el id en la URL).
 *
 *  - Ver un proyecto: si es público (ACTIVO o COMPLETADO) o si eres integrante.
 *  - Integrante: administrador, líder del proyecto o voluntario inscrito.
 *  - Gestionar: administrador o líder del proyecto.
 */
public final class ApiAcceso {

    private ApiAcceso() {
    }

    public static Usuarios usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth == null ? null : auth.getPrincipal();
        if (principal instanceof Usuarios usuario) {
            return usuario;
        }
        throw new AccessDeniedException("No hay un usuario autenticado");
    }

    public static boolean esAdmin(Usuarios u) {
        return SesionUtil.esAdmin(u);
    }

    public static boolean esLiderDe(Proyecto p, Usuarios u) {
        return p != null && u != null && Objects.equals(p.getIdLider(), u.getId());
    }

    public static boolean gestiona(Proyecto p, Usuarios u) {
        return esAdmin(u) || esLiderDe(p, u);
    }

    public static boolean esIntegrante(Proyecto p, Usuarios u) {
        return gestiona(p, u) || (p != null && u != null && p.getParticipantes() != null
                && p.getParticipantes().contains(u.getId()));
    }

    public static boolean esPublico(Proyecto p) {
        return p != null && p.getEstado() != null && Catalogos.ESTADOS_PUBLICOS.contains(p.getEstado().toUpperCase());
    }

    public static boolean puedeVer(Proyecto p, Usuarios u) {
        return esPublico(p) || esIntegrante(p, u);
    }

    public static boolean puedeVerTarea(Tarea t, Proyecto p, Usuarios u) {
        return esIntegrante(p, u) || (t != null && u != null && t.getUsuariosAsignados() != null
                && t.getUsuariosAsignados().contains(u.getId()));
    }

    /** 404 en vez de 403 para no revelar que existe un proyecto privado. */
    public static void exigirVer(Proyecto p) {
        if (!puedeVer(p, usuarioActual())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        }
    }

    public static void exigirIntegrante(Proyecto p, String mensaje) {
        if (!esIntegrante(p, usuarioActual())) {
            throw new AccessDeniedException(mensaje);
        }
    }

    public static void exigirGestion(Proyecto p, String mensaje) {
        if (!gestiona(p, usuarioActual())) {
            throw new AccessDeniedException(mensaje);
        }
    }
}
