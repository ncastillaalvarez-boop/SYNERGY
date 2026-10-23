package com.proaula.spring.synergy.Config;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.proaula.spring.synergy.Model.Catalogos;
import com.proaula.spring.synergy.Model.Comentario;
import com.proaula.spring.synergy.Model.EntregaTarea;
import com.proaula.spring.synergy.Model.Participacion;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Service.CodigoProyectoService;

/**
 * Genera un volumen realista de datos (≈17.000 documentos) ambientados en Cartagena,
 * con TODAS las relaciones coherentes entre colecciones:
 *
 *   proyectos.participantes  ⇄  participaciones  ⇄  usuarios.proyectos
 *   proyectos.tareas         ⇄  tareas.idProyecto
 *   tareas.usuariosAsignados ⇄  usuarios.tareasAsignadas  (solo integrantes del proyecto)
 *   tareas.entregas          ⇄  entregas_tareas.idTarea
 *   proyectos.comentarios    ⇄  comentarios.idProyecto
 *
 * Las 3 cuentas de prueba reciben datos propios para poder probar cada panel:
 * lider@synergy.com lidera 12 proyectos y voluntario@synergy.com participa en varios.
 *
 * Todos los usuarios generados usan la contraseña {@value #CLAVE_GENERADOS}.
 * Usa una semilla fija: cada ejecución genera exactamente los mismos datos.
 */
@Component
public class GeneradorDatos {

    public static final String CLAVE_GENERADOS = "Synergy123*";
    private static final Logger log = LoggerFactory.getLogger(GeneradorDatos.class);

    private static final String[] NOMBRES = {
        "Andrés", "Camila", "Santiago", "Valentina", "Juan", "Daniela", "Carlos", "Mariana", "Luis", "Laura",
        "Jorge", "Sofía", "Miguel", "Isabella", "Diego", "Natalia", "Sebastián", "Paula", "Alejandro", "Gabriela",
        "Felipe", "Juliana", "David", "Carolina", "Mateo", "Andrea", "Nicolás", "Luisa", "Samuel", "Manuela",
        "Óscar", "Ana", "Ricardo", "María José", "Kevin", "Yuliana", "Jhon", "Estefanía", "Esteban", "Vanessa",
        "Rafael", "Melissa", "Álvaro", "Tatiana", "Edwin", "Lina", "Fabián", "Diana", "Hernán", "Karen",
        "Iván", "Lorena", "Wilson", "Paola", "Emilio", "Ximena", "Julián", "Rosa", "Gustavo", "Angélica"
    };

    private static final String[] APELLIDOS = {
        "Pérez", "Gómez", "Rodríguez", "Martínez", "Hernández", "García", "López", "Díaz", "Torres", "Ramírez",
        "Castro", "Vargas", "Rojas", "Moreno", "Jiménez", "Suárez", "Ortiz", "Romero", "Herrera", "Medina",
        "Barrios", "Julio", "Cassiani", "Caraballo", "Pájaro", "Marrugo", "Villalba", "Castellar", "Arrieta", "Padilla",
        "Julio", "Guerrero", "Mendoza", "Rincón", "Salcedo", "Blanco", "Tovar", "Cabarcas", "Puello", "Ospina",
        "Zapata", "Valdés", "Canate", "Olivares", "Meza", "Acosta", "Ruiz", "Bustillo", "Anaya", "Fontalvo"
    };

    private static final String[] PREFIJOS_TAREA = {
        "Organizar jornada de", "Coordinar brigada de", "Realizar diagnóstico de", "Capacitar voluntarios en",
        "Recolectar materiales para", "Gestionar permisos de", "Convocar reunión de", "Evaluar resultados de",
        "Documentar avances de", "Difundir campaña de", "Contactar aliados para", "Preparar logística de",
        "Distribuir insumos de", "Monitorear indicadores de", "Registrar evidencia fotográfica de"
    };

    private static final String[] COMENTARIOS = {
        "Excelente avance en %s, sigamos así.",
        "Propongo ampliar la cobertura a otros sectores de %s.",
        "Necesitamos más voluntarios para la jornada del próximo fin de semana.",
        "Los resultados de la última actividad superaron las expectativas.",
        "Sugiero coordinar con la alcaldía el retiro de los residuos recolectados.",
        "Recordatorio: la reunión de seguimiento es este jueves a las 3:00 p. m.",
        "Gracias al equipo por el compromiso mostrado en %s.",
        "La comunidad de %s está muy motivada con el proyecto.",
        "Subiré el informe fotográfico en la próxima entrega.",
        "Hay que reforzar la convocatoria en redes sociales.",
        "Recomiendo invitar a los colegios cercanos como aliados.",
        "Pendiente: conseguir donaciones de bolsas y guantes.",
        "La junta de acción comunal de %s confirmó su apoyo.",
        "Solicito reprogramar la actividad por la lluvia.",
        "Ya contactamos a EPA Cartagena para validar el cronograma.",
        "Los jóvenes del barrio están liderando muy bien las brigadas."
    };

