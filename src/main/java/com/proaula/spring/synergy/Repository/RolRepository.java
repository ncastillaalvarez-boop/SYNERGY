package com.proaula.spring.synergy.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.proaula.spring.synergy.Model.Rol;

@Repository
public interface RolRepository extends MongoRepository<Rol, String> {

    Optional<Rol> findByNombre(String nombre);

    List<Rol> findByActivo(Boolean activo);
}
