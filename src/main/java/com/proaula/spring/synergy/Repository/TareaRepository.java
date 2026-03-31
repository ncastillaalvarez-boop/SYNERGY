package com.proaula.spring.synergy.Repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.proaula.spring.synergy.Model.Tarea;

@Repository
public interface TareaRepository extends MongoRepository<Tarea, String> {

    // Tareas asignadas a un usuario
    List<Tarea> findByUsuariosAsignadosContains(String idUsuario);

    // Tareas de un proyecto específico
    List<Tarea> findByIdProyecto(String idProyecto);

    List<Tarea> findByEstado(String estado);
}
