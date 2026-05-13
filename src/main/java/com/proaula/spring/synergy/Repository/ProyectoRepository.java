package com.proaula.spring.synergy.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.proaula.spring.synergy.Model.Proyecto;

@Repository
public interface ProyectoRepository extends MongoRepository<Proyecto, String> {

    List<Proyecto> findByIdLider(String idLider);

    List<Proyecto> findByEstado(String estado);
}
