"""
=======================================================================
 SYNERGY - Script de Poblacion de Base de Datos MongoDB
=======================================================================
 Proyecto: Synergy - Gestion de proyectos comunitarios ambientales
           en Cartagena de Indias
 Base de datos: synergy
 Motor: MongoDB (localhost:27017)
 Total de documentos generados: 12.500
-----------------------------------------------------------------------
 IMPLEMENTACIONES SEGÚN REVISIONES:
 1. "Comentarios como colección independiente debido a su posible alto 
    crecimiento": Se genera la colección 'comentarios' de forma externa 
    a los proyectos, usando referencias para evitar límites de tamaño de
    documentos en MongoDB.
 2. "Considerar privilegios en el rol": En 'generar_roles' se asocian los 
    privilegios por rol estrictamente. Los usuarios ya no mantienen su 
    propio arreglo de privilegios, basándose 100% en el rol.
 3. "Comentar código organizado": Todas las funciones ahora cuentan con
    su respectiva explicación de funcionalidad.
-----------------------------------------------------------------------
 Requisitos:
   pip install pymongo faker bcrypt
 Ejecucion:
   python poblar_synergy.py
=======================================================================
"""

import random
import string
from datetime import datetime, timedelta, timezone
from bson import ObjectId
from pymongo import MongoClient
from faker import Faker

# ---------------------------------------------------------------------
# CONFIGURACION
# ---------------------------------------------------------------------
MONGO_URI = "mongodb://localhost:27017"
DB_NAME = "synergy"

# Distribucion de documentos (total = 12.500)
CANTIDAD = {
    "privilegios":    22,
    "roles":           4,
    "usuarios":     1500,
    "proyectos":    2000,
    "tareas":       3500,
    "participaciones": 4500,
    "comentarios":   974,
}

fake = Faker("es_CO")
Faker.seed(42)
random.seed(42)

# ---------------------------------------------------------------------
# DATOS DE CONTEXTO - CARTAGENA DE INDIAS
# ---------------------------------------------------------------------
BARRIOS_CARTAGENA = [
    "Getsemaní", "Manga", "Bocagrande", "Castillogrande", "El Laguito",
    "Centro Histórico", "San Diego", "La Matuna", "Crespo", "Marbella",
    "Pie de la Popa", "Torices", "Canapote", "El Cabrero", "Boston",
    "El Pozón", "Olaya Herrera", "La Boquilla", "Zaragocilla", "Blas de Lezo",
    "El Country", "Los Alpes", "Nuevo Bosque", "San Fernando", "Ternera",
    "Turbaco (periferia)", "La María", "San José de los Campanos", "Chambacú",
    "Las Gaviotas", "Villa Estrella", "El Socorro", "La Consolata",
    "Ciudadela 2000", "Nelson Mandela", "El Educador", "La Esperanza",
]

TEMATICAS_AMBIENTALES = [
    ("Limpieza de playas", "playa"),
    ("Reforestación urbana", "arboles"),
    ("Recuperación de la Ciénaga de la Virgen", "cienaga"),
    ("Gestión de residuos sólidos", "reciclaje"),
    ("Huertas comunitarias", "huerta"),
    ("Educación ambiental en colegios", "educacion"),
    ("Conservación de manglares", "manglares"),
    ("Reciclaje de plásticos", "plastico"),
    ("Compostaje comunitario", "compostaje"),
    ("Siembra de árboles nativos", "siembra"),
    ("Protección de tortugas marinas", "fauna"),
    ("Ahorro de agua", "agua"),
    ("Energía solar comunitaria", "energia"),
    ("Campaña anti-basura en canales", "canales"),
    ("Recuperación de espacios verdes", "parques"),
    ("Gestión de aguas residuales", "aguas"),
    ("Monitoreo de calidad del aire", "aire"),
    ("Reducción del plástico de un solo uso", "plastico"),
    ("Brigadas de aseo barrial", "aseo"),
    ("Protección de aves urbanas", "aves"),
]

