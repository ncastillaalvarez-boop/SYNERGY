package com.proaula.spring.synergy.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Security.AuthResponse;
import com.proaula.spring.synergy.Security.CookieUtil;
import com.proaula.spring.synergy.Security.JwtTokenProvider;
import com.proaula.spring.synergy.Security.LoginRequest;
import com.proaula.spring.synergy.Security.SecurityUtils;
import com.proaula.spring.synergy.Service.AuthService;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.TareaService;
import com.proaula.spring.synergy.Service.UsuarioService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class UsuarioWebController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private TareaService tareaService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CookieUtil cookieUtil;

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

    @PostMapping("/registrarse")
    public String registrar(
            @ModelAttribute Usuarios usuario,
            @RequestParam(required = false) String confirmarContrasena,
            HttpServletResponse response,
            Model model,
            RedirectAttributes ra) {

        try {
            // Validar contraseñas coincidan
            if (confirmarContrasena != null && !usuario.getContrasena().equals(confirmarContrasena)) {
                model.addAttribute("error", "Las contraseñas no coinciden");
                model.addAttribute("usuario", usuario);
                return determinarVistaRegistro(usuario.getRol());
            }

            // Validar correo no duplicado ANTES de intentar guardar
            if (usuarioService.buscarPorCorreo(usuario.getCorreo()) != null) {
                model.addAttribute("error", "El correo ya está registrado. Intenta iniciar sesión.");
                model.addAttribute("usuario", usuario);
                return determinarVistaRegistro(usuario.getRol());
            }

            // Si no viene rol, asignar PARTICIPANTE por defecto
            if (usuario.getRol() == null || usuario.getRol().isEmpty()) {
                usuario.setRol("PARTICIPANTE");
            }

            // Registrar usando AuthService (encripta contraseña + genera JWT)
            AuthResponse authResponse = authService.registrar(usuario);

            // Generar refresh token
            String refreshToken = jwtTokenProvider.generateRefreshToken(usuario.getCorreo());

            // Setear cookies HttpOnly con los tokens
            cookieUtil.createAccessTokenCookie(response, authResponse.getToken());
            cookieUtil.createRefreshTokenCookie(response, refreshToken);

            // Redirigir según el rol
            return switch (usuario.getRol().toUpperCase()) {
                case "ADMINISTRADOR" -> "redirect:/admin/proyectos";
                case "LIDER"         -> "redirect:/lider/dashboard";
                case "PARTICIPANTE"  -> "redirect:/dashboard";
                default              -> "redirect:/login";
            };

        } catch (Exception e) {
            model.addAttribute("error", "No se pudo completar el registro: " + e.getMessage());
            model.addAttribute("usuario", usuario);
            return determinarVistaRegistro(usuario.getRol());
        }
    }

    // Helper para devolver el HTML correcto según el rol
    private String determinarVistaRegistro(String rol) {
        if ("LIDER".equalsIgnoreCase(rol)) {
            return "registro-lider";
        }
        return "Registro";
    }

    // ============================================
    // LOGIN
    // ============================================
    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        return "Login";
    }

    @PostMapping("/login")
    public String procesarLogin(
            @RequestParam String correo,
            @RequestParam String contrasena,
            HttpServletResponse response,
            Model model) {

        try {
            // Crear LoginRequest y delegar a AuthService (que genera JWT)
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setCorreo(correo);
            loginRequest.setContrasena(contrasena);

            AuthResponse authResponse = authService.login(loginRequest);

            // Generar refresh token
            String refreshToken = jwtTokenProvider.generateRefreshToken(correo);

            // Setear cookies HttpOnly
            cookieUtil.createAccessTokenCookie(response, authResponse.getToken());
            cookieUtil.createRefreshTokenCookie(response, refreshToken);

            // Redirigir según rol
            String rol = authResponse.getRol().toUpperCase();
            return switch (rol) {
                case "ADMINISTRADOR", "ADMIN" -> "redirect:/admin/proyectos";
                case "LIDER" -> "redirect:/lider/dashboard";
                case "PARTICIPANTE", "USUARIO" -> "redirect:/dashboard";
                default -> "redirect:/login?error=RoleInvalido";
            };

        } catch (Exception e) {
            model.addAttribute("error", "Correo o contraseña incorrectos");
            return "Login";
        }
    }

    // ============================================
    // LOGOUT (borra cookies JWT)
    // ============================================
    @GetMapping("/logout")
    public String logout(HttpServletResponse response, RedirectAttributes redirectAttributes) {
        cookieUtil.clearCookies(response);
        redirectAttributes.addFlashAttribute("mensaje", "Sesión cerrada correctamente");
        return "redirect:/login";
    }

    // ============================================
    // DASHBOARD PRINCIPAL
    // ============================================
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Usuarios usuario = SecurityUtils.getUsuarioActual();

        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);
        return "dashboard_Usuario";
    }

    @GetMapping("/administrador_dashboard")
    public String adminDashboard(Model model) {
        Usuarios usuario = SecurityUtils.getUsuarioActual();
        if (usuario == null) return "redirect:/login";

        model.addAttribute("usuario", usuario);
        return "Administrador_Dashboard";
    }

    // ============================================
    // BUZÓN DE TAREAS
    // ============================================
    @GetMapping("/tareas/buzon")
    public String mostrarBuzonTareas(Model model) {
        Usuarios usuario = SecurityUtils.getUsuarioActual();

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
    public String verCredenciales(Model model) {
        Usuarios usuario = SecurityUtils.getUsuarioActual();

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
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        Usuarios usuario = SecurityUtils.getUsuarioActual();

        if (usuario == null) {
            return "redirect:/login";
        }

        // Actualizar nombre si se proporciona
        if (nombre != null && !nombre.trim().isEmpty()) {
            usuario.setNombre(nombre.trim());
        }

        // Actualizar correo si se proporciona y no existe
        boolean correoCambio = false;
        if (correo != null && !correo.trim().isEmpty() && !correo.equals(usuario.getCorreo())) {
            Usuarios usuarioExistente = usuarioService.buscarPorCorreo(correo.trim());
            if (usuarioExistente != null) {
                redirectAttributes.addFlashAttribute("error", "El correo ya está en uso");
                return "redirect:/usuario/credenciales";
            }
            usuario.setCorreo(correo.trim());
            correoCambio = true;
        }

        // Cambiar contraseña si se solicita
        if (contrasenaNueva != null && !contrasenaNueva.isEmpty()) {

            // Validar contraseña actual con BCrypt
            if (contrasenaActual == null || !passwordEncoder.matches(contrasenaActual, usuario.getContrasena())) {
                redirectAttributes.addFlashAttribute("error", "Contraseña actual incorrecta");
                return "redirect:/usuario/credenciales";
            }

            if (!contrasenaNueva.equals(contrasenaConfirmacion)) {
                redirectAttributes.addFlashAttribute("error", "Las contraseñas nuevas no coinciden");
                return "redirect:/usuario/credenciales";
            }

            if (contrasenaNueva.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "La contraseña debe tener al menos 6 caracteres");
                return "redirect:/usuario/credenciales";
            }

            usuario.setContrasena(passwordEncoder.encode(contrasenaNueva));
        }

        // Guardar cambios
        usuarioService.guardarUsuario(usuario);

        // Si cambió el correo, regenerar el JWT con el nuevo correo
        if (correoCambio) {
            String nuevoToken = jwtTokenProvider.generateToken(usuario.getCorreo(), usuario.getRol());
            String nuevoRefresh = jwtTokenProvider.generateRefreshToken(usuario.getCorreo());
            cookieUtil.createAccessTokenCookie(response, nuevoToken);
            cookieUtil.createRefreshTokenCookie(response, nuevoRefresh);
        }

        redirectAttributes.addFlashAttribute("mensaje", "✅ Credenciales actualizadas correctamente");
        return "redirect:/usuario/credenciales";
    }

    // ============================================
    // ASIGNACIÓN DE TAREAS (LÍDER)
    // ============================================
    @GetMapping("/lider/asignar-tareas")
    public String mostrarAsignacionTareas(Model model) {
        Usuarios usuario = SecurityUtils.getUsuarioActual();

        if (usuario == null) {
            return "redirect:/login";
        }

        if (!"LIDER".equals(usuario.getRol())) {
            return "redirect:/dashboard";
        }

        return "Lider_AsignarTareas";
    }

    // ============================================
    // SELECCIONAR ROL Y REGISTROS POR ROL
    // ============================================
    @GetMapping("/seleccionar-rol")
    public String seleccionarRol() {
        return "seleccionar-rol";
    }

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

    @GetMapping("/publico/proyectos")
    public String verProyectosPublico(Model model) {
        model.addAttribute("proyectos", proyectoService.listar());
        Usuarios usuario = SecurityUtils.getUsuarioActual();
        model.addAttribute("usuario", usuario);
        return "Participacion_Proyecto_Usuario";
    }
}