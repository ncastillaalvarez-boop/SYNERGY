package com.proaula.spring.synergy.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Model.Notificacion;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Security.ApiAcceso;
import com.proaula.spring.synergy.Security.SesionUtil;
import com.proaula.spring.synergy.Service.NotificacionService;

import jakarta.servlet.http.HttpSession;

/** Campana de notificaciones (web) y su versión JSON para la app móvil. */
@Controller
public class NotificacionController {

    private final NotificacionService notificaciones;

    public NotificacionController(NotificacionService notificaciones) {
        this.notificaciones = notificaciones;
    }

    @GetMapping("/notificaciones")
    public String ver(HttpSession session, Model model) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        List<Notificacion> lista = notificaciones.recientes(usuario.getId(), 60);
        model.addAttribute("notificaciones", lista);
        model.addAttribute("usuario", usuario);
        return "Notificaciones";
    }

    /** Abre el enlace del aviso y lo marca como leído. */
    @GetMapping("/notificaciones/{id}/abrir")
    public String abrir(@PathVariable String id, HttpSession session) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        String enlace = notificaciones.marcarLeida(id, usuario.getId());
        // Solo rutas internas (evita redirecciones a otros sitios)
        if (enlace == null || !enlace.startsWith("/") || enlace.startsWith("//") || enlace.contains("\\")) {
            return "redirect:/notificaciones";
        }
        return "redirect:" + enlace;
    }

    @PostMapping("/notificaciones/leidas")
    public String marcarLeidas(HttpSession session, RedirectAttributes ra) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        notificaciones.marcarTodasLeidas(usuario.getId());
        ra.addFlashAttribute("mensaje", "Todas las notificaciones quedaron como leídas");
        return "redirect:/notificaciones";
    }

    // ----------------- API (app móvil) -----------------

    @GetMapping("/api/notificaciones")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> listarApi() {
        Usuarios u = ApiAcceso.usuarioActual();
        return Map.of("noLeidas", notificaciones.contarNoLeidas(u.getId()),
                "notificaciones", notificaciones.recientes(u.getId(), 50));
    }

    @PostMapping("/api/notificaciones/leidas")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> marcarLeidasApi() {
        notificaciones.marcarTodasLeidas(ApiAcceso.usuarioActual().getId());
        return ResponseEntity.ok(Map.of("message", "Notificaciones marcadas como leídas"));
    }
}
