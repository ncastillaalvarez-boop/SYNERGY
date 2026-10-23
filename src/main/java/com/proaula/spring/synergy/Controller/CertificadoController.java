package com.proaula.spring.synergy.Controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Model.EntregaTarea;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.UsuarioRepository;
import com.proaula.spring.synergy.Security.SesionUtil;
import com.proaula.spring.synergy.Service.ProyectoService;

import jakarta.servlet.http.HttpSession;

/**
 * Certificado de participación (para las horas sociales). Solo para proyectos COMPLETADOS y
 * solo para quien participó. Es una página imprimible: el navegador la guarda como PDF.
 */
@Controller
public class CertificadoController {

    private final ProyectoService proyectoService;
    private final UsuarioRepository usuarioRepository;
    private final MongoTemplate mongoTemplate;

    public CertificadoController(ProyectoService proyectoService, UsuarioRepository usuarioRepository, MongoTemplate mongoTemplate) {
        this.proyectoService = proyectoService;
        this.usuarioRepository = usuarioRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/certificado/{proyectoId}")
    public String certificado(@PathVariable String proyectoId, HttpSession session, Model model, RedirectAttributes ra) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        Proyecto proyecto = proyectoService.buscarPorId(proyectoId);
        if (proyecto == null || proyecto.getParticipantes() == null || !proyecto.getParticipantes().contains(usuario.getId())) {
            ra.addFlashAttribute("error", "Solo puedes descargar certificados de proyectos en los que participaste");
            return "redirect:/dashboard";
        }
        if (!"COMPLETADO".equals(proyecto.getEstado())) {
            ra.addFlashAttribute("error", "El certificado estará disponible cuando el proyecto se complete");
            return "redirect:/dashboard";
        }
        Query qt = Query.query(Criteria.where("idProyecto").is(proyectoId));
        qt.fields().include("_id", "estado", "usuariosAsignados");
        List<Tarea> tareas = mongoTemplate.find(qt, Tarea.class);
        long asignadas = tareas.stream().filter(t -> t.getUsuariosAsignados() != null && t.getUsuariosAsignados().contains(usuario.getId())).count();
        List<String> ids = tareas.stream().map(Tarea::getId).toList();
        long entregas = ids.isEmpty() ? 0 : mongoTemplate.count(Query.query(Criteria.where("idTarea").in(ids)
                .and("idUsuario").is(usuario.getId())), EntregaTarea.class);
        String lider = proyecto.getIdLider() == null ? "" : usuarioRepository.findById(proyecto.getIdLider())
                .map(Usuarios::getNombre).orElse("");

        model.addAttribute("usuario", usuario);
        model.addAttribute("proyecto", proyecto);
        model.addAttribute("nombreLider", lider);
        model.addAttribute("tareasAsignadas", asignadas);
        model.addAttribute("entregas", entregas);
        model.addAttribute("emitido", LocalDate.now());
        // Código de verificación sencillo (no reemplaza una firma): proyecto + usuario
        model.addAttribute("codigoVerificacion", (proyecto.getCodigo() == null ? "SYN" : proyecto.getCodigo()) + "-"
                + Integer.toHexString((proyecto.getId() + usuario.getId()).hashCode()).toUpperCase());
        return "Certificado";
    }
}
