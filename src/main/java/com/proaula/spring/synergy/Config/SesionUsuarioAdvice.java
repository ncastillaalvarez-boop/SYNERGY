package com.proaula.spring.synergy.Config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Map;

import com.proaula.spring.synergy.Model.Catalogos;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Security.SesionUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Expone el usuario de la sesión a TODAS las vistas web como "usuarioActual"
 * y su rol normalizado como "rolActual".
 * Lo usa el menú lateral (fragments/sidebar.html) para mostrar las opciones de cada rol.
 * No crea sesiones nuevas (importante para la API REST de la app móvil).
 */
@ControllerAdvice(annotations = Controller.class)
public class SesionUsuarioAdvice {

    private final com.proaula.spring.synergy.Service.NotificacionService notificaciones;

    public SesionUsuarioAdvice(com.proaula.spring.synergy.Service.NotificacionService notificaciones) {
        this.notificaciones = notificaciones;
    }

    /** Número de avisos sin leer para la campana del menú lateral. */
    @ModelAttribute("notificacionesNoLeidas")
    public long notificacionesNoLeidas(HttpServletRequest request) {
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            return 0;
        }
        HttpSession session = request.getSession(false);
        Usuarios u = session == null ? null : SesionUtil.usuario(session);
        return u == null ? 0 : notificaciones.contarNoLeidas(u.getId());
    }

    @ModelAttribute("usuarioActual")
    public Usuarios usuarioActual(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : SesionUtil.usuario(session);
    }

    @ModelAttribute("rolActual")
    public String rolActual(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? "" : SesionUtil.rol(SesionUtil.usuario(session));
    }

    /** Ruta del panel del usuario en sesión (para "Ir a mi panel" en la portada y el login). */
    @ModelAttribute("panelActual")
    public String panelActual(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Usuarios u = session == null ? null : SesionUtil.usuario(session);
        return u == null ? null : SesionUtil.panelPorRol(u);
    }

    // ── Listas para formularios y filtros (ver Model/Catalogos) ──
    @ModelAttribute("estadosProyecto")
    public Map<String, String> estadosProyecto() {
        return Catalogos.ESTADOS_PROYECTO;
    }

    @ModelAttribute("categorias")
    public List<String> categorias() {
        return Catalogos.CATEGORIAS;
    }

    @ModelAttribute("organizaciones")
    public List<String> organizaciones() {
        return Catalogos.ORGANIZACIONES;
    }

    @ModelAttribute("barrios")
    public List<String> barrios() {
        return Catalogos.BARRIOS;
    }

    @ModelAttribute("audiencias")
    public List<String> audiencias() {
        return Catalogos.AUDIENCIAS;
    }

    @ModelAttribute("ordenesProyecto")
    public Map<String, String> ordenesProyecto() {
        return Catalogos.ORDENES_PROYECTO;
    }
}
