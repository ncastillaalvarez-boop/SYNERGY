package com.proaula.spring.synergy.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
import com.proaula.spring.synergy.Service.ParticipacionService;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.TareaService;
import com.proaula.spring.synergy.Service.UsuarioService;

import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import com.proaula.spring.synergy.Security.JwtTokenProvider;
import com.proaula.spring.synergy.Security.JwtCookieService;

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
    private ParticipacionService participacionService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtCookieService jwtCookieService;

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
            RedirectAttributes ra) {

        // Encriptar contraseña antes de guardar
        String contrasenaEncriptada = passwordEncoder.encode(usuario.getContrasena());
        usuario.setContrasena(contrasenaEncriptada);

        // Determinar nombre del rol (viene del formulario o PARTICIPANTE por defecto)
        String nombreRol = (usuario.getRol() != null && !usuario.getRol().isEmpty())
                ? usuario.getRol().toUpperCase()
                : "PARTICIPANTE";
        usuario.setRol(nombreRol);

        // Buscar el ObjectId del rol en la colección roles y asignarlo
        try {
            String idRol = usuarioService.obtenerIdRolPorNombre(nombreRol);
            usuario.setIdRol(idRol);
        } catch (Exception ignored) { }

        usuario.setEstado("ACTIVO");

        // Guardar en BD
        usuarioService.guardarUsuario(usuario);

      // Iniciar sesión automáticamente (JWT)
      String token = jwtTokenProvider.generateToken(usuario.getCorreo(), usuario.getRol());
      jwtCookieService.addJwtCookie(response, token);

      // Redirigir según el rol
      return switch (usuario.getRol()) {
          case "ADMINISTRADOR" -> "redirect:/admin/proyectos";
          case "LIDER"         -> "redirect:/lider/dashboard";
          case "PARTICIPANTE"  -> "redirect:/dashboard";
          default              -> "redirect:/login";
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
            HttpServletResponse response,
            Model model) {

        Usuarios usuario = usuarioService.buscarPorCorreo(correo);

        if (usuario == null || !passwordEncoder.matches(contrasena, usuario.getContrasena())) {
            model.addAttribute("error", "Correo o contraseña incorrectos");
            return "Login";
        }

        // Guardar en Cookie usando JWT STATELESS
        String token = jwtTokenProvider.generateToken(usuario.getCorreo(), usuario.getRol());
        jwtCookieService.addJwtCookie(response, token);

        String rol = usuario.getRol().toUpperCase();

        return switch (rol) {
            case "ADMINISTRADOR", "ADMINISTRADOR_ROL", "ADMIN" -> "redirect:/admin/proyectos";
            case "LIDER", "LIDER_ROL" -> "redirect:/lider/dashboard";
            case "PARTICIPANTE", "PARTICIPANTE_ROL", "USUARIO" -> "redirect:/dashboard";
            default -> "redirect:/login?error=RoleInvalido";
        };
    }

    // ============================================
    // LOGOUT
    // ============================================
    @GetMapping("/logout")
    public String logout(HttpServletResponse response, RedirectAttributes redirectAttributes) {
        jwtCookieService.clearJwtCookie(response);
        redirectAttributes.addFlashAttribute("mensaje", "Sesión cerrada correctamente");
        return "redirect:/login";
    }

    // ============================================
    // DASHBOARD PRINCIPAL
    // ============================================
    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {

        if (principal == null) return "redirect:/login";

        Usuarios usuario = usuarioService.buscarPorCorreo(principal.getName());
        if (usuario == null) return "redirect:/login";

        List<Proyecto> proyectosInscritos =
            participacionService.listarProyectosPorUsuario(usuario.getId());

        model.addAttribute("usuario", usuario);
        model.addAttribute("proyectosInscritos", proyectosInscritos);
        return "dashboard_Usuario";
    }

    @GetMapping("/administrador_dashboard")
    public String adminDashboard(Principal principal, Model model) {

        if (principal == null) return "redirect:/login";
        Usuarios usuario = usuarioService.buscarPorCorreo(principal.getName());
        if (usuario == null)
            return "redirect:/login";

        model.addAttribute("usuario", usuario);
        return "Administrador_Dashboard"; // nombre EXACTO del HTML
    }

    // ============================================
    // BUZÓN DE TAREAS
    // ============================================
    @GetMapping("/tareas/buzon")
    public String mostrarBuzonTareas(
            Principal principal,
            @RequestParam(required = false) String proyectoId,
            Model model) {

        if (principal == null) return "redirect:/login";

        Usuarios usuario = usuarioService.buscarPorCorreo(principal.getName());
        if (usuario == null) return "redirect:/login";

        List<Tarea> tareas = tareaService.obtenerTareasPorUsuario(usuario.getId());

        if (proyectoId != null && !proyectoId.isEmpty()) {
            tareas = tareas.stream()
                .filter(t -> proyectoId.equals(t.getIdProyecto()))
                .collect(java.util.stream.Collectors.toList());
            Proyecto proyecto = proyectoService.buscarPorId(proyectoId);
            model.addAttribute("proyectoFiltro", proyecto);
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("tareas", tareas);
        return "Buzon_Tareas_Usuario";
    }

    // ============================================
    // VER CREDENCIALES (PERFIL)
    // ============================================
    @GetMapping("/usuario/credenciales")
    public String verCredenciales(Principal principal, Model model) {

        if (principal == null) return "redirect:/login";

        Usuarios usuario = usuarioService.buscarPorCorreo(principal.getName());
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
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) return "redirect:/login";

        Usuarios usuario = usuarioService.buscarPorCorreo(principal.getName());
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

            // Encriptar la contraseña nueva usando BCrypt
            usuario.setContrasena(passwordEncoder.encode(contrasenaNueva));
        }

        // Guardar cambios
        usuarioService.guardarUsuario(usuario);

        redirectAttributes.addFlashAttribute("mensaje", "✅ Credenciales actualizadas correctamente");
        return "redirect:/usuario/credenciales";
    }

    // ============================================
    // ASIGNACIÓN DE TAREAS (LÍDER)
    // ============================================
    @GetMapping("/lider/asignar-tareas")
    public String mostrarAsignacionTareas(Principal principal, Model model) {

        if (principal == null) return "redirect:/login";

        Usuarios usuario = usuarioService.buscarPorCorreo(principal.getName());
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

    @GetMapping("/publico/proyectos")
    public String verProyectosPublico(
            @RequestParam(defaultValue = "0") int page,
            Principal principal, Model model) {

        PageRequest pageable = PageRequest.of(
                Math.max(page, 0), 12,
                Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        Page<Proyecto> pagina = proyectoService.listarPaginadoSinDatos(pageable);
        List<Proyecto> proyectos = pagina.getContent();

        Map<String, String> liderPorProyecto = new HashMap<>();
        for (Proyecto p : proyectos) {
            if (p.getIdLider() != null) {
                try {
                    Usuarios lider = usuarioService.obtenerUsuarioPorId(p.getIdLider());
                    liderPorProyecto.put(p.getId(), lider != null ? lider.getNombre() : "—");
                } catch (Exception e) {
                    liderPorProyecto.put(p.getId(), "—");
                }
            } else {
                liderPorProyecto.put(p.getId(), "—");
            }
        }

        if (principal != null) {
            Usuarios usuario = usuarioService.buscarPorCorreo(principal.getName());
            if (usuario != null) {
                model.addAttribute("usuario", usuario);
                model.addAttribute("inscritosSet",
                        participacionService.obtenerIdsProyectosPorUsuario(usuario.getId()));
            }
        }

        model.addAttribute("proyectos", proyectos);
        model.addAttribute("liderPorProyecto", liderPorProyecto);
        model.addAttribute("currentPage", pagina.getNumber());
        model.addAttribute("totalPages", pagina.getTotalPages());
        model.addAttribute("hasPrev", pagina.hasPrevious());
        model.addAttribute("hasNext", pagina.hasNext());
        model.addAttribute("returnUrl", "/");
        model.addAttribute("baseUrl", "/publico/proyectos");
        return "Participacion_Proyecto_Usuario";
    }
}