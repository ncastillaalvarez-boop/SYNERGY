package com.proaula.spring.synergy.Controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Security.SecurityUtils;
import com.proaula.spring.synergy.Service.ParticipacionService;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.UsuarioService;

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
    public String dashboardProyectos(Model model) {

        Usuarios usuario = SecurityUtils.getUsuarioActual();

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
            Model model) {

        Usuarios lider = SecurityUtils.getUsuarioActual();

        if (lider == null) {
            return "redirect:/login";
        }

        proyecto.setIdLider(lider.getId());

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
    public String mostrarListaParticipacion(Model model) {

        Usuarios usuario = SecurityUtils.getUsuarioActual();

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
    public String inscribir(@PathVariable String proyectoId,
            @PathVariable String usuarioId,
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
    public String salirDelProyecto(@PathVariable String proyectoId,
            @PathVariable String usuarioId,
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

        Usuarios usuario = usuarioService.buscarPorCorreo(principal.getName());

        if (usuario == null) {
            return "redirect:/login";
        }

        List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(usuario.getId());

        model.addAttribute("proyectos", proyectos);

        return "Lider_MisProyectos";
    }

    @GetMapping("/mis_proyectos/{idUsuario}")
    public String listarMisProyectos(
            @PathVariable String idUsuario,
            Model model) {

        Usuarios usuario = usuarioService.obtenerUsuarioPorId(idUsuario);

        List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(idUsuario);

        model.addAttribute("usuario", usuario);
        model.addAttribute("proyectos", proyectos);

        return "Dashboard_MisProyectos";
    }
}
