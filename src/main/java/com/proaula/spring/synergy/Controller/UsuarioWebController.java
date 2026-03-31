package com.proaula.spring.synergy.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Service.TareaService;
import com.proaula.spring.synergy.Service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@Controller
public class UsuarioWebController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private TareaService tareaService;

    // ============================================
    // PÁGINA PRINCIPAL
    // ============================================
    @GetMapping("/")
    public String mostrarIndex() {
        return "index";
    }

    @GetMapping("/index")
    public String index() {
        return "index";
    }

    // ============================================
    // REGISTRO
    // ============================================
    @GetMapping("/registrarse")
    public String mostrarRegistro(Model model) {
        model.addAttribute("usuario", new Usuarios());
        return "Registro";
    }

    // ============================================
    // ASIGNAR ROL AUTOMÁTICO POR DOMINIO
    // ============================================
    @PostMapping("/registrarse")
    public String registrar(
             @ModelAttribute Usuarios usuario,
             @RequestParam(required = false) String confirmarContrasena,
             HttpSession session,
             RedirectAttributes ra) {

        // Si no viene rol del formulario, asignar Participante por defecto
        if (usuario.getRol() == null) {
            usuario.setRol("PARTICIPANTE");
        }

        // Guardar en BD
        usuarioService.guardarUsuario(usuario);

        // Guardar en sesión
        session.setAttribute("usuario", usuario);
        session.setAttribute("usuarioId", usuario.getId());

        // Redirigir según el rol
        return switch (usuario.getRol()) {
            case "ADMINISTRADOR" -> "redirect:/admin/proyectos";
            case "LIDER" -> "redirect:/lider/dashboard";
            case "PARTICIPANTE" -> "redirect:/dashboard";
            default -> "redirect:/login";
        };
    }

    // ============================================
    // LOGIN
    // ============================================
    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        model.addAttribute("loginDTO", new Object());
        return "Login";
    }

    @PostMapping("/login")
    public String procesarLogin(
            @RequestParam String correo,
            @RequestParam String contrasena,
            HttpSession session,
            Model model) {

        Usuarios usuario = usuarioService.buscarPorCorreo(correo);

        if (usuario == null || !usuario.getContrasena().equals(contrasena)) {
            model.addAttribute("error", "Correo o contraseña incorrectos");
            return "Login";
        }

        // Guardar en sesión
        session.setAttribute("usuario", usuario);
        session.setAttribute("usuarioId", usuario.getId());

        String rol = usuario.getRol();

        return switch (rol) {
            case "ADMINISTRADOR" -> "redirect:/admin/proyectos";
            case "LIDER" -> "redirect:/lider/dashboard";
            case "PARTICIPANTE" -> "redirect:/dashboard";
            default -> "redirect:/login?error";
        };
    }

    // ============================================
    // LOGOUT
    // ============================================
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("mensaje", "Sesión cerrada correctamente");
        return "redirect:/login";
    }

    // ============================================
    // DASHBOARD PRINCIPAL
    // ============================================
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {

        Usuarios usuario = (Usuarios) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);
        return "dashboard_Usuario";
    }

    @GetMapping("/administrador_dashboard")
    public String adminDashboard(HttpSession session, Model model) {

        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/login";

        model.addAttribute("usuario", usuario);
        return "Administrador_Dashboard"; // nombre EXACTO del HTML
    }

    // ============================================
    // BUZÓN DE TAREAS
    // ============================================
    @GetMapping("/tareas/buzon")
    public String mostrarBuzonTareas(HttpSession session, Model model) {

        Usuarios usuario = (Usuarios) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        List<Tarea> tareas = tareaService.obtenerTareasPorUsuario(usuario.getId());

        model.addAttribute("usuario", usuario);
        model.addAttribute("tareas", tareas);

        return "Buzon_Tareas_Usuario";
    }

    // ============================================
    // VER CREDENCIALES (PERFIL)
    // ============================================
    @GetMapping("/usuario/credenciales")
    public String verCredenciales(HttpSession session, Model model) {

        Usuarios usuario = (Usuarios) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);
        return "usuario_credenciales";
    }

    // ============================================
    // ACTUALIZAR CREDENCIALES
    // ============================================
    @PostMapping("/usuario/actualizar-credenciales")
    public String actualizarCredenciales(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String contrasenaActual,
            @RequestParam(required = false) String contrasenaNueva,
            @RequestParam(required = false) String contrasenaConfirmacion,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuarios usuario = (Usuarios) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        // Actualizar nombre si se proporciona
        if (nombre != null && !nombre.trim().isEmpty()) {
            usuario.setNombre(nombre.trim());
        }

        // Actualizar correo si se proporciona y no existe
        if (correo != null && !correo.trim().isEmpty() && !correo.equals(usuario.getCorreo())) {
            Usuarios usuarioExistente = usuarioService.buscarPorCorreo(correo.trim());
            if (usuarioExistente != null) {
                redirectAttributes.addFlashAttribute("error", "El correo ya está en uso");
                return "redirect:/usuario/credenciales";
            }
            usuario.setCorreo(correo.trim());
        }

        // Cambiar contraseña si se solicita
        if (contrasenaNueva != null && !contrasenaNueva.isEmpty()) {

            // Validar contraseña actual
            if (contrasenaActual == null || !usuario.getContrasena().equals(contrasenaActual)) {
                redirectAttributes.addFlashAttribute("error", "Contraseña actual incorrecta");
                return "redirect:/usuario/credenciales";
            }

            // Validar confirmación
            if (!contrasenaNueva.equals(contrasenaConfirmacion)) {
                redirectAttributes.addFlashAttribute("error", "Las contraseñas nuevas no coinciden");
                return "redirect:/usuario/credenciales";
            }

            // Validar longitud mínima
            if (contrasenaNueva.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "La contraseña debe tener al menos 6 caracteres");
                return "redirect:/usuario/credenciales";
            }

            // TODO: Aquí deberías usar BCrypt para encriptar
            usuario.setContrasena(contrasenaNueva);
        }

        // Guardar cambios
        usuarioService.guardarUsuario(usuario);

        // Actualizar sesión
        session.setAttribute("usuario", usuario);

        redirectAttributes.addFlashAttribute("mensaje", "✅ Credenciales actualizadas correctamente");
        return "redirect:/usuario/credenciales";
    }

    // ============================================
    // ASIGNACIÓN DE TAREAS (LÍDER)
    // ============================================
    @GetMapping("/lider/asignar-tareas")
    public String mostrarAsignacionTareas(HttpSession session, Model model) {

        Usuarios usuario = (Usuarios) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        // Validar que sea líder
        if (!"LIDER".equals(usuario.getRol())) {
            return "redirect:/dashboard";
        }

        return "Lider_AsignarTareas";
    }

    //seleccionar-rol
    @GetMapping("/seleccionar-rol")
    public String seleccionarRol() {
        return "seleccionar-rol";
    }

    //Registro Lider
    @GetMapping("/registro/lider")
    public String registroLider(Model model) {
        model.addAttribute("usuario", new Usuarios());
        return "registro-lider";
    }

    @GetMapping("/registro/voluntario")
    public String registroVoluntario(Model model) {
        model.addAttribute("usuario", new Usuarios());
        return "Registro";
    }
}