    /** Proyectos con nombre propio para la cuenta de prueba del líder. */
    private static final String[][] PROYECTOS_DEMO = {
        {"Limpieza de playas en La Boquilla", "Limpieza de playas", "La Boquilla", "ACTIVO"},
        {"Siembra de manglar en la Ciénaga de la Virgen", "Manglares y ciénagas", "La Boquilla", "ACTIVO"},
        {"Huertas escolares en El Pozón", "Huertas comunitarias", "El Pozón", "PENDIENTE"},
        {"Reciclaje de plástico en Getsemaní", "Reciclaje y residuos", "Getsemaní", "ACTIVO"},
        {"Aulas verdes en colegios de Olaya Herrera", "Educación ambiental", "Olaya Herrera", "ACTIVO"},
        {"Recuperación del caño Juan de Angola", "Canales y caños", "Torices", "ACTIVO"},
        {"Censo de aves urbanas en Manga", "Fauna y aves", "Manga", "ACTIVO"},
        {"Parque vivo en Nuevo Bosque", "Espacios verdes", "Nuevo Bosque", "ACTIVO"},
        {"Paneles solares comunitarios en Nelson Mandela", "Energía limpia", "Nelson Mandela", "PENDIENTE"},
        {"Brigada de aseo en Bocagrande", "Limpieza de playas", "Bocagrande", "COMPLETADO"},
        {"Compostaje barrial en Crespo", "Reciclaje y residuos", "Crespo", "COMPLETADO"},
        {"Monitoreo de aire en Mamonal", "Calidad del aire", "El Socorro", "CANCELADO"}
    };

    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;
    private final CodigoProyectoService codigoService;

    private Random rnd;
    private LocalDate hoy;
    private final Set<String> correosUsados = new HashSet<>();

    public GeneradorDatos(MongoTemplate mongoTemplate, PasswordEncoder passwordEncoder,
                          CodigoProyectoService codigoService) {
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = passwordEncoder;
        this.codigoService = codigoService;
    }

    /** Cantidades objetivo (se pueden cambiar en application.properties). */
    public record Volumen(int usuarios, int proyectos, int tareas, int comentarios) {
    }

