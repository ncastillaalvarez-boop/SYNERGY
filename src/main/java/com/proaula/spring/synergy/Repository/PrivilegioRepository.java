package com.proaula.spring.synergy.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.proaula.spring.synergy.Model.Privilegio;

@Repository
public interface PrivilegioRepository extends MongoRepository<Privilegio, String> {

    Optional<Privilegio> findByNombre(String nombre);

    List<Privilegio> findByRecurso(String recurso);

    List<Privilegio> findByAccion(String accion);

    List<Privilegio> findByActivo(Boolean activo);

    List<Privilegio> findByRecursoAndAccion(String recurso, String accion);
}
