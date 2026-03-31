// ============================================================
// SYNERGY - Script de Inicialización MongoDB
// Ejecutar en mongosh: load("synergy_mongo_setup.js")
// O pegar directamente en PowerShell de mongosh
// ============================================================

use("synergy");

// ============================================================
// 1. CREAR ÍNDICES - COLECCIÓN: usuarios
// ============================================================
db.usuarios.createIndex({ "correo": 1 }, { unique: true });
db.usuarios.createIndex({ "rol": 1 });
db.usuarios.createIndex({ "estado": 1 });
db.usuarios.createIndex({ "activo": 1 });

// ============================================================
// 2. CREAR ÍNDICES - COLECCIÓN: proyectos
// ============================================================
db.proyectos.createIndex({ "id_lider": 1 });
db.proyectos.createIndex({ "participantes": 1 });
db.proyectos.createIndex({ "estado": 1 });
db.proyectos.createIndex({ "fecha_creacion": -1 });

// ============================================================
// 3. CREAR ÍNDICES - COLECCIÓN: tareas
// ============================================================
db.tareas.createIndex({ "id_proyecto": 1 });
db.tareas.createIndex({ "usuarios_asignados": 1 });
db.tareas.createIndex({ "estado": 1 });
db.tareas.createIndex({ "fecha_entrega": 1 });

// ============================================================
// 4. CREAR ÍNDICES - COLECCIÓN: participaciones
// ============================================================
db.participaciones.createIndex({ "id_usuario": 1, "id_proyecto": 1 }, { unique: true });
db.participaciones.createIndex({ "id_proyecto": 1 });
db.participaciones.createIndex({ "id_usuario": 1 });

// ============================================================
// 5. CREAR ÍNDICES - COLECCIÓN: entregas_tareas
// ============================================================
db.entregas_tareas.createIndex({ "id_tarea": 1 });
db.entregas_tareas.createIndex({ "id_usuario": 1 });
db.entregas_tareas.createIndex({ "fecha_entrega": -1 });

// ============================================================
// 6. CREAR ÍNDICES - COLECCIÓN: comentarios
// ============================================================
db.comentarios.createIndex({ "id_proyecto": 1 });
db.comentarios.createIndex({ "id_usuario": 1 });
db.comentarios.createIndex({ "id_tarea": 1 });
db.comentarios.createIndex({ "fecha": -1 });

// ============================================================
// 7. CREAR ÍNDICES - COLECCIÓN: roles
// ============================================================
db.roles.createIndex({ "nombre": 1 }, { unique: true });
db.roles.createIndex({ "activo": 1 });

// ============================================================
// 8. CREAR ÍNDICES - COLECCIÓN: privilegios
// ============================================================
db.privilegios.createIndex({ "nombre": 1 }, { unique: true });
db.privilegios.createIndex({ "recurso": 1 });
db.privilegios.createIndex({ "accion": 1 });
db.privilegios.createIndex({ "activo": 1 });

print("✔ Índices creados correctamente.");

// ============================================================
// RESUMEN FINAL
// ============================================================
print("");
print("============================================");
print("  SYNERGY - Índices de MongoDB creados");
print("============================================");
print("  Colecciones con índices:");
print("    - usuarios");
print("    - proyectos");
print("    - tareas");
print("    - participaciones");
print("    - comentarios");
print("    - roles");
print("    - privilegios");
print("    - entregas_tareas");
print("");
print("  Los datos de inicialización serán");
print("  insertados automáticamente por el");
print("  DataInitializer de la aplicación");
print("============================================");
