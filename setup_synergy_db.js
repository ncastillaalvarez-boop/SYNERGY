// Archivo de Configuración de Base de Datos MongoDB para SYNERGY
// Ejecutar con: mongosh "mongodb://localhost:27017" setup_synergy_db.js

print("Iniciando la configuración de la base de datos de Synergy...");

// Conectar a la base de datos synergy (la creará si no existe)
db = db.getSiblingDB("synergy");

// ==========================================
// 1. LIMPIAR COLECCIONES (opcional)
// ==========================================
db.usuarios.drop();
db.proyectos.drop();
db.roles.drop();
db.privilegios.drop();

// ==========================================
// 2. CREAR PRIVILEGIOS
// ==========================================
print("Creando privilegios de seguridad...");
db.privilegios.insertMany([
    { _id: "ADMIN_READ", descripcion: "Lectura de nivel administrador" },
    { _id: "ADMIN_CREATE", descripcion: "Creación de nivel administrador" },
    { _id: "ADMIN_UPDATE", descripcion: "Modificación de nivel administrador" },
    { _id: "ADMIN_DELETE", descripcion: "Borrado de nivel administrador" },
    { _id: "MANAGEMENT_READ", descripcion: "Lectura de nivel de gestión" },
    { _id: "MANAGEMENT_CREATE", descripcion: "Creación de nivel de gestión" },
    { _id: "MANAGEMENT_UPDATE", descripcion: "Modificación de nivel de gestión" },
    { _id: "MANAGEMENT_DELETE", descripcion: "Borrado de nivel de gestión" },
    { _id: "USER_READ", descripcion: "Lectura básica" }
]);

// ==========================================
// 3. CREAR ROLES (ADMINISTRADOR, LIDER, PARTICIPANTE)
// ==========================================
print("Creando Roles...");
db.roles.insertMany([
    {
        _id: "ADMINISTRADOR",
        descripcion: "Administrador general del sistema",
        privilegios: [
            "ADMIN_READ", "ADMIN_CREATE", "ADMIN_UPDATE", "ADMIN_DELETE",
            "MANAGEMENT_READ", "MANAGEMENT_CREATE", "MANAGEMENT_UPDATE", "MANAGEMENT_DELETE",
            "USER_READ"
        ]
    },
    {
        _id: "LIDER",
        descripcion: "Líder Comunitario gestor de proyectos",
        privilegios: [
            "MANAGEMENT_READ", "MANAGEMENT_CREATE", "MANAGEMENT_UPDATE", "MANAGEMENT_DELETE",
            "USER_READ"
        ]
    },
    {
        _id: "PARTICIPANTE",
        descripcion: "Usuario voluntario estándar",
        privilegios: [
            "USER_READ"
        ]
    }
]);

// ==========================================
// 4. CREAR USUARIOS DE PRUEBA
// ==========================================
// Contraseña por defecto: 12345 (Hash BCrypt aproximado para "12345")
// Importante: En un entorno de Spring actual, Spring Security validará esto contra BCrypt
const passwordHash = "$2a$10$A0TohZ5H.7Q8Q.O6E9d7XOn1xWd6e9q98WnK16L3fG/5zX1j9.xN."; 

print("Creando usuarios base...");
db.usuarios.insertMany([
    {
        _id: "usr_admin_01",
        nombre: "Super Administrador",
        correo: "admin@synergy.com",
        contrasena: passwordHash,
        salt: "salt",
        rol: "ADMINISTRADOR",
        estado: "ACTIVO",
        activo: true,
        privilegios: ["ADMIN_READ", "ADMIN_UPDATE", "ADMIN_DELETE"],
        proyectos: [],
        tareasAsignadas: [],
        fechaCreacion: new Date(),
        ultimaCambioContrasena: new Date()
    },
    {
        _id: "usr_lider_01",
        nombre: "Juan Lider Comunitario",
        correo: "lider@synergy.com",
        contrasena: passwordHash,
        salt: "salt",
        rol: "LIDER",
        estado: "ACTIVO",
        activo: true,
        privilegios: ["MANAGEMENT_READ", "MANAGEMENT_CREATE"],
        proyectos: [],
        tareasAsignadas: [],
        fechaCreacion: new Date(),
        ultimaCambioContrasena: new Date()
    },
    {
        _id: "usr_participante_01",
        nombre: "Maria Voluntaria",
        correo: "user@synergy.com",
        contrasena: passwordHash,
        salt: "salt",
        rol: "PARTICIPANTE",
        estado: "ACTIVO",
        activo: true,
        privilegios: ["USER_READ"],
        proyectos: [],
        tareasAsignadas: [],
        fechaCreacion: new Date(),
        ultimaCambioContrasena: new Date()
    }
]);

// ==========================================
// 5. CREAR UN PROYECTO DE PRUEBA
// ==========================================
print("Creando proyectos iniciales...");
db.proyectos.insertOne({
    _id: "PRJ-001",
    nombre: "Limpieza del Río Principal",
    descripcion: "Campaña de recuperación y limpieza comunitaria",
    codigoUnico: "C-PRJ-001",
    estado: "EN_EJECUCION", // PENDIENTE, EN_EJECUCION, FINALIZADO, CANCELADO
    porcentajeAvance: 15.0,
    fechaInicio: new Date("2024-01-01T00:00:00Z"),
    fechaFin: new Date("2024-12-31T23:59:59Z"),
    liderId: "usr_lider_01",
    voluntariosIds: ["usr_participante_01"],
    tieneAnomalia: false,
    documentoAdjunto: null
});

// ==========================================
// 6. CREAR ÍNDICES IMPORTANTES
// ==========================================
print("Creando índices...");
db.usuarios.createIndex({ correo: 1 }, { unique: true });
db.proyectos.createIndex({ codigoUnico: 1 }, { unique: true });
db.proyectos.createIndex({ estado: 1 });

print("¡Configuración de Base de Datos completada con éxito!");
