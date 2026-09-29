package com.proaula.spring.synergy.Service;

import java.time.Year;

import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.Proyecto;

/**
 * Genera el código único de cada proyecto: SYN-AAAA-NNNNN.
 * Usa un contador atómico en la colección "contadores", así dos proyectos
 * creados al mismo tiempo nunca reciben el mismo número.
 */
@Service
public class CodigoProyectoService {

    public static final String COLECCION = "contadores";
    private static final String CONTADOR = "proyectos";

    private final MongoTemplate mongoTemplate;

    public CodigoProyectoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public String siguienteCodigo() {
        Document contador = mongoTemplate.findAndModify(
            Query.query(Criteria.where("_id").is(CONTADOR)),
            new Update().inc("seq", 1L),
            FindAndModifyOptions.options().returnNew(true).upsert(true),
            Document.class,
            COLECCION);
        long numero = contador == null ? 1 : ((Number) contador.get("seq")).longValue();
        return formatear(Year.now().getValue(), numero);
    }

    /** Asigna código al proyecto si todavía no tiene. */
    public void asignarSiFalta(Proyecto proyecto) {
        if (proyecto.getCodigo() == null || proyecto.getCodigo().isBlank()) {
            proyecto.setCodigo(siguienteCodigo());
        }
    }

    /** Deja el contador en un valor concreto (lo usa el generador de datos). */
    public void fijarContador(long valor) {
        mongoTemplate.upsert(
            Query.query(Criteria.where("_id").is(CONTADOR)),
            new Update().set("seq", valor),
            COLECCION);
    }

    public static String formatear(int anio, long numero) {
        return String.format("SYN-%d-%05d", anio, numero);
    }
}
