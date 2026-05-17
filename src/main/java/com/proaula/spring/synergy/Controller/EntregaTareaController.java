package com.proaula.spring.synergy.Controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Model.EntregaTarea;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Service.EntregaTareaService;
import com.proaula.spring.synergy.Service.TareaService;
import com.proaula.spring.synergy.Service.UsuarioService;

import java.security.Principal;

@Controller
@RequestMapping("/tareas")
public class EntregaTareaController {

    @Autowired
    private EntregaTareaService entregaTareaService;

    @Autowired
    private TareaService tareaService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/entregar/{id}")
    public String mostrarFormularioEntrega(
            @PathVariable String id,
            Principal principal,
            Model model) {

        if (principal == null) return "redirect:/login";

        Tarea tarea = tareaService.obtenerTareaPorId(id);
        if (tarea == null) {
            model.addAttribute("error", "La tarea no existe.");
            return "redirect:/tareas/buzon";
        }

        model.addAttribute("tarea", tarea);
        return "Formulario_Entrega_Tarea";
    }

    @PostMapping("/entregar/{id}")
    public String procesarEntrega(
            @PathVariable String id,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "descripcionEvidencia", required = false) String descripcionEvidencia,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        Tarea tarea = tareaService.obtenerTareaPorId(id);
        model.addAttribute("tarea", tarea);

        if (tarea == null) {
            model.addAttribute("error", "La tarea no existe.");
            return "Formulario_Entrega_Tarea";
        }

        String idUsuario = null;
        if (principal != null) {
            Usuarios usuario = usuarioService.buscarPorCorreo(principal.getName());
            if (usuario != null) idUsuario = usuario.getId();
        }

        try {
            entregaTareaService.guardarEntrega(id, idUsuario, archivo, descripcionEvidencia);
            tareaService.marcarComoCompletada(id);
        } catch (IOException e) {
            model.addAttribute("error", "Error al enviar el archivo: " + e.getMessage());
            return "Formulario_Entrega_Tarea";
        }

        redirectAttributes.addFlashAttribute("mensajeExito",
                "✅ Tu tarea \"" + tarea.getTitulo() + "\" ha sido enviada correctamente.");
        return "redirect:/tareas/buzon";
    }

    @GetMapping("/entrega/{id}/imagen")
    public ResponseEntity<byte[]> verImagen(@PathVariable String id) {
        EntregaTarea entrega = entregaTareaService.obtenerEntregaPorId(id);
        if (entrega == null || entrega.getDatos() == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType tipo = MediaType.IMAGE_JPEG;
        if (entrega.getTipoArchivo() != null) {
            try { tipo = MediaType.parseMediaType(entrega.getTipoArchivo()); } catch (Exception ignored) {}
        }
        return new ResponseEntity<>(entrega.getDatos(), buildHeaders(tipo), HttpStatus.OK);
    }

    private org.springframework.http.HttpHeaders buildHeaders(MediaType tipo) {
        org.springframework.http.HttpHeaders h = new org.springframework.http.HttpHeaders();
        h.setContentType(tipo);
        return h;
    }
}
