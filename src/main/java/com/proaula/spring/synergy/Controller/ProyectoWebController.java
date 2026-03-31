package com.proaula.spring.synergy.Controller;

import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Service.UsuarioService;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.ParticipacionService;

@Controller
@RequestMapping("/proyectos")
public class ProyectoWebController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private ParticipacionService participacionService;

    // ==============================
    // DASHBOARD DE PROYECTOS
    // ==============================
    @GetMapping("/dashboard")
    public String dashboardProyectos(HttpSession session, Model model) {

        Usuarios usuario = (Usuarios) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);

        return "Dashboard_Usuarios_Proyecto";
    }

    // ==============================
    // REGISTRO DE PROYECTOS
    // ==============================
    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("proyecto", new Proyecto());
        model.addAttribute("proyectos", proyectoService.listar());
        return "registro_proyectos";
    }

    @PostMapping("/guardar")
public String guardarProyecto(
        @ModelAttribute Proyecto proyecto,
        @RequestParam("archivo") MultipartFile archivo,
        HttpSession session,
        Model model) {

    Long usuarioId = (Long) session.getAttribute("usuarioId");

    if (usuarioId == null) {
        return "redirect:/login";
    }

    // Asignar líder correctamente
    Usuarios lider = usuarioService.buscarPorId(usuarioId).orElse(null);
    proyecto.setLider(lider);

    try {
        proyectoService.guardar(proyecto, archivo);
        model.addAttribute("mensajeExito", "Proyecto registrado correctamente");
    } catch (RuntimeException e) {
        model.addAttribute("error", e.getMessage());
    }

    model.addAttribute("proyecto", new Proyecto());
    model.addAttribute("proyectos", proyectoService.listar());

    return "registro_proyectos";
}


    // ==============================
    // LISTA GENERAL PARA INSCRIBIRSE
    // ==============================
    @GetMapping("/participar")
    public String mostrarListaParticipacion(HttpSession session, Model model) {

        Usuarios usuario = (Usuarios) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("proyectos", proyectoService.listar());

        return "Participacion_Proyecto_Usuario";
    }

    // ==============================
    // INSCRIBIR A UN PROYECTO
    // ==============================
    @PostMapping("/participacion/inscribir/{proyectoId}/{usuarioId}")
    public String inscribir(@PathVariable Long proyectoId,
            @PathVariable Long usuarioId,
            RedirectAttributes redirectAttributes) {

        boolean inscrito = participacionService.inscribirUsuario(proyectoId, usuarioId);

        if (inscrito) {
            redirectAttributes.addFlashAttribute("popup", "success");
            redirectAttributes.addFlashAttribute("mensaje", "Te has inscrito exitosamente 🎉");
        } else {
            redirectAttributes.addFlashAttribute("popup", "error");
            redirectAttributes.addFlashAttribute("mensaje", "Ya estabas inscrito en este proyecto ❗");
        }

        return "redirect:/proyectos/participar";
    }

    // ==============================
    // SALIR DEL PROYECTO
    // ==============================
    @PostMapping("/participacion/eliminar/{proyectoId}/{usuarioId}")
    public String salirDelProyecto(@PathVariable Long proyectoId,
            @PathVariable Long usuarioId,
            RedirectAttributes redirectAttributes) {

        participacionService.eliminarParticipacion(proyectoId, usuarioId);

        redirectAttributes.addFlashAttribute("popup", "success");
        redirectAttributes.addFlashAttribute("mensaje", "Has salido del proyecto.");

        return "redirect:/proyectos/dashboard";
    }

    // ==============================
    // MIS PROYECTOS (DEL LÍDER)
    // ==============================
    @GetMapping("/mis_proyectos")
    public String listarMisProyectos(Model model, Principal principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        Usuarios usuario = usuarioService.buscarPorCorreo(principal.getName())
                .orElse(null);

        if (usuario == null) {
            return "redirect:/login";
        }

        List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(usuario.getId());

        model.addAttribute("proyectos", proyectos);

        return "Lider_MisProyectos";
    }

    @GetMapping("/mis_proyectos/{idUsuario}")
    public String listarMisProyectos(
            @PathVariable Long idUsuario,
            Model model) {

        // Obtener usuario por ID
        Usuarios usuario = usuarioService.buscarPorId(idUsuario)
                .orElse(null);

        // Proyectos donde el usuario es líder
        List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(idUsuario);

        model.addAttribute("usuario", usuario);
        model.addAttribute("proyectos", proyectos);

        return "Dashboard_MisProyectos";
    }

    //vista publica para los invitados
    @GetMapping("/publico")
    public String verProyectosPublico(Model model) {
        model.addAttribute("proyectos", proyectoService.listar());
        return "Participacion_Proyecto_Usuario";
    }
    

}