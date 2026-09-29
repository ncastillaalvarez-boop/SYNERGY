package com.proaula.spring.synergy.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.proaula.spring.synergy.Model.Usuarios;

@Repository
public interface UsuarioRepository extends MongoRepository<Usuarios, String> {

    Usuarios findByCorreo(String correo);

    List<Usuarios> findByRol(String rol);

    List<Usuarios> findByActivo(Boolean activo);

    List<Usuarios> findByEstado(String estado);
}

