package com.proaula.spring.synergy.Service.Impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.ProyectoRepository;
import com.proaula.spring.synergy.Repository.UsuarioRepository;
import com.proaula.spring.synergy.Service.ProyectoService;

@Service
public class ProyectoServiceImpl implements ProyectoService {

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<Proyecto> listar() {
        return proyectoRepository.findAll();
    }

    public Proyecto guardar(Proyecto proyecto, MultipartFile archivo) {
        try {
            if (archivo != null && !archivo.isEmpty()) {
                try {
                    proyecto.setDatos(archivo.getBytes());
                    proyecto.setNombreArchivo(archivo.getOriginalFilename());
                    proyecto.setTipoArchivo(archivo.getContentType());
                } catch (java.io.IOException e) {
                    throw new RuntimeException("Error al procesar la imagen: " + e.getMessage());
                }
            }

            if (proyecto.getIdLider() == null) {
                throw new RuntimeException("El proyecto debe tener un líder asignado");
            }

            proyecto.setFechaActualizacion(LocalDateTime.now());
            return proyectoRepository.save(proyecto);

        } catch (Exception e) {
            System.out.println("❌ Error guardando proyecto: " + e.getMessage());
            throw e;
        }
    }

    public Proyecto buscarPorId(String id) {
        return proyectoRepository.findById(id).orElse(null);
    }

    public void eliminar(String id) {
        proyectoRepository.deleteById(id);
    }

    public List<Proyecto> obtenerProyectosDeLider(String idLider) {
        return proyectoRepository.findByIdLider(idLider);
    }

    public List<Proyecto> listarProyectos() {
        return proyectoRepository.findAll();
    }

    public List<Proyecto> listarSinDatos() {
        return proyectoRepository.findAllWithoutData();
    }

    public Page<Proyecto> listarPaginado(Pageable pageable) {
        return proyectoRepository.findAll(pageable);
    }

    public Page<Proyecto> listarPaginadoSinDatos(Pageable pageable) {
        return proyectoRepository.findAllPagedWithoutData(pageable);
    }

    public List<Proyecto> obtenerProyectosDeLiderSinDatos(String idLider) {
        return proyectoRepository.findByIdLiderWithoutData(idLider);
    }
}

