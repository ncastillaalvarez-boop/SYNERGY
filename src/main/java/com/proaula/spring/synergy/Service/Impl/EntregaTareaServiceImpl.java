package com.proaula.spring.synergy.Service.Impl;

import com.proaula.spring.synergy.Model.EntregaTarea;
import com.proaula.spring.synergy.Repository.EntregaTareaRepository;
import com.proaula.spring.synergy.Service.EntregaTareaService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class EntregaTareaServiceImpl implements EntregaTareaService {

    @Autowired
    private EntregaTareaRepository entregaTareaRepository;

    public void guardarEntrega(String idTarea, MultipartFile archivo) throws IOException {
        guardarEntrega(idTarea, null, archivo);
    }

    public void guardarEntrega(String idTarea, String idUsuario, MultipartFile archivo) throws IOException {
        EntregaTarea entrega = new EntregaTarea();
        entrega.setIdTarea(idTarea);
        entrega.setIdUsuario(idUsuario);
        entrega.setNombreArchivo(archivo.getOriginalFilename());
        entrega.setTipoArchivo(archivo.getContentType());
        entrega.setDatos(archivo.getBytes());

        entregaTareaRepository.save(entrega);
    }

    public List<EntregaTarea> obtenerEntregasPorTarea(String idTarea) {
        return entregaTareaRepository.findByIdTarea(idTarea);
    }

    public boolean tareaYaEntregada(String idTarea) {
        return entregaTareaRepository.existsByIdTarea(idTarea);
    }

    public List<EntregaTarea> listar() {
        return entregaTareaRepository.findAll();
    }

    public EntregaTarea guardar(EntregaTarea entrega) {
        return entregaTareaRepository.save(entrega);
    }

    public void eliminar(String id) {
        entregaTareaRepository.deleteById(id);
    }
}
