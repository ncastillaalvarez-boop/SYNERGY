package com.proaula.spring.synergy.Controller;

import java.util.List;

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
import com.proaula.spring.synergy.Security.SecurityUtils;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.UsuarioService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private UsuarioService usuarioService;

    // ---------------------------------------------------------
    // MÉTODO REUTILIZABLE DE VALIDACIÓN DE ADMIN
    // ---------------------------------------------------------
    private Usuarios validarAdmin() {
        Usuarios usuario = SecurityUtils.getUsuarioActual();
        if (usuario == null) return null;
        if (!"ADMINISTRADOR".equalsIgnoreCase(usuario.getRol())) return null;
        return usuario;
    }

    // ---------------------------------------------------------
    // VER TODOS LOS USUARIOS
    // ---------------------------------------------------------
    @GetMapping("/usuarios")
    public String verUsuarios(Model model) {
        Usuarios usuario = validarAdmin();
        if (usuario == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("usuarios", usuarioService.obtenerTodosUsuarios());
        return "Admin_Usuarios_Dashboard";
    }

    // ---------------------------------------------------------
    // VER PROYECTO
    // ---------------------------------------------------------
    @GetMapping("/proyecto/{id}")
    public String verProyecto(@PathVariable String id, Model model) {

        Usuarios admin = validarAdmin();
        if (admin == null)
            return "redirect:/login";

        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto == null)
            return "redirect:/admin/proyectos";

        model.addAttribute("usuario", admin);
        model.addAttribute("proyecto", proyecto);

        return "Admin_Ver_Proyecto";
    }

    @GetMapping("/proyectos")
    public String verProyectos(Model model) {

        Usuarios admin = validarAdmin();
        if (admin == null)
            return "redirect:/login";

        List<Proyecto> proyectos = proyectoService.listar();

        model.addAttribute("usuario", admin);
        model.addAttribute("proyectos", proyectos);

        return "Administrador_Dashboard";
    }

    // ---------------------------------------------------------
    // ACEPTAR PROYECTO
    // ---------------------------------------------------------
    @PostMapping("/proyecto/{id}/aceptar")
    public String aceptarProyecto(@PathVariable String id,
            RedirectAttributes redirectAttributes) {

        Usuarios admin = validarAdmin();
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
            RedirectAttributes redirectAttributes) {

        Usuarios admin = validarAdmin();
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
            RedirectAttributes redirectAttributes) {

        Usuarios admin = validarAdmin();
        if (admin == null)
            return "redirect:/login";

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
            RedirectAttributes redirectAttributes) {

        Usuarios admin = validarAdmin();
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
