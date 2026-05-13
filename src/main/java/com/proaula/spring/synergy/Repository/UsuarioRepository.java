package com.proaula.spring.synergy.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.proaula.spring.synergy.Model.Usuarios;

@Repository
public interface UsuarioRepository extends MongoRepository<Usuarios, String> {

    Usuarios findByCorreo(String correo);

    List<Usuarios> findByIdRol(String idRol);

    List<Usuarios> findByEstado(String estado);
}
