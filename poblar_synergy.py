"""
=======================================================================
 SYNERGY - Script de poblacion de base de datos MongoDB (MEJORADO)
=======================================================================
 Proyecto: Synergy - Gestion de proyectos comunitarios ambientales
           en Cartagena de Indias
 Base de datos: synergy
 Motor: MongoDB (localhost:27017)
-----------------------------------------------------------------------
 MEJORAS APLICADAS RESPECTO A LA VERSION ANTERIOR:
 -----------------------------------------------------------------
 1. Eliminado el campo redundante 'rol' en participaciones
    (los lideres se identifican solo con proyectos.idLider).
 2. Eliminados arrays embebidos duplicados:
       - proyectos.participantes[]   (ya esta en participaciones)
       - proyectos.tareas[]          (ya esta en tareas.idProyecto)
       - proyectos.comentarios[]     (ya esta en comentarios.idProyecto)
       - usuarios.proyectos[]        (ya esta en participaciones)
       - usuarios.tareasAsignadas[]  (ya esta en tareas.usuariosAsignados)
 3. Eliminado usuarios.privilegios[] (se consulta dinamicamente via rol).
 4. Reemplazado usuarios.rol (string) por usuarios.idRol (ObjectId)
    para referenciar correctamente la coleccion 'roles'.
 5. Eliminado el campo 'activo' en usuarios (redundante con 'estado').
 6. Eliminado el campo 'salt' (bcrypt ya lo incluye en el hash).
 7. Agregada fechaActualizacion en usuarios.
 8. Renombrado 'ultimaCambioContrasena' -> 'ultimoCambioContrasena'.
 9. Corregido _class: 'Usuarios' (plural) -> 'Usuario' (singular).
 10. Indices adicionales para garantizar integridad e integridad.
-----------------------------------------------------------------------
 Requisitos:
   pip install pymongo faker bcrypt
 Ejecucion:
   python poblar_synergy_mejorado.py
=======================================================================
"""

import random
from datetime import datetime, timedelta, timezone
from bson import ObjectId
from pymongo import MongoClient
from faker import Faker

# ---------------------------------------------------------------------
# CONFIGURACION
# ---------------------------------------------------------------------
MONGO_URI = "mongodb://localhost:27017"
DB_NAME = "synergy"

CANTIDAD = {
    "privilegios":    22,
    "roles":           4,
    "usuarios":     2500,   # +1000 (antes 1500)
    "proyectos":    3000,   # +1000 (antes 2000)
    "tareas":       3500,
    "participaciones": 5000,  # +500 (antes 4500)
    "comentarios":   974,
}
# Total: 22 + 4 + 2500 + 3000 + 3500 + 5000 + 974 = 15000

fake = Faker("es_CO")
Faker.seed(42)
random.seed(42)

# ---------------------------------------------------------------------
# DATOS DE CONTEXTO - CARTAGENA DE INDIAS
# ---------------------------------------------------------------------
BARRIOS_CARTAGENA = [
    "Getsemani", "Manga", "Bocagrande", "Castillogrande", "El Laguito",
    "Centro Historico", "San Diego", "La Matuna", "Crespo", "Marbella",
    "Pie de la Popa", "Torices", "Canapote", "El Cabrero", "Boston",
    "El Pozon", "Olaya Herrera", "La Boquilla", "Zaragocilla", "Blas de Lezo",
    "El Country", "Los Alpes", "Nuevo Bosque", "San Fernando", "Ternera",
    "Turbaco (periferia)", "La Maria", "San Jose de los Campanos", "Chambacu",
    "Las Gaviotas", "Villa Estrella", "El Socorro", "La Consolata",
    "Ciudadela 2000", "Nelson Mandela", "El Educador", "La Esperanza",
]