    public void generar(Volumen volumen, Usuarios admin, Usuarios liderDemo, Usuarios voluntarioDemo) {
        long inicio = System.currentTimeMillis();
        rnd = new Random(42);
        hoy = LocalDate.now();
        correosUsados.clear();
        correosUsados.add(admin.getCorreo());
        correosUsados.add(liderDemo.getCorreo());
        correosUsados.add(voluntarioDemo.getCorreo());

        // ---------- 1. Usuarios ----------
        String hash = passwordEncoder.encode(CLAVE_GENERADOS); // un solo hash: BCrypt es lento a propósito
        List<Usuarios> usuarios = new ArrayList<>();
        List<Usuarios> lideres = new ArrayList<>();
        List<Usuarios> voluntarios = new ArrayList<>();

        int nAdmins = 4;
        int nLideres = Math.max(10, volumen.usuarios() * 15 / 100);
        for (int i = 0; i < volumen.usuarios(); i++) {
            String rol = i < nAdmins ? "ADMINISTRADOR" : (i < nAdmins + nLideres ? "LIDER" : "PARTICIPANTE");
            Usuarios u = nuevoUsuario(rol, hash);
            usuarios.add(u);
            if ("LIDER".equals(rol)) {
                lideres.add(u);
            } else if ("PARTICIPANTE".equals(rol) && u.isEnabled()) {
                voluntarios.add(u);
            }
        }

        // ---------- 2. Proyectos ----------
        List<Proyecto> proyectos = new ArrayList<>();
        for (String[] demo : PROYECTOS_DEMO) {
            proyectos.add(nuevoProyecto(demo[0], demo[1], demo[2], demo[3], liderDemo));
        }
        for (int i = proyectos.size(); i < volumen.proyectos(); i++) {
            String categoria = elegir(Catalogos.CATEGORIAS);
            String barrio = elegir(Catalogos.BARRIOS);
            String nombre = categoria + " en " + barrio + (rnd.nextInt(100) < 35 ? " (Fase " + (1 + rnd.nextInt(4)) + ")" : "");
            proyectos.add(nuevoProyecto(nombre, categoria, barrio, estadoAleatorio(), elegir(lideres)));
        }

        // Códigos SYN-AAAA-NNNNN en orden de creación
        proyectos.sort(Comparator.comparing(Proyecto::getFechaCreacion));
        long n = 0;
        for (Proyecto p : proyectos) {
            p.setCodigo(CodigoProyectoService.formatear(p.getFechaCreacion().getYear(), ++n));
        }

        // ---------- 3. Participaciones ----------
        List<Participacion> participaciones = new ArrayList<>();
        for (Proyecto p : proyectos) {
            if (!admiteVoluntarios(p)) {
                continue;
            }
            boolean esDemo = p.getIdLider().equals(liderDemo.getId());
            int cupo = esDemo ? 6 + rnd.nextInt(10) : rnd.nextInt(5);
            Set<Usuarios> elegidos = new LinkedHashSet<>();
            while (elegidos.size() < cupo && elegidos.size() < voluntarios.size()) {
                elegidos.add(elegir(voluntarios));
            }
            // El voluntario de prueba participa en los proyectos activos del líder de prueba y en algunos más
            if ((esDemo && "ACTIVO".equals(p.getEstado())) || (!esDemo && rnd.nextInt(400) == 0)) {
                elegidos.add(voluntarioDemo);
            }
            for (Usuarios v : elegidos) {
                Participacion part = new Participacion(v.getId(), p.getId(), "PARTICIPANTE");
                part.setId(nuevoId());
                part.setFechaUnion(entre(p.getFechaCreacion(), minimo(ahoraEn(p.getFechaFin()), LocalDateTime.now())));
                participaciones.add(part);
                p.addParticipante(v.getId());
                v.addProyecto(p.getId());
            }
        }

        // ---------- 4. Tareas y entregas ----------
        List<Tarea> tareas = new ArrayList<>();
        List<EntregaTarea> entregas = new ArrayList<>();
        List<Proyecto> conEquipo = proyectos.stream().filter(p -> p.getTotalVoluntarios() > 0).toList();
        java.util.Map<String, Usuarios> porId = new java.util.HashMap<>();
        for (Usuarios u : usuarios) {
            porId.put(u.getId(), u);
        }
        porId.put(voluntarioDemo.getId(), voluntarioDemo);

        // Primero 3-6 tareas en cada proyecto del líder de prueba, luego el resto al azar
        List<Proyecto> orden = new ArrayList<>();
        for (Proyecto p : conEquipo) {
            if (p.getIdLider().equals(liderDemo.getId())) {
                for (int k = 3 + rnd.nextInt(4); k > 0; k--) {
                    orden.add(p);
                }
            }
        }
        while (orden.size() < volumen.tareas() && !conEquipo.isEmpty()) {
            orden.add(elegir(conEquipo));
        }
        for (Proyecto p : orden) {
            Tarea t = nuevaTarea(p);
            List<String> equipo = p.getParticipantes().stream().filter(id -> !id.equals(p.getIdLider())).toList();
            int asignados = 1 + rnd.nextInt(Math.min(3, equipo.size()));
            Set<String> ids = new LinkedHashSet<>();
            // Casi todas las tareas de los proyectos de prueba incluyen al voluntario de prueba
            if (equipo.contains(voluntarioDemo.getId()) && rnd.nextInt(100) < 70) {
                ids.add(voluntarioDemo.getId());
            }
            while (ids.size() < asignados) {
                ids.add(equipo.get(rnd.nextInt(equipo.size())));
            }
            for (String id : ids) {
                t.addUsuarioAsignado(id);
                Usuarios u = porId.get(id);
                if (u != null) {
                    u.addTareaAsignada(t.getId());
                }
            }
            p.addTarea(t.getId());
            tareas.add(t);

            // Evidencia entregada: tareas completadas y algunas en progreso
            if ("COMPLETADA".equals(t.getEstado()) || ("EN_PROGRESO".equals(t.getEstado()) && rnd.nextInt(100) < 35)) {
                String autor = t.getUsuariosAsignados().get(0);
                EntregaTarea e = nuevaEntrega(t, p, porId.get(autor));
                entregas.add(e);
                t.addEntrega(e.getId());
            }
        }

        // ---------- 5. Comentarios ----------
        List<Comentario> comentarios = new ArrayList<>();
        List<Proyecto> comentables = proyectos.stream().filter(p -> !"PENDIENTE".equals(p.getEstado())).toList();
        for (int i = 0; i < volumen.comentarios() && !comentables.isEmpty(); i++) {
            Proyecto p = i < 30 ? proyectos.stream().filter(x -> x.getIdLider().equals(liderDemo.getId())
                    && "ACTIVO".equals(x.getEstado())).skip(i % 6).findFirst().orElse(elegir(comentables))
                    : elegir(comentables);
            String autor = p.getParticipantes().get(rnd.nextInt(p.getParticipantes().size()));
            String plantilla = COMENTARIOS[rnd.nextInt(COMENTARIOS.length)];
            Comentario c = new Comentario(String.format(plantilla, p.getUbicacion()), p.getId(), autor);
            c.setId(nuevoId());
            c.setFecha(entre(p.getFechaCreacion(), LocalDateTime.now()));
            comentarios.add(c);
            p.addComentario(c.getId());
        }

        // ---------- 6. Guardar las listas en las cuentas de prueba e insertar todo ----------
        mongoTemplate.save(liderDemo);
        mongoTemplate.save(voluntarioDemo);
        insertar(usuarios, Usuarios.class);
        insertar(proyectos, Proyecto.class);
        insertar(participaciones, Participacion.class);
        insertar(tareas, Tarea.class);
        insertar(entregas, EntregaTarea.class);
        insertar(comentarios, Comentario.class);
        codigoService.fijarContador(n);

        long total = usuarios.size() + proyectos.size() + participaciones.size() + tareas.size()
                + entregas.size() + comentarios.size();
        log.info("Datos generados: {} usuarios, {} proyectos, {} participaciones, {} tareas, {} entregas, {} comentarios ({} documentos en {} s)",
                usuarios.size(), proyectos.size(), participaciones.size(), tareas.size(), entregas.size(),
                comentarios.size(), total, (System.currentTimeMillis() - inicio) / 1000.0);
    }

