// =============================================================
//  MIGRACIÓN: asignar tipoOrganizacion y audiencia a proyectos
//  Ejecutar con:
//    mongosh "mongodb://localhost:27017" migrar_organizacion.js
//
//  PASO 1 — preview (DRY_RUN = true):  solo muestra qué cambiaría
//  PASO 2 — aplicar  (DRY_RUN = false): guarda el backup y actualiza
// =============================================================

const DRY_RUN = true; // ← Cambia a false para aplicar los cambios

// =============================================================
//  DATOS DE MIGRACIÓN
// =============================================================

const ORGANIZACIONES = [
    "Empresa",
    "Proyecto estudiantil",
    "ONG",
    "Institución educativa",
    "Entidad gubernamental",
    "Comunidad"
];

const AUDIENCIA_POR_ORG = {
    "Empresa": [
        "Clientes corporativos y socios comerciales",
        "Empleados y colaboradores internos",
        "Emprendedores y startups tecnológicas",
        "Pequeñas y medianas empresas de la región",
        "Empresas del sector industrial"
    ],
    "Proyecto estudiantil": [
        "Estudiantes universitarios de primeros semestres",
        "Jóvenes de secundaria interesados en ciencia",
        "Niños y niñas de educación primaria",
        "Docentes y educadores de la región",
        "Comunidad académica en general"
    ],
    "ONG": [
        "Comunidades en situación de vulnerabilidad",
        "Adultos mayores sin redes de apoyo",
        "Jóvenes en riesgo social",
        "Familias de escasos recursos",
        "Personas en situación de calle"
    ],
    "Institución educativa": [
        "Estudiantes de pregrado y posgrado",
        "Docentes y personal académico",
        "Padres de familia y acudientes",
        "Comunidad educativa local",
        "Egresados y exalumnos"
    ],
    "Entidad gubernamental": [
        "Ciudadanos en general",
        "Comunidades rurales y periurbanas",
        "Líderes comunitarios y juntas de acción",
        "Empresas locales y comerciantes",
        "Funcionarios y servidores públicos"
    ],
    "Comunidad": [
        "Habitantes del barrio y sectores aledaños",
        "Jóvenes voluntarios de la zona",
        "Familias locales con niños a cargo",
        "Adultos activos de la comunidad",
        "Grupos de interés y colectivos locales"
    ]
};

// =============================================================
//  UTILIDADES
// =============================================================

function escogerAlAzar(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

function limpioONulo(valor) {
    return valor === null || valor === undefined || valor === "";
}

// =============================================================
//  CONEXIÓN
// =============================================================

db = db.getSiblingDB("synergy");

// =============================================================
//  1. BACKUP (solo cuando DRY_RUN = false)
// =============================================================

if (!DRY_RUN) {
    try {
        const todos = db.proyectos.find({}).toArray();
        const nombreArchivo = "backup_proyectos_" + Date.now() + ".json";
        const contenido = JSON.stringify(todos, null, 2);

        // fs está disponible en mongosh 1.1+
        const fs = require("fs");
        fs.writeFileSync(nombreArchivo, contenido);
        print("\n💾 Backup guardado en: " + nombreArchivo);
        print("   (" + todos.length + " proyectos respaldados)\n");
    } catch (e) {
        print("\n⚠️  No se pudo guardar el backup automático: " + e.message);
        print("   Ejecuta este comando manualmente ANTES de continuar:");
        print('   mongoexport --db synergy --collection proyectos --out backup_proyectos.json\n');
    }
}

// =============================================================
//  2. IDENTIFICAR PROYECTOS A MIGRAR
// =============================================================

const sinOrg = db.proyectos.find({
    $or: [
        { tipoOrganizacion: null },
        { tipoOrganizacion: { $exists: false } },
        { tipoOrganizacion: "" }
    ]
}).toArray();

const total       = db.proyectos.countDocuments({});
const aMigrar     = sinOrg.length;
const yaCompletos = total - aMigrar;

print("=============================================================");
print("  MIGRACIÓN: tipoOrganizacion + audiencia");
print("  Modo: " + (DRY_RUN ? "PREVIEW (sin cambios)" : "APLICAR CAMBIOS"));
print("=============================================================");
print("  Total proyectos en BD:         " + total);
print("  Ya tienen organización:        " + yaCompletos);
print("  Proyectos a actualizar:        " + aMigrar);
print("=============================================================\n");

if (aMigrar === 0) {
    print("✅ Todos los proyectos ya tienen organización asignada. Nada que hacer.\n");
    quit();
}

// =============================================================
//  3. PREVIEW DE CAMBIOS
// =============================================================

print("Cambios que se " + (DRY_RUN ? "APLICARÍAN" : "APLICARÁN") + ":\n");

// Generar el plan de cambios (mismo seed para preview y ejecución)
const planCambios = sinOrg.map(function(p) {
    const org      = escogerAlAzar(ORGANIZACIONES);
    const audiencia = escogerAlAzar(AUDIENCIA_POR_ORG[org]);
    return {
        _id:      p._id,
        nombre:   p.nombre || "(sin nombre)",
        org:      org,
        audiencia: audiencia
    };
});

planCambios.forEach(function(cambio, i) {
    print("  " + (i + 1) + ". \"" + cambio.nombre + "\"");
    print("     → Organización: " + cambio.org);
    print("     → Audiencia:    " + cambio.audiencia);
    print("");
});

// =============================================================
//  4. EJECUTAR CAMBIOS (solo si DRY_RUN = false)
// =============================================================

if (DRY_RUN) {
    print("-------------------------------------------------------------");
    print("  MODO PREVIEW — ningún dato fue modificado.");
    print("  Revisa los cambios arriba. Si estás de acuerdo:");
    print("  1. Abre el archivo migrar_organizacion.js");
    print("  2. Cambia  DRY_RUN = true  →  DRY_RUN = false");
    print("  3. Vuelve a ejecutar el script");
    print("-------------------------------------------------------------\n");
} else {
    print("Aplicando cambios...\n");

    let actualizados = 0;
    let errores      = 0;

    planCambios.forEach(function(cambio) {
        try {
            const resultado = db.proyectos.updateOne(
                { _id: cambio._id },
                {
                    $set: {
                        tipoOrganizacion:  cambio.org,
                        audiencia:         cambio.audiencia,
                        fechaActualizacion: new Date()
                    }
                }
            );
            if (resultado.modifiedCount === 1) {
                actualizados++;
            } else {
                print("  ⚠️  Sin cambio en: " + cambio.nombre);
            }
        } catch (e) {
            errores++;
            print("  ❌ Error en \"" + cambio.nombre + "\": " + e.message);
        }
    });

    print("=============================================================");
    print("  ✅ Proyectos actualizados: " + actualizados);
    if (errores > 0) {
        print("  ❌ Errores:               " + errores);
    }
    print("=============================================================\n");

    // Verificación final
    const pendientes = db.proyectos.countDocuments({
        $or: [
            { tipoOrganizacion: null },
            { tipoOrganizacion: { $exists: false } },
            { tipoOrganizacion: "" }
        ]
    });

    if (pendientes === 0) {
        print("✅ Verificación OK — todos los proyectos tienen organización.\n");
    } else {
        print("⚠️  Aún quedan " + pendientes + " proyecto(s) sin organización.\n");
    }
}