TEMATICAS_AMBIENTALES = [
    ("Limpieza de playas", "playa"),
    ("Reforestacion urbana", "arboles"),
    ("Recuperacion de la Cienaga de la Virgen", "cienaga"),
    ("Gestion de residuos solidos", "reciclaje"),
    ("Huertas comunitarias", "huerta"),
    ("Educacion ambiental en colegios", "educacion"),
    ("Conservacion de manglares", "manglares"),
    ("Reciclaje de plasticos", "plastico"),
    ("Compostaje comunitario", "compostaje"),
    ("Siembra de arboles nativos", "siembra"),
    ("Proteccion de tortugas marinas", "fauna"),
    ("Ahorro de agua", "agua"),
    ("Energia solar comunitaria", "energia"),
    ("Campaña anti-basura en canales", "canales"),
    ("Recuperacion de espacios verdes", "parques"),
    ("Gestion de aguas residuales", "aguas"),
    ("Monitoreo de calidad del aire", "aire"),
    ("Reduccion del plastico de un solo uso", "plastico"),
    ("Brigadas de aseo barrial", "aseo"),
    ("Proteccion de aves urbanas", "aves"),
]

PALABRAS_TAREA = [
    "Organizar jornada de", "Coordinar brigada de", "Realizar diagnostico de",
    "Capacitar voluntarios en", "Recolectar materiales para", "Gestionar permisos de",
    "Convocar reunion de", "Ejecutar actividad de", "Evaluar resultados de",
    "Documentar avances de", "Difundir campaña de", "Contactar aliados para",
    "Preparar logistica de", "Distribuir insumos de", "Monitorear indicadores de",
]

ESTADOS_PROYECTO = ["ACTIVO", "PLANIFICACION", "EN_EJECUCION", "FINALIZADO", "SUSPENDIDO"]
ESTADOS_TAREA = ["PENDIENTE", "EN_PROGRESO", "COMPLETADA", "CANCELADA", "REVISION"]
ESTADOS_USUARIO = ["ACTIVO", "INACTIVO", "SUSPENDIDO"]

COMENTARIOS_PLANTILLAS = [
    "Excelente avance en {tema}, sigamos asi.",
    "Propongo ampliar la cobertura a otros sectores de {barrio}.",
    "Necesitamos mas voluntarios para la jornada del proximo fin de semana.",
    "Los resultados de la ultima actividad superaron las expectativas.",
    "Sugiero coordinar con la alcaldia para el retiro de residuos.",
    "Recordatorio: la reunion de seguimiento es este jueves a las 3pm.",
    "Gracias al equipo por el compromiso mostrado en {barrio}.",
    "Se requiere revisar el presupuesto asignado para {tema}.",
    "La comunidad de {barrio} esta muy motivada con el proyecto.",
    "Adjuntare el informe fotografico en la proxima entrega.",
    "Hay que reforzar la convocatoria en redes sociales.",
    "Recomiendo incluir a los colegios cercanos como aliados.",
    "El impacto ambiental es notable, felicitaciones al lider.",
    "Pendiente: gestionar donaciones de bolsas y guantes.",
    "La alianza con la junta de accion comunal esta funcionando bien.",
    "Solicito reprogramar la actividad por condiciones climaticas.",
    "Ya contactamos a la EPA Cartagena para validar el cronograma.",
    "Los jovenes del barrio estan liderando muy bien las brigadas.",
    "Necesitamos mas material educativo sobre {tema}.",
    "El trabajo articulado con la Universidad de Cartagena fue clave.",
]


# ---------------------------------------------------------------------
# UTILIDADES
# ---------------------------------------------------------------------
def fecha_aleatoria(inicio, fin):
    """Fecha aleatoria entre dos datetimes (timezone-aware, UTC).
    Si fin <= inicio, devuelve inicio para evitar rangos invalidos."""
    if inicio.tzinfo is None:
        inicio = inicio.replace(tzinfo=timezone.utc)
    if fin.tzinfo is None:
        fin = fin.replace(tzinfo=timezone.utc)
    if fin <= inicio:
        return inicio
    delta = fin - inicio
    segundos = random.randint(0, int(delta.total_seconds()))
    return inicio + timedelta(seconds=segundos)


