package com.proaula.spring.synergy.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.proaula.spring.synergy.Model.Comentario;

@Repository
public interface ComentarioRepository extends MongoRepository<Comentario, String> {
    List<Comentario> findByIdProyecto(String idProyecto);
    List<Comentario> findByIdUsuario(String idUsuario);
    List<Comentario> findByIdTarea(String idTarea);
}