    // =====================================================================
    // HISTORIAL PARA EL MODELO PREDICTIVO
    // =====================================================================

    /** Marca en la colección "contadores" que indica que el historial ya se generó. */
    public static final String MARCA_HISTORIAL = "historial_riesgo_v1";

    /** Documentos de un historial generado (se insertan juntos). */
    public record Historial(List<Proyecto> proyectos, List<Participacion> participaciones, List<Tarea> tareas,
                            List<EntregaTarea> entregas, List<Comentario> comentarios) {
    }

    /**
     * Proyectos YA TERMINADOS con un historial completo (tareas con fechas de vencimiento y de
     * cumplimiento, entregas y comentarios con fecha). Son los datos con los que se entrena el
     * modelo de riesgo: se miden las variables a la mitad del proyecto y se etiqueta con cómo terminó.
     *
     * Cada proyecto tiene un "compromiso" oculto (0 a 1) que influye, con ruido, en cuántas tareas
     * se cumplen, con cuánto retraso y cuántos voluntarios participan; así hay proyectos cumplidos
     * a tiempo, con retrasos moderados y con retrasos graves, como pide la propuesta.
     */
    public Historial construirHistorial(List<String> idsLideres, List<String> idsVoluntarios, int cantidad, long semilla) {
        Random r = new Random(semilla);
        LocalDate hoyLocal = LocalDate.now();
        List<Proyecto> proyectos = new ArrayList<>();
        List<Participacion> participaciones = new ArrayList<>();
        List<Tarea> tareas = new ArrayList<>();
        List<EntregaTarea> entregas = new ArrayList<>();
        List<Comentario> comentarios = new ArrayList<>();
        if (idsLideres.isEmpty() || idsVoluntarios.size() < 12) {
            return new Historial(proyectos, participaciones, tareas, entregas, comentarios);
        }

        for (int k = 0; k < cantidad; k++) {
            double compromiso = r.nextDouble();
            String categoria = Catalogos.CATEGORIAS.get(r.nextInt(Catalogos.CATEGORIAS.size()));
            String barrio = Catalogos.BARRIOS.get(r.nextInt(Catalogos.BARRIOS.size()));
            int duracion = 45 + r.nextInt(136);
            LocalDate fin = hoyLocal.minusDays(2 + r.nextInt(700));
            LocalDate inicio = fin.minusDays(duracion);
            boolean cancelado = compromiso < 0.3 && r.nextInt(100) < 45;
            String idLider = idsLideres.get(r.nextInt(idsLideres.size()));

            Proyecto p = new Proyecto(categoria + " en " + barrio + " (" + inicio.getYear() + ")",
                    "Iniciativa comunitaria de " + categoria.toLowerCase() + " en " + barrio
                            + ", Cartagena de Indias. Proyecto finalizado con historial de seguimiento.",
                    inicio, fin, idLider);
            p.setId(nuevoId());
            p.setEstado(cancelado ? "CANCELADO" : "COMPLETADO");
            p.setCategoria(categoria);
            p.setUbicacion(barrio);
            p.setTipoOrganizacion(Catalogos.ORGANIZACIONES.get(r.nextInt(Catalogos.ORGANIZACIONES.size() - 1)));
            p.setAudiencia(Catalogos.AUDIENCIAS.get(r.nextInt(Catalogos.AUDIENCIAS.size())));
            LocalDateTime creado = inicio.minusDays(3 + r.nextInt(27)).atTime(8 + r.nextInt(10), r.nextInt(60));
            p.setFechaCreacion(creado);
            p.setFechaActualizacion(fin.atTime(17, 0));
            p.addParticipante(idLider);

            // Equipo y quiénes participan de verdad (más compromiso -> más voluntarios activos)
            int tamano = 3 + r.nextInt(8);
            List<String> equipo = new ArrayList<>(new LinkedHashSet<>(muestra(idsVoluntarios, tamano, r)));
            equipo.remove(idLider);
            List<String> activos = new ArrayList<>();
            double probActivo = clamp(0.25 + 0.75 * compromiso + (r.nextDouble() - 0.5) * 0.25);
            for (String v : equipo) {
                Participacion part = new Participacion(v, p.getId(), "PARTICIPANTE");
                part.setId(nuevoId());
                part.setFechaUnion(inicio.minusDays(r.nextInt(10)).atTime(10, 0));
                participaciones.add(part);
                p.addParticipante(v);
                if (r.nextDouble() < probActivo) {
                    activos.add(v);
                }
            }

            // Tareas
            int nTareas = 5 + r.nextInt(10);
            double probCumplir = clamp(0.45 + 0.6 * compromiso + (r.nextDouble() - 0.5) * 0.2);
            for (int j = 0; j < nTareas; j++) {
                LocalDate creada = inicio.plusDays(r.nextInt(Math.max(1, (int) (duracion * 0.7))));
                LocalDate vence = creada.plusDays(7 + r.nextInt(Math.max(4, duracion / 3)));
                if (vence.isAfter(fin)) {
                    vence = fin;
                }
                String estado;
                LocalDate completada = null;
                if (r.nextInt(100) < 4) {
                    estado = "CANCELADA";
                } else if (r.nextDouble() < probCumplir) {
                    long retraso = Math.round(r.nextGaussian() * 4 + (1 - compromiso) * 22 - 7);
                    completada = vence.plusDays(retraso);
                    if (completada.isBefore(creada.plusDays(1))) {
                        completada = creada.plusDays(1);
                    }
                    if (completada.isAfter(fin)) { // no alcanzó a terminarse
                        completada = null;
                    }
                    estado = completada != null ? "COMPLETADA" : (r.nextBoolean() ? "EN_PROGRESO" : "PENDIENTE");
                } else {
                    estado = r.nextInt(100) < 45 ? "EN_PROGRESO" : "PENDIENTE";
                }

                // En proyectos comprometidos lo que queda sin terminar suele ser lo programado al final
                if (completada == null && !"CANCELADA".equals(estado) && r.nextDouble() < 0.2 + compromiso) {
                    vence = fin.minusDays(r.nextInt(8));
                    if (vence.isBefore(creada)) {
                        vence = creada;
                    }
                }
                String prefijo = PREFIJOS_TAREA[r.nextInt(PREFIJOS_TAREA.length)];
                Tarea t = new Tarea(prefijo + " " + categoria.toLowerCase(),
                        prefijo + " " + categoria.toLowerCase() + " en " + barrio + ".", estado, vence, p.getId());
                t.setId(nuevoId());
                t.setFechaCreacion(creada.atTime(9, 0));
                t.setFechaActualizacion(completada != null ? completada.atTime(16, 0) : fin.atTime(18, 0));
                List<String> responsables = activos.isEmpty() || r.nextInt(100) < 20 ? equipo : activos;
                if (!responsables.isEmpty()) {
                    t.addUsuarioAsignado(responsables.get(r.nextInt(responsables.size())));
                }
                p.addTarea(t.getId());
                tareas.add(t);

                // Evidencia: las tareas cumplidas tienen entrega del responsable (que cuenta como activo)
                if (completada != null && !t.getUsuariosAsignados().isEmpty()) {
                    String autor = t.getUsuariosAsignados().get(0);
                    if (!activos.contains(autor)) {
                        activos.add(autor);
                    }
                    EntregaTarea e = new EntregaTarea(t.getId(), autor, "evidencia-" + t.getId().substring(18) + ".txt",
                            "text/plain", ("Evidencia de: " + t.getTitulo()).getBytes(StandardCharsets.UTF_8));
                    e.setId(nuevoId());
                    e.setFechaEntrega(completada.atTime(15, 0));
                    e.setEstado(r.nextInt(100) < 90 ? "REVISADO" : "RECHAZADO");
                    entregas.add(e);
                    t.addEntrega(e.getId());
                }
            }

            // Comentarios de los voluntarios activos, repartidos durante el proyecto
            for (String v : activos) {
                int n = 1 + r.nextInt(3);
                for (int c = 0; c < n; c++) {
                    LocalDate fecha = inicio.plusDays(r.nextInt(duracion + 1));
                    Comentario com = new Comentario(String.format(COMENTARIOS[r.nextInt(COMENTARIOS.length)], barrio), p.getId(), v);
                    com.setId(nuevoId());
                    com.setFecha(fecha.atTime(11, 0));
                    comentarios.add(com);
                    p.addComentario(com.getId());
                }
            }
            proyectos.add(p);
        }
        return new Historial(proyectos, participaciones, tareas, entregas, comentarios);
    }

