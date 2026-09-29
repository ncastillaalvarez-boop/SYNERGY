package com.proaula.spring.synergy.Controller;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.util.UriComponentsBuilder;

import com.proaula.spring.synergy.Dto.FiltroProyectos;
import com.proaula.spring.synergy.Dto.Paginacion;
import com.proaula.spring.synergy.Model.Catalogos;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Security.SesionUtil;
import com.proaula.spring.synergy.Service.BusquedaService;
import com.proaula.spring.synergy.Service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * "Explorar proyectos" con búsqueda, filtros y paginación.
 *
 *  - /publico/proyectos   → SOLO invitados. Si hay una sesión abierta se redirige a
 *                            /proyectos/participar (antes el invitado veía la página
 *                            con la cuenta del último usuario que había entrado).
 *  - /proyectos/participar → usuarios con sesión (pueden inscribirse).
 */
@Controller
public class ExplorarController {

    public static final int POR_PAGINA = 12;

    private final BusquedaService busquedaService;
    private final UsuarioService usuarioService;

    public ExplorarController(BusquedaService busquedaService, UsuarioService usuarioService) {
        this.busquedaService = busquedaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping({"/publico/proyectos", "/proyectos/publico"})
    public String explorarComoInvitado(@ModelAttribute("filtro") FiltroProyectos filtro,
                                       HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session != null && SesionUtil.usuario(session) != null) {
            String query = request.getQueryString();
            return "redirect:/proyectos/participar" + (query == null ? "" : "?" + query);
        }
        prepararVista(filtro, null, "/publico/proyectos", model);
        model.addAttribute("modoInvitado", true);
        return "Participacion_Proyecto_Usuario";
    }

    @GetMapping("/proyectos/participar")
    public String explorarConSesion(@ModelAttribute("filtro") FiltroProyectos filtro,
                                    HttpSession session, Model model) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        // Datos frescos: la lista de proyectos inscritos cambia al participar
        Usuarios fresco = usuarioService.obtenerUsuarioPorId(usuario.getId());
        prepararVista(filtro, fresco, "/proyectos/participar", model);
        model.addAttribute("modoInvitado", false);
        return "Participacion_Proyecto_Usuario";
    }

    private void prepararVista(FiltroProyectos filtro, Usuarios usuario, String ruta, Model model) {
        filtro.estadoPorDefecto("ACTIVO");
        Page<Proyecto> pagina = busquedaService.buscarProyectos(filtro, Catalogos.ESTADOS_PUBLICOS, POR_PAGINA);

        Set<String> idsLideres = pagina.getContent().stream()
                .map(Proyecto::getIdLider)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Set<String> inscritos = usuario == null || usuario.getProyectos() == null
                ? Set.of()
                : new HashSet<>(usuario.getProyectos());

        model.addAttribute("usuario", usuario);
        model.addAttribute("proyectos", pagina.getContent());
        model.addAttribute("paginacion", new Paginacion(pagina, ruta, filtro.comoParametros()));
        model.addAttribute("lideres", busquedaService.nombresDe(idsLideres));
        model.addAttribute("inscritos", inscritos);
        model.addAttribute("rutaExplorar", ruta);
        // URL actual (filtros + página) para volver aquí después de inscribirse
        UriComponentsBuilder volver = UriComponentsBuilder.fromPath(ruta).queryParams(filtro.comoParametros());
        if (pagina.getNumber() > 0) {
            volver.queryParam("page", pagina.getNumber() + 1);
        }
        model.addAttribute("volver", volver.encode().build().toUriString());
    }
}
