# Synergy — Plataforma de voluntariado ambiental

Synergy conecta a **administradores**, **líderes de proyecto** y **voluntarios** para organizar proyectos ambientales (limpieza de playas, siembra de manglar, huertas…), asignar tareas, recibir entregas con evidencias y comentar el avance.

- **Web:** Spring Boot 3.5 + Thymeleaf + Spring Security (sesión web y Google).
- **API móvil:** REST con JWT en `/api/**`, usada por la app Flutter de la carpeta `mobile/`.
- **Base de datos:** MongoDB (base `synergy`).

Documentación: este README, `docs/MANUAL_USUARIO.md` (por rol), `docs/MODELO_PREDICTIVO.md` (documento del modelo), `docs/GUION_DEMO.md` (sustentación) y la API en `/swagger-ui.html`.

---

## 1. Requisitos

| Herramienta | Versión |
|---|---|
| Java (JDK) | 21 o superior |
| MongoDB | 6 o superior, corriendo en `localhost:27017` |
| Maven | incluido (`mvnw` / `mvnw.cmd`) |
| Flutter | 3.x (solo para la app móvil) |

## 2. Cómo arrancar

```bash
# Windows: mvnw.cmd clean spring-boot:run
mvnw clean spring-boot:run
```

Abrir <http://localhost:8080>. No hace falta ningún script: en el **primer arranque** la aplicación prepara sola la base de datos (tarda unos segundos):

1. Si encuentra datos de la **versión anterior** (los de `poblar_synergy.py`: usuarios con `idRol`, ids como ObjectId, estados `EN_EJECUCION`/`FINALIZADO`…), que no funcionan con el código actual, los **respalda** en la carpeta `respaldos/AAAAMMDD_HHmmss/` (un `.json` por colección) y los reemplaza.
2. Crea o repara los índices, los roles y los privilegios.
3. Crea las 3 cuentas de prueba.
4. Si hay menos de 200 proyectos, genera **≈19.000 documentos** de prueba ambientados en Cartagena.

## 3. Datos y cuentas de prueba

| Rol | Correo | Contraseña | Qué tiene |
|---|---|---|---|
| Administrador | `admin@synergy.com` | `Admin123*` | Todo el panel: ~240 propuestas por aprobar |
| Líder | `lider@synergy.com` | `Lider123*` | 12 proyectos propios (activos, pendientes, completados), equipos, tareas y entregas por revisar |
| Voluntario | `voluntario@synergy.com` | `Voluntario123*` | Inscrito en ~13 proyectos, con tareas y entregas |
| Cualquier usuario generado | ver *Usuarios* en el panel de admin | `Synergy123*` | Voluntarios y líderes de ejemplo |

Volumen generado (`Config/GeneradorDatos.java`, semilla fija → siempre los mismos datos):

| Colección | Documentos | Relación que se mantiene coherente |
|---|---|---|
| usuarios | 2.500 | `proyectos[]` y `tareasAsignadas[]` coinciden con las demás colecciones |
| proyectos | 3.000 | código único `SYN-AAAA-NNNNN`, categoría, barrio, organización, audiencia |
| participaciones | ~5.600 | = `proyectos.participantes[]` |
| tareas | 4.000 | solo se asignan a integrantes del proyecto |
| entregas_tareas | ~2.300 | archivo de evidencia `.txt`; estados ENTREGADO / REVISADO / RECHAZADO |
| comentarios | 1.500 | escritos por integrantes del proyecto |

Opciones en `application.properties` (sección *DATOS DE PRUEBA*):

- `synergy.datos.usuarios / proyectos / tareas / comentarios` → cantidades.
- `synergy.datos.regenerar-todo=true` → en el siguiente arranque respalda, borra y regenera todo. **Vuelve a ponerlo en `false`.** (Alternativa con mongosh: `mongosh "mongodb://localhost:27017" setup_synergy_db.js`.)
- `synergy.demo.reset-passwords=true` → restablece las contraseñas de las 3 cuentas en cada arranque. **En producción: `false`**, igual que `synergy.datos.generar=false`.

## 4. Inicio de sesión con Google

