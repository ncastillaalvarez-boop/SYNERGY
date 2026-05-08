package com.proaula.spring.synergy.Controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Service.ParticipacionService;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.UsuarioService;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ParticipacionService participacionService;

    // ---------------------------------------------------------
    // MÉTODO REUTILIZABLE DE VALIDACIÓN DE ADMIN
    // ---------------------------------------------------------
    private Usuarios validarAdmin(Principal principal) {
        if (principal == null) return null;
        Usuarios usuario = usuarioService.buscarPorCorreo(principal.getName());

        if (usuario == null)
            return null;
        if (!"ADMINISTRADOR".equalsIgnoreCase(usuario.getRol()))
            return null;

        return usuario;
    }

    // ---------------------------------------------------------
    // VER TODOS LOS USUARIOS
    // ---------------------------------------------------------
    @GetMapping("/usuarios")
    public String verUsuarios(Model model, Principal principal) {

        Usuarios usuario = validarAdmin(principal);
        if (usuario == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("usuarios", usuarioService.obtenerTodosUsuarios());

        // Vista Thymeleaf: src/main/resources/templates/Admin_Usuarios_Dashboard.html
        return "Admin_Usuarios_Dashboard";
    }

    // ---------------------------------------------------------
    // VER PROYECTO
    // ---------------------------------------------------------
    @GetMapping("/proyecto/{id}")
    public String verProyecto(@PathVariable String id, Principal principal, Model model) {

        Usuarios admin = validarAdmin(principal);
        if (admin == null)
            return "redirect:/login";

        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto == null)
            return "redirect:/admin/proyectos";

        model.addAttribute("usuario", admin);
        model.addAttribute("proyecto", proyecto);

        // Vista: Admin_Ver_Proyecto.html
        return "Admin_Ver_Proyecto";
    }

    @GetMapping("/proyectos")
    public String verProyectos(Principal principal, Model model) {

        Usuarios admin = validarAdmin(principal);
        if (admin == null)
            return "redirect:/login";

        List<Proyecto> proyectos = proyectoService.listar();

        // Ordenar por fechaCreacion descendente (más recientes primero)
        proyectos.sort((a, b) -> {
            if (a.getFechaCreacion() == null) return 1;
            if (b.getFechaCreacion() == null) return -1;
            return b.getFechaCreacion().compareTo(a.getFechaCreacion());
        });

        // Mapa proyectoId → lista de participantes inscritos
        Map<String, List<Usuarios>> participantesPorProyecto = new HashMap<>();
        for (Proyecto p : proyectos) {
            participantesPorProyecto.put(p.getId(),
                    participacionService.listarUsuariosPorProyecto(p.getId()));
        }

        model.addAttribute("usuario", admin);
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("participantesPorProyecto", participantesPorProyecto);

        // Vista: Administrador_Dashboard.html
        return "Administrador_Dashboard";
    }

    // ---------------------------------------------------------
    // ACEPTAR PROYECTO
    // ---------------------------------------------------------
    @PostMapping("/proyecto/{id}/aceptar")
    public String aceptarProyecto(@PathVariable String id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        Usuarios admin = validarAdmin(principal);
        if (admin == null)
            return "redirect:/login";

        Proyecto proyecto = proyectoService.buscarPorId(id);

        if (proyecto != null) {
            redirectAttributes.addFlashAttribute("mensaje",
                    "✅ Proyecto '" + proyecto.getNombre() + "' aceptado correctamente");
        }

        return "redirect:/admin/proyectos";
    }

    // ---------------------------------------------------------
    // ELIMINAR PROYECTO
    // ---------------------------------------------------------
    @PostMapping("/proyecto/{id}/eliminar")
    public String eliminarProyecto(@PathVariable String id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        Usuarios admin = validarAdmin(principal);
        if (admin == null)
            return "redirect:/login";

        Proyecto proyecto = proyectoService.buscarPorId(id);

        if (proyecto != null) {
            proyectoService.eliminar(id);

            redirectAttributes.addFlashAttribute("mensaje",
                    "🗑️ Proyecto '" + proyecto.getNombre() + "' eliminado correctamente");
        }

        return "redirect:/admin/proyectos";
    }

    // ---------------------------------------------------------
    // ELIMINAR USUARIO
    // ---------------------------------------------------------
    @PostMapping("/usuario/{id}/eliminar")
    public String eliminarUsuario(@PathVariable String id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        Usuarios admin = validarAdmin(principal);
        if (admin == null)
            return "redirect:/login";

        // Evitar que el admin se elimine a sí mismo
        if (admin.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error",
                    "❌ No puedes eliminar tu propia cuenta de administrador");
            return "redirect:/admin/usuarios";
        }

        Usuarios usuario = usuarioService.obtenerUsuarioPorId(id);

        if (usuario != null) {
            usuarioService.eliminarUsuario(id);

            redirectAttributes.addFlashAttribute("mensaje",
                    "🗑️ Usuario '" + usuario.getNombre() + "' eliminado correctamente");
        }

        return "redirect:/admin/usuarios";
    }

    // ---------------------------------------------------------
    // CAMBIAR ROL DE USUARIO
    // ---------------------------------------------------------
    @PostMapping("/usuario/{id}/cambiar-rol")
    public String cambiarRol(@PathVariable String id,
            @RequestParam String nuevoRol,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        Usuarios admin = validarAdmin(principal);
        if (admin == null)
            return "redirect:/login";

        Usuarios usuario = usuarioService.obtenerUsuarioPorId(id);

        if (usuario != null) {
            usuario.setRol(nuevoRol);
            usuarioService.guardarUsuario(usuario);

            redirectAttributes.addFlashAttribute("mensaje",
                    "✅ Rol de '" + usuario.getNombre() + "' cambiado a " + nuevoRol);
        }

        return "redirect:/admin/usuarios";
    }
}