    /** Inserta el historial y actualiza las listas de proyectos y tareas de cada usuario. */
    public int generarHistorial(List<String> idsLideres, List<String> idsVoluntarios, int cantidad) {
        Historial h = construirHistorial(idsLideres, idsVoluntarios, cantidad, 7L);
        if (h.proyectos().isEmpty()) {
            return 0;
        }
        for (Proyecto p : h.proyectos()) {
            p.setCodigo(codigoService.siguienteCodigo());
        }
        insertar(h.proyectos(), Proyecto.class);
        insertar(h.participaciones(), Participacion.class);
        insertar(h.tareas(), Tarea.class);
        insertar(h.entregas(), EntregaTarea.class);
        insertar(h.comentarios(), Comentario.class);

        java.util.Map<String, Set<String>> proyectosPorUsuario = new java.util.HashMap<>();
        java.util.Map<String, Set<String>> tareasPorUsuario = new java.util.HashMap<>();
        for (Proyecto p : h.proyectos()) {
            for (String id : p.getParticipantes()) {
                proyectosPorUsuario.computeIfAbsent(id, x -> new HashSet<>()).add(p.getId());
            }
        }
        for (Tarea t : h.tareas()) {
            for (String id : t.getUsuariosAsignados()) {
                tareasPorUsuario.computeIfAbsent(id, x -> new HashSet<>()).add(t.getId());
            }
        }
        org.springframework.data.mongodb.core.BulkOperations bulk = mongoTemplate.bulkOps(
                org.springframework.data.mongodb.core.BulkOperations.BulkMode.UNORDERED, Usuarios.class);
        Set<String> ids = new HashSet<>(proyectosPorUsuario.keySet());
        ids.addAll(tareasPorUsuario.keySet());
        for (String id : ids) {
            org.springframework.data.mongodb.core.query.Update u = new org.springframework.data.mongodb.core.query.Update();
            if (proyectosPorUsuario.containsKey(id)) {
                u.addToSet("proyectos").each(proyectosPorUsuario.get(id).toArray());
            }
            if (tareasPorUsuario.containsKey(id)) {
                u.addToSet("tareasAsignadas").each(tareasPorUsuario.get(id).toArray());
            }
            bulk.updateOne(org.springframework.data.mongodb.core.query.Query.query(
                    org.springframework.data.mongodb.core.query.Criteria.where("_id").is(id)), u);
        }
        if (!ids.isEmpty()) {
            bulk.execute();
        }
        mongoTemplate.getCollection(CodigoProyectoService.COLECCION).insertOne(
                new org.bson.Document("_id", MARCA_HISTORIAL).append("proyectos", h.proyectos().size()));
        log.info("Historial para el modelo predictivo: {} proyectos terminados, {} tareas, {} entregas, {} comentarios",
                h.proyectos().size(), h.tareas().size(), h.entregas().size(), h.comentarios().size());
        return h.proyectos().size();
    }