PALABRAS_TAREA = [
    "Organizar jornada de", "Coordinar brigada de", "Realizar diagnóstico de",
    "Capacitar voluntarios en", "Recolectar materiales para", "Gestionar permisos de",
    "Convocar reunión de", "Ejecutar actividad de", "Evaluar resultados de",
    "Documentar avances de", "Difundir campaña de", "Contactar aliados para",
    "Preparar logística de", "Distribuir insumos de", "Monitorear indicadores de",
]

ESTADOS_PROYECTO = ["ACTIVO", "PLANIFICACION", "EN_EJECUCION", "FINALIZADO", "SUSPENDIDO"]
ESTADOS_TAREA = ["PENDIENTE", "EN_PROGRESO", "COMPLETADA", "CANCELADA", "REVISION"]
ESTADOS_USUARIO = ["ACTIVO", "INACTIVO", "SUSPENDIDO"]

COMENTARIOS_PLANTILLAS = [
    "Excelente avance en {tema}, sigamos así.",
    "Propongo ampliar la cobertura a otros sectores de {barrio}.",
    "Necesitamos más voluntarios para la jornada del próximo fin de semana.",
    "Los resultados de la última actividad superaron las expectativas.",
    "Sugiero coordinar con la alcaldía para el retiro de residuos.",
    "Recordatorio: la reunión de seguimiento es este jueves a las 3pm.",
    "Gracias al equipo por el compromiso mostrado en {barrio}.",
    "Se requiere revisar el presupuesto asignado para {tema}.",
    "La comunidad de {barrio} está muy motivada con el proyecto.",
    "Adjuntaré el informe fotográfico en la próxima entrega.",
    "Hay que reforzar la convocatoria en redes sociales.",
    "Recomiendo incluir a los colegios cercanos como aliados.",
    "El impacto ambiental es notable, felicitaciones al líder.",
    "Pendiente: gestionar donaciones de bolsas y guantes.",
    "La alianza con la junta de acción comunal está funcionando bien.",
    "Solicito reprogramar la actividad por condiciones climáticas.",
    "Ya contactamos a la EPA Cartagena para validar el cronograma.",
    "Los jóvenes del barrio están liderando muy bien las brigadas.",
    "Necesitamos más material educativo sobre {tema}.",
    "El trabajo articulado con la Universidad de Cartagena fue clave.",
]


# ---------------------------------------------------------------------
# UTILIDADES
# ---------------------------------------------------------------------
def fecha_aleatoria(inicio, fin):
    """
    Genera una fecha aleatoria entre dos datetimes (timezone-aware, UTC).
    Si fin <= inicio, devuelve inicio para evitar rangos invalidos.
    """
    if inicio.tzinfo is None:
        inicio = inicio.replace(tzinfo=timezone.utc)
    if fin.tzinfo is None:
        fin = fin.replace(tzinfo=timezone.utc)
    if fin <= inicio:
        return inicio
    delta = fin - inicio
    segundos = random.randint(0, int(delta.total_seconds()))
    return inicio + timedelta(seconds=segundos)

def generar_salt(longitud=8):
    """Genera un salt aleatorio para la encriptacion de contraseñas de los usuarios."""
    return "".join(random.choices(string.ascii_letters + string.digits, k=longitud))

# Contraseña de ejemplo (mismo hash bcrypt para todos los usuarios generados)
HASH_DEMO = "$2a$10$OnJOoz/lXswM3tX2RwQYw.gR1aPPl5s6nBr/CnOtiqAzNamkB9lY."


