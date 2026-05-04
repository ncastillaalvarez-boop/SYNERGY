package com.proaula.spring.synergy.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Security.SecurityUtils;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.UsuarioService;
import com.proaula.spring.synergy.Service.TareaService;

@Controller
@RequestMapping("/lider")
public class LiderController {

    @Autowired
    private TareaService tareaService;

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private UsuarioService usuarioService;

    // =======================================================
    // VALIDAR QUE EL USUARIO SEA LÍDER
    // =======================================================
    private Usuarios validarLider() {
        Usuarios usuario = SecurityUtils.getUsuarioActual();

        if (usuario == null)
            return null;

        if (!"LIDER".equalsIgnoreCase(usuario.getRol()))
            return null;

        return usuario;
    }

    // =======================================================
    // DASHBOARD DEL LÍDER
    // =======================================================
    @GetMapping("/dashboard")
    public String dashboardLider(Model model) {

        Usuarios lider = validarLider();
        if (lider == null)
            return "redirect:/login";

        List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(lider.getId());

        model.addAttribute("usuario", lider);
        model.addAttribute("proyectos", proyectos);

        return "Lider_Proyecto_Dashboard";
    }

    // =======================================================
    // FORMULARIO CREAR PROYECTO
    // =======================================================
    @GetMapping("/proyecto/nuevo")
    public String formularioNuevoProyecto(Model model) {

        Usuarios lider = validarLider();
        if (lider == null)
            return "redirect:/login";

        model.addAttribute("proyecto", new Proyecto());
        return "Lider_Crear_Proyecto";
    }

    // =======================================================
    // GUARDAR PROYECTO
    // =======================================================
    @PostMapping("/proyecto/guardar")
    public String guardarProyecto(@ModelAttribute Proyecto proyecto) {

        Usuarios lider = validarLider();
        if (lider == null)
            return "redirect:/login";

        proyecto.setIdLider(lider.getId());
        proyectoService.guardar(proyecto, null);

        return "redirect:/lider/dashboard";
    }

    // =======================================================
    // EDITAR PROYECTO
    // =======================================================
    @GetMapping("/proyecto/{id}/editar")
    public String editarProyecto(@PathVariable String id, Model model) {

        Usuarios lider = validarLider();
        if (lider == null)
            return "redirect:/login";

        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto == null || !proyecto.getIdLider().equals(lider.getId()))
            return "redirect:/lider/dashboard";

        model.addAttribute("proyecto", proyecto);

        return "Lider_Editar_Proyecto";
    }

    // =======================================================
    // ACTUALIZAR PROYECTO
    // =======================================================
    @PostMapping("/proyecto/{id}/actualizar")
    public String actualizarProyecto(@PathVariable String id,
                                     @ModelAttribute Proyecto proyecto) {

        Usuarios lider = validarLider();
        if (lider == null)
            return "redirect:/login";

        proyecto.setId(id);
        proyecto.setIdLider(lider.getId());

        proyectoService.guardar(proyecto, null);

        return "redirect:/lider/dashboard";
    }

    // =======================================================
    // ASIGNAR TAREAS
    // =======================================================
    @GetMapping("/tareas/asignar")
    public String asignarTareas(Model model) {

        Usuarios lider = validarLider();
        if (lider == null)
            return "redirect:/login";

        List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(lider.getId());
        model.addAttribute("proyectos", proyectos);

        List<Usuarios> usuarios = usuarioService.obtenerTodosUsuarios();
        model.addAttribute("usuarios", usuarios);

        return "Lider_Asignar_Tareas";
    }

    // VER USUARIOS INSCRITOS AL PROYECTO
    @GetMapping("/usuarios")
    public String usuariosInscritos(Model model) {

        Usuarios lider = validarLider();
        if (lider == null)
            return "redirect:/login";

        return "Lider_Usuarios_Proyecto";
    }

    // VER TAREAS RECIBIDAS
    @GetMapping("/tareas/recibidas")
    public String tareasRecibidas(Model model) {

        Usuarios lider = validarLider();
        if (lider == null)
            return "redirect:/login";

        return "Lider_Tareas_Recibidas";
    }

    // =======================================================
    // ELIMINAR PROYECTO
    // =======================================================
    @PostMapping("/proyecto/{id}/eliminar")
    public String eliminarProyecto(@PathVariable String id,
                                   RedirectAttributes redirectAttributes) {

        Usuarios lider = validarLider();
        if (lider == null)
            return "redirect:/login";

        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto == null || !proyecto.getIdLider().equals(lider.getId()))
            return "redirect:/lider/dashboard";

        proyectoService.eliminar(id);

        redirectAttributes.addFlashAttribute("popup", "success");
        redirectAttributes.addFlashAttribute("mensaje", "Proyecto eliminado correctamente.");

        return "redirect:/lider/dashboard";
    }

    // =======================================================
    // VER DETALLE DEL PROYECTO CON TAREAS
    // =======================================================
    @GetMapping("/proyecto/{id}/detalle")
    public String verDetalleProyecto(@PathVariable String id, Model model) {

        Usuarios lider = validarLider();
        if (lider == null)
            return "redirect:/login";

        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto == null || !proyecto.getIdLider().equals(lider.getId()))
            return "redirect:/lider/tareas/asignar";

        List<Tarea> tareas = tareaService.obtenerTareasPorProyecto(id);

        List<Usuarios> integrantes = proyecto.getParticipantes().stream()
            .map(uid -> usuarioService.obtenerUsuarioPorId(uid))
            .filter(u -> u != null)
            .collect(java.util.stream.Collectors.toList());

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("tareas", tareas);
        model.addAttribute("integrantes", integrantes);
        model.addAttribute("usuario", lider);

        return "Lider_Detalle_Proyecto";
    }

    @PostMapping("/proyecto/{id}/aceptar")
    public String aceptarProyecto(@PathVariable String id,
                                  RedirectAttributes redirectAttributes) {

        Usuarios lider = validarLider();
        if (lider == null)
            return "redirect:/login";

        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto != null) {
            proyecto.setEstado("COMPLETADO");
            proyectoService.guardar(proyecto, null);
            redirectAttributes.addFlashAttribute("mensaje", "✅ Proyecto aceptado correctamente");
        }

        return "redirect:/lider/tareas/asignar";
    }
}