    private static <T> List<T> muestra(List<T> lista, int n, Random r) {
        Set<T> elegidos = new LinkedHashSet<>();
        while (elegidos.size() < Math.min(n, lista.size())) {
            elegidos.add(lista.get(r.nextInt(lista.size())));
        }
        return new ArrayList<>(elegidos);
    }

    private static double clamp(double v) {
        return Math.max(0.02, Math.min(0.98, v));
    }

    // =====================================================================
    // FÁBRICAS
    // =====================================================================

    private Usuarios nuevoUsuario(String rol, String hash) {
        String nombre = elegir(NOMBRES) + " " + elegir(APELLIDOS) + " " + elegir(APELLIDOS);
        Usuarios u = new Usuarios(nombre, correoPara(nombre), hash, null, rol);
        u.setId(nuevoId());
        u.setProveedor("LOCAL");
        boolean inactivo = "PARTICIPANTE".equals(rol) && rnd.nextInt(100) < 6;
        u.setEstado(inactivo ? "INACTIVO" : "ACTIVO");
        u.setActivo(!inactivo);
        LocalDateTime creado = LocalDateTime.now().minusDays(30 + rnd.nextInt(1000)).minusMinutes(rnd.nextInt(1440));
        u.setFechaCreacion(creado);
        u.setUltimaCambioContrasena(creado);
        if (rnd.nextInt(100) < 70) {
            u.setUltimoAcceso(entre(creado, LocalDateTime.now()));
        }
        return u;
    }