# Hash bcrypt de ejemplo. En produccion cada usuario tendria el suyo propio.
# bcrypt YA incluye el salt en el hash, por eso no se guarda en columna aparte.
HASH_DEMO = "$2a$10$OnJOoz/lXswM3tX2RwQYw.gR1aPPl5s6nBr/CnOtiqAzNamkB9lY."


# ---------------------------------------------------------------------
# GENERADORES POR COLECCION
# ---------------------------------------------------------------------
def generar_privilegios():
    """22 privilegios fijos del sistema."""
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
    """4 roles: ADMINISTRADOR, LIDER, PARTICIPANTE, USUARIO."""
    ahora = datetime.now(timezone.utc)
    priv_ids = {p["nombre"]: p["_id"] for p in privilegios}
    todos = list(priv_ids.values())

    privs_lider = list(dict.fromkeys([priv_ids[n] for n in [
        "CREAR_PROYECTO", "VER_PROYECTO", "EDITAR_PROYECTO",
        "CREAR_TAREA", "VER_TAREA", "EDITAR_TAREA", "ELIMINAR_TAREA", "ASIGNAR_TAREA",
        "VER_USUARIO", "VER_ROL",
        "CREAR_COMENTARIO", "VER_COMENTARIO", "ELIMINAR_COMENTARIO",
        "UNIRSE_PROYECTO", "SALIR_PROYECTO", "VER_REPORTES",
        "EDITAR_USUARIO",
    ]]))

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
    """Distribucion: 1% admin, 15% lider, 60% participante, 24% usuario.

    MEJORAS:
    - Se usa idRol (ObjectId) en lugar de rol (string) para referenciar la coleccion roles.
    - Se elimina el campo 'salt' (bcrypt ya lo incluye en el hash).
    - Se elimina el campo 'activo' (redundante con 'estado').
    - Se elimina 'privilegios' (se consultan dinamicamente via idRol -> roles.privilegios).
    - Se elimina 'proyectos[]' (ya esta en la coleccion participaciones).
    - Se elimina 'tareasAsignadas[]' (ya esta en tareas.usuariosAsignados).
    - Se agrega 'fechaActualizacion'.
    - Se corrige el nombre 'ultimaCambioContrasena' -> 'ultimoCambioContrasena'.
    - Se corrige _class: 'Usuarios' -> 'Usuario' (singular).
    """
    rol_por_nombre = {r["nombre"]: r for r in roles}

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
        fecha_actualizacion = fecha_aleatoria(
            fecha_creacion, datetime(2026, 4, 17, tzinfo=timezone.utc)
        )

        docs.append({
            "_id": ObjectId(),
            "nombre": nombre_completo,
            "correo": correo,
            "contrasena": HASH_DEMO,
            "idRol": rol["_id"],                    # referencia limpia a roles
            "estado": estado,                       # ACTIVO | INACTIVO | SUSPENDIDO
            "fechaCreacion": fecha_creacion,
            "fechaActualizacion": fecha_actualizacion,
            "ultimoCambioContrasena": fecha_creacion,
            # Campo auxiliar interno (no persistido) para los generadores que vienen.
            # Se elimina antes de insertar.
            "_rolNombre": rol_nombre,
            "_class": "com.proaula.spring.synergy.Model.Usuario",
        })
    return docs


def generar_proyectos(cantidad, usuarios):
    """Proyectos ambientales distribuidos en los barrios de Cartagena.

    MEJORAS:
    - Se elimina 'participantes[]' (ya esta en participaciones).
    - Se elimina 'tareas[]' (ya esta en tareas.idProyecto).
    - Se elimina 'comentarios[]' (ya esta en comentarios.idProyecto).
    """
    lideres = [u for u in usuarios if u["_rolNombre"] == "LIDER"]

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

        estado = random.choices(
            ESTADOS_PROYECTO,
            weights=[30, 15, 30, 20, 5],
        )[0]

        fecha_creacion = fecha_inicio - timedelta(days=random.randint(1, 15))

        docs.append({
            "_id": ObjectId(),
            "nombre": nombre,
            "descripcion": descripcion,
            "fechaInicio": fecha_inicio,
            "fechaFin": fecha_fin,
            "idLider": lider["_id"],
            "estado": estado,
            "fechaCreacion": fecha_creacion,
            "fechaActualizacion": fecha_aleatoria(
                fecha_creacion, datetime(2026, 4, 17, tzinfo=timezone.utc)
            ),
            "_class": "com.proaula.spring.synergy.Model.Proyecto",
            # Campos auxiliares (no persistidos)
            "_tema": tema,
            "_barrio": barrio,
        })
    return docs


