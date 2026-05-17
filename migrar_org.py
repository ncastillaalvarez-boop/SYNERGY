"""
Migración: asignar tipoOrganizacion y audiencia a proyectos sin ella.
Uso:
  python migrar_org.py preview   → muestra cambios sin tocar nada
  python migrar_org.py aplicar  → hace backup y aplica los cambios
"""

import sys
import json
import random
from datetime import datetime, timezone
from pymongo import MongoClient
from bson import ObjectId

# ── Configuración ────────────────────────────────────────────────
MONGO_URI = "mongodb://localhost:27017"
DB_NAME   = "synergy"

# ── Datos de migración ───────────────────────────────────────────
AUDIENCIA_POR_ORG = {
    "Empresa": [
        "Clientes corporativos y socios comerciales",
        "Empleados y colaboradores internos",
        "Emprendedores y startups tecnológicas",
        "Pequeñas y medianas empresas de la región",
        "Empresas del sector industrial",
    ],
    "Proyecto estudiantil": [
        "Estudiantes universitarios de primeros semestres",
        "Jóvenes de secundaria interesados en ciencia",
        "Niños y niñas de educación primaria",
        "Docentes y educadores de la región",
        "Comunidad académica en general",
    ],
    "ONG": [
        "Comunidades en situación de vulnerabilidad",
        "Adultos mayores sin redes de apoyo",
        "Jóvenes en riesgo social",
        "Familias de escasos recursos",
        "Personas en situación de calle",
    ],
    "Institución educativa": [
        "Estudiantes de pregrado y posgrado",
        "Docentes y personal académico",
        "Padres de familia y acudientes",
        "Comunidad educativa local",
        "Egresados y exalumnos",
    ],
    "Entidad gubernamental": [
        "Ciudadanos en general",
        "Comunidades rurales y periurbanas",
        "Líderes comunitarios y juntas de acción",
        "Empresas locales y comerciantes",
        "Funcionarios y servidores públicos",
    ],
    "Comunidad": [
        "Habitantes del barrio y sectores aledaños",
        "Jóvenes voluntarios de la zona",
        "Familias locales con niños a cargo",
        "Adultos activos de la comunidad",
        "Grupos de interés y colectivos locales",
    ],
}

ORGANIZACIONES = list(AUDIENCIA_POR_ORG.keys())

# ── Serializador JSON para tipos de MongoDB ──────────────────────
class MongoEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, ObjectId):
            return str(o)
        if isinstance(o, datetime):
            return o.isoformat()
        if isinstance(o, bytes):
            return "<binary>"
        return super().default(o)

# ── Utilidades ───────────────────────────────────────────────────

def sin_org(doc):
    v = doc.get("tipoOrganizacion")
    return v is None or v == ""

def generar_plan(proyectos):
    random.seed(42)   # seed fijo → preview y ejecución asignan lo mismo
    plan = []
    for p in proyectos:
        org      = random.choice(ORGANIZACIONES)
        audiencia = random.choice(AUDIENCIA_POR_ORG[org])
        plan.append({"_id": p["_id"], "nombre": p.get("nombre", "(sin nombre)"),
                     "org": org, "audiencia": audiencia})
    return plan

# ── Lógica principal ─────────────────────────────────────────────

def main():
    modo = sys.argv[1] if len(sys.argv) > 1 else "preview"

    client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=4000)
    try:
        client.admin.command("ping")
    except Exception as e:
        print(f"❌ No se pudo conectar a MongoDB: {e}")
        sys.exit(1)

    col = client[DB_NAME]["proyectos"]

    total      = col.count_documents({})
    sin_org_docs = [d for d in col.find({}) if sin_org(d)]
    a_migrar   = len(sin_org_docs)
    ya_listos  = total - a_migrar

    print("=" * 60)
    print(f"  MIGRACIÓN tipoOrganizacion + audiencia  [{modo.upper()}]")
    print("=" * 60)
    print(f"  Total proyectos en BD:      {total}")
    print(f"  Ya tienen organización:     {ya_listos}")
    print(f"  Proyectos a actualizar:     {a_migrar}")
    print("=" * 60)

    if a_migrar == 0:
        print("\n✅ Todos los proyectos ya tienen organización. Nada que hacer.\n")
        return

    plan = generar_plan(sin_org_docs)

    # Mostrar solo los primeros 15 como muestra (son 3001 en total)
    muestra = plan[:15]
    resto   = len(plan) - len(muestra)
    print(f"Muestra de los primeros {len(muestra)} cambios (de {len(plan)} en total):\n")
    for i, c in enumerate(muestra, 1):
        print(f"  {i}. \"{c['nombre']}\"")
        print(f"     Organizacion: {c['org']}")
        print(f"     Audiencia:    {c['audiencia']}")
        print()
    if resto > 0:
        print(f"  ... y {resto} proyectos mas con asignaciones similares.\n")

    if modo == "preview":
        print("-" * 60)
        print("  MODO PREVIEW - ningun dato fue modificado.")
        print("  Si los cambios te parecen correctos, confirma")
        print("  y el script se ejecutara en modo [aplicar].")
        print("-" * 60)
        return

    # ── BACKUP ──────────────────────────────────────────────────
    ts           = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup_path  = f"backup_proyectos_{ts}.json"
    todos        = list(col.find({}))

    with open(backup_path, "w", encoding="utf-8") as f:
        json.dump(todos, f, cls=MongoEncoder, ensure_ascii=False, indent=2)

    print(f"💾 Backup guardado en: {backup_path}  ({total} proyectos)\n")

    # ── APLICAR ─────────────────────────────────────────────────
    actualizados = 0
    errores      = 0
    for c in plan:
        try:
            r = col.update_one(
                {"_id": c["_id"]},
                {"$set": {"tipoOrganizacion": c["org"],
                           "audiencia":        c["audiencia"],
                           "fechaActualizacion": datetime.now(timezone.utc)}}
            )
            if r.modified_count == 1:
                actualizados += 1
            else:
                print(f"  ⚠️  Sin cambio en: {c['nombre']}")
        except Exception as e:
            errores += 1
            print(f"  ❌ Error en \"{c['nombre']}\": {e}")

    print("=" * 60)
    print(f"  ✅ Proyectos actualizados: {actualizados}")
    if errores:
        print(f"  ❌ Errores:              {errores}")
    print("=" * 60)

    # ── VERIFICACIÓN ────────────────────────────────────────────
    pendientes = sum(1 for d in col.find({}) if sin_org(d))
    if pendientes == 0:
        print("\n✅ Verificación OK — todos los proyectos tienen organización.\n")
    else:
        print(f"\n⚠️  Aún quedan {pendientes} proyecto(s) sin organización.\n")

    client.close()


if __name__ == "__main__":
    main()
