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

    @Autowired
    private com.proaula.spring.synergy.Security.IntentosLoginService intentosLogin;

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
    /*
     * "volver" = lista de proyectos (con filtros) desde donde el invitado pulsó Participar.
     * Se pasa de pantalla en pantalla para que Volver y el registro regresen allí.
     */
    @GetMapping("/seleccionar-rol")
    public String seleccionarRol(@RequestParam(required = false) String volver, Model model) {
        model.addAttribute("volver", SesionUtil.rutaVolverSegura(volver));
        return "seleccionar-rol";
    }

    @GetMapping({"/registrarse", "/registro/voluntario"})
    public String registroVoluntario(@RequestParam(required = false) String volver, Model model) {
        model.addAttribute("usuario", new Usuarios());
        model.addAttribute("volver", SesionUtil.rutaVolverSegura(volver));
        return "Registro";
    }

    @GetMapping("/registro/lider")
    public String registroLider(@RequestParam(required = false) String volver, Model model) {
        model.addAttribute("usuario", new Usuarios());
        model.addAttribute("volver", SesionUtil.rutaVolverSegura(volver));
        return "registro-lider";
    }

    @PostMapping("/registrarse")
    public String registrar(@ModelAttribute("usuario") Usuarios usuario,
                            @RequestParam(required = false) String confirmarContrasena,
                            @RequestParam(required = false) String volver,
                            HttpSession session,
                            jakarta.servlet.http.HttpServletRequest request,
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
        } else if (nombre.length() > 80 || correo.length() > 120 || !correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            error = "Revisa tu nombre y tu correo.";
        } else if (com.proaula.spring.synergy.Security.PoliticaContrasena.validar(contrasena) != null) {
            error = com.proaula.spring.synergy.Security.PoliticaContrasena.validar(contrasena);
        } else if (!contrasena.equals(confirmarContrasena)) {
            error = "Las contraseñas no coinciden.";
        } else if (usuarioService.buscarPorCorreo(correo) != null) {
            error = "Ya existe una cuenta con ese correo. Inicia sesión.";
        }

        if (error != null) {
            usuario.setContrasena(null);
            model.addAttribute("error", error);
            model.addAttribute("volver", SesionUtil.rutaVolverSegura(volver));
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

        // Nuevo id de sesión al autenticarse (evita la fijación de sesión)
        request.changeSessionId();
        SesionUtil.iniciar(session, nuevo);
        // Si venía de la lista de proyectos, vuelve allí (ya con sesión puede inscribirse)
        return "redirect:" + SesionUtil.destinoTrasEntrar(session, nuevo, volver);
    }

    // ============================================
    // LOGIN
    // ============================================
    @GetMapping("/login")
    public String mostrarLogin(@RequestParam(required = false) String volver, HttpSession session) {
        Usuarios actual = SesionUtil.usuario(session);
        if (actual != null) {
            return "redirect:" + SesionUtil.panelPorRol(actual);
        }
        String destino = SesionUtil.rutaVolverSegura(volver);
        if (destino != null) {
            session.setAttribute(SesionUtil.ATTR_VOLVER, destino);
        }
        return "Login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String correo,
                                @RequestParam String contrasena,
                                HttpSession session,
                                jakarta.servlet.http.HttpServletRequest request,
                                Model model) {

        String correoNormalizado = correo == null ? "" : correo.trim().toLowerCase();
        model.addAttribute("correo", correo == null ? "" : correo.trim());
        if (intentosLogin.estaBloqueado(correoNormalizado)) {
            model.addAttribute("error", intentosLogin.mensajeBloqueo(correoNormalizado));
            return "Login";
        }

        Usuarios usuario = usuarioService.buscarPorCorreo(correo == null ? "" : correo.trim().toLowerCase());
        if (usuario == null) {
            // Compatibilidad con correos guardados con mayúsculas
            usuario = usuarioService.buscarPorCorreo(correo);
        }

        if (usuario == null || usuario.getContrasena() == null
                || !passwordEncoder.matches(contrasena, usuario.getContrasena())) {
            boolean bloqueado = intentosLogin.registrarFallo(correoNormalizado);
            model.addAttribute("error", bloqueado ? intentosLogin.mensajeBloqueo(correoNormalizado)
                    : "Correo o contraseña incorrectos");
            return "Login";
        }
        intentosLogin.registrarExito(correoNormalizado);

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
        // Nuevo id de sesión al autenticarse (evita la fijación de sesión)
        request.changeSessionId();
        SesionUtil.iniciar(session, usuario);
        return "redirect:" + SesionUtil.destinoTrasEntrar(session, usuario, null);
    }

    /**
     * Botón "Continuar con Google". Si viene ?rol=LIDER, la cuenta nueva se crea como líder.
     */
    @GetMapping("/login/google")
    public String loginGoogle(@RequestParam(required = false) String rol,
                              @RequestParam(required = false) String volver,
                              HttpSession session) {
        String destino = SesionUtil.rutaVolverSegura(volver);
        if (destino != null) {
            session.setAttribute(SesionUtil.ATTR_VOLVER, destino);
        }
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
    @Autowired
    private com.proaula.spring.synergy.Service.PanelVoluntarioService panelVoluntario;

    @Autowired
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String mes, HttpSession session, Model model) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        if (!"PARTICIPANTE".equals(SesionUtil.rol(usuario))) {
            return "redirect:" + SesionUtil.panelPorRol(usuario);
        }

        com.proaula.spring.synergy.Service.PanelVoluntarioService.Panel panel = panelVoluntario.preparar(usuario);
        model.addAttribute("usuario", usuario);
        model.addAttribute("panel", panel);
        model.addAttribute("totalTareas", panel.totalTareas());
        model.addAttribute("tareasPendientes", panel.totalTareas() - panel.tareasCompletadas());
        model.addAttribute("tareasCompletadas", panel.tareasCompletadas());
        model.addAttribute("totalProyectos", panel.proyectos());
        // Línea de tiempo del mes con las fechas de entrega de sus tareas abiertas
        model.addAttribute("calendario", com.proaula.spring.synergy.Dto.Calendario.de(
                com.proaula.spring.synergy.Dto.Calendario.leerMes(mes, java.time.LocalDate.now()),
                panel.todasAbiertas(), Tarea::getFechaEntrega));
        model.addAttribute("diasSemana", com.proaula.spring.synergy.Dto.Calendario.diasSemana());
        model.addAttribute("hoy", java.time.LocalDate.now());
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

        // Tareas que este usuario ya entregó (para mostrar "Entregada"); sin descargar los archivos
        org.springframework.data.mongodb.core.query.Query qe = org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("idUsuario").is(usuario.getId()));
        qe.fields().include("idTarea");
        Set<String> entregadas = mongoTemplate.find(qe, com.proaula.spring.synergy.Model.EntregaTarea.class).stream()
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
            String problema = com.proaula.spring.synergy.Security.PoliticaContrasena.validar(contrasenaNueva);
            if (problema != null) {
                ra.addFlashAttribute("error", problema);
                return "redirect:/usuario/credenciales";
            }
            usuario.setContrasena(passwordEncoder.encode(contrasenaNueva));
            usuario.setUltimaCambioContrasena(LocalDateTime.now());
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
