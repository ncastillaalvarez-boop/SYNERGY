# SYNERGY: Plataforma de Gestión y Participación Comunitaria

Este documento técnico sirve como guía y sustento de evaluación para la defensa y sustentación del proyecto **Synergy**, enfocado a evidenciar de forma directa el cumplimiento de cada uno de los ítems requeridos en la arquitectura y desarrollo.

---

##  1. Autenticación y Registro de Usuarios (1 punto)

### a. Autenticación en Base de Datos (NoSQL - MongoDB)
El sistema gestiona la autenticación leyendo directamente desde nuestra base de datos **MongoDB**. Para ello, se cuenta con la clase `Usuarios` (mapeada a la colección `usuarios`) que implementa la interfaz **`UserDetails`** de Spring Security. 
*   **Roles Implementados:** El sistema diferencia el acceso al menos con tres tipos distintos de roles: `ADMINISTRADOR`, `LIDER` y `PARTICIPANTE` (Usuario Regular).
*   **Hash/Cifrado:** Ninguna contraseña es almacenada en texto plano; su verificación en el login y en base de datos se hace mediante **`BCryptPasswordEncoder`**.

### b. Registro de Usuarios
Contamos con lógica de controladores tanto a nivel de `Thymeleaf` como en `AuthService` para crear nuevas cuentas ("Signup"). Los usuarios que se registran desde la plataforma son almacenados inmediatamente en la base de datos, cifrando su contraseña. (De hecho, cualquier usuario nuevo registrado puede iniciar sesión con éxito demostrando la integridad de nuestro enrutamiento y servicio `PasswordEncoder`).

---

##  2. Gestión de Autorizaciones y Accesos Restringidos (1 punto)

### a. Definición de Privilegios y Reglas de Acceso
Nuestra arquitectura utiliza un modelo donde existen "Roles" que a su vez contienen "Privilegios" (Permisos de lectura, creación, edición, etc.).
*   **En Base de Datos:** Las colecciones `roles` y `privilegios` se asocian a cada usuario según su jerarquía.
*   **Endpoints Restringidos:** En el archivo maestro de configuración `SecurityConfig.java`, restringimos explícitamente el acceso usando nuestro middleware `SecurityFilterChain`:
    ```java
    .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
    .requestMatchers("/api/lider/**").hasAnyRole("ADMINISTRADOR", "LIDER")
    .requestMatchers("/api/**").authenticated()
    ```
    Ningún visitante puede ingresar a estas rutas si no presenta el nivel de autorización requerido.

### b. Configuración de Formularios de Login
*   **Vistas Frontend:** Existen formularios pre-armados para manejar el Login (`/login`, `/seleccionar-rol`) y Registro (`/registrarse`), soportadas de vista nativa con **Thymeleaf**.
*   **Integración de Backend:** El controlador intercepta el Request y delega a `AuthenticationManager` quien emite el veredicto junto a `DaoAuthenticationProvider`.

---

## ⏳ 3. Administración de Sesiones y Expiración (1 punto)

La plataforma maneja con rigurosidad cómo persisten o mueren las sesiones del usuario de forma automática y controlada para prevenir ataques o mal uso de recursos:
*   **Tiempo de Vida (Time-Out):** Configurado directamente a 13 minutos de inactividad a través del archivo de propiedades `server.servlet.session.timeout=13m` y reforzado con vencimientos JWT (`jwt.expiration=780000`). Pasado este tiempo, la sesión muere.
*   **Limite de Concurrencias:** Se configuró que exista un límite de un *máximo de 2 sesiones activas (*en`SecurityConfig.java` > `.maximumSessions(2)`).
*   **Seguridad de Cookie:** Las cookies (llamada `SYNERGY_SESSION`) corren bajó los flags estandar de `HttpOnly` y `Secure`.

---
## 🚫 4. Manejo de Control de Errores y Seguridad Estricta (1 punto)

La robustez de la app se evidencia en su capa de presentación ante escenarios imprevistos, evitando colapsos del servidor o información expuesta ("stack traces" feos):
*   **Páginas Customizadas (403 y 404):** Cuando un usuario intenta acceder a una ruta restringida sin el rol adecuado, Spring Security dispara un error de prohibición (403). Nuestro sistema lo atrapa y direcciona a una página visualmente impecable (`403.html`). Idénticamente ocurre si navegan a algo que no existe (`404.html`), previniendo caídas totales.
*   **Blindaje ante "Case Sensitivity":** Se reforzaron los Endpoints (`LiderController`, `UsuarioWebController`) validando los roles de la base de datos indiferentemente del case sensitivity de la inyección manual (`.equalsIgnoreCase`, `.toUpperCase()`), cortando de raíz los errores de log-in rebotados.
*   **Limpieza de Binding Interno:** Se sanó todo error del servidor tipo 500 originado por llamadas Strict de Enums (`u.rol.name()`), permitiendo mallas limpias de renderizado Thymeleaf en las tablas de Usuarios.

---

## 🗄️ 5. Avance Frontend, Backend y CRUD de Entidades (1 punto)

El desarrollo cumple metodologías cliente-servidor para presentar funcionalidades asíncronas y de renderizado:
*   **Backend:** Todo el ciclo en Java usando anotaciones Controller/Service/Repository, acoplados a Spring Data MongoDB. Tenemos colecciones funcionales reales: `Proyectos`, `Usuarios`, `Tareas`, `Roles`.
*   **Operaciones CRUD:**
    *   *Create:* Generación de Proyectos por líderes comunitarios o Usuarios nuevos (Registro).
    *   *Read:* Visualizar listas de proyectos, conteo de voluntarios, dashboard de gestión o la visualización de los detalles en `/proyectos/publico`.
    *   *Update:* Asignación de roles y actualizaciones de avance del proyecto o del estado de las tareas.
    *   *Delete:* Eliminación bajo restricciones administrativas (solo administradores pueden desaparecer proyectos inválidos).

---

## 🎨 6. Escalabilidad Frontend y Diseño Unificado

A nivel visual, Synergy cuenta con una Arquitectura SaaS extremadamente limpia, pensada en la retención del usuario:
*   **Unified Global Dashboard:** Todo panel interior (Admin, Usuario o Líder) ha sido sincronizado a utilizar una sola matriz de *Sistema de Diseño*.
*   **Premium Glassmorphism:** Se han descartado colores genéricos o archivos de imágenes de fondo pesadas. El sistema opera modularmente con tipografía `Inter`, degradados Radiales CSS Nativos rápidos y un balance perfecto entre `Dark Theme` profundo e interacciones reactivas impecables.
*   **Control de Resources (Case-Insensitive Automático):** La inyección de imágenes está configurada a respetar de forma milimétrica su ruta para ser a prueba de caídas en despliegues como Linux O Windows.

---
##  Resumen de Inicialización (Seed DB)

El sistema cuenta con **dos mecanismos** para asegurar datos iniciales:
1.  Un archivo interno programado en backend: `DataInitializer.java` (si la base de datos arranca vacía, Spring crea todo lo necesario en la colección, los usuarios, y hashea las contraseñas internamente).
2.  Un script de mongo: `setup_synergy_db.js` (como alternativa que se ejecuta directo en el shell de MongoDB para ambientes independientes o para re-seteos de despliegue).
