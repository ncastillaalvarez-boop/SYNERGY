// =====================================================================
//  SYNERGY - Reinicio de la base de datos (OPCIONAL)
//  Ejecutar:  mongosh "mongodb://localhost:27017" setup_synergy_db.js
//
//  Borra las colecciones de la base "synergy". Al volver a arrancar la
//  aplicación, Config/DataInitializer + Config/GeneradorDatos crean de nuevo:
//    - roles, privilegios e índices
//    - las 3 cuentas de prueba
//    - ~19.000 documentos de prueba coherentes (usuarios, proyectos,
//      participaciones, tareas, entregas y comentarios)
//
//  Si no tienes mongosh, no hace falta este script: pon
//  synergy.datos.regenerar-todo=true en application.properties, arranca
//  una vez (hace un respaldo en la carpeta respaldos/) y vuelve a ponerlo en false.
//
//  Cuentas de prueba:
//    ADMINISTRADOR  admin@synergy.com       Admin123*
//    LIDER          lider@synergy.com       Lider123*
//    VOLUNTARIO     voluntario@synergy.com  Voluntario123*
//    Usuarios generados: cualquier correo de la lista de usuarios / Synergy123*
// =====================================================================

db = db.getSiblingDB("synergy");

const colecciones = ["usuarios", "proyectos", "tareas", "participaciones", "comentarios",
                     "entregas_tareas", "roles", "privilegios", "contadores"];

colecciones.forEach(function (c) {
    const n = db.getCollection(c).countDocuments();
    db.getCollection(c).drop();
    print("  - " + c + ": " + n + " documentos eliminados");
});

print("\nListo. Arranca Synergy (mvnw spring-boot:run) y los datos se generarán solos.");
