# Despliegue de Synergy en Render (paso a paso)

Resultado: Synergy publicada en internet en `https://synergy-expoferia.onrender.com`
(o un nombre parecido), con HTTPS, base de datos en la nube y despliegue automático
en cada `git push`. **Costo: $0** (Render Free + MongoDB Atlas Free).

```
 Navegador / app móvil ──HTTPS──> Render (Dockerfile de Synergy) ──> MongoDB Atlas (M0)
```

| Qué | Dónde | Plan |
|---|---|---|
| App Spring Boot | Render, construida con el `Dockerfile` del repositorio | Free (512 MB RAM, se duerme a los 15 min sin visitas) |
| Base de datos | MongoDB Atlas | M0 Free (512 MB) |
| Configuración | `render.yaml` en la raíz del repositorio | — |

Tiempo total aproximado: 30–40 minutos la primera vez.

---

## Paso 0. Antes de empezar

- [ ] El repositorio `SYNERGY` en GitHub tiene la última versión (`git push` hecho), incluido `render.yaml`.
- [ ] Tienes a mano el **ID** y el **secreto** del cliente OAuth nuevo de Google
      (están en tu `application-secrets.properties` / `.env`).
- [ ] Una cuenta de correo para Atlas y Render (puede ser la de GitHub).

---

## Paso 1. Base de datos en MongoDB Atlas

1. Entra a <https://www.mongodb.com/cloud/atlas/register> y crea la cuenta (puedes usar Google).
2. **Create a cluster** → elige **M0 (Free)**.
   - Provider: **AWS** · Region: **N. Virginia (us-east-1)** (la misma zona que Render en `render.yaml`).
   - Name: `synergy` → **Create Deployment**.
3. Te pide crear un **usuario de base de datos**:
   - Username: `synergy_app`
   - Password: pulsa **Autogenerate Secure Password** y **cópiala** (solo letras y números, así no rompe la URL).
   - **Create Database User**.
4. **Network Access** (menú izquierdo) → **Add IP Address** → **Allow access from anywhere** (`0.0.0.0/0`) → **Confirm**.
   > Render no tiene una IP fija; por eso hay que permitir todas. La seguridad la da el usuario y la contraseña.
5. **Database** → botón **Connect** → **Drivers** → copia la cadena. Se ve así:
   ```
   mongodb+srv://synergy_app:<db_password>@synergy.ab1cd.mongodb.net/?retryWrites=true&w=majority&appName=synergy
   ```
6. Arma tu `MONGODB_URI` final: cambia `<db_password>` por la contraseña y agrega **`synergy`** después del `/`:
   ```
   mongodb+srv://synergy_app:TU_CLAVE@synergy.ab1cd.mongodb.net/synergy?retryWrites=true&w=majority&appName=synergy
   ```
   Guárdala en un bloc de notas (no la subas a GitHub).

---

## Paso 2. Generar la clave JWT

En PowerShell:

```powershell
-join ((48..57)+(65..90)+(97..122) | Get-Random -Count 62 | % {[char]$_}) + (Get-Random -Minimum 10 -Maximum 99)
```

Copia el resultado (64 caracteres). Es tu `JWT_SECRET`.

---

## Paso 3. Crear el servicio en Render (con el Blueprint)

1. Entra a <https://dashboard.render.com> → **Sign up with GitHub**.
2. Autoriza a Render para ver el repositorio **SYNERGY** (puedes elegir “Only select repositories”).
3. Arriba a la derecha: **New +** → **Blueprint**.
4. Elige el repositorio **ncastillaalvarez-boop/SYNERGY**, rama **main**. Render encuentra `render.yaml`.
5. Blueprint Name: `synergy`. Render muestra el servicio **synergy-expoferia** (Docker, Free) y te pide 4 valores:

   | Variable | Qué pegar |
   |---|---|
   | `MONGODB_URI` | la cadena del Paso 1.6 |
   | `JWT_SECRET` | la clave del Paso 2 |
   | `GOOGLE_CLIENT_ID` | el ID de cliente nuevo (termina en `.apps.googleusercontent.com`) |
   | `GOOGLE_CLIENT_SECRET` | el secreto nuevo (empieza por `GOCSPX-`) |

   Todo lo demás (perfil, memoria, HTTPS, datos de prueba) ya viene en `render.yaml`.
6. **Deploy Blueprint**.
7. Abre el servicio **synergy-expoferia** → pestaña **Logs**. La primera vez:
   - 3–6 min: compila con Maven dentro de Docker.
   - 2–4 min: arranca y genera los datos de prueba en Atlas (solo la primera vez).
   - Listo cuando ves `Started SynergyApplication` y arriba dice **Live** en verde.
8. Arriba del servicio está tu dirección, por ejemplo:
   **`https://synergy-expoferia.onrender.com`** ← anótala exacta (si el nombre estaba ocupado, Render agrega letras al final).

> Si prefieres no usar Blueprint: **New + → Web Service** → repositorio SYNERGY → Language **Docker** →
> Region **Virginia** → Instance **Free** → en *Environment* agrega a mano TODAS las variables de `render.yaml`
> (las 4 claves y las de la sección `value:`) → *Advanced* → Health Check Path `/login` → **Create Web Service**.

---

## Paso 4. Autorizar la nueva dirección en Google (inicio de sesión con Google)