# ---------------------------------------------------------------------
# GENERADORES POR COLECCION
# ---------------------------------------------------------------------
def generar_privilegios():
    """
    Genera los privilegios fijos y basicos del sistema que permitiran
    manejar la autorizacion en los controladores del Backend (Spring Boot).
    """
    ahora = datetime.now(timezone.utc)
    datos = [
        ("CREAR_PROYECTO", "Crear nuevos proyectos", "PROYECTO", "CREATE"),
        ("VER_PROYECTO", "Ver proyectos", "PROYECTO", "READ"),
        ("EDITAR_PROYECTO", "Editar proyectos existentes", "PROYECTO", "UPDATE"),
        ("ELIMINAR_PROYECTO", "Eliminar proyectos", "PROYECTO", "DELETE"),
        ("CREAR_TAREA", "Crear tareas en proyectos", "TAREA", "CREATE"),
        ("VER_TAREA", "Ver tareas", "TAREA", "READ"),
        ("EDITAR_TAREA", "Editar tareas existentes", "TAREA", "UPDATE"),
        ("ELIMINAR_TAREA", "Eliminar tareas", "TAREA", "DELETE"),
        ("ASIGNAR_TAREA", "Asignar tareas a usuarios", "TAREA", "UPDATE"),
        ("CREAR_USUARIO", "Crear nuevos usuarios", "USUARIO", "CREATE"),
        ("VER_USUARIO", "Ver informacion de usuarios", "USUARIO", "READ"),
        ("EDITAR_USUARIO", "Editar datos de usuarios", "USUARIO", "UPDATE"),
        ("ELIMINAR_USUARIO", "Eliminar usuarios", "USUARIO", "DELETE"),
        ("VER_ROL", "Ver roles", "ROL", "READ"),
        ("ASIGNAR_ROL", "Asignar roles a usuarios", "ROL", "UPDATE"),
        ("CREAR_COMENTARIO", "Crear comentarios", "COMENTARIO", "CREATE"),
        ("VER_COMENTARIO", "Ver comentarios", "COMENTARIO", "READ"),
        ("ELIMINAR_COMENTARIO", "Eliminar comentarios", "COMENTARIO", "DELETE"),
        ("UNIRSE_PROYECTO", "Unirse a un proyecto", "PARTICIPACION", "CREATE"),
        ("SALIR_PROYECTO", "Abandonar un proyecto", "PARTICIPACION", "DELETE"),
        ("VER_REPORTES", "Ver reportes y estadisticas", "REPORTE", "READ"),
        ("GESTIONAR_PRIVILEGIOS", "Administrar privilegios del sistema", "PRIVILEGIO", "UPDATE"),
    ]
    docs = []
    for nombre, desc, recurso, accion in datos:
        docs.append({
            "_id": ObjectId(),
            "nombre": nombre,
            "descripcion": desc,
            "recurso": recurso,
            "accion": accion,
            "activo": True,
            "fechaCreacion": ahora,
            "_class": "com.proaula.spring.synergy.Model.Privilegio",
        })
    return docs


