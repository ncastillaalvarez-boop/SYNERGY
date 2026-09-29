package com.proaula.spring.synergy.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.EntregaTareaRepository;
import com.proaula.spring.synergy.Security.GoogleLoginSuccessHandler;
import com.proaula.spring.synergy.Security.SesionUtil;
import com.proaula.spring.synergy.Service.HistorialService;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.TareaService;
import com.proaula.spring.synergy.Service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@Controller
public class UsuarioWebController {

    private static final int MIN_CONTRASENA = 6;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private TareaService tareaService;

    @Autowired
    private EntregaTareaRepository entregaTareaRepository;

    @Autowired
    private HistorialService historialService;

    // ============================================
    // PÁGINA PRINCIPAL
    // ============================================
    @GetMapping({"/", "/index"})
    public String index() {
        return "index";
    }

    // ============================================
    // REGISTRO
    // ============================================
    @GetMapping("/seleccionar-rol")
    public String seleccionarRol() {
        return "seleccionar-rol";
    }

    @GetMapping({"/registrarse", "/registro/voluntario"})
    public String registroVoluntario(Model model) {
        model.addAttribute("usuario", new Usuarios());
        return "Registro";
    }

    @GetMapping("/registro/lider")
    public String registroLider(Model model) {
        model.addAttribute("usuario", new Usuarios());
        return "registro-lider";
    }

    @PostMapping("/registrarse")
    public String registrar(@ModelAttribute("usuario") Usuarios usuario,
                            @RequestParam(required = false) String confirmarContrasena,
                            HttpSession session,
                            Model model) {

        // Solo se permite registrarse como LIDER o PARTICIPANTE (nunca ADMINISTRADOR)
        String rol = "LIDER".equalsIgnoreCase(usuario.getRol()) ? "LIDER" : "PARTICIPANTE";
        String vista = "LIDER".equals(rol) ? "registro-lider" : "Registro";

        String correo = usuario.getCorreo() == null ? "" : usuario.getCorreo().trim().toLowerCase();
        String nombre = usuario.getNombre() == null ? "" : usuario.getNombre().trim();
        String contrasena = usuario.getContrasena() == null ? "" : usuario.getContrasena();

        String error = null;
        if (nombre.isEmpty() || correo.isEmpty()) {
            error = "Completa tu nombre y tu correo.";
        } else if (contrasena.length() < MIN_CONTRASENA) {
            error = "La contraseña debe tener al menos " + MIN_CONTRASENA + " caracteres.";
        } else if (!contrasena.equals(confirmarContrasena)) {
            error = "Las contraseñas no coinciden.";
        } else if (usuarioService.buscarPorCorreo(correo) != null) {
            error = "Ya existe una cuenta con ese correo. Inicia sesión.";
        }

        if (error != null) {
            usuario.setContrasena(null);
            model.addAttribute("error", error);
            return vista;
        }

        Usuarios nuevo = new Usuarios();
        nuevo.setNombre(nombre);
        nuevo.setCorreo(correo);
        nuevo.setContrasena(passwordEncoder.encode(contrasena));
        nuevo.setRol(rol);
        nuevo.setActivo(true);
        nuevo.setEstado("ACTIVO");
        nuevo.setProveedor("LOCAL");
        nuevo.setFechaCreacion(LocalDateTime.now());
        nuevo = usuarioService.guardarUsuario(nuevo);

        historialService.registrarAccion("CREAR", "USUARIO", "Nuevo registro: " + correo + " (" + rol + ")", correo);

        SesionUtil.iniciar(session, nuevo);
        return "redirect:" + SesionUtil.panelPorRol(nuevo);
    }

    // ============================================
    // LOGIN
    // ============================================
    @GetMapping("/login")
    public String mostrarLogin(HttpSession session) {
        Usuarios actual = SesionUtil.usuario(session);
        if (actual != null) {
            return "redirect:" + SesionUtil.panelPorRol(actual);
        }
        return "Login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String correo,
                                @RequestParam String contrasena,
                                HttpSession session,
                                Model model) {

        Usuarios usuario = usuarioService.buscarPorCorreo(correo == null ? "" : correo.trim().toLowerCase());
        if (usuario == null) {
            // Compatibilidad con correos guardados con mayúsculas
            usuario = usuarioService.buscarPorCorreo(correo);
        }

        if (usuario == null || usuario.getContrasena() == null
                || !passwordEncoder.matches(contrasena, usuario.getContrasena())) {
            model.addAttribute("error", "Correo o contraseña incorrectos");
            return "Login";
        }

        if (!usuario.isEnabled()) {
            model.addAttribute("error", "Tu cuenta está inactiva. Contacta al administrador.");
            return "Login";
        }

        String panel = SesionUtil.panelPorRol(usuario);
        if ("/login".equals(panel)) {
            model.addAttribute("error", "Tu cuenta no tiene un rol válido. Contacta al administrador.");
            return "Login";
        }

        usuario.setUltimoAcceso(LocalDateTime.now());
        usuario = usuarioService.guardarUsuario(usuario);
        SesionUtil.iniciar(session, usuario);
        return "redirect:" + panel;
    }

    /**
     * Botón "Continuar con Google". Si viene ?rol=LIDER, la cuenta nueva se crea como líder.
     */
    @GetMapping("/login/google")
    public String loginGoogle(@RequestParam(required = false) String rol, HttpSession session) {
        if ("LIDER".equalsIgnoreCase(rol)) {
            session.setAttribute(GoogleLoginSuccessHandler.ATTR_ROL_GOOGLE, "LIDER");
        } else {
            session.removeAttribute(GoogleLoginSuccessHandler.ATTR_ROL_GOOGLE);
        }
        return "redirect:/oauth2/authorization/google";
    }

