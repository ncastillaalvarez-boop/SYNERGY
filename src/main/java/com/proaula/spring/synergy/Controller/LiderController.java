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

import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Service.ProyectoService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/lider")
public class LiderController {

    @Autowired
    private ProyectoService proyectoService;

    // =======================================================
    // VALIDAR QUE EL USUARIO SEA LÍDER
    // =======================================================
    private Usuarios validarLider(HttpSession session) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");

        if (usuario == null)
            return null;

        if (!"LIDER".equals(usuario.getRol()))
            return null;

        return usuario;
    }

    // =======================================================
    // DASHBOARD DEL LÍDER
    // =======================================================
    @GetMapping("/dashboard")
    public String dashboardLider(HttpSession session, Model model) {

        Usuarios lider = validarLider(session);
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
    public String formularioNuevoProyecto(HttpSession session, Model model) {

        Usuarios lider = validarLider(session);
        if (lider == null)
            return "redirect:/login";

        model.addAttribute("proyecto", new Proyecto());
        return "Lider_Crear_Proyecto";
    }

    // =======================================================
    // GUARDAR PROYECTO
    // =======================================================
    @PostMapping("/proyecto/guardar")
    public String guardarProyecto(@ModelAttribute Proyecto proyecto,
                                  HttpSession session) {

        Usuarios lider = validarLider(session);
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
    public String editarProyecto(@PathVariable String id,
                                 HttpSession session,
                                 Model model) {

        Usuarios lider = validarLider(session);
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
                                     @ModelAttribute Proyecto proyecto,
                                     HttpSession session) {

        Usuarios lider = validarLider(session);
        if (lider == null)
            return "redirect:/login";

        proyecto.setId(id);
        proyecto.setIdLider(lider.getId());

        proyectoService.guardar(proyecto, null);

        return "redirect:/lider/dashboard";  // ← CORREGIDO
    }

    // =======================================================
    // PÁGINAS NUEVAS QUE PEDISTE
    // =======================================================

    @GetMapping("/tareas/asignar")
public String asignarTareas(HttpSession session, Model model) {

    Usuarios lider = validarLider(session);
    if (lider == null)
        return "redirect:/login";
    // Obtener proyectos del líder
    List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(lider.getId());
    model.addAttribute("proyectos", proyectos);
    
    return "Lider_Asignar_Tareas";
}

    // VER USUARIOS INSCRITOS AL PROYECTO
    @GetMapping("/usuarios")
    public String usuariosInscritos(HttpSession session, Model model) {

        Usuarios lider = validarLider(session);
        if (lider == null)
            return "redirect:/login";

        return "Lider_Usuarios_Proyecto";
    }

    // VER TAREAS RECIBIDAS
    @GetMapping("/tareas/recibidas")
    public String tareasRecibidas(HttpSession session, Model model) {

        Usuarios lider = validarLider(session);
        if (lider == null)
            return "redirect:/login";

        return "Lider_Tareas_Recibidas";
    }

}