def generar_tareas(cantidad, proyectos, usuarios):
    """Tareas distribuidas entre los proyectos.

    MEJORAS:
    - Ya no se sincroniza un array 'tareas[]' en proyectos.
    - Ya no se sincroniza 'tareasAsignadas[]' en usuarios.
    - La relacion vive en tareas.idProyecto y tareas.usuariosAsignados.
    """
    asignables = [u for u in usuarios if u["_rolNombre"] in ("PARTICIPANTE", "USUARIO", "LIDER")]
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

        estado = random.choices(
            ESTADOS_TAREA,
            weights=[25, 30, 35, 5, 5],
        )[0]

        fecha_entrega = fecha_aleatoria(
            proy["fechaInicio"], proy["fechaFin"],
        )

        n_asig = random.randint(1, 4)
        asignados = random.sample(
            [u["_id"] for u in asignables],
            k=min(n_asig, len(asignables)),
        )

        fecha_creacion = proy["fechaInicio"] + timedelta(
            days=random.randint(0, max(1, (proy["fechaFin"] - proy["fechaInicio"]).days // 2))
        )

        docs.append({
            "_id": ObjectId(),
            "titulo": titulo,
            "descripcion": descripcion,
            "estado": estado,
            "fechaEntrega": fecha_entrega,
            "idProyecto": proy["_id"],
            "usuariosAsignados": asignados,
            "fechaCreacion": fecha_creacion,
            "fechaActualizacion": fecha_aleatoria(
                fecha_creacion, datetime(2026, 4, 17, tzinfo=timezone.utc)
            ),
            "_class": "com.proaula.spring.synergy.Model.Tarea",
        })
    return docs


def generar_participaciones(cantidad, proyectos, usuarios):
    """Participaciones (join) entre usuarios y proyectos.

    MEJORAS:
    - Se elimina el campo 'rol' (los lideres viven en proyectos.idLider).
    - Esta coleccion solo registra PARTICIPANTES reales.
    - No se sincroniza ningun array embebido en usuarios o proyectos.
    """
    # Solo usuarios que pueden ser participantes (no ADMIN).
    # Los LIDER tampoco se duplican aqui: su relacion con el proyecto que
    # lideran esta en proyectos.idLider. Si lidera un proyecto y ademas
    # participa en otro distinto, si puede aparecer aqui para ese otro.
    candidatos = [u for u in usuarios if u["_rolNombre"] != "ADMINISTRADOR"]
    docs = []
    vistos = set()

    intentos = 0
    max_intentos = cantidad * 5

    while len(docs) < cantidad and intentos < max_intentos:
        intentos += 1
        proy = random.choice(proyectos)
        u = random.choice(candidatos)

        # No registrar al lider como participante de su propio proyecto.
        if u["_id"] == proy["idLider"]:
            continue

        clave = (u["_id"], proy["_id"])
        if clave in vistos:
            continue
        vistos.add(clave)

        fecha_union = fecha_aleatoria(
            proy["fechaInicio"],
            min(datetime(2026, 4, 17, tzinfo=timezone.utc), proy["fechaFin"]),
        )

        docs.append({
            "_id": ObjectId(),
            "idUsuario": u["_id"],
            "idProyecto": proy["_id"],
            "fechaUnion": fecha_union,
            "_class": "com.proaula.spring.synergy.Model.Participacion",
        })

    return docs


def generar_comentarios(cantidad, proyectos, usuarios):
    """Comentarios en proyectos por parte de usuarios.

    MEJORAS:
    - Ya no se sincroniza un array 'comentarios[]' en proyectos.
    """
    usuarios_activos = [u for u in usuarios if u["estado"] == "ACTIVO"]
    docs = []

    for _ in range(cantidad):
        proy = random.choice(proyectos)
        u = random.choice(usuarios_activos)

        plantilla = random.choice(COMENTARIOS_PLANTILLAS)
        contenido = plantilla.format(tema=proy["_tema"].lower(), barrio=proy["_barrio"])

        fecha = fecha_aleatoria(
            proy["fechaCreacion"],
            datetime(2026, 4, 17, tzinfo=timezone.utc),
        )

        docs.append({
            "_id": ObjectId(),
            "contenido": contenido,
            "fecha": fecha,
            "idProyecto": proy["_id"],
            "idUsuario": u["_id"],
            "_class": "com.proaula.spring.synergy.Model.Comentario",
        })

    return docs


# ---------------------------------------------------------------------
# LIMPIEZA ANTES DE INSERTAR
# ---------------------------------------------------------------------
def limpiar_campos_auxiliares(proyectos, usuarios):
    """Elimina campos internos usados solo durante la generacion."""
    for p in proyectos:
        p.pop("_tema", None)
        p.pop("_barrio", None)
    for u in usuarios:
        u.pop("_rolNombre", None)


# ---------------------------------------------------------------------
# INSERCION EN MONGO
# ---------------------------------------------------------------------
def insertar(db, nombre, docs):
    col = db[nombre]
    col.delete_many({})
    if docs:
        col.insert_many(docs)
    print(f"  [OK] {nombre:20s} -> {len(docs):6d} documentos")


# ---------------------------------------------------------------------
# INDICES
# ---------------------------------------------------------------------
def crear_indices(db):
    """Indices que garantizan integridad y aceleran consultas comunes."""
    # Unicidad
    db.usuarios.create_index("correo", unique=True)
    db.roles.create_index("nombre", unique=True)
    db.privilegios.create_index("nombre", unique=True)

    # Evita que un mismo usuario se inscriba dos veces al mismo proyecto.
    db.participaciones.create_index(
        [("idUsuario", 1), ("idProyecto", 1)], unique=True
    )

    # Consultas frecuentes
    db.usuarios.create_index("idRol")
    db.proyectos.create_index("idLider")
    db.proyectos.create_index("estado")
    db.tareas.create_index("idProyecto")
    db.tareas.create_index("usuariosAsignados")
    db.tareas.create_index("estado")
    db.participaciones.create_index("idUsuario")
    db.participaciones.create_index("idProyecto")
    db.comentarios.create_index("idProyecto")
    db.comentarios.create_index("idUsuario")
    print("  [OK] Indices creados.")


# ---------------------------------------------------------------------
# MAIN
# ---------------------------------------------------------------------
def main():
    print("=" * 70)
    print("  SYNERGY - Poblacion de base de datos MongoDB (MEJORADO)")
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

    limpiar_campos_auxiliares(proyectos, usuarios)

    print("\nInsertando en MongoDB...")
    insertar(db, "privilegios",     privilegios)
    insertar(db, "roles",           roles)
    insertar(db, "usuarios",        usuarios)
    insertar(db, "proyectos",       proyectos)
    insertar(db, "tareas",          tareas)
    insertar(db, "participaciones", participaciones)
    insertar(db, "comentarios",     comentarios)

    total = (
        len(privilegios) + len(roles) + len(usuarios) + len(proyectos)
        + len(tareas) + len(participaciones) + len(comentarios)
    )

    print("\n" + "=" * 70)
    print(f"  TOTAL INSERTADO: {total} documentos")
    print("=" * 70)

    print("\nCreando indices...")
    crear_indices(db)

    print("\nPoblacion finalizada correctamente.\n")


if __name__ == "__main__":
    main()
