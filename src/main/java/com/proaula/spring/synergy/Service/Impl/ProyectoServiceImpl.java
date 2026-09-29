package com.proaula.spring.synergy.Service.Impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.proaula.spring.synergy.Model.Comentario;
import com.proaula.spring.synergy.Model.EntregaTarea;
import com.proaula.spring.synergy.Model.Participacion;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.ProyectoRepository;
import com.proaula.spring.synergy.Repository.UsuarioRepository;
import com.proaula.spring.synergy.Service.CodigoProyectoService;
import com.proaula.spring.synergy.Service.ProyectoService;

@Service
public class ProyectoServiceImpl implements ProyectoService {

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CodigoProyectoService codigoProyectoService;

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Proyecto> listar() {
        return proyectoRepository.findAll();
    }

    public Proyecto guardar(Proyecto proyecto, MultipartFile archivo) {
        if (proyecto.getIdLider() == null) {
            throw new RuntimeException("El proyecto debe tener un líder asignado");
        }
        Usuarios lider = usuarioRepository.findById(proyecto.getIdLider())
                .orElseThrow(() -> new RuntimeException("El líder asignado no existe"));

        // Nota: ya NO se asciende a LIDER al proponer. Un voluntario que propone un
        // proyecto pasa a ser líder cuando el administrador lo aprueba (AdminController).

        codigoProyectoService.asignarSiFalta(proyecto);
        proyecto.addParticipante(lider.getId());
        proyecto.setNombre(limpiar(proyecto.getNombre()));
        proyecto.setDescripcion(limpiar(proyecto.getDescripcion()));
        proyecto.setFechaActualizacion(LocalDateTime.now());
        Proyecto guardado = proyectoRepository.save(proyecto);

        if (!lider.getProyectos().contains(guardado.getId())) {
            lider.addProyecto(guardado.getId());
            usuarioRepository.save(lider);
        }
        return guardado;
    }

    private static String limpiar(String texto) {
        return texto == null ? null : texto.trim();
    }

    public Proyecto buscarPorId(String id) {
        return proyectoRepository.findById(id).orElse(null);
    }

    /**
     * Elimina el proyecto y todo lo que depende de él (tareas, entregas, comentarios,
     * participaciones) y lo quita de las listas de los usuarios. Antes quedaban
     * documentos huérfanos que rompían otras pantallas.
     */
    public void eliminar(String id) {
        Query deTareas = Query.query(Criteria.where("idProyecto").is(id));
        deTareas.fields().include("_id");
        List<String> idsTareas = mongoTemplate.find(deTareas, Tarea.class).stream().map(Tarea::getId).toList();

        if (!idsTareas.isEmpty()) {
            mongoTemplate.remove(Query.query(Criteria.where("idTarea").in(idsTareas)), EntregaTarea.class);
            mongoTemplate.updateMulti(new Query(Criteria.where("tareasAsignadas").in(idsTareas)),
                    new Update().pullAll("tareasAsignadas", idsTareas.toArray()), Usuarios.class);
        }
        mongoTemplate.remove(Query.query(Criteria.where("idProyecto").is(id)), Tarea.class);
        mongoTemplate.remove(Query.query(Criteria.where("idProyecto").is(id)), Comentario.class);
        mongoTemplate.remove(Query.query(Criteria.where("idProyecto").is(id)), Participacion.class);
        mongoTemplate.updateMulti(new Query(Criteria.where("proyectos").is(id)),
                new Update().pull("proyectos", id), Usuarios.class);
        proyectoRepository.deleteById(id);
    }

    public List<Proyecto> obtenerProyectosDeLider(String idLider) {
        return proyectoRepository.findByIdLider(idLider);
    }

    public List<Proyecto> listarProyectos() {
        return proyectoRepository.findAll();
    }
}

