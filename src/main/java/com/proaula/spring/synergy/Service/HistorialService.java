package com.proaula.spring.synergy.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.RegistroAuditoria;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Historial de acciones con dos estructuras:
 *  - una PILA en memoria (deshacer / rehacer) que muestra la página /historial;
 *  - el registro de AUDITORÍA en MongoDB (colección "auditoria"), que no se pierde al
 *    reiniciar y no se puede borrar desde la interfaz (página /admin/auditoria).
 */
@Service
public class HistorialService {

    private static final Logger log = LoggerFactory.getLogger(HistorialService.class);
    /** Acciones que se cargan en la pila al arrancar. */
    private static final int CARGAR_AL_INICIAR = 200;

    @Autowired(required = false)
    private MongoTemplate mongoTemplate;
    
    // Clase para representar una acción en el historial
    public static class AccionHistorial {
        private final String tipo; // "CREAR", "EDITAR", "ELIMINAR"
        private final String entidad; // "PROYECTO", "TAREA", "USUARIO"
        private final String descripcion;
        private final LocalDateTime fecha;
        private final String usuario;
        private Map<String, Object> datosAnteriores;
        
        public AccionHistorial(String tipo, String entidad, String descripcion, String usuario) {
            this(tipo, entidad, descripcion, usuario, LocalDateTime.now());
        }

        public AccionHistorial(String tipo, String entidad, String descripcion, String usuario, LocalDateTime fecha) {
            this.tipo = tipo;
            this.entidad = entidad;
            this.descripcion = descripcion;
            this.usuario = usuario;
            this.fecha = fecha;
            this.datosAnteriores = new HashMap<>();
        }
        
        public void agregarDatoAnterior(String clave, Object valor) {
            this.datosAnteriores.put(clave, valor);
        }
        
        public String getFechaFormateada() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            return fecha.format(formatter);
        }
        
        public String getIcono() {
            return switch (tipo) {
                case "CREAR" -> "➕";
                case "EDITAR" -> "✏️";
                case "ELIMINAR" -> "🗑️";
                default -> "📌";
            };
        }
        
