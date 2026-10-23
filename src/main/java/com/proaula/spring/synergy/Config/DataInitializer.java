package com.proaula.spring.synergy.Config;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.proaula.spring.synergy.Model.Privilegio;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Rol;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.PrivilegioRepository;
import com.proaula.spring.synergy.Repository.ProyectoRepository;
import com.proaula.spring.synergy.Repository.RolRepository;
import com.proaula.spring.synergy.Repository.UsuarioRepository;
import com.proaula.spring.synergy.Service.CodigoProyectoService;

/**
 * Prepara la base de datos al arrancar, en este orden:
 *
 * 1. Detecta datos con el ESQUEMA ANTIGUO (los del script poblar_synergy.py de la
 *    versión anterior: usuarios con "idRol" y sin "rol", ids guardados como ObjectId,
 *    estados EN_EJECUCION/FINALIZADO...). Esos datos no funcionan con el código actual,
 *    así que se RESPALDAN en la carpeta respaldos/ y se reemplazan.
 * 2. Crea/repara los índices (MongoIndexConfig).
 * 3. Crea privilegios y roles si no existen.
 * 4. Garantiza las 3 cuentas de prueba (admin, líder y voluntario).
 * 5. Si hay pocos proyectos, genera el volumen de datos de prueba (GeneradorDatos).
 * 6. Asigna código SYN-AAAA-NNNNN a los proyectos que no tengan.
 *
 * Todo se configura en application.properties (sección DATOS DE PRUEBA).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    // ── Cuentas de prueba (documentadas en README.md) ──
    public static final String ADMIN_CORREO = "admin@synergy.com";
    public static final String ADMIN_CLAVE = "Admin123*";
    public static final String LIDER_CORREO = "lider@synergy.com";
    public static final String LIDER_CLAVE = "Lider123*";
    public static final String VOLUNTARIO_CORREO = "voluntario@synergy.com";
    public static final String VOLUNTARIO_CLAVE = "Voluntario123*";

    /** Colecciones de la aplicación (en el orden en que se respaldan). */
    private static final List<String> COLECCIONES = List.of(
        "usuarios", "proyectos", "tareas", "participaciones", "comentarios",
        "entregas_tareas", "roles", "privilegios", CodigoProyectoService.COLECCION);

    private static final List<String> ESTADOS_ANTIGUOS = List.of(
        "PLANIFICACION", "EN_EJECUCION", "FINALIZADO", "SUSPENDIDO");

    @Value("${synergy.demo.enabled:true}")
    private boolean demoHabilitado;

    @Value("${synergy.demo.reset-passwords:true}")
    private boolean reiniciarClaves;

    @Value("${synergy.datos.generar:true}")
    private boolean generarDatos;

    @Value("${synergy.datos.reemplazar-esquema-antiguo:true}")
    private boolean reemplazarAntiguos;

    @Value("${synergy.datos.minimo-proyectos:200}")
    private int minimoProyectos;

    @Value("${synergy.datos.usuarios:2500}")
    private int cantidadUsuarios;

    @Value("${synergy.datos.proyectos:3000}")
    private int cantidadProyectos;

    @Value("${synergy.datos.tareas:4000}")
    private int cantidadTareas;

    @Value("${synergy.datos.comentarios:1500}")
    private int cantidadComentarios;

    /** Proyectos terminados con historial completo para entrenar el modelo predictivo (0 = no generar). */
    @Value("${synergy.datos.historial-riesgo:600}")
    private int cantidadHistorial;

    /** true = en el próximo arranque respalda, borra y regenera TODO (volver a false después). */
    @Value("${synergy.datos.regenerar-todo:false}")
    private boolean regenerarTodo;

    @Value("${synergy.datos.carpeta-respaldos:respaldos}")
    private String carpetaRespaldos;

    /** Primer administrador en producción (sin cuentas demo): variables SYNERGY_ADMIN_CORREO y SYNERGY_ADMIN_CLAVE. */
    @Value("${synergy.admin.correo:${SYNERGY_ADMIN_CORREO:}}")
    private String adminInicialCorreo;

    @Value("${synergy.admin.clave:${SYNERGY_ADMIN_CLAVE:}}")
    private String adminInicialClave;

    private final MongoTemplate mongoTemplate;
    private final MongoIndexConfig indices;
    private final GeneradorDatos generador;
    private final CodigoProyectoService codigoService;
    private final PrivilegioRepository privilegioRepository;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProyectoRepository proyectoRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(MongoTemplate mongoTemplate, MongoIndexConfig indices, GeneradorDatos generador,
                           CodigoProyectoService codigoService, PrivilegioRepository privilegioRepository,
                           RolRepository rolRepository, UsuarioRepository usuarioRepository,
                           ProyectoRepository proyectoRepository, PasswordEncoder passwordEncoder) {
        this.mongoTemplate = mongoTemplate;
        this.indices = indices;
        this.generador = generador;
        this.codigoService = codigoService;
        this.privilegioRepository = privilegioRepository;
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.proyectoRepository = proyectoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 1. Datos antiguos incompatibles (o regeneración pedida a mano)
        if (regenerarTodo) {
            log.warn("synergy.datos.regenerar-todo=true: se respaldará y regenerará toda la base. Vuelve a ponerlo en false.");
            respaldarYLimpiar();
        } else if (reemplazarAntiguos && hayEsquemaAntiguo()) {
            log.warn("Se detectaron datos con el esquema ANTIGUO de Synergy (incompatibles con esta versión).");
            respaldarYLimpiar();
        }

        // 2. Índices
        indices.crearIndices();

        // 3. Privilegios y roles (también en producción)
        if (privilegioRepository.count() == 0) {
            crearPrivilegios();
        }
        if (rolRepository.count() == 0) {
            crearRoles();
        }

        if (!demoHabilitado) {
            crearAdministradorInicial();
            asignarCodigosFaltantes();
            return;
        }

        // 4. Cuentas de prueba
        Usuarios admin = asegurarUsuario("Administrador Synergy", ADMIN_CORREO, ADMIN_CLAVE, "ADMINISTRADOR");
        Usuarios lider = asegurarUsuario("Laura Líder", LIDER_CORREO, LIDER_CLAVE, "LIDER");
        Usuarios voluntario = asegurarUsuario("Víctor Voluntario", VOLUNTARIO_CORREO, VOLUNTARIO_CLAVE, "PARTICIPANTE");

        // 5. Volumen de datos de prueba
        long proyectos = proyectoRepository.count();
        if (generarDatos && proyectos < minimoProyectos) {
            log.info("Hay {} proyectos (mínimo {}): generando datos de prueba, puede tardar unos segundos...",
                    proyectos, minimoProyectos);
            generador.generar(new GeneradorDatos.Volumen(cantidadUsuarios, cantidadProyectos, cantidadTareas,
                    cantidadComentarios), admin, lider, voluntario);
        }

        // 6. Códigos de proyecto
        asignarCodigosFaltantes();

        // 7. Historial de proyectos terminados para el modelo predictivo (se agrega una sola vez,
        //    sin tocar los datos existentes)
        if (generarDatos && cantidadHistorial > 0 && !mongoTemplate.exists(
                Query.query(Criteria.where("_id").is(GeneradorDatos.MARCA_HISTORIAL)), CodigoProyectoService.COLECCION)) {
            generador.generarHistorial(idsPorRol("LIDER"), idsPorRol("PARTICIPANTE"), cantidadHistorial);
        }

        log.info("""

            ✔ Synergy listo: {} usuarios, {} proyectos.
              Cuentas de prueba:
               - Admin:      {} / {}
               - Líder:      {} / {}
               - Voluntario: {} / {}
               - Usuarios generados: cualquier correo del panel de admin / {}
            """, usuarioRepository.count(), proyectoRepository.count(),
                ADMIN_CORREO, ADMIN_CLAVE, LIDER_CORREO, LIDER_CLAVE, VOLUNTARIO_CORREO, VOLUNTARIO_CLAVE,
                GeneradorDatos.CLAVE_GENERADOS);
    }

    /**
     * Sin cuentas demo (perfil prod) la base empieza sin administrador. Si se definieron
     * SYNERGY_ADMIN_CORREO y SYNERGY_ADMIN_CLAVE y no existe ningún administrador, se crea uno.
     */
    private void crearAdministradorInicial() {
        if (mongoTemplate.exists(Query.query(Criteria.where("rol").is("ADMINISTRADOR")), Usuarios.class)) {
            return;
        }
        if (adminInicialCorreo == null || adminInicialCorreo.isBlank() || adminInicialClave == null || adminInicialClave.isBlank()) {
            log.warn("No hay ningún administrador. Define SYNERGY_ADMIN_CORREO y SYNERGY_ADMIN_CLAVE para crear el primero.");
            return;
        }
        String problema = com.proaula.spring.synergy.Security.PoliticaContrasena.validar(adminInicialClave);
        if (problema != null) {
            log.warn("No se creó el administrador inicial: {}", problema);
            return;
        }
        asegurarUsuario("Administrador", adminInicialCorreo.trim().toLowerCase(), adminInicialClave, "ADMINISTRADOR");
        log.info("Administrador inicial creado: {}", adminInicialCorreo.trim().toLowerCase());
    }

    // =====================================================================
    // ESQUEMA ANTIGUO
    // =====================================================================

    private boolean hayEsquemaAntiguo() {
        boolean usuariosViejos = mongoTemplate.exists(new Query(new Criteria().orOperator(
                Criteria.where("idRol").exists(true),
                Criteria.where("rol").exists(false))), "usuarios");
        boolean idsComoObjectId = mongoTemplate.exists(
                Query.query(Criteria.where("idLider").type(7)), "proyectos"); // 7 = ObjectId
        boolean estadosViejos = mongoTemplate.exists(
                Query.query(Criteria.where("estado").in(ESTADOS_ANTIGUOS)), "proyectos");
        return usuariosViejos || idsComoObjectId || estadosViejos;
    }

    /** Guarda cada colección en respaldos/AAAAMMDD_HHmmss/coleccion.json y luego la borra. */
    /** Ids de los usuarios activos con un rol (solo el campo _id). */
    private List<String> idsPorRol(String rol) {
        Query q = Query.query(Criteria.where("rol").is(rol).and("activo").is(true));
        q.fields().include("_id");
        return mongoTemplate.find(q, org.bson.Document.class, "usuarios").stream()
                .map(d -> String.valueOf(d.get("_id"))).toList();
    }

    private void respaldarYLimpiar() {
        String marca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path carpeta = Paths.get(carpetaRespaldos, marca).toAbsolutePath();
        JsonWriterSettings json = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
        try {
            Files.createDirectories(carpeta);
            for (String coleccion : COLECCIONES) {
                if (!mongoTemplate.collectionExists(coleccion)) {
                    continue;
                }
                try (Writer w = Files.newBufferedWriter(carpeta.resolve(coleccion + ".json"), StandardCharsets.UTF_8)) {
                    w.write("[\n");
                    boolean primero = true;
                    for (Document d : mongoTemplate.getCollection(coleccion).find()) {
                        if (!primero) {
                            w.write(",\n");
                        }
                        w.write(d.toJson(json));
                        primero = false;
                    }
                    w.write("\n]\n");
                }
            }
        } catch (IOException e) {
            // Sin respaldo no se borra nada: se avisa y se sigue con los datos como están
            log.error("No se pudo crear el respaldo en {} ({}). Los datos antiguos NO se borraron.", carpeta, e.getMessage());
            return;
        }

        for (String coleccion : COLECCIONES) {
            mongoTemplate.dropCollection(coleccion);
        }
        log.warn("Datos antiguos respaldados en {} y eliminados. Se generarán datos nuevos compatibles.", carpeta);
    }

    // =====================================================================
    // CÓDIGOS DE PROYECTO
    // =====================================================================

    /** Proyectos creados antes de existir el campo "codigo" (o por la API) reciben uno. */
    private void asignarCodigosFaltantes() {
        Query sinCodigo = new Query(new Criteria().orOperator(
                Criteria.where("codigo").exists(false), Criteria.where("codigo").is(null)))
                .with(Sort.by("fechaCreacion"));
        sinCodigo.fields().include("_id");
        List<Proyecto> pendientes = mongoTemplate.find(sinCodigo, Proyecto.class);
        for (Proyecto p : pendientes) {
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(p.getId())),
                    Update.update("codigo", codigoService.siguienteCodigo()), Proyecto.class);
        }
        if (!pendientes.isEmpty()) {
            log.info("Código asignado a {} proyectos que no tenían", pendientes.size());
        }
    }

    // =====================================================================
    // CUENTAS, PRIVILEGIOS Y ROLES
    // =====================================================================

    /** Crea la cuenta si no existe; si existe, la activa y (opcionalmente) restablece su contraseña. */
    private Usuarios asegurarUsuario(String nombre, String correo, String clave, String rol) {
        Usuarios usuario = usuarioRepository.findByCorreo(correo);
        if (usuario == null) {
            usuario = new Usuarios(nombre, correo, passwordEncoder.encode(clave), null, rol);
            usuario.setProveedor("LOCAL");
            usuario.setFechaCreacion(LocalDateTime.now());
        } else {
            if (reiniciarClaves) {
                usuario.setContrasena(passwordEncoder.encode(clave));
            }
            usuario.setRol(rol);
        }
        usuario.setActivo(true);
        usuario.setEstado("ACTIVO");
        return usuarioRepository.save(usuario);
    }

    private void crearPrivilegios() {
        List<Privilegio> privilegios = Arrays.asList(
            new Privilegio("CREAR_PROYECTO", "Crear nuevos proyectos", "PROYECTO", "CREATE"),
            new Privilegio("VER_PROYECTO", "Ver proyectos", "PROYECTO", "READ"),
            new Privilegio("EDITAR_PROYECTO", "Editar proyectos existentes", "PROYECTO", "UPDATE"),
            new Privilegio("ELIMINAR_PROYECTO", "Eliminar proyectos", "PROYECTO", "DELETE"),
            new Privilegio("CREAR_TAREA", "Crear nuevas tareas", "TAREA", "CREATE"),
            new Privilegio("VER_TAREA", "Ver tareas", "TAREA", "READ"),
            new Privilegio("EDITAR_TAREA", "Editar tareas existentes", "TAREA", "UPDATE"),
            new Privilegio("ELIMINAR_TAREA", "Eliminar tareas", "TAREA", "DELETE"),
            new Privilegio("ASIGNAR_TAREA", "Asignar tareas a usuarios", "TAREA", "UPDATE"),
            new Privilegio("CREAR_USUARIO", "Crear nuevos usuarios", "USUARIO", "CREATE"),
            new Privilegio("VER_USUARIO", "Ver información de usuarios", "USUARIO", "READ"),
            new Privilegio("EDITAR_USUARIO", "Editar usuarios existentes", "USUARIO", "UPDATE"),
            new Privilegio("ELIMINAR_USUARIO", "Eliminar usuarios", "USUARIO", "DELETE"),
            new Privilegio("CAMBIAR_ROL", "Cambiar rol de usuario", "USUARIO", "UPDATE"),
            new Privilegio("CREAR_COMENTARIO", "Crear comentarios en proyectos y tareas", "COMENTARIO", "CREATE"),
            new Privilegio("EDITAR_COMENTARIO", "Editar comentarios", "COMENTARIO", "UPDATE"),
            new Privilegio("ELIMINAR_COMENTARIO", "Eliminar comentarios", "COMENTARIO", "DELETE"),
            new Privilegio("CREAR_ENTREGA", "Entregar tareas", "ENTREGA", "CREATE"),
            new Privilegio("VER_ENTREGA", "Ver entregas de tareas", "ENTREGA", "READ"),
            new Privilegio("CALIFICAR_ENTREGA", "Calificar entregas de tareas", "ENTREGA", "UPDATE"),
            new Privilegio("ACCESO_ADMIN", "Acceso al panel de administración", "ADMIN", "READ"),
            new Privilegio("GENERAR_REPORTES", "Generar reportes del sistema", "ADMIN", "READ")
        );
        privilegioRepository.saveAll(privilegios);
    }

    private void crearRoles() {
        List<Privilegio> privilegios = privilegioRepository.findByActivo(true);

        Rol admin = new Rol("ADMINISTRADOR", "Acceso total al sistema");
        admin.setPrivilegios(privilegios.stream().map(Privilegio::getId).toList());
        rolRepository.save(admin);

        Rol lider = new Rol("LIDER", "Líder de proyecto: crea proyectos, asigna tareas y revisa entregas");
        lider.setPrivilegios(privilegios.stream()
            .filter(p -> !List.of("ELIMINAR_USUARIO", "ELIMINAR_PROYECTO", "ACCESO_ADMIN", "CREAR_USUARIO").contains(p.getNombre()))
            .map(Privilegio::getId)
            .toList());
        rolRepository.save(lider);

        Rol participante = new Rol("PARTICIPANTE", "Voluntario: se une a proyectos y entrega tareas");
        participante.setPrivilegios(privilegios.stream()
            .filter(p -> "READ".equals(p.getAccion()) || List.of("CREAR_COMENTARIO", "CREAR_ENTREGA").contains(p.getNombre()))
            .map(Privilegio::getId)
            .toList());
        rolRepository.save(participante);
    }
}
