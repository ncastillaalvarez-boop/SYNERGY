package com.proaula.spring.synergy.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.proaula.spring.synergy.Model.EntregaTarea;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.UsuarioRepository;

/** Datos del reporte imprimible de un proyecto (líder y administrador). */
@Service
public class ReporteService {

    private final TareaService tareaService;
    private final UsuarioRepository usuarioRepository;
    private final VoluntariosService voluntariosService;
    private final RiesgoProyectoService riesgoService;
    private final MongoTemplate mongoTemplate;

    public ReporteService(TareaService tareaService, UsuarioRepository usuarioRepository,
                          VoluntariosService voluntariosService, RiesgoProyectoService riesgoService,
                          MongoTemplate mongoTemplate) {
        this.tareaService = tareaService;
        this.usuarioRepository = usuarioRepository;
        this.voluntariosService = voluntariosService;
        this.riesgoService = riesgoService;
        this.mongoTemplate = mongoTemplate;
    }

    private List<EntregaTarea> entregasSinArchivo(Collection<String> idsTareas) {
        if (idsTareas.isEmpty()) {
            return new ArrayList<>();
        }
        Query q = Query.query(Criteria.where("idTarea").in(idsTareas));
        q.fields().exclude("datos");
        return mongoTemplate.find(q, EntregaTarea.class);
    }

    public void preparar(Proyecto proyecto, String nombreLider, Model model) {
        List<Tarea> tareas = tareaService.obtenerTareasPorProyecto(proyecto.getId());
        List<Usuarios> integrantes = new ArrayList<>();
        usuarioRepository.findAllById(proyecto.getParticipantes()).forEach(integrantes::add);
        Set<String> activos = voluntariosService.activos(proyecto);
        Map<String, Long> entregasPorUsuario = new HashMap<>();
        List<EntregaTarea> entregas = entregasSinArchivo(tareas.stream().map(Tarea::getId).toList());
        for (EntregaTarea e : entregas) {
            entregasPorUsuario.merge(e.getIdUsuario() == null ? "" : e.getIdUsuario(), 1L, Long::sum);
        }
        long hechas = tareas.stream().filter(t -> "COMPLETADA".equalsIgnoreCase(t.getEstado())).count();
        long voluntarios = integrantes.stream().filter(u -> !u.getId().equals(proyecto.getIdLider())).count();
        long voluntariosActivos = integrantes.stream()
                .filter(u -> !u.getId().equals(proyecto.getIdLider()) && activos.contains(u.getId())).count();
        model.addAttribute("proyecto", proyecto);
        model.addAttribute("nombreLider", nombreLider);
        model.addAttribute("tareas", tareas);
        model.addAttribute("integrantes", integrantes);
        model.addAttribute("activosIds", activos);
        model.addAttribute("entregasPorUsuario", entregasPorUsuario);
        model.addAttribute("totalEntregas", entregas.size());
        model.addAttribute("entregasAprobadas", entregas.stream().filter(e -> "REVISADO".equals(e.getEstado())).count());
        model.addAttribute("tareasHechas", hechas);
        model.addAttribute("progreso", tareas.isEmpty() ? 0 : Math.round(hechas * 100f / tareas.size()));
        model.addAttribute("voluntarios", voluntarios);
        model.addAttribute("participacion", voluntarios == 0 ? 0 : Math.round(voluntariosActivos * 100f / voluntarios));
        model.addAttribute("historialRiesgo", riesgoService.historial(proyecto.getId(), 70));
        model.addAttribute("generado", LocalDateTime.now());
    }
}
