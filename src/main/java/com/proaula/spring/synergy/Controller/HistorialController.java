package com.proaula.spring.synergy.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Security.SesionUtil;
import com.proaula.spring.synergy.Service.HistorialService;
import com.proaula.spring.synergy.Service.HistorialService.AccionHistorial;

import jakarta.servlet.http.HttpSession;

/**
 * Historial de acciones (estructura de datos tipo pila: deshacer / rehacer).
 * Solo lo ve el administrador.
 */
@Controller
@RequestMapping("/historial")
public class HistorialController {

    @Autowired
    private HistorialService historialService;

    private boolean noEsAdmin(HttpSession session) {
        return !SesionUtil.esAdmin(SesionUtil.usuario(session));
    }

    @GetMapping
    public String verHistorial(HttpSession session, Model model) {
        if (noEsAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("historial", historialService.obtenerHistorialCompleto());
        model.addAttribute("totalAcciones", historialService.contarAcciones());
        model.addAttribute("hayDeshacer", historialService.hayAccionesParaDeshacer());
        model.addAttribute("hayRehacer", historialService.hayAccionesParaRehacer());
        return "Historial";
    }

    @PostMapping("/deshacer")
    public String deshacer(HttpSession session, RedirectAttributes ra) {
        if (noEsAdmin(session)) {
            return "redirect:/login";
        }
        AccionHistorial accion = historialService.deshacer();
        ra.addFlashAttribute("mensaje", accion != null
                ? "Acción deshecha: " + accion.getDescripcion()
                : "No hay acciones para deshacer");
        return "redirect:/historial";
    }

    @PostMapping("/rehacer")
    public String rehacer(HttpSession session, RedirectAttributes ra) {
        if (noEsAdmin(session)) {
            return "redirect:/login";
        }
        AccionHistorial accion = historialService.rehacer();
        ra.addFlashAttribute("mensaje", accion != null
                ? "Acción rehecha: " + accion.getDescripcion()
                : "No hay acciones para rehacer");
        return "redirect:/historial";
    }

    @PostMapping("/limpiar")
    public String limpiarHistorial(HttpSession session, RedirectAttributes ra) {
        if (noEsAdmin(session)) {
            return "redirect:/login";
        }
        historialService.limpiarHistorial();
        ra.addFlashAttribute("mensaje", "Historial limpiado");
        return "redirect:/historial";
    }

    // Antes estos dos endpoints JSON no pedían sesión: cualquiera veía quién hizo qué.
    @GetMapping("/ultimas/{cantidad}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<List<AccionHistorial>> obtenerUltimas(@PathVariable int cantidad, HttpSession session) {
        if (noEsAdmin(session)) {
            return org.springframework.http.ResponseEntity.status(403).build();
        }
        return org.springframework.http.ResponseEntity.ok(historialService.obtenerUltimasAcciones(Math.max(0, Math.min(cantidad, 500))));
    }

    @GetMapping("/tipo/{tipo}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<List<AccionHistorial>> filtrarPorTipo(@PathVariable String tipo, HttpSession session) {
        if (noEsAdmin(session)) {
            return org.springframework.http.ResponseEntity.status(403).build();
        }
        return org.springframework.http.ResponseEntity.ok(historialService.filtrarPorTipo(tipo));
    }
}