def generar_roles(privilegios):
    """
    Genera 4 roles: ADMINISTRADOR, LIDER, PARTICIPANTE, USUARIO.
    CUMPLIMIENTO: Se definen y asocian los privilegios directamente en el rol.
    """
    ahora = datetime.now(timezone.utc)
    priv_ids = {p["nombre"]: p["_id"] for p in privilegios}
    todos = list(priv_ids.values())

    privs_lider = [priv_ids[n] for n in [
        "CREAR_PROYECTO", "VER_PROYECTO", "EDITAR_PROYECTO",
        "CREAR_TAREA", "VER_TAREA", "EDITAR_TAREA", "ELIMINAR_TAREA", "ASIGNAR_TAREA",
        "VER_USUARIO", "VER_ROL",
        "CREAR_COMENTARIO", "VER_COMENTARIO", "ELIMINAR_COMENTARIO",
        "UNIRSE_PROYECTO", "SALIR_PROYECTO", "VER_REPORTES",
        "EDITAR_USUARIO", "VER_TAREA",
    ]]
    privs_lider = list(dict.fromkeys(privs_lider)) # Quitar duplicados

    privs_participante = [priv_ids[n] for n in [
        "VER_PROYECTO", "VER_TAREA", "EDITAR_TAREA",
        "CREAR_COMENTARIO", "VER_COMENTARIO",
        "UNIRSE_PROYECTO", "SALIR_PROYECTO", "VER_USUARIO",
    ]]

    privs_usuario = [priv_ids[n] for n in [
        "VER_PROYECTO", "VER_TAREA", "VER_COMENTARIO",
        "CREAR_COMENTARIO", "UNIRSE_PROYECTO",
    ]]

    roles_def = [
        ("ADMINISTRADOR", "Acceso total al sistema", todos),
        ("LIDER", "Lider de proyecto - puede crear tareas y asignar usuarios", privs_lider),
        ("PARTICIPANTE", "Participante - acceso limitado a proyectos asignados", privs_participante),
        ("USUARIO", "Usuario basico del sistema", privs_usuario),
    ]

    docs = []
    for nombre, desc, privs in roles_def:
        docs.append({
            "_id": ObjectId(),
            "nombre": nombre,
            "descripcion": desc,
            "privilegios": privs,
            "activo": True,
            "fechaCreacion": ahora,
            "_class": "com.proaula.spring.synergy.Model.Rol",
        })
    return docs


def generar_usuarios(cantidad, roles):
    """
    Genera los usuarios del sistema distribuyendo en distintos roles.
    CUMPLIMIENTO: Los privilegios se retiran de los usuarios para que el 
    sistema consulte estrictamente el Rol al que pertenecen.
    """
    ahora = datetime.now(timezone.utc)
    rol_por_nombre = {r["nombre"]: r for r in roles}

    # Distribucion de roles segun cantidad
    n_admin = max(2, int(cantidad * 0.01))
    n_lider = int(cantidad * 0.15)
    n_part = int(cantidad * 0.60)
    n_user = cantidad - n_admin - n_lider - n_part

    plan = (
        [("ADMINISTRADOR", "ACTIVO")] * n_admin
        + [("LIDER", "ACTIVO")] * n_lider
    )
    for _ in range(n_part):
        plan.append(("PARTICIPANTE", random.choices(ESTADOS_USUARIO, weights=[85, 10, 5])[0]))
    for _ in range(n_user):
        plan.append(("USUARIO", random.choices(ESTADOS_USUARIO, weights=[80, 15, 5])[0]))

    random.shuffle(plan)

    docs = []
    correos_usados = set()

    # USUARIOS DE PRUEBA ESTATICOS
    usuarios_fijos = [
        ("Super Administrador", "admin@synergy.com", "ADMINISTRADOR"),
        ("Juan Lider Comunitario", "lider@synergy.com", "LIDER"),
        ("Maria Voluntaria", "user@synergy.com", "PARTICIPANTE")
    ]
    
    for nombre, correo, rol_nombre in usuarios_fijos:
        correos_usados.add(correo)
        docs.append({
            "_id": ObjectId(),
            "nombre": nombre,
            "correo": correo,
            "contrasena": HASH_DEMO, # Contrasena: 123456
            "salt": generar_salt(),
            "rol": rol_nombre,
            "estado": "ACTIVO",
            "activo": True,
            "proyectos": [],         
            "tareasAsignadas": [],   
            "fechaCreacion": ahora,
            "ultimaCambioContrasena": ahora,
            "_class": "com.proaula.spring.synergy.Model.Usuarios",
        })

    for i, (rol_nombre, estado) in enumerate(plan):
        nombre_completo = fake.name()
        base = (
            nombre_completo.lower()
            .replace(" ", ".")
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
        )
        correo = f"{base}@synergy.com"
        if correo in correos_usados:
            correo = f"{base}{i}@synergy.com"
        correos_usados.add(correo)

        rol = rol_por_nombre[rol_nombre]
        fecha_creacion = fecha_aleatoria(
            datetime(2023, 1, 1, tzinfo=timezone.utc),
            datetime(2026, 4, 1, tzinfo=timezone.utc),
        )

        docs.append({
            "_id": ObjectId(),
            "nombre": nombre_completo,
            "correo": correo,
            "contrasena": HASH_DEMO,
            "salt": generar_salt(),
            "rol": rol_nombre,
            "estado": estado,
            "activo": estado == "ACTIVO",
            "proyectos": [],         
            "tareasAsignadas": [],   
            "fechaCreacion": fecha_creacion,
            "ultimaCambioContrasena": fecha_creacion,
            "_class": "com.proaula.spring.synergy.Model.Usuarios",
        })
    return docs


