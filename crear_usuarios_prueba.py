"""
Crea 3 usuarios de prueba en la base de datos Synergy:
- Un ADMINISTRADOR
- Un LIDER
- Un USUARIO (rol "USUARIO")

Si los usuarios ya existen (mismo correo), los actualiza con el hash nuevo
y los deja activos. Si no existen, los crea desde cero.

Todas las contrasenas son la misma para que sea facil de probar:
  Contrasena para los 3 usuarios:  synergy123
"""

import bcrypt
from datetime import datetime, timezone
from pymongo import MongoClient

# ============================================================
# CONFIGURACION
# ============================================================
MONGO_URI = "mongodb://localhost:27017"
DB_NAME = "synergy"
CONTRASENA = "synergy123"   # contrasena para los 3 usuarios

# Lista de usuarios a crear. Solo agrega/edita esta lista si quieres mas.
USUARIOS = [
    {
        "nombre":  "Super Administrador",
        "correo":  "admin@synergy.com",
        "rol":     "ADMINISTRADOR",
    },
    {
        "nombre":  "Juan Lider Comunitario",
        "correo":  "lider@synergy.com",
        "rol":     "LIDER",
    },
    {
        "nombre":  "Maria Usuario Prueba",
        "correo":  "usuario@synergy.com",
        "rol":     "USUARIO",
    },
]

# ============================================================
# EJECUCION
# ============================================================
print("=" * 60)
print("  Creacion/Actualizacion de usuarios de prueba")
print("=" * 60)

# Generar un hash bcrypt unico y valido para la contrasena
hash_pass = bcrypt.hashpw(CONTRASENA.encode("utf-8"), bcrypt.gensalt(10)).decode("utf-8")
print(f"\nHash bcrypt generado para '{CONTRASENA}':")
print(f"  {hash_pass}")

# Conectar a Mongo
client = MongoClient(MONGO_URI)
db = client[DB_NAME]

# Verificar que existan los roles
roles_disponibles = {r["nombre"]: r["_id"] for r in db.roles.find()}
print(f"\nRoles encontrados en la BD: {list(roles_disponibles.keys())}")

ahora = datetime.now(timezone.utc)

print("\nProcesando usuarios:\n")

for u in USUARIOS:
    nombre = u["nombre"]
    correo = u["correo"]
    rol_nombre = u["rol"]

    # Validar que el rol existe
    if rol_nombre not in roles_disponibles:
        print(f"  [ERROR] El rol '{rol_nombre}' no existe en la BD. Saltando {correo}.")
        continue

    id_rol = roles_disponibles[rol_nombre]

    # Insertar o actualizar (upsert)
    resultado = db.usuarios.update_one(
        {"correo": correo},
        {
            "$set": {
                "nombre": nombre,
                "contrasena": hash_pass,
                "idRol": id_rol,
                "estado": "ACTIVO",
                "fechaActualizacion": ahora,
                "ultimoCambioContrasena": ahora,
                "_class": "com.proaula.spring.synergy.Model.Usuario",
            },
            "$setOnInsert": {
                "fechaCreacion": ahora,
            },
        },
        upsert=True,
    )

    if resultado.upserted_id:
        accion = "CREADO"
    elif resultado.modified_count > 0:
        accion = "ACTUALIZADO"
    else:
        accion = "SIN CAMBIOS"

    print(f"  [{accion:11s}] {rol_nombre:15s} -> {correo}")

print("\n" + "=" * 60)
print("  CREDENCIALES DE LOGIN")
print("=" * 60)
print(f"  Contrasena para todos: {CONTRASENA}\n")
for u in USUARIOS:
    print(f"  {u['rol']:15s} -> Correo: {u['correo']}")
print("=" * 60)
print("\nListo!")