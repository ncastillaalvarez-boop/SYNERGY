package com.proaula.spring.synergy.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.Notificacion;
import com.proaula.spring.synergy.Model.Usuarios;

/**
 * Avisos dentro de la aplicación (campana de la web y pantalla de la app).
 * Crear un aviso nunca debe impedir la acción principal: los errores solo se registran.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    /** Los avisos leídos se borran después de este tiempo. */
    private static final int DIAS_CONSERVAR_LEIDAS = 90;

    private final MongoTemplate mongoTemplate;

    public NotificacionService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void notificar(String idUsuario, String tipo, String titulo, String mensaje, String enlace) {
        if (idUsuario == null) {
            return;
        }
        try {
            mongoTemplate.insert(new Notificacion(idUsuario, tipo, recortar(titulo, 120), recortar(mensaje, 500), enlace));
        } catch (RuntimeException e) {
            log.warn("No se pudo crear la notificación para {}: {}", idUsuario, e.getMessage());
        }
    }

    public int notificarVarios(Collection<String> idsUsuarios, String tipo, String titulo, String mensaje, String enlace) {
        if (idsUsuarios == null || idsUsuarios.isEmpty()) {
            return 0;
        }
        List<Notificacion> lote = new ArrayList<>();
        for (String id : new LinkedHashSet<>(idsUsuarios)) {
            if (id != null) {
                lote.add(new Notificacion(id, tipo, recortar(titulo, 120), recortar(mensaje, 500), enlace));
            }
        }
        try {
            mongoTemplate.insert(lote, Notificacion.class);
            return lote.size();
        } catch (RuntimeException e) {
            log.warn("No se pudieron crear {} notificaciones: {}", lote.size(), e.getMessage());
            return 0;
        }
    }

    public void notificarAdministradores(String tipo, String titulo, String mensaje, String enlace) {
        try {
            Query q = Query.query(Criteria.where("rol").is("ADMINISTRADOR").and("activo").is(true));
            q.fields().include("_id");
            List<String> ids = mongoTemplate.find(q, Usuarios.class).stream().map(Usuarios::getId).toList();
            notificarVarios(ids, tipo, titulo, mensaje, enlace);
        } catch (RuntimeException e) {
            log.warn("No se pudo avisar a los administradores: {}", e.getMessage());
        }
    }

    public long contarNoLeidas(String idUsuario) {
        if (idUsuario == null) {
            return 0;
        }
        try {
            return mongoTemplate.count(Query.query(Criteria.where("idUsuario").is(idUsuario).and("leida").is(false)), Notificacion.class);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    public List<Notificacion> recientes(String idUsuario, int cantidad) {
        Query q = Query.query(Criteria.where("idUsuario").is(idUsuario))
                .with(Sort.by(Sort.Direction.DESC, "fecha")).limit(Math.max(1, Math.min(cantidad, 200)));
        return mongoTemplate.find(q, Notificacion.class);
    }

    public void marcarTodasLeidas(String idUsuario) {
        mongoTemplate.updateMulti(Query.query(Criteria.where("idUsuario").is(idUsuario).and("leida").is(false)),
                Update.update("leida", true), Notificacion.class);
    }

    /** Marca una sola (solo si pertenece al usuario). Devuelve el enlace del aviso. */
    public String marcarLeida(String idNotificacion, String idUsuario) {
        Notificacion n = mongoTemplate.findById(idNotificacion, Notificacion.class);
        if (n == null || !idUsuario.equals(n.getIdUsuario())) {
            return null;
        }
        if (!n.isLeida()) {
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(idNotificacion)), Update.update("leida", true), Notificacion.class);
        }
        return n.getEnlace();
    }

    @Scheduled(cron = "0 15 3 * * *")
    public void limpiarAntiguas() {
        try {
            mongoTemplate.remove(Query.query(Criteria.where("leida").is(true)
                    .and("fecha").lt(LocalDateTime.now().minusDays(DIAS_CONSERVAR_LEIDAS))), Notificacion.class);
        } catch (RuntimeException e) {
            log.warn("No se pudieron limpiar las notificaciones antiguas: {}", e.getMessage());
        }
    }

    private static String recortar(String texto, int max) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }
}