    private String correoPara(String nombre) {
        String[] partes = sinTildes(nombre.toLowerCase()).replaceAll("[^a-z ]", "").split(" ");
        String base = partes[0] + "." + partes[1];
        String correo = base + "@correo.com";
        int i = 2;
        while (!correosUsados.add(correo)) {
            correo = base + i++ + "@correo.com";
        }
        return correo;
    }

    private Proyecto nuevoProyecto(String nombre, String categoria, String barrio, String estado, Usuarios lider) {
        LocalDate inicio;
        LocalDate fin;
        LocalDateTime creado;
        switch (estado) {
            case "PENDIENTE" -> {
                inicio = hoy.plusDays(7 + rnd.nextInt(60));
                fin = inicio.plusDays(60 + rnd.nextInt(240));
                creado = LocalDateTime.now().minusDays(1 + rnd.nextInt(20));
            }
            case "COMPLETADO" -> {
                inicio = hoy.minusDays(200 + rnd.nextInt(700));
                fin = inicio.plusDays(30 + rnd.nextInt(150));
                creado = inicio.minusDays(3 + rnd.nextInt(27)).atTime(8 + rnd.nextInt(10), rnd.nextInt(60));
            }
            case "CANCELADO" -> {
                inicio = hoy.minusDays(30 + rnd.nextInt(470));
                fin = inicio.plusDays(30 + rnd.nextInt(170));
                creado = inicio.minusDays(3 + rnd.nextInt(27)).atTime(8 + rnd.nextInt(10), rnd.nextInt(60));
            }
            default -> { // ACTIVO
                inicio = hoy.minusDays(5 + rnd.nextInt(300));
                fin = hoy.plusDays(15 + rnd.nextInt(225));
                creado = inicio.minusDays(3 + rnd.nextInt(27)).atTime(8 + rnd.nextInt(10), rnd.nextInt(60));
            }
        }

        Proyecto p = new Proyecto(nombre, descripcion(categoria, barrio), inicio, fin, lider.getId());
        p.setId(nuevoId());
        p.setEstado(estado);
        p.setCategoria(categoria);
        p.setUbicacion(barrio);
        p.setTipoOrganizacion(elegir(Catalogos.ORGANIZACIONES.subList(0, Catalogos.ORGANIZACIONES.size() - 1)));
        p.setAudiencia(elegir(Catalogos.AUDIENCIAS));
        p.setFechaCreacion(creado);
        p.setFechaActualizacion(entre(creado, LocalDateTime.now()));
        lider.addProyecto(p.getId());
        return p;
    }

    private String descripcion(String categoria, String barrio) {
        String[] objetivos = {
            "generar un impacto ambiental positivo y sostenible en el sector",
            "involucrar a las familias y a los jóvenes del barrio en el cuidado del entorno",
            "recuperar espacios naturales y mejorar la calidad de vida de los vecinos",
            "crear hábitos ambientales que perduren después del proyecto",
            "articular a la comunidad con colegios, universidades y entidades locales"
        };
        return "Iniciativa comunitaria de " + categoria.toLowerCase() + " en " + barrio
                + ", Cartagena de Indias. Busca " + objetivos[rnd.nextInt(objetivos.length)] + ".";
    }

    private Tarea nuevaTarea(Proyecto p) {
        LocalDate entrega = entreFechas(p.getFechaInicio(), p.getFechaFin());
        String estado = estadoTarea(p, entrega);
        String tema = p.getCategoria() == null ? "la actividad" : p.getCategoria().toLowerCase();
        String prefijo = PREFIJOS_TAREA[rnd.nextInt(PREFIJOS_TAREA.length)];
        Tarea t = new Tarea(prefijo + " " + tema,
                prefijo + " " + tema + " en " + p.getUbicacion() + ". Coordinar con el líder y dejar evidencia en la plataforma.",
                estado, entrega, p.getId());
        t.setId(nuevoId());
        LocalDateTime creada = entre(p.getFechaCreacion(), minimo(entrega.atStartOfDay(), LocalDateTime.now()));
        t.setFechaCreacion(creada);
        t.setFechaActualizacion(entre(creada, LocalDateTime.now()));
        return t;
    }