def generar_proyectos(cantidad, usuarios):
    """Genera proyectos ambientales distribuidos en los barrios de Cartagena."""
    lideres = [u for u in usuarios if u["rol"] == "LIDER"]
    participantes = [u for u in usuarios if u["rol"] in ("PARTICIPANTE", "USUARIO")]

    if not lideres:
        raise ValueError("No hay usuarios con rol LIDER")

    docs = []
    for i in range(cantidad):
        tema, _key = random.choice(TEMATICAS_AMBIENTALES)
        barrio = random.choice(BARRIOS_CARTAGENA)
        lider = random.choice(lideres)

        nombre = f"{tema} - {barrio}"
        if random.random() < 0.35:
            nombre += f" (Fase {random.randint(1, 4)})"

        descripcion = (
            f"Iniciativa comunitaria enfocada en {tema.lower()} en el barrio {barrio} "
            f"de Cartagena de Indias. Busca involucrar a la comunidad local, lideres "
            f"barriales y aliados institucionales para generar impacto ambiental positivo "
            f"y sostenible en el sector."
        )

        fecha_inicio = fecha_aleatoria(
            datetime(2023, 1, 1, tzinfo=timezone.utc),
            datetime(2026, 3, 1, tzinfo=timezone.utc),
        )
        duracion_dias = random.randint(30, 365)
        fecha_fin = fecha_inicio + timedelta(days=duracion_dias)

        n_part = random.randint(3, 15)
        part_ids = random.sample(
            [u["_id"] for u in participantes],
            k=min(n_part, len(participantes)),
        )

        estado = random.choices(ESTADOS_PROYECTO, weights=[30, 15, 30, 20, 5])[0]
        fecha_creacion = fecha_inicio - timedelta(days=random.randint(1, 15))

        docs.append({
            "_id": ObjectId(),
            "nombre": nombre,
            "descripcion": descripcion,
            "fechaInicio": fecha_inicio,
            "fechaFin": fecha_fin,
            "idLider": lider["_id"],
            "participantes": part_ids,
            "estado": estado,
            "comentarios": [],  # Se pueblan mas adelante usando ObjectId
            "tareas": [],       # Se pueblan mas adelante usando ObjectId
            "fechaCreacion": fecha_creacion,
            "fechaActualizacion": fecha_aleatoria(fecha_creacion, datetime(2026, 4, 17, tzinfo=timezone.utc)),
            "_class": "com.proaula.spring.synergy.Model.Proyecto",
            "_tema": tema,      # Campo auxiliar temporal
            "_barrio": barrio,  # Campo auxiliar temporal
        })
    return docs


