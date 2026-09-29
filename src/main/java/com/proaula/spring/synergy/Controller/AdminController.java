package com.proaula.spring.synergy.Controller;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.util.UriComponentsBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Dto.FiltroProyectos;
import com.proaula.spring.synergy.Dto.Paginacion;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.ProyectoRepository;
import com.proaula.spring.synergy.Security.SesionUtil;
import com.proaula.spring.synergy.Service.BusquedaService;
import com.proaula.spring.synergy.Service.HistorialService;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Set<String> ROLES_VALIDOS = Set.of("ADMINISTRADOR", "LIDER", "PARTICIPANTE");
    private static final int POR_PAGINA = 15;
    private static final int POR_PAGINA_USUARIOS = 20;

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private HistorialService historialService;

    @Autowired
    private BusquedaService busquedaService;

    private Usuarios validarAdmin(HttpSession session) {
        Usuarios usuario = SesionUtil.usuario(session);
        return SesionUtil.esAdmin(usuario) ? usuario : null;
    }

    // ---------------------------------------------------------
    // PROYECTOS
    // ---------------------------------------------------------
    @GetMapping({"", "/", "/proyectos"})
    public String verProyectos(@ModelAttribute("filtro") FiltroProyectos filtro,
                               HttpSession session, Model model) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        // null = el administrador ve todos los estados, incluidos PENDIENTE y CANCELADO
        Page<Proyecto> pagina = busquedaService.buscarProyectos(filtro, null, POR_PAGINA);
        Set<String> idsLideres = pagina.getContent().stream()
                .map(Proyecto::getIdLider).filter(id -> id != null).collect(Collectors.toSet());

        model.addAttribute("usuario", admin);
        model.addAttribute("proyectos", pagina.getContent());
        model.addAttribute("paginacion", new Paginacion(pagina, "/admin/proyectos", filtro.comoParametros()));
        model.addAttribute("lideres", busquedaService.nombresDe(idsLideres));
        model.addAttribute("porEstado", busquedaService.proyectosPorEstado());
        model.addAttribute("volverProyectos", volverActual("/admin/proyectos", filtro.comoParametros(), filtro.getPage()));
        model.addAttribute("totalProyectos", busquedaService.contarProyectos(null));
        model.addAttribute("pendientes", busquedaService.contarProyectos("PENDIENTE"));
        model.addAttribute("activos", busquedaService.contarProyectos("ACTIVO"));
        model.addAttribute("totalUsuarios", busquedaService.contarUsuarios(null));
        return "Administrador_Dashboard";
    }

    @GetMapping("/proyecto/{id}")
    public String verProyecto(@PathVariable String id, HttpSession session, Model model) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto == null) {
            return "redirect:/admin/proyectos";
        }

        String nombreLider = "Sin asignar";
        if (proyecto.getIdLider() != null) {
            try {
                nombreLider = usuarioService.obtenerUsuarioPorId(proyecto.getIdLider()).getNombre();
            } catch (RuntimeException e) {
                nombreLider = "Líder no encontrado";
            }
        }

        model.addAttribute("usuario", admin);
        model.addAttribute("proyecto", proyecto);
        model.addAttribute("nombreLider", nombreLider);
        return "Admin_Ver_Proyecto";
    }

    /** Aprueba un proyecto propuesto: pasa de PENDIENTE a ACTIVO. */
    @PostMapping("/proyecto/{id}/aceptar")
    public String aceptarProyecto(@PathVariable String id,
                                  @RequestParam(value = "volver", required = false) String volver,
                                  HttpSession session, RedirectAttributes ra) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto != null && "PENDIENTE".equalsIgnoreCase(proyecto.getEstado())) {
            proyecto.setEstado("ACTIVO");
            proyecto.setFechaActualizacion(LocalDateTime.now());
            proyectoRepository.save(proyecto);

            // Quien propuso el proyecto pasa a ser LÍDER al aprobarse (no antes)
            try {
                Usuarios proponente = usuarioService.obtenerUsuarioPorId(proyecto.getIdLider());
                if ("PARTICIPANTE".equalsIgnoreCase(proponente.getRol())) {
                    proponente.setRol("LIDER");
                    usuarioService.guardarUsuario(proponente);
                }
            } catch (RuntimeException e) {
                // proponente eliminado: el proyecto queda aprobado igual
            }

            historialService.registrarAccion("EDITAR", "PROYECTO", "Proyecto aprobado: " + proyecto.getNombre(), admin.getCorreo());
            ra.addFlashAttribute("mensaje", "Proyecto '" + proyecto.getNombre() + "' aprobado y publicado");
        }
        return "redirect:" + volverA(volver, "/admin/proyectos");
    }

    /** Rechaza una propuesta: pasa de PENDIENTE a CANCELADO (queda registro, no se borra). */
    @PostMapping("/proyecto/{id}/rechazar")
    public String rechazarProyecto(@PathVariable String id,
                                   @RequestParam(value = "volver", required = false) String volver,
                                   HttpSession session, RedirectAttributes ra) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }
        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto != null && "PENDIENTE".equalsIgnoreCase(proyecto.getEstado())) {
            proyecto.setEstado("CANCELADO");
            proyecto.setFechaActualizacion(LocalDateTime.now());
            proyectoRepository.save(proyecto);
            historialService.registrarAccion("EDITAR", "PROYECTO", "Propuesta rechazada: " + proyecto.getNombre(), admin.getCorreo());
            ra.addFlashAttribute("mensaje", "Propuesta '" + proyecto.getNombre() + "' rechazada");
        }
        return "redirect:" + volverA(volver, "/admin/proyectos");
    }

    @PostMapping("/proyecto/{id}/eliminar")
    public String eliminarProyecto(@PathVariable String id,
                                   @RequestParam(value = "volver", required = false) String volver,
                                   HttpSession session, RedirectAttributes ra) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto != null) {
            proyectoService.eliminar(id);
            historialService.registrarAccion("ELIMINAR", "PROYECTO", "Proyecto eliminado: " + proyecto.getNombre(), admin.getCorreo());
            ra.addFlashAttribute("mensaje", "Proyecto '" + proyecto.getNombre() + "' eliminado");
        }
        return "redirect:" + volverA(volver, "/admin/proyectos");
    }

    // ---------------------------------------------------------
    // USUARIOS
    // ---------------------------------------------------------
    @GetMapping("/usuarios")
    public String verUsuarios(@RequestParam(required = false) String q,
                              @RequestParam(required = false) String rol,
                              @RequestParam(required = false) String estado,
                              @RequestParam(defaultValue = "1") int page,
                              Model model, HttpSession session) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }
        Page<Usuarios> pagina = busquedaService.buscarUsuarios(q, rol, estado, page, POR_PAGINA_USUARIOS);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (q != null && !q.isBlank()) params.add("q", q.trim());
        if (rol != null && !rol.isBlank()) params.add("rol", rol);
        if (estado != null && !estado.isBlank()) params.add("estado", estado);

        model.addAttribute("usuario", admin);
        model.addAttribute("usuarios", pagina.getContent());
        model.addAttribute("paginacion", new Paginacion(pagina, "/admin/usuarios", params));
        model.addAttribute("q", q);
        model.addAttribute("rolFiltro", rol);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("hayFiltros", !params.isEmpty());
        model.addAttribute("volverUsuarios", volverActual("/admin/usuarios", params, page));
        model.addAttribute("totalUsuarios", busquedaService.contarUsuarios(null));
        model.addAttribute("totalAdmins", busquedaService.contarUsuarios("ADMINISTRADOR"));
        model.addAttribute("totalLideres", busquedaService.contarUsuarios("LIDER"));
        model.addAttribute("totalVoluntarios", busquedaService.contarUsuarios("PARTICIPANTE"));
        return "Admin_Usuarios_Dashboard";
    }

    /** URL de la página actual (con filtros) para volver después de una acción. */
    private static String volverActual(String ruta, MultiValueMap<String, String> params, int page) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath(ruta).queryParams(params);
        if (page > 1) {
            b.queryParam("page", page);
        }
        return b.encode().build().toUriString();
    }

    /** Solo permite volver a rutas internas del panel de administración. */
    private static String volverA(String volver, String porDefecto) {
        if (volver != null && volver.startsWith("/admin/") && !volver.contains("//")) {
            return volver;
        }
        return porDefecto;
    }

    @PostMapping("/usuario/{id}/eliminar")
    public String eliminarUsuario(@PathVariable String id, @RequestParam(value = "volver", required = false) String volver,
                             HttpSession session, RedirectAttributes ra) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }
        if (id.equals(admin.getId())) {
            ra.addFlashAttribute("error", "No puedes eliminar tu propia cuenta de administrador");
            return "redirect:" + volverA(volver, "/admin/usuarios");
        }

        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorId(id);
            usuarioService.eliminarUsuario(id);
            historialService.registrarAccion("ELIMINAR", "USUARIO", "Usuario eliminado: " + usuario.getCorreo(), admin.getCorreo());
            ra.addFlashAttribute("mensaje", "Usuario '" + usuario.getNombre() + "' eliminado");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "El usuario no existe");
        }
        return "redirect:" + volverA(volver, "/admin/usuarios");
    }

    @PostMapping("/usuario/{id}/cambiar-rol")
    public String cambiarRol(@PathVariable String id, @RequestParam String nuevoRol,
                             @RequestParam(value = "volver", required = false) String volver,
                             HttpSession session, RedirectAttributes ra) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        String rol = nuevoRol == null ? "" : nuevoRol.trim().toUpperCase();
        if (!ROLES_VALIDOS.contains(rol)) {
            ra.addFlashAttribute("error", "Rol no válido");
            return "redirect:" + volverA(volver, "/admin/usuarios");
        }
        if (id.equals(admin.getId()) && !"ADMINISTRADOR".equals(rol)) {
            ra.addFlashAttribute("error", "No puedes quitarte el rol de administrador a ti mismo");
            return "redirect:" + volverA(volver, "/admin/usuarios");
        }

        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorId(id);
            // Un líder con proyectos no puede pasar a voluntario: sus proyectos quedarían sin gestor
            if ("PARTICIPANTE".equals(rol) && "LIDER".equalsIgnoreCase(usuario.getRol())
                    && !proyectoService.obtenerProyectosDeLider(id).isEmpty()) {
                ra.addFlashAttribute("error", "'" + usuario.getNombre() + "' lidera proyectos: no puede pasar a voluntario");
                return "redirect:" + volverA(volver, "/admin/usuarios");
            }
            usuario.setRol(rol);
            usuarioService.guardarUsuario(usuario);
            historialService.registrarAccion("EDITAR", "USUARIO", "Rol de " + usuario.getCorreo() + " cambiado a " + rol, admin.getCorreo());
            ra.addFlashAttribute("mensaje", "Rol de '" + usuario.getNombre() + "' cambiado a " + rol);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "El usuario no existe");
        }
        return "redirect:" + volverA(volver, "/admin/usuarios");
    }

    /** Activa o desactiva una cuenta (un usuario inactivo no puede iniciar sesión). */
    @PostMapping("/usuario/{id}/estado")
    public String cambiarEstado(@PathVariable String id, @RequestParam(value = "volver", required = false) String volver,
                             HttpSession session, RedirectAttributes ra) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }
        if (id.equals(admin.getId())) {
            ra.addFlashAttribute("error", "No puedes desactivar tu propia cuenta");
            return "redirect:" + volverA(volver, "/admin/usuarios");
        }

        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorId(id);
            if (usuario.isEnabled()) {
                usuarioService.desactivarUsuario(id);
                ra.addFlashAttribute("mensaje", "Cuenta de '" + usuario.getNombre() + "' desactivada");
            } else {
                usuarioService.activarUsuario(id);
                ra.addFlashAttribute("mensaje", "Cuenta de '" + usuario.getNombre() + "' activada");
            }
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "El usuario no existe");
        }
        return "redirect:" + volverA(volver, "/admin/usuarios");
    }
}