    private String estadoTarea(Proyecto p, LocalDate entrega) {
        int r = rnd.nextInt(100);
        return switch (p.getEstado()) {
            case "COMPLETADO" -> r < 92 ? "COMPLETADA" : "CANCELADA";
            case "CANCELADO" -> r < 70 ? "CANCELADA" : "COMPLETADA";
            default -> entrega.isBefore(hoy)
                    ? (r < 60 ? "COMPLETADA" : r < 80 ? "EN_PROGRESO" : r < 95 ? "PENDIENTE" : "CANCELADA")
                    : (r < 55 ? "PENDIENTE" : r < 93 ? "EN_PROGRESO" : "COMPLETADA");
        };
    }

    private EntregaTarea nuevaEntrega(Tarea t, Proyecto p, Usuarios autor) {
        String autorNombre = autor == null ? "Voluntario" : autor.getNombre();
        String contenido = "INFORME DE EVIDENCIA - SYNERGY\n"
                + "Proyecto: " + p.getNombre() + " (" + p.getCodigo() + ")\n"
                + "Tarea: " + t.getTitulo() + "\n"
                + "Responsable: " + autorNombre + "\n"
                + "Resumen: actividad realizada con la comunidad de " + p.getUbicacion()
                + ". Se adjuntan registros de asistencia y observaciones.\n";
        String archivo = "evidencia-" + t.getId().substring(18) + ".txt";
        EntregaTarea e = new EntregaTarea(t.getId(), autor == null ? null : autor.getId(), archivo,
                "text/plain", contenido.getBytes(StandardCharsets.UTF_8));
        e.setId(nuevoId());
        LocalDateTime fecha = minimo(t.getFechaEntrega().atTime(9 + rnd.nextInt(9), rnd.nextInt(60)), LocalDateTime.now());
        e.setFechaEntrega(fecha.isBefore(t.getFechaCreacion()) ? t.getFechaCreacion() : fecha);
        if ("COMPLETADA".equals(t.getEstado())) {
            e.setEstado(rnd.nextInt(100) < 90 ? "REVISADO" : "RECHAZADO");
            e.setComentariosRevisor("REVISADO".equals(e.getEstado()) ? "Buen trabajo, evidencia completa." : "Falta el registro de asistencia.");
        } else {
            e.setEstado("ENTREGADO"); // pendiente de revisión por el líder
        }
        return e;
    }

    // =====================================================================
    // UTILIDADES
    // =====================================================================

    private String estadoAleatorio() {
        int r = rnd.nextInt(100);
        return r < 8 ? "PENDIENTE" : r < 63 ? "ACTIVO" : r < 93 ? "COMPLETADO" : "CANCELADO";
    }

    private static boolean admiteVoluntarios(Proyecto p) {
        return "ACTIVO".equals(p.getEstado()) || "COMPLETADO".equals(p.getEstado()) || "CANCELADO".equals(p.getEstado());
    }

    private <T> void insertar(Collection<T> docs, Class<T> tipo) {
        List<T> lote = new ArrayList<>(1000);
        for (T d : docs) {
            lote.add(d);
            if (lote.size() == 1000) {
                mongoTemplate.insert(lote, tipo);
                lote = new ArrayList<>(1000);
            }
        }
        if (!lote.isEmpty()) {
            mongoTemplate.insert(lote, tipo);
        }
    }

    private <T> T elegir(List<T> lista) {
        return lista.get(rnd.nextInt(lista.size()));
    }

    private <T> T elegir(T[] arreglo) {
        return arreglo[rnd.nextInt(arreglo.length)];
    }

    private static String nuevoId() {
        return new ObjectId().toHexString();
    }

    private LocalDateTime entre(LocalDateTime desde, LocalDateTime hasta) {
        if (!hasta.isAfter(desde)) {
            return desde;
        }
        long segundos = java.time.Duration.between(desde, hasta).getSeconds();
        return desde.plusSeconds((long) (rnd.nextDouble() * segundos));
    }

    private LocalDate entreFechas(LocalDate desde, LocalDate hasta) {
        long dias = java.time.temporal.ChronoUnit.DAYS.between(desde, hasta);
        return dias <= 0 ? desde : desde.plusDays(rnd.nextInt((int) dias + 1));
    }

    private static LocalDateTime ahoraEn(LocalDate fecha) {
        return fecha.atTime(18, 0);
    }

    private static LocalDateTime minimo(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }

    private static String sinTildes(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