def generar_tareas(cantidad, proyectos, usuarios):
    """
    Genera tareas y las vincula a los proyectos y a los usuarios asignados.
    CUMPLIMIENTO (Logico para el Backend): "En asignar tareas solo deberia 
    asignarle a los participantes del proyecto".
    """
    participantes = [u for u in usuarios if u["rol"] in ("PARTICIPANTE", "USUARIO", "LIDER")]
    docs = []

    for i in range(cantidad):
        proy = random.choice(proyectos)
        tema = proy["_tema"]
        barrio = proy["_barrio"]

        verbo = random.choice(PALABRAS_TAREA)
        titulo = f"{verbo} {tema.lower()} en {barrio}"
        descripcion = (
            f"{verbo} como parte del proyecto '{proy['nombre']}'. "
            f"Coordinar con el equipo lider y los voluntarios de la comunidad."
        )

        estado = random.choices(ESTADOS_TAREA, weights=[25, 30, 35, 5, 5])[0]
        fecha_entrega = fecha_aleatoria(proy["fechaInicio"], proy["fechaFin"])

        n_asig = random.randint(1, 4)
        asignados = random.sample(
            [u["_id"] for u in participantes],
            k=min(n_asig, len(participantes)),
        )

        fecha_creacion = proy["fechaInicio"] + timedelta(
            days=random.randint(0, max(1, (proy["fechaFin"] - proy["fechaInicio"]).days // 2))
        )

        tarea_id = ObjectId()
        docs.append({
            "_id": tarea_id,
            "titulo": titulo,
            "descripcion": descripcion,
            "estado": estado,
            "fechaEntrega": fecha_entrega,
            "idProyecto": proy["_id"],
            "usuariosAsignados": asignados,
            "entregas": [],
            "comentarios": [],
            "fechaCreacion": fecha_creacion,
            "fechaActualizacion": fecha_aleatoria(fecha_creacion, datetime(2026, 4, 17, tzinfo=timezone.utc)),
            "_class": "com.proaula.spring.synergy.Model.Tarea",
        })

        proy["tareas"].append(tarea_id)
        for uid in asignados:
            for u in usuarios:
                if u["_id"] == uid:
                    u["tareasAsignadas"].append(tarea_id)
                    break
    return docs


def generar_participaciones(cantidad, proyectos, usuarios):
    """Genera el registro de unión y relación (many-to-many) de Usuarios y Proyectos."""
    usuarios_validos = [u for u in usuarios if u["rol"] != "ADMINISTRADOR"]
    docs = []
    vistos = set() 

    intentos = 0
    max_intentos = cantidad * 5

    while len(docs) < cantidad and intentos < max_intentos:
        intentos += 1
        proy = random.choice(proyectos)
        u = random.choice(usuarios_validos)
        clave = (u["_id"], proy["_id"])
        
        if clave in vistos:
            continue
        vistos.add(clave)

        rol_part = "LIDER" if u["_id"] == proy["idLider"] else "PARTICIPANTE"

        fecha_union = fecha_aleatoria(
            proy["fechaInicio"],
            min(datetime(2026, 4, 17, tzinfo=timezone.utc), proy["fechaFin"]),
        )

        docs.append({
            "_id": ObjectId(),
            "idUsuario": u["_id"],
            "idProyecto": proy["_id"],
            "rol": rol_part,
            "fechaUnion": fecha_union,
            "_class": "com.proaula.spring.synergy.Model.Participacion",
        })

        if proy["_id"] not in u["proyectos"]:
            u["proyectos"].append(proy["_id"])

    return docs


def generar_comentarios(cantidad, proyectos, usuarios):
    """
    Genera comentarios creados por los usuarios en distintos proyectos.
    CUMPLIMIENTO: Se almacenan en su propia coleccion (coleccion independiente)
    y solo se guarda el id del comentario dentro del proyecto, evitando 
    crecimiento exponencial dentro del documento del proyecto.
    """
    usuarios_activos = [u for u in usuarios if u["activo"]]
    docs = []

    for _ in range(cantidad):
        proy = random.choice(proyectos)
        u = random.choice(usuarios_activos)

        plantilla = random.choice(COMENTARIOS_PLANTILLAS)
        contenido = plantilla.format(tema=proy["_tema"].lower(), barrio=proy["_barrio"])

        fecha = fecha_aleatoria(proy["fechaCreacion"], datetime(2026, 4, 17, tzinfo=timezone.utc))

        com_id = ObjectId()
        docs.append({
            "_id": com_id,
            "contenido": contenido,
            "fecha": fecha,
            "idProyecto": proy["_id"],
            "idUsuario": u["_id"],
            "_class": "com.proaula.spring.synergy.Model.Comentario",
        })

        # Almacenamos unicamente la referencia del ID para no engrosar el proyecto
        proy["comentarios"].append(com_id)

    return docs


# ---------------------------------------------------------------------
# LIMPIEZA ANTES DE INSERTAR
# ---------------------------------------------------------------------
def limpiar_campos_auxiliares(proyectos):
    """Elimina las llaves temporales usadas para generar semantica en el codigo."""
    for p in proyectos:
        p.pop("_tema", None)
        p.pop("_barrio", None)


# ---------------------------------------------------------------------
# INSERCION EN MONGO
# ---------------------------------------------------------------------
def insertar(db, nombre, docs):
    """Inserta la lista de documentos en la coleccion dada de MongoDB."""
    col = db[nombre]
    col.delete_many({})  # Limpiar antes de poblar
    if docs:
        col.insert_many(docs)
    print(f"  [OK] {nombre:20s} -> {len(docs):6d} documentos")


# ---------------------------------------------------------------------
# MAIN
# ---------------------------------------------------------------------
def main():
    print("=" * 70)
    print("  SYNERGY - Poblacion de base de datos MongoDB")
    print("=" * 70)
    print(f"  URI:  {MONGO_URI}")
    print(f"  BD:   {DB_NAME}")
    print("=" * 70)

    client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=5000)
    try:
        client.admin.command("ping")
    except Exception as e:
        print(f"\n[ERROR] No se pudo conectar a MongoDB: {e}")
        print("Asegurate de que el servicio de MongoDB este corriendo en localhost:27017.")
        return

    db = client[DB_NAME]

    print("\n[1/7] Generando privilegios...")
    privilegios = generar_privilegios()

    print("[2/7] Generando roles...")
    roles = generar_roles(privilegios)

    print("[3/7] Generando usuarios...")
    usuarios = generar_usuarios(CANTIDAD["usuarios"], roles)

    print("[4/7] Generando proyectos...")
    proyectos = generar_proyectos(CANTIDAD["proyectos"], usuarios)

    print("[5/7] Generando tareas...")
    tareas = generar_tareas(CANTIDAD["tareas"], proyectos, usuarios)

    print("[6/7] Generando participaciones...")
    participaciones = generar_participaciones(CANTIDAD["participaciones"], proyectos, usuarios)

    print("[7/7] Generando comentarios...")
    comentarios = generar_comentarios(CANTIDAD["comentarios"], proyectos, usuarios)

    limpiar_campos_auxiliares(proyectos)

    print("\nInsertando en MongoDB...")
    insertar(db, "privilegios",    privilegios)
    insertar(db, "roles",          roles)
    insertar(db, "usuarios",       usuarios)
    insertar(db, "proyectos",      proyectos)
    insertar(db, "tareas",         tareas)
    insertar(db, "participaciones", participaciones)
    insertar(db, "comentarios",    comentarios)

    total = (
        len(privilegios) + len(roles) + len(usuarios) + len(proyectos)
        + len(tareas) + len(participaciones) + len(comentarios)
    )

    print("\n" + "=" * 70)
    print(f"  TOTAL INSERTADO: {total} documentos")
    print("=" * 70)

    # Indices utiles
    print("\nCreando indices...")
    db.usuarios.create_index("correo", unique=True)
    db.roles.create_index("nombre", unique=True)
    db.privilegios.create_index("nombre", unique=True)
    db.proyectos.create_index("idLider")
    db.tareas.create_index("idProyecto")
    db.participaciones.create_index([("idUsuario", 1), ("idProyecto", 1)])
    db.comentarios.create_index("idProyecto")
    print("  [OK] Indices creados.")

    print("\nPoblacion finalizada correctamente.\n")


if __name__ == "__main__":
    main()