    // (GET /logout lo atiende Spring Security: cierra la sesión y lleva a /login?logout)

    // ============================================
    // DASHBOARD DEL PARTICIPANTE
    // ============================================
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        if (!"PARTICIPANTE".equals(SesionUtil.rol(usuario))) {
            return "redirect:" + SesionUtil.panelPorRol(usuario);
        }

        List<Tarea> tareas = tareaService.obtenerTareasPorUsuario(usuario.getId());
        long pendientes = tareas.stream().filter(t -> !"COMPLETADA".equalsIgnoreCase(t.getEstado())).count();

        model.addAttribute("usuario", usuario);
        model.addAttribute("totalTareas", tareas.size());
        model.addAttribute("tareasPendientes", pendientes);
        model.addAttribute("tareasCompletadas", tareas.size() - pendientes);
        model.addAttribute("totalProyectos", usuario.getProyectos() == null ? 0 : usuario.getProyectos().size());
        return "dashboard_Usuario";
    }

    @GetMapping("/administrador_dashboard")
    public String adminDashboard() {
        return "redirect:/admin/proyectos";
    }

    // ============================================
    // BUZÓN DE TAREAS
    // ============================================
    @GetMapping("/tareas/buzon")
    public String mostrarBuzonTareas(HttpSession session, Model model) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }

        List<Tarea> tareas = tareaService.obtenerTareasPorUsuario(usuario.getId());

        // Tareas que este usuario ya entregó (para mostrar "Entregada")
        Set<String> entregadas = entregaTareaRepository.findByIdUsuario(usuario.getId()).stream()
                .map(e -> e.getIdTarea())
                .collect(Collectors.toSet());

        model.addAttribute("usuario", usuario);
        model.addAttribute("tareas", tareas);
        model.addAttribute("entregadas", entregadas);
        return "Buzon_Tareas_Usuario";
    }

    // ============================================
    // PERFIL / CREDENCIALES
    // ============================================
    @GetMapping("/usuario/credenciales")
    public String verCredenciales(HttpSession session, Model model) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", usuario);
        return "usuario_credenciales";
    }

    @PostMapping("/usuario/actualizar-credenciales")
    public String actualizarCredenciales(@RequestParam(required = false) String nombre,
                                         @RequestParam(required = false) String correo,
                                         @RequestParam(required = false) String contrasenaActual,
                                         @RequestParam(required = false) String contrasenaNueva,
                                         @RequestParam(required = false) String contrasenaConfirmacion,
                                         HttpSession session,
                                         RedirectAttributes ra) {

        Usuarios enSesion = SesionUtil.usuario(session);
        if (enSesion == null) {
            return "redirect:/login";
        }
        // Siempre trabajar con la versión más reciente de la base de datos
        Usuarios usuario = usuarioService.obtenerUsuarioPorId(enSesion.getId());

        if (nombre != null && !nombre.trim().isEmpty()) {
            usuario.setNombre(nombre.trim());
        }

        if (correo != null && !correo.trim().isEmpty() && !correo.trim().equalsIgnoreCase(usuario.getCorreo())) {
            String nuevoCorreo = correo.trim().toLowerCase();
            if (usuarioService.buscarPorCorreo(nuevoCorreo) != null) {
                ra.addFlashAttribute("error", "El correo ya está en uso");
                return "redirect:/usuario/credenciales";
            }
            usuario.setCorreo(nuevoCorreo);
        }

        if (contrasenaNueva != null && !contrasenaNueva.isEmpty()) {
            boolean esGoogle = "GOOGLE".equalsIgnoreCase(usuario.getProveedor());
            // Las cuentas creadas con Google pueden definir su primera contraseña sin la actual
            if (!esGoogle && (contrasenaActual == null
                    || !passwordEncoder.matches(contrasenaActual, usuario.getContrasena()))) {
                ra.addFlashAttribute("error", "La contraseña actual es incorrecta");
                return "redirect:/usuario/credenciales";
            }
            if (!contrasenaNueva.equals(contrasenaConfirmacion)) {
                ra.addFlashAttribute("error", "Las contraseñas nuevas no coinciden");
                return "redirect:/usuario/credenciales";
            }
            if (contrasenaNueva.length() < MIN_CONTRASENA) {
                ra.addFlashAttribute("error", "La contraseña debe tener al menos " + MIN_CONTRASENA + " caracteres");
                return "redirect:/usuario/credenciales";
            }
            usuario.setContrasena(passwordEncoder.encode(contrasenaNueva));
            if (esGoogle) {
                usuario.setProveedor("GOOGLE+LOCAL");
            }
        }

        usuario = usuarioService.guardarUsuario(usuario);
        SesionUtil.iniciar(session, usuario);

        ra.addFlashAttribute("mensaje", "Credenciales actualizadas correctamente");
        return "redirect:/usuario/credenciales";
    }

    // Ruta antigua: la asignación de tareas del líder vive en /lider/tareas/asignar
    @GetMapping("/lider/asignar-tareas")
    public String mostrarAsignacionTareas() {
        return "redirect:/lider/tareas/asignar";
    }

    // Los proyectos públicos (invitados) están en ExplorarController.
}
