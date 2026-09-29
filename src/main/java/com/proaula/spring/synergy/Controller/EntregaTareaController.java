package com.proaula.spring.synergy.Controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Security.SesionUtil;
import com.proaula.spring.synergy.Service.EntregaTareaService;
import com.proaula.spring.synergy.Service.TareaService;

import jakarta.servlet.http.HttpSession;

/**
 * Entrega de tareas por parte del participante asignado.
 */
@Controller
@RequestMapping("/tareas")
public class EntregaTareaController {

    @Autowired
    private EntregaTareaService entregaTareaService;

    @Autowired
    private TareaService tareaService;

    /** Devuelve la tarea solo si está asignada al usuario de la sesión. */
    private Tarea tareaAsignada(String id, Usuarios usuario) {
        Tarea tarea = tareaService.obtenerTareaPorId(id);
        if (tarea == null || usuario == null || !tarea.getUsuariosAsignados().contains(usuario.getId())) {
            return null;
        }
        return tarea;
    }

    @GetMapping("/entregar/{id}")
    public String mostrarFormularioEntrega(@PathVariable String id, HttpSession session,
                                           Model model, RedirectAttributes ra) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        Tarea tarea = tareaAsignada(id, usuario);
        if (tarea == null) {
            ra.addFlashAttribute("error", "La tarea no existe o no está asignada a ti.");
            return "redirect:/tareas/buzon";
        }
        model.addAttribute("tarea", tarea);
        return "Formulario_Entrega_Tarea";
    }

    @PostMapping("/entregar/{id}")
    public String procesarEntrega(@PathVariable String id,
                                  @RequestParam("archivo") MultipartFile archivo,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes ra) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        Tarea tarea = tareaAsignada(id, usuario);
        if (tarea == null) {
            ra.addFlashAttribute("error", "La tarea no existe o no está asignada a ti.");
            return "redirect:/tareas/buzon";
        }
        model.addAttribute("tarea", tarea);

        if (archivo == null || archivo.isEmpty()) {
            model.addAttribute("error", "Selecciona un archivo para entregar.");
            return "Formulario_Entrega_Tarea";
        }

        try {
            entregaTareaService.guardarEntrega(id, usuario.getId(), archivo);
            tareaService.marcarComoCompletada(id);
        } catch (IOException e) {
            model.addAttribute("error", "No se pudo guardar el archivo: " + e.getMessage());
            return "Formulario_Entrega_Tarea";
        }

        ra.addFlashAttribute("mensaje", "Tarea '" + tarea.getTitulo() + "' entregada correctamente");
        return "redirect:/tareas/buzon";
    }
}
