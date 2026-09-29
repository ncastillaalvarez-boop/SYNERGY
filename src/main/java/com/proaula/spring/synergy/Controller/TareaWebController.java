package com.proaula.spring.synergy.Controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.ProyectoRepository;
import com.proaula.spring.synergy.Repository.UsuarioRepository;
import com.proaula.spring.synergy.Security.SesionUtil;
import com.proaula.spring.synergy.Service.HistorialService;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.TareaService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/tareas")
public class TareaWebController {

    private static final Set<String> ESTADOS_TAREA = Set.of("PENDIENTE", "EN_PROGRESO", "COMPLETADA", "CANCELADA");

    @Autowired
    private TareaService tareaService;

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HistorialService historialService;

    private Proyecto proyectoDelLider(String idProyecto, HttpSession session) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (!SesionUtil.esLider(usuario)) {
            return null;
        }
        Proyecto proyecto = proyectoService.buscarPorId(idProyecto);
        if (proyecto == null || !Objects.equals(proyecto.getIdLider(), usuario.getId())) {
            return null;
        }
        return proyecto;
    }

    // ============================================
    // FORMULARIO: NUEVA TAREA (LÍDER)
    // ============================================
    @GetMapping("/asignar/{idProyecto}")
    public String asignarTarea(@PathVariable String idProyecto, HttpSession session, Model model) {
        Proyecto proyecto = proyectoDelLider(idProyecto, session);
        if (proyecto == null) {
            return SesionUtil.usuario(session) == null ? "redirect:/login" : "redirect:/lider/tareas/asignar";
        }

        // Integrantes del proyecto (sin el propio líder)
        List<Usuarios> integrantes = new ArrayList<>();
        for (String uid : proyecto.getParticipantes()) {
            if (!uid.equals(proyecto.getIdLider())) {
                usuarioRepository.findById(uid).ifPresent(integrantes::add);
            }
        }

        Map<String, String> nombres = new HashMap<>();
        integrantes.forEach(u -> nombres.put(u.getId(), u.getNombre()));

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("integrantes", integrantes);
        model.addAttribute("nombres", nombres);
        model.addAttribute("tareas", tareaService.obtenerTareasPorProyecto(idProyecto));
        model.addAttribute("hoy", LocalDate.now());
        return "Lider_Nueva_Tarea";
    }

    // ============================================
    // GUARDAR TAREA
    // ============================================
    @PostMapping("/guardar")
    public String guardarTarea(@RequestParam String proyectoId,
                               @RequestParam String titulo,
                               @RequestParam(required = false) String descripcion,
                               @RequestParam("fechaEntrega") String fechaEntregaStr,
                               @RequestParam(value = "usuariosSeleccionados", required = false) List<String> usuariosIds,
                               HttpSession session,
                               RedirectAttributes ra) {

        Proyecto proyecto = proyectoDelLider(proyectoId, session);
        if (proyecto == null) {
            return "redirect:/login";
        }

        if (titulo == null || titulo.isBlank()) {
            ra.addFlashAttribute("error", "La tarea necesita un título");
            return "redirect:/tareas/asignar/" + proyectoId;
        }

        LocalDate fecha;
        try {
            fecha = LocalDate.parse(fechaEntregaStr);
        } catch (DateTimeParseException | NullPointerException e) {
            ra.addFlashAttribute("error", "Fecha de entrega no válida");
            return "redirect:/tareas/asignar/" + proyectoId;
        }

        // Solo se pueden asignar integrantes del proyecto
        List<String> asignados = new ArrayList<>();
        if (usuariosIds != null) {
            for (String uid : usuariosIds) {
                if (proyecto.getParticipantes().contains(uid)) {
                    asignados.add(uid);
                }
            }
        }

        Tarea tarea = new Tarea(titulo.trim(), descripcion == null ? "" : descripcion.trim(),
                "PENDIENTE", fecha, proyecto.getId());
        tarea = tareaService.guardarTarea(tarea, asignados);

        // Mantener las referencias de MongoDB sincronizadas
        proyecto.addTarea(tarea.getId());
        proyectoRepository.save(proyecto);
        for (String uid : asignados) {
            Tarea guardada = tarea;
            usuarioRepository.findById(uid).ifPresent(u -> {
                u.addTareaAsignada(guardada.getId());
                usuarioRepository.save(u);
            });
        }

        Usuarios lider = SesionUtil.usuario(session);
        historialService.registrarAccion("CREAR", "TAREA", "Tarea creada: " + tarea.getTitulo(), lider.getCorreo());
        ra.addFlashAttribute("mensaje", "Tarea '" + tarea.getTitulo() + "' asignada");
        return "redirect:/tareas/asignar/" + proyectoId;
    }

    /** El líder elimina una tarea de su proyecto. */
    @PostMapping("/{idTarea}/eliminar")
    public String eliminarTarea(@PathVariable String idTarea, HttpSession session, RedirectAttributes ra) {
        Tarea tarea = tareaService.obtenerTareaPorId(idTarea);
        if (tarea == null) {
            return "redirect:/lider/tareas/asignar";
        }
        Proyecto proyecto = proyectoDelLider(tarea.getIdProyecto(), session);
        if (proyecto == null) {
            return "redirect:/login";
        }
        tareaService.eliminar(idTarea);
        proyecto.getTareas().remove(idTarea);
        proyectoRepository.save(proyecto);
        ra.addFlashAttribute("mensaje", "Tarea eliminada");
        return "redirect:/tareas/asignar/" + proyecto.getId();
    }

    // ============================================
    // BUZÓN (ruta antigua con id en la URL)
    // ============================================
    @GetMapping("/buzon/{idUsuario}")
    public String buzon(@PathVariable String idUsuario) {
        return "redirect:/tareas/buzon";
    }

    // ============================================
    // CAMBIO DE ESTADO (el participante asignado)
    // ============================================
    @PostMapping("/estado/{idTarea}")
    public String actualizarEstado(@PathVariable String idTarea,
                                   @RequestParam String estado,
                                   HttpSession session,
                                   RedirectAttributes ra) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        Tarea tarea = tareaService.obtenerTareaPorId(idTarea);
        String nuevo = estado == null ? "" : estado.trim().toUpperCase();
        if (tarea != null && ESTADOS_TAREA.contains(nuevo)
                && tarea.getUsuariosAsignados().contains(usuario.getId())) {
            tareaService.cambiarEstado(idTarea, nuevo);
            ra.addFlashAttribute("mensaje", "Estado actualizado");
        }
        return "redirect:/tareas/buzon";
    }
}
