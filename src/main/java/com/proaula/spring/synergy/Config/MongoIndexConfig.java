package com.proaula.spring.synergy.Config;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.stereotype.Component;

/**
 * Crea los índices de MongoDB (@Indexed de los modelos) SIN tumbar la aplicación.
 *
 * Con spring.data.mongodb.auto-index-creation=true, Spring fallaba al iniciar si la
 * base ya tenía un índice igual con otro nombre (p. ej. "idLider_1" creado por un
 * script de Python/mongosh) -> "IndexOptionsConflict". Aquí:
 *   1. se crea cada índice por separado;
 *   2. si choca con uno viejo sobre los mismos campos, se borra el viejo y se reintenta;
 *   3. si aun así falla, solo se registra un aviso y la aplicación sigue.
 *
 * Lo invoca DataInitializer al arrancar (después de limpiar datos antiguos si los hay).
 */
@Component
public class MongoIndexConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexConfig.class);

    private final MongoTemplate mongoTemplate;
    private final MongoMappingContext mappingContext;

    public MongoIndexConfig(MongoTemplate mongoTemplate, MongoMappingContext mappingContext) {
        this.mongoTemplate = mongoTemplate;
        this.mappingContext = mappingContext;
    }

    public void crearIndices() {
        MongoPersistentEntityIndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);
        int creados = 0;

        for (MongoPersistentEntity<?> entidad : mappingContext.getPersistentEntities()) {
            if (!entidad.isAnnotationPresent(org.springframework.data.mongodb.core.mapping.Document.class)) {
                continue;
            }
            IndexOperations ops = mongoTemplate.indexOps(entidad.getType());
            for (IndexDefinition indice : resolver.resolveIndexFor(entidad.getTypeInformation())) {
                if (crear(ops, indice, entidad.getCollection())) {
                    creados++;
                }
            }
        }
        log.info("Índices de MongoDB verificados ({} definidos en los modelos)", creados);
    }

    private boolean crear(IndexOperations ops, IndexDefinition indice, String coleccion) {
        try {
            ops.createIndex(indice);
            return true;
        } catch (RuntimeException primerError) {
            // Índice viejo con los mismos campos pero otro nombre u opciones: se reemplaza
            Document claves = indice.getIndexKeys();
            for (IndexInfo existente : ops.getIndexInfo()) {
                if (!"_id_".equals(existente.getName()) && mismasClaves(existente, claves)) {
                    try {
                        ops.dropIndex(existente.getName());
                        ops.createIndex(indice);
                        log.info("Índice '{}' de '{}' reemplazado por la versión del modelo", existente.getName(), coleccion);
                        return true;
                    } catch (RuntimeException segundoError) {
                        log.warn("No se pudo recrear el índice {} de '{}': {}", claves.toJson(), coleccion, causaRaiz(segundoError));
                        return false;
                    }
                }
            }
            log.warn("Índice {} de '{}' no creado: {}", claves.toJson(), coleccion, causaRaiz(primerError));
            return false;
        }
    }

    private static boolean mismasClaves(IndexInfo info, Document claves) {
        if (info.getIndexFields().size() != claves.size()) {
            return false;
        }
        return info.getIndexFields().stream().allMatch(f -> claves.containsKey(f.getKey()));
    }

    private static String causaRaiz(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getMessage();
    }
}
