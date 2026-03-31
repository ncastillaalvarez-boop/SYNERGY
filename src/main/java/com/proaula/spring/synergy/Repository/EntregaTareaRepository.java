package com.proaula.spring.synergy.Repository;

import com.proaula.spring.synergy.Model.EntregaTarea;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EntregaTareaRepository extends MongoRepository<EntregaTarea, String> {

    List<EntregaTarea> findByIdTarea(String idTarea);

    List<EntregaTarea> findByIdUsuario(String idUsuario);

    boolean existsByIdTarea(String idTarea);
}

