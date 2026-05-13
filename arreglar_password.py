import bcrypt
from pymongo import MongoClient

# Generar hash bcrypt valido para "admin123"
hash_nuevo = bcrypt.hashpw("admin123".encode(), bcrypt.gensalt(10)).decode()
print(f"Hash nuevo: {hash_nuevo}")

# Conectar y actualizar
db = MongoClient("mongodb://localhost:27017").synergy
resultado = db.usuarios.update_one(
    {"correo": "admin@synergy.com"},
    {"$set": {"contrasena": hash_nuevo}}
)

print(f"Usuarios actualizados: {resultado.modified_count}")
if resultado.modified_count == 1:
    print("Listo! Ahora puedes hacer login con:")
    print("  Correo:     admin@synergy.com")
    print("  Contrasena: admin123")
else:
    print("No se actualizo ningun usuario. Verifica el correo.")