1. <https://console.cloud.google.com> → **APIs y servicios → Credenciales** → abre el cliente **nuevo** (`463644300683-be5r43…`).
2. **Orígenes autorizados de JavaScript** → **Agregar URI**: `https://synergy-expoferia.onrender.com`
3. **URIs de redireccionamiento autorizados** → **Agregar URI**:
   `https://synergy-expoferia.onrender.com/login/oauth2/code/google`
4. **Guardar** (deja también las de `localhost`). Google tarda hasta 5 minutos en aplicarlo.

Usa **tu** dirección exacta del Paso 3.8, sin `/` al final.

---

## Paso 5. Probar

| Prueba | Cómo | Esperado |
|---|---|---|
| Página | Abre tu dirección `.onrender.com` | Página de inicio de Synergy con candado (HTTPS) |
| Login normal | `admin@synergy.com` / `Admin123*` | Panel del administrador con proyectos |
| Roles | `lider@synergy.com` / `Lider123*` y `voluntario@synergy.com` / `Voluntario123*` | Paneles de líder y voluntario |
| Google | **Iniciar sesión con Google** | Entra (la cuenta debe estar en *Usuarios de prueba* de Google) |
| API | `https://TU-DIRECCION.onrender.com/swagger-ui.html` | Documentación de la API |
| Riesgo | Panel admin → *Riesgo de los proyectos* | Alto / Medio / Bajo con números (no todo en “Sin datos”) |

---

## Paso 6. App móvil apuntando a Render

En la pantalla de inicio de sesión de la app → **Servidor** → escribe solo:

```
synergy-expoferia.onrender.com
```

La app agrega `https://` y `/api` sola. Funciona con datos móviles o cualquier Wi-Fi.

Para dejarlo fijo al compilar el APK de la feria:

```bash
cd mobile
flutter build apk --release --dart-define=API_BASE_URL=https://synergy-expoferia.onrender.com/api
```

El APK queda en `mobile/build/app/outputs/flutter-apk/app-release.apk`.

---

## Paso 7. Que no se duerma durante la Expoferia (17 de noviembre)

El plan gratis se apaga a los 15 minutos sin visitas y tarda ~1 minuto en despertar.

- **El día de la feria:** abre la página 5 minutos antes y deja una pestaña abierta que recargue
  (o entra cada 10 minutos).
- **Automático (gratis):** crea un monitor en <https://uptimerobot.com> → *New monitor* → HTTP(s) →
  URL `https://TU-DIRECCION.onrender.com/login` → intervalo **5 minutos**. Lo mantiene despierto.
  (Render Free da 750 horas al mes: alcanza para 1 servicio encendido todo el mes.)
- Si en los logs aparece `Out of memory` o se reinicia solo: Render → *Settings* → *Instance type* →
  **Starter** (US$7/mes, se cobra por días). Puedes subirlo solo la semana de la feria y bajarlo después.

---

## Paso 8. Actualizar la app

```powershell
git add -A
git commit -m "lo que cambiaste"
git push
```

Render detecta el push y despliega solo (pestaña **Events**). Mientras despliega, la versión anterior sigue funcionando.

---

## Problemas comunes

| Síntoma (en Logs o en el navegador) | Causa y solución |
|---|---|
| `MongoTimeoutException` / `Timed out after 30000 ms` | Atlas no deja entrar: revisa **Network Access = 0.0.0.0/0** y que el cluster esté encendido. |
| `Authentication failed` / `bad auth` | Usuario o contraseña de Atlas mal en `MONGODB_URI`. Cambia la contraseña en Atlas (sin símbolos) y actualiza la variable en Render → *Environment* → *Save, rebuild and deploy*. |
| Google: `redirect_uri_mismatch` | Falta la URI del Paso 4, o tiene `/` al final, o la dirección no es exactamente la de Render. |
| Google: `invalid_client` | `GOOGLE_CLIENT_ID` / `SECRET` mal copiados (espacio al final). |
| Google: `access_denied` / “app no verificada” | Agrega el correo en **Pantalla de consentimiento → Usuarios de prueba**. |
| Login normal vuelve al formulario | Borra las cookies del sitio. Comprueba que la dirección empiece por **https://**. |
| `Exited with status 137` o `Out of memory` | Memoria: ver Paso 7 (Starter). |
| La página tarda ~1 minuto | Se había dormido (plan Free). Normal; ver Paso 7. |
| `Port scan timeout` / “No open ports detected” | La app no arrancó: revisa el error más arriba en *Logs* (casi siempre MongoDB). |
| Error en el build `mvn ... package` | Ejecuta `.\mvnw clean package` en tu PC: si falla allí también, es el código. |

## Seguridad

- Las claves (`MONGODB_URI`, `JWT_SECRET`, `GOOGLE_*`) viven **solo** en Render → *Environment*. Nunca en `render.yaml` ni en GitHub.
- El perfil `default` crea cuentas demo con contraseñas conocidas: perfecto para la feria. Para uso real cambia
  `SPRING_PROFILES_ACTIVE` a `prod` y define `SYNERGY_ADMIN_CORREO` y `SYNERGY_ADMIN_CLAVE`.
- Atlas Free no hace respaldos: si necesitas uno, usa `scripts/respaldo_mongo.ps1` con la `MONGODB_URI` de Atlas.
