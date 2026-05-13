package com.proaula.spring.synergy.Config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.proaula.spring.synergy.Model.Comentario;
import com.proaula.spring.synergy.Model.Participacion;
import com.proaula.spring.synergy.Model.Privilegio;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Rol;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.ComentarioRepository;
import com.proaula.spring.synergy.Repository.ParticipacionRepository;
import com.proaula.spring.synergy.Repository.PrivilegioRepository;
import com.proaula.spring.synergy.Repository.ProyectoRepository;
import com.proaula.spring.synergy.Repository.RolRepository;
import com.proaula.spring.synergy.Repository.TareaRepository;
import com.proaula.spring.synergy.Repository.UsuarioRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private PrivilegioRepository privilegioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private TareaRepository tareaRepository;

    @Autowired
    private ParticipacionRepository participacionRepository;

    @Autowired
    private ComentarioRepository comentarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (usuarioRepository.count() > 0) {
            System.out.println("✔ Base de datos ya inicializada");
            return;
        }

        System.out.println("🔧 Inicializando base de datos...");

        createPrivilegios();
        createRoles();
        createUsuarios();
        createProyectos();
        createParticipaciones();
        createTareas();
        createComentarios();

        System.out.println("✔ Base de datos inicializada correctamente");
    }

    private void createPrivilegios() {
        System.out.println("📋 Creando privilegios...");

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
        System.out.println("  ✓ " + privilegios.size() + " privilegios creados");
    }

    private void createRoles() {
        System.out.println("👤 Creando roles...");

        List<Privilegio> privilegios = privilegioRepository.findByActivo(true);

        Rol admin = new Rol("ADMINISTRADOR", "Acceso total al sistema");
        admin.setPrivilegios(privilegios.stream().map(Privilegio::getId).toList());
        rolRepository.save(admin);

        Rol lider = new Rol("LIDER", "Líder de proyecto - puede crear tareas y asignar usuarios");
        List<String> privilegiosLider = privilegios.stream()
            .filter(p -> !p.getNombre().startsWith("ELIMINAR_USUARIO") &&
                         !p.getNombre().startsWith("ELIMINAR_PROYECTO") &&
                         !p.getNombre().equals("ACCESO_ADMIN") &&
                         !p.getNombre().equals("CREAR_USUARIO"))
            .map(Privilegio::getId)
            .toList();
        lider.setPrivilegios(privilegiosLider);
        rolRepository.save(lider);

        Rol participante = new Rol("PARTICIPANTE", "Participante - acceso limitado a proyectos asignados");
        List<String> privilegiosParticipante = privilegios.stream()
            .filter(p -> p.getAccion().equals("READ") || p.getNombre().equals("CREAR_COMENTARIO") ||
                         p.getNombre().equals("CREAR_ENTREGA"))
            .map(Privilegio::getId)
            .toList();
        participante.setPrivilegios(privilegiosParticipante);
        rolRepository.save(participante);

        System.out.println("  ✓ 3 roles creados");
    }

    private void createUsuarios() {
        System.out.println("👥 Creando usuarios de prueba...");

        String passwordEncriptada = passwordEncoder.encode("123456");

        Rol adminRol      = rolRepository.findByNombre("ADMINISTRADOR").orElse(null);
        Rol liderRol      = rolRepository.findByNombre("LIDER").orElse(null);
        Rol participanteRol = rolRepository.findByNombre("PARTICIPANTE").orElse(null);

        Usuarios admin = new Usuarios("Administrador", "admin@synergy.com", passwordEncriptada);
        if (adminRol != null) admin.setIdRol(adminRol.getId());
        admin.setEstado("ACTIVO");
        admin.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(admin);

        Usuarios lider = new Usuarios("Líder de Proyecto", "lider@synergy.com", passwordEncriptada);
        if (liderRol != null) lider.setIdRol(liderRol.getId());
        lider.setEstado("ACTIVO");
        lider.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(lider);

        Usuarios participante = new Usuarios("Usuario Participante", "usuario@synergy.com", passwordEncriptada);
        if (participanteRol != null) participante.setIdRol(participanteRol.getId());
        participante.setEstado("ACTIVO");
        participante.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(participante);

        System.out.println("  ✓ 3 usuarios de prueba creados");
        System.out.println("     - admin@synergy.com (ADMINISTRADOR)");
        System.out.println("     - lider@synergy.com (LIDER)");
        System.out.println("     - usuario@synergy.com (PARTICIPANTE)");
    }

    private void createProyectos() {
        System.out.println("📦 Creando proyectos de ejemplo...");

        Usuarios lider = usuarioRepository.findByCorreo("lider@synergy.com");

        Proyecto proyecto = new Proyecto(
            "Proyecto de Gestión de Inventario",
            "Sistema para gestionar inventario de la empresa",
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 6, 30),
            lider.getId()
        );
        proyectoRepository.save(proyecto);

        System.out.println("  ✓ 1 proyecto creado");
    }

    private void createParticipaciones() {
        System.out.println("📝 Creando participaciones...");

        Usuarios lider       = usuarioRepository.findByCorreo("lider@synergy.com");
        Usuarios participante = usuarioRepository.findByCorreo("usuario@synergy.com");
        Proyecto proyecto    = proyectoRepository.findAll().get(0);

        participacionRepository.save(new Participacion(lider.getId(), proyecto.getId()));
        participacionRepository.save(new Participacion(participante.getId(), proyecto.getId()));

        System.out.println("  ✓ 2 participaciones creadas");
    }

    private void createTareas() {
        System.out.println("📌 Creando tareas de ejemplo...");

        Usuarios participante = usuarioRepository.findByCorreo("usuario@synergy.com");
        Proyecto proyecto     = proyectoRepository.findAll().get(0);

        Tarea tarea = new Tarea(
            "Implementar módulo de autenticación",
            "Crear sistema de login seguro con Spring Security",
            "PENDIENTE",
            LocalDate.of(2024, 4, 15),
            proyecto.getId()
        );
        tarea.addUsuarioAsignado(participante.getId());
        tareaRepository.save(tarea);

        System.out.println("  ✓ 1 tarea creada");
    }

    private void createComentarios() {
        System.out.println("💬 Creando comentarios de ejemplo...");

        Usuarios lider = usuarioRepository.findByCorreo("lider@synergy.com");
        Proyecto proyecto = proyectoRepository.findAll().get(0);

        Comentario comentario = new Comentario(
            "Proyecto iniciado, por favor revisar los requerimientos.",
            proyecto.getId(),
            lider.getId()
        );
        comentarioRepository.save(comentario);

        System.out.println("  ✓ 1 comentario creado");
    }
}
