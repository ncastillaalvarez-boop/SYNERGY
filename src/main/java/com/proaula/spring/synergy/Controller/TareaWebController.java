package com.proaula.spring.synergy.Controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.TareaService;
import com.proaula.spring.synergy.Service.UsuarioService;
import com.proaula.spring.synergy.Service.ParticipacionService;

@Controller
@RequestMapping("/tareas")
public class TareaWebController {

    @Autowired
    private TareaService tareaService;

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ParticipacionService participacionService;


    // ============================================
    //              PÁGINA DEL LÍDER
    // ============================================
    @GetMapping("/asignar/{idProyecto}")
    public String asignarTarea(@PathVariable String idProyecto, Model model) {

        Proyecto proyecto = proyectoService.buscarPorId(idProyecto);
        List<Usuarios> usuarios = participacionService.listarUsuariosPorProyecto(idProyecto);

        // Tareas ya existentes del proyecto
        List<Tarea> tareas = tareaService.obtenerTareasPorProyecto(idProyecto);

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("tarea", new Tarea());
        model.addAttribute("tareas", tareas);

        return "Lider_Asignar_Tareas";
    }


    // ============================================
    //                 GUARDAR TAREA
    // ============================================
    @PostMapping("/guardar")
    public String guardarTarea(
            @ModelAttribute Tarea tarea,
            @RequestParam String proyectoId,
            @RequestParam(value = "usuariosSeleccionados", required = false) List<String> usuariosIds,
            @RequestParam("fechaEntrega") String fechaEntregaStr,
            @RequestParam(value = "subtareasTitulo", required = false) List<String> subtareasTitulos,
            RedirectAttributes redirectAttributes) {

        Proyecto proyecto = proyectoService.buscarPorId(proyectoId);
        tarea.setIdProyecto(proyecto.getId());

        LocalDate fechaEntrega = LocalDate.parse(fechaEntregaStr);
        tarea.setFechaEntrega(fechaEntrega);

        // Validar que la fecha de entrega no supere la fecha fin del proyecto
        if (proyecto.getFechaFin() != null && fechaEntrega.isAfter(proyecto.getFechaFin())) {
            redirectAttributes.addFlashAttribute("errorFecha",
                    "La fecha de entrega (" + fechaEntrega + ") supera la fecha límite del proyecto ("
                    + proyecto.getFechaFin() + "). Por favor elige una fecha válida.");
            return "redirect:/lider/proyecto/" + proyectoId + "/detalle";
        }

        // Estado por defecto
        tarea.setEstado("PENDIENTE");

        // Subtareas
        if (subtareasTitulos != null) {
            List<com.proaula.spring.synergy.Model.Tarea.Subtarea> subtareas = subtareasTitulos.stream()
                .filter(t -> t != null && !t.trim().isEmpty())
                .map(com.proaula.spring.synergy.Model.Tarea.Subtarea::new)
                .collect(java.util.stream.Collectors.toList());
            tarea.setSubtareas(subtareas);
        }

        tareaService.guardarTarea(tarea, usuariosIds);

        return "redirect:/lider/proyecto/" + proyectoId + "/detalle";
    }

    // ============================================
    //               ELIMINAR TAREA
    // ============================================
    @PostMapping("/{id}/eliminar")
    public String eliminarTarea(
            @PathVariable String id,
            @RequestParam String proyectoId) {
        tareaService.eliminar(id);
        return "redirect:/lider/proyecto/" + proyectoId + "/detalle";
    }

    // ============================================
    //       TOGGLE SUBTAREA (vista del líder)
    // ============================================
    @PostMapping("/{id}/subtarea/{subtareaId}/toggle")
    public String toggleSubtarea(
            @PathVariable String id,
            @PathVariable String subtareaId,
            @RequestParam String proyectoId) {

        toggleSubtareaLogic(id, subtareaId);
        return "redirect:/lider/proyecto/" + proyectoId + "/detalle";
    }

    // ============================================
    //       TOGGLE SUBTAREA (vista del usuario)
    // ============================================
    @PostMapping("/{id}/subtarea/{subtareaId}/toggle-usuario")
    public String toggleSubtareaUsuario(
            @PathVariable String id,
            @PathVariable String subtareaId,
            @RequestParam(required = false) String proyectoId) {

        toggleSubtareaLogic(id, subtareaId);
        String redirect = "/tareas/buzon";
        if (proyectoId != null && !proyectoId.isEmpty()) {
            redirect += "?proyectoId=" + proyectoId;
        }
        return "redirect:" + redirect;
    }

    private void toggleSubtareaLogic(String tareaId, String subtareaId) {
        Tarea tarea = tareaService.obtenerTareaPorId(tareaId);
        if (tarea != null && tarea.getSubtareas() != null) {
            tarea.getSubtareas().stream()
                .filter(s -> subtareaId.equals(s.getId()))
                .findFirst()
                .ifPresent(s -> s.setCompletada(!s.isCompletada()));
            tareaService.guardarTarea(tarea, null);
        }
    }


    // ============================================
    //                     BUZÓN
    // ============================================
    @GetMapping("/buzon/{idUsuario}")
    public String buzon(@PathVariable String idUsuario, Model model) {

        List<Tarea> tareas = tareaService.obtenerTareasPorUsuario(idUsuario);

        Usuarios usuario = null;
        try {
            usuario = usuarioService.obtenerUsuarioPorId(idUsuario);
        } catch (Exception e) {}

        model.addAttribute("usuario", usuario);
        model.addAttribute("tareas", tareas);

        return "Usuarios_Buzon_Tareas";
    }


    // ============================================
    //            CAMBIO DE ESTADO
    // ============================================
    @PostMapping("/estado/{idTarea}")
    public String actualizarEstado(
            @PathVariable String idTarea,
            @RequestParam String estado,
            @RequestParam String idUsuario) {

        tareaService.cambiarEstado(idTarea, estado);
        return "redirect:/tareas/buzon/" + idUsuario;
    }

    
}