1. En [Google Cloud Console](https://console.cloud.google.com/apis/credentials) → *Credenciales* → tu cliente OAuth 2.0 (tipo **Aplicación web**), agrega:
   - **Orígenes de JavaScript autorizados:** `http://localhost:8080`
   - **URI de redireccionamiento autorizados:** `http://localhost:8080/login/oauth2/code/google`
2. Las claves van en `src/main/resources/application-secrets.properties` (ya está creado y **no se sube a GitHub** porque está en `.gitignore`):

   ```properties
   spring.security.oauth2.client.registration.google.client-id=TU_CLIENT_ID
   spring.security.oauth2.client.registration.google.client-secret=TU_CLIENT_SECRET
   ```

   También se pueden pasar como variables de entorno `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET`.
3. Funcionamiento:
   - Botón **Continuar con Google** en el login y en los registros.
   - Si el correo ya existe, entra con su rol actual. Si la cuenta está inactiva, se rechaza.
   - Si es nuevo, se crea como **voluntario**; desde *Registro de líder* se crea como **líder** (`/login/google?rol=LIDER`).
   - La foto de Google se usa como avatar en el menú lateral.

> Si el secreto del cliente se compartió por chat o correo, conviene regenerarlo en Google Cloud Console y actualizar el archivo.

## 5. Roles y funciones

| Rol | Puede |
|---|---|
| **Administrador** | Buscar y filtrar proyectos y usuarios (paginado), aprobar o rechazar propuestas, eliminar proyectos (con sus tareas, entregas y comentarios), cambiar roles, activar/desactivar cuentas, ver historial y el árbol de proyectos (categoría → barrio → proyecto). Ve gráficas por estado, categoría y riesgo, el **semáforo de riesgo** de cada proyecto activo, la alerta de proyectos en riesgo alto y descarga el dataset del modelo. |
| **Líder** | Crear/editar/completar/eliminar sus proyectos, asignar tareas a los inscritos, ver avance con gráficas, tareas vencidas y por vencer, el riesgo de cada proyecto con motivos y sugerencia, revisar y descargar entregas, comentar. Lista de voluntarios con buscador, filtro por proyecto y páginas de 20. |
| **Voluntario** | Explorar proyectos con buscador y filtros (estado, categoría, barrio, organización), inscribirse o salir, ver sus tareas, entregar archivos (máx. 10 MB), comentar, proponer proyectos (quedan *pendientes*; al aprobarse pasa a ser líder). |
| **Invitado** | Ver los proyectos activos y completados con los mismos filtros; al pulsar *Participar* se le pide crear cuenta. *Volver* (en elegir rol y en el registro) regresa a la lista que estaba viendo, y al registrarse vuelve allí con su cuenta para inscribirse. |

Búsqueda: no distingue mayúsculas ni tildes (`reforestacion` encuentra *Reforestación*) y busca en nombre, descripción, código, barrio y categoría. Las listas se paginan en el servidor (12 proyectos o 20 usuarios por página), así funcionan igual con 30 que con 30.000 registros.

Flujo: el voluntario/líder propone un proyecto → queda `PENDIENTE` → el administrador lo aprueba (`ACTIVO`) → los voluntarios se inscriben → el líder asigna tareas → el voluntario entrega → el líder revisa.

## 6. Configuración (`application.properties`)

| Propiedad / variable | Por defecto | Para qué |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/synergy` | Conexión a MongoDB (Atlas en producción). |
| `JWT_SECRET` | valor de `application-secrets.properties` | Firma de los tokens de la app móvil. |
| `SESSION_COOKIE_SECURE` | `false` | Ponlo en `true` cuando la web use HTTPS. |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | secrets | Login con Google. |
| `spring.servlet.multipart.max-file-size` | `10MB` | Tamaño máximo de las entregas. |

Sesión web: 13 minutos de inactividad, máximo 2 sesiones por usuario.

## 7. Estructura

```
Synergy/
├── pom.xml
├── setup_synergy_db.js            Borra las colecciones (los datos se regeneran solos)
├── docs/diagramas/                Diagramas del modelo 4+1 (PNG y PDF)
├── docs/modelo-predictivo/        Notebook de Colab, dataset (CSV/ARFF) y prueba del modelo
├── mobile/                        App Flutter (cliente de /api/**)
└── src/main/
    ├── java/com/proaula/spring/synergy/
    │   ├── Config/                SecurityConfig, DataInitializer, SesionUsuarioAdvice, errores globales
    │   ├── Controller/            Web (Thymeleaf) y REST para la app (Api*, AuthController, JWT)
    │   ├── Dto/                   Objetos de vista (EntregaVista, ComentarioVista…)
    │   ├── Model/                 Documentos de MongoDB
    │   ├── Service/ModeloRiesgo.java      Modelo exportado desde Colab (m2cgen)
    │   ├── Repository/            Repositorios Spring Data
    │   ├── Security/              SesionUtil, GoogleLoginSuccessHandler, JWT
    │   └── Service/               Lógica de negocio
    └── resources/
        ├── application.properties
        ├── application-secrets.properties   (privado, no se sube)
        ├── static/css/            core.css, auth.css, landing.css, dashboard.css
        ├── static/js/script.js    Animaciones, tema claro/oscuro, menú, confirmaciones
        ├── static/img/            logo.png, logo-mark.png, icons/*.png
        └── templates/             Vistas + fragments/sidebar.html (menú lateral por rol)
```

### Diseño

- Paleta: fondo `#0B0E14`, verde esmeralda `#10B981`; **tema claro** con el botón de luna/sol (se recuerda en el navegador).
- Menú lateral según el rol, barra superior en móvil, gráficas de avance en el panel del líder, ventanas de confirmación propias (`data-confirm="¿Seguro?"` en cualquier enlace, botón o formulario).
- Iconos propios en `static/img/icons/` (minúsculas): `inicio, proyectos, comunidad, perfil, notificaciones, ajustes, buscar, agregar, reforestacion, logro, tareas, meta-semanal, progreso, reporte-fotografico`.
- Todas las rutas de imágenes usan `/img/...` en **minúsculas** (la carpeta debe llamarse `img`, no `IMG`; en Linux/servidores importa).

## 8. Modelo predictivo de riesgo

Clasifica cada proyecto **activo** en riesgo **Bajo, Medio o Alto** de no cumplir sus objetivos (propuesta del proyecto de aula).

| Paso | Dónde |
|---|---|
| 1. Datos | Al primer arranque se agregan **600 proyectos terminados** con historial completo (tareas con vencimiento y fecha de cumplimiento, entregas y comentarios). `synergy.datos.historial-riesgo=600` (se agregan una sola vez, sin tocar lo existente). |
| 2. Dataset | Panel del administrador → *Dataset CSV* / *ARFF (Weka)*. Cada fila es un proyecto terminado: variables medidas **a la mitad del proyecto** y `nivel_riesgo` según **cómo terminó** (así el modelo anticipa y no copia la regla). |
| 3. Colab | `docs/modelo-predictivo/Synergy_modelo_predictivo.ipynb`: pandas, numpy y matplotlib para explorar y preparar; compara árbol de decisión, random forest y regresión logística (exactitud, matriz de confusión, precisión y recall por clase). |
| 4. Exportar | El notebook usa **m2cgen** para convertir el modelo elegido en `Service/ModeloRiesgo.java` (sin Python dentro de Synergy). `prueba_modelo.csv` comprueba que Java y Python dan el mismo resultado. |
| 5. Uso | `RiesgoProyectoService` calcula las variables con `RiesgoCalculadora` (las mismas fórmulas del dataset), predice con `ModeloRiesgo` y guarda nivel, probabilidad, motivos y sugerencia en cada proyecto. Se recalcula al arrancar, cada noche (`synergy.riesgo.cron`), al cambiar tareas y con el botón *Recalcular*. App: `GET /api/proyectos/{id}/riesgo` (líder o administrador). |

Variables: días de retraso, % de tareas pendientes, participantes, tasa de participación (voluntarios con al menos una entrega o comentario), duración, avance real, avance esperado, brecha de avance, número de actividades y categoría.

Para volver a entrenar con datos nuevos: descargar el CSV, abrir el notebook en Google Colab, ejecutar todo y reemplazar `ModeloRiesgo.java` por el que descarga el notebook.

### Funciones del modelo en la plataforma

- **Explicación:** cada proyecto muestra *lo que más influye* (se "arregla" una variable a la vez y se mide cuánto baja el riesgo con el mismo modelo).
- **Historial:** se guarda el nivel cada día (colección `historial_riesgo`) y se dibuja una mini gráfica: "Subió de Medio a Alto hace 3 días".
- **Avisos:** cuando un proyecto pasa a riesgo Alto se notifica al líder y a los administradores (campana de notificaciones).
- **Simulador "¿qué pasa si…?":** en el detalle del proyecto el líder prueba completar tareas o sumar voluntarios y ve cómo cambia el riesgo.
- **Fecha estimada de fin** al ritmo actual de tareas completadas.
- **Ficha del modelo** (*Administración → Modelo de riesgo*): versión, métricas, matriz de confusión y comparación con random forest y regresión logística. Al reentrenar, actualizar también `Service/ModeloInfo.java`.

## 9. App móvil (Flutter)

```bash
cd mobile
flutter pub get
flutter run
```

**Dirección del servidor:** en la pantalla de inicio de sesión, botón *Servidor* (abajo).
- Emulador de Android: `10.0.2.2:8080` (valor por defecto).
- Teléfono real: la IP de tu PC (en Windows: `ipconfig` → *Dirección IPv4*, p. ej. `192.168.1.10:8080`), con el teléfono **en la misma red Wi-Fi**. Si no conecta, permite Java en el Firewall de Windows (o abre el puerto 8080 para redes privadas).

Novedades: riesgo de cada proyecto en la lista (líder y administrador, los de riesgo alto primero), campana de notificaciones, deslizar hacia abajo para recargar, copia sin conexión de la lista de proyectos, sesión que se renueva sola (*refresh token*) y las mismas reglas de contraseña que la web. El identificador de la app es `co.synergy.app`.

Si no hay conexión, la app ahora muestra el motivo (antes no mostraba nada y parecía que el botón no funcionaba). Firebase es opcional: sin `android/app/google-services.json` la app funciona igual, solo sin notificaciones push. Las cuentas de prueba sirven también en la app.

Endpoints principales: `POST /api/auth/login`, `POST /api/auth/registrar`, `/api/proyectos/**`, `/api/tareas/**`, `/api/entregas/**`, `/api/comentarios/**`, `/api/admin/**` (solo administrador). Sin token válido responden `401` en JSON.

## 10. Docker, pruebas, API y respaldos

**Levantar con Docker** (necesita Docker Desktop abierto). Desde la carpeta del proyecto:

```bash
docker compose up --build        # 1ª vez: compila y arranca app + MongoDB (tarda varios minutos)
docker compose up -d             # siguientes veces, en segundo plano
docker compose logs -f app       # ver los logs de la app
docker compose down              # apagar (los datos se conservan)
docker compose down -v           # apagar y borrar la base del contenedor
```

- App: <http://localhost:8080> (`admin@synergy.com` / `Admin123*` y las demás cuentas de prueba). La app móvil en el emulador sigue usando `http://10.0.2.2:8080`.
- MongoDB del contenedor: `mongodb://localhost:27018` (Compass). Es una base **nueva**, distinta de la de tu PC; la primera vez se generan los datos de prueba.
- Para usar tu MongoDB local con tus datos: copiar `.env.example` como `.env` y descomentar `MONGODB_URI=mongodb://host.docker.internal:27017/synergy`.
- El login con Google necesita `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET` en `.env` (no se copian las claves de `application-secrets.properties` a la imagen).
- Si después de cambiar código no ves los cambios: `docker compose up --build` otra vez.

**Pruebas automáticas** (JUnit, sin base de datos): `mvnw test`. Cubren el modelo de riesgo (variables, reglas, predicción, historial, simulador), validaciones de proyectos/tareas/archivos, contraseñas, bloqueo por intentos, redirecciones seguras y exportación a CSV. GitHub Actions (`.github/workflows/ci.yml`) las corre en cada *push* junto con `flutter analyze`, `flutter test` y el APK de depuración.

**Documentación de la API:** <http://localhost:8080/swagger-ui.html> (springdoc).

**Perfiles:** por defecto se generan datos de prueba. Con `--spring.profiles.active=prod` (o `SPRING_PROFILES_ACTIVE=prod`) no se generan datos, `JWT_SECRET` es obligatorio, cookies solo por HTTPS y logs reducidos (`application-prod.properties`).

**Respaldo de MongoDB:** `scripts/respaldo_mongo.ps1` (Windows) o `scripts/respaldo_mongo.sh` (Linux/macOS). Guarda los últimos 7 en `respaldos/`; se puede programar cada noche.

## 11. Seguridad

- Contraseñas nuevas: mínimo 8 caracteres con mayúscula, minúscula y número (web, API y app).
- 5 contraseñas incorrectas seguidas bloquean ese correo 5 minutos (web y app).
- CSRF activo en los formularios web (la API usa JWT y queda excluida); cabeceras de seguridad (CSP, *frame*, *referrer*).
- JWT de 13 minutos + *refresh token* de 7 días para la app (`POST /api/auth/refresh`). La clave va en `JWT_SECRET` o `application-secrets.properties`.
- Las entregas solo aceptan PDF, imágenes, Office, TXT, CSV y ZIP de hasta 10 MB, y se revisa que el contenido corresponda a la extensión.
- Cada endpoint revisa que el usuario sea dueño del recurso (un voluntario no puede ver tareas de proyectos ajenos cambiando el id).
- Auditoría persistente (colección `auditoria`): aprobaciones, rechazos, eliminaciones, cambios de rol y de estado de cuentas. Panel *Auditoría*.
- Recuperar contraseña: el administrador genera una contraseña temporal en *Usuarios* (no hay correo configurado).
- **Pendiente del equipo:** generar un secreto nuevo de Google OAuth en Google Cloud (el anterior se compartió) y guardarlo solo en `application-secrets.properties`.

## 12. Problemas comunes

| Síntoma | Solución |
|---|---|
| En la consola sale `IndexOptionsConflict` o `Index already exists with a different name` | Ya no detiene la aplicación: `MongoIndexConfig` reemplaza el índice viejo. Si persiste, usa `synergy.datos.regenerar-todo=true` una vez. |
| La página sale sin estilos o con el diseño viejo | `mvnw clean spring-boot:run` y recargar con `Ctrl + F5`. |
| Google dice `redirect_uri_mismatch` | Agregar exactamente `http://localhost:8080/login/oauth2/code/google` en Google Cloud Console. |
| Google dice `invalid_client` | Revisar client-id / secret en `application-secrets.properties`. |
| No entra con las cuentas de prueba | Poner `synergy.demo.reset-passwords=true` una vez, o ejecutar `setup_synergy_db.js`. |
| `Connection refused` a MongoDB | Iniciar el servicio de MongoDB. |
| La app del teléfono dice "No se pudo conectar con el servidor" | Poner la IP del PC en *Servidor* (login), misma red Wi-Fi y permitir el puerto 8080 en el firewall. |
| El semáforo dice "Sin datos" | El proyecto apenas empieza y no tiene tareas, o falta recalcular: botón *Recalcular* en el panel del administrador. |
| Docker: `port is already allocated` (8080) | Ya tienes Synergy corriendo con `mvnw spring-boot:run`: detenlo (Ctrl + C) o cambia el puerto en `docker-compose.yml` (`"8081:8080"`). |
| Docker: `Cannot connect to the Docker daemon` / `pipe/dockerDesktopLinuxEngine` | Abrir Docker Desktop y esperar a que diga *Engine running*. |
| Docker: login no entra (vuelve al formulario) | Dejar `COOKIE_SOLO_HTTPS=false` mientras uses `http://localhost`. |
| Docker: no veo mis proyectos de siempre | El contenedor usa su propia base. Usa `MONGODB_URI=mongodb://host.docker.internal:27017/synergy` en `.env`. |