        // Getters
        public String getTipo() { return tipo; }
        public String getEntidad() { return entidad; }
        public String getDescripcion() { return descripcion; }
        public LocalDateTime getFecha() { return fecha; }
        public String getUsuario() { return usuario; }
        public Map<String, Object> getDatosAnteriores() { return datosAnteriores; }
    }
    
    // Pila principal de acciones
    private final Stack<AccionHistorial> pilaAcciones = new Stack<>();
    
    // Pila para rehacer
    private final Stack<AccionHistorial> pilaDeshacer = new Stack<>();
    
    /** Al arrancar, la pila se llena con las últimas acciones guardadas en la auditoría. */
    @jakarta.annotation.PostConstruct
    void cargarDesdeAuditoria() {
        if (mongoTemplate == null) {
            return;
        }
        try {
            Query q = new Query().with(Sort.by(Sort.Direction.DESC, "fecha")).limit(CARGAR_AL_INICIAR);
            List<RegistroAuditoria> ultimos = new ArrayList<>(mongoTemplate.find(q, RegistroAuditoria.class));
            Collections.reverse(ultimos);
            for (RegistroAuditoria r : ultimos) {
                pilaAcciones.push(new AccionHistorial(r.getTipo(), r.getEntidad(), r.getDescripcion(), r.getUsuario(),
                        r.getFecha() != null ? r.getFecha() : LocalDateTime.now()));
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo cargar el historial guardado: {}", e.getMessage());
        }
    }

    // Registrar acción
    public synchronized void registrarAccion(String tipo, String entidad, String descripcion, String usuario) {
        AccionHistorial accion = new AccionHistorial(tipo, entidad, descripcion, usuario);
        pilaAcciones.push(accion);
        pilaDeshacer.clear(); // Limpiar historial de rehacer
        guardarAuditoria(accion);
        log.info("Acción registrada: {} {} - {} ({})", tipo, entidad, descripcion, usuario);
    }

    private void guardarAuditoria(AccionHistorial a) {
        if (mongoTemplate == null) {
            return;
        }
        try {
            mongoTemplate.insert(new RegistroAuditoria(a.getTipo(), a.getEntidad(), a.getDescripcion(), a.getUsuario(), a.getFecha()));
        } catch (RuntimeException e) {
            // La auditoría nunca debe impedir la acción del usuario
            log.warn("No se pudo guardar la auditoría: {}", e.getMessage());
        }
    }
    
    // Registrar con datos anteriores (para deshacer)
    public void registrarAccionConDatos(String tipo, String entidad, String descripcion, 
                                       String usuario, Map<String, Object> datosAnteriores) {
        AccionHistorial accion = new AccionHistorial(tipo, entidad, descripcion, usuario);
        accion.datosAnteriores = datosAnteriores;
        pilaAcciones.push(accion);
        pilaDeshacer.clear();
        guardarAuditoria(accion);
    }
    
    // Deshacer última acción
    public AccionHistorial deshacer() {
        if (!pilaAcciones.isEmpty()) {
            AccionHistorial accion = pilaAcciones.pop();
            pilaDeshacer.push(accion);
            log.info("Deshaciendo: {}", accion.getDescripcion());
            return accion;
        }
        log.info("No hay acciones para deshacer");
        return null;
    }
    
    // Rehacer última acción deshecha
    public AccionHistorial rehacer() {
        if (!pilaDeshacer.isEmpty()) {
            AccionHistorial accion = pilaDeshacer.pop();
            pilaAcciones.push(accion);
            log.info("Rehaciendo: {}", accion.getDescripcion());
            return accion;
        }
        log.info("No hay acciones para rehacer");
        return null;
    }
    
    // Ver última acción sin eliminarla
    public AccionHistorial verUltimaAccion() {
        return pilaAcciones.isEmpty() ? null : pilaAcciones.peek();
    }
    
    // Obtener historial completo (del más reciente al más antiguo)
    public List<AccionHistorial> obtenerHistorialCompleto() {
        List<AccionHistorial> historial = new ArrayList<>(pilaAcciones);
        Collections.reverse(historial);
        return historial;
    }
    
    // Obtener últimas N acciones
    public List<AccionHistorial> obtenerUltimasAcciones(int cantidad) {
        List<AccionHistorial> historial = obtenerHistorialCompleto();
        return historial.subList(0, Math.min(cantidad, historial.size()));
    }
    
    // Filtrar por tipo de acción
    public List<AccionHistorial> filtrarPorTipo(String tipo) {
        return pilaAcciones.stream()
                .filter(a -> a.getTipo().equals(tipo))
                .toList();
    }
    
    // Filtrar por entidad
    public List<AccionHistorial> filtrarPorEntidad(String entidad) {
        return pilaAcciones.stream()
                .filter(a -> a.getEntidad().equals(entidad))
                .toList();
    }
    
    // Limpiar historial
    public void limpiarHistorial() {
        pilaAcciones.clear();
        pilaDeshacer.clear();
        log.info("Pila del historial limpiada (la auditoría en MongoDB se conserva)");
    }
    
    // Contar acciones
    public int contarAcciones() {
        return pilaAcciones.size();
    }
    
    // Hay acciones para deshacer
    public boolean hayAccionesParaDeshacer() {
        return !pilaAcciones.isEmpty();
    }
    
    // Hay acciones para rehacer
    public boolean hayAccionesParaRehacer() {
        return !pilaDeshacer.isEmpty();
    }
    
    // Mostrar historial en consola
    public void mostrarHistorial() {
        System.out.println("\n📚 HISTORIAL DE ACCIONES:");
        List<AccionHistorial> historial = obtenerHistorialCompleto();
        
        if (historial.isEmpty()) {
            System.out.println("   (vacío)");
            return;
        }
        
        for (int i = 0; i < historial.size(); i++) {
            AccionHistorial accion = historial.get(i);
            System.out.println((i + 1) + ". " + accion.getIcono() + " " + 
                             accion.getTipo() + " " + accion.getEntidad() + ": " +
                             accion.getDescripcion() + 
                             " [" + accion.getFechaFormateada() + "]");
        }
    }
}