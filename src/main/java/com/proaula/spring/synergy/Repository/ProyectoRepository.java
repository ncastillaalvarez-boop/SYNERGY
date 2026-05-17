package com.proaula.spring.synergy.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import com.proaula.spring.synergy.Model.Proyecto;

@Repository
public interface ProyectoRepository extends MongoRepository<Proyecto, String> {

    List<Proyecto> findByIdLider(String idLider);

    List<Proyecto> findByEstado(String estado);

    /** Lista todos los proyectos excluyendo el campo 'datos' (bytes de imagen). */
    @Query(value = "{}", fields = "{'datos': 0}")
    List<Proyecto> findAllWithoutData();

    /** Versión paginada sin bytes de imagen. */
    @Query(value = "{}", fields = "{'datos': 0}")
    Page<Proyecto> findAllPagedWithoutData(Pageable pageable);

    /** Proyectos de un líder excluyendo bytes de imagen. */
    @Query(value = "{'idLider': ?0}", fields = "{'datos': 0}")
    List<Proyecto> findByIdLiderWithoutData(String idLider);
}
