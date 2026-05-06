# Guía del Proyecto Synergy

Bienvenido a la guía oficial del proyecto **Synergy**. Esta guía consolida todos los requerimientos funcionales, historias de usuario, características no funcionales y la arquitectura de seguridad implementada en nuestro aplicativo.

---

## 1. Descripción del Proyecto
Synergy es una plataforma orientada a la gestión y participación ciudadana en proyectos comunitarios. Permite la colaboración entre los líderes, los administradores y los ciudadanos, centralizando la información de avances, reporte de seguimiento e incidencias.

---

## 2. Requerimientos Funcionales y Roles (Historias de Usuario)

El sistema define los siguientes niveles de acceso y sus historias de usuario asociadas:

### 🌟 Administrador
Encargado de la supervisión general y mantenimiento de la plataforma.
*   **Gestión de Proyectos:** Eliminar proyectos cancelados o ingresados por error.
*   **Gestión Financiera:** Mostrar información de pagos de administración (referencias, cuentas, etc.).
*   **Informes y Seguimiento:** Reportes de seguimiento de proyectos (terminados, en proceso, con anomalías o incidencias).
*   **Gestión de Líderes:** Visualizar las acciones de los líderes e incidencias de gestión.
*   **Filtros:** Filtrar proyectos por estado para una gestión eficiente y recibir alertas de confirmación al eliminar.

### 👤 Líder Comunitario
Gestor principal del proyecto.
*   **Creación y Edición:** Dar de alta nuevos proyectos mediante formulario y modificarlos posteriormente para mantener información actualizada sin crear de cero.
*   **Soporte Documental:** Adjuntar archivos de soporte (PDF o imagen) en los proyectos.
*   **Codificación Automática:** Todo proyecto registrado recibe un código único auto-generado por el sistema.

### 👥 Usuario (Ciudadano Registrado)
Voluntario interesado en hacer parte de las iniciativas.
*   **Descubrimiento:** Visualizar lista de proyectos (nombre, estado, avance) y buscar mediante palabras clave (ej: "medio ambiente").
*   **Participación:** Ver la cantidad de voluntarios activos y utilizar el botón "Participar" para sumarse al esfuerzo comunitario.

### 🕵️‍♂️ Invitado (No Autorizado)
*   **Exploración Básica:** Ver listado básico de proyectos y filtrarlos por estado o buscar por nombre.
*   **Validación y Redirección:** Al intentar "Participar" en un proyecto, el sistema detecta que es un invitado y lo redirige automáticamente a la página de `/login` para que se autentique o se registre.

---

## 3. Requerimientos No Funcionales

El aplicativo fue configurado para satisfacer las siguientes políticas operativas y de seguridad:

### **Seguridad (RNF-S01 a RNF-S07)**
1.  **Protección de Sesión y Autenticación STATELESS (RNF-S01):** La configuración de Spring Security se migró a `STATELESS`. Las sesiones no se guardan en el servidor; toda la autorización viaja en un Token JWT resguardado de forma segura en una Cookie con banderas `HttpOnly`.
2.  **Inactividad (RNF-S02):** El Token JWT caduca en un tiempo establecido (ej. 13 minutos). Al ser *Stateless*, no hay control de "sesiones activas simultáneas" convencional en base de datos.
3.  **Gestor de Variables (RNF-S04) y Cifrado (RNF-S05 / RNF-S07):** La administración y protección confidencial se realiza a través de algoritmos BCrypt para contraseñas, y codificación mediante la clave secreta pre-configurada (`528670c5...`).
4.  **Auditoría (RNF-S06):** Quedará habilitado un Log Seguro de todas las transacciones críticas.
5.  **CSRF Deshabilitado:** Debido a que operamos mediante validación de Tokens (JWT en Cookies para la capa web y API), la protección CSRF fue deshabilitada (`csrf.disable()`) por diseño.

### **Despliegue y Confiabilidad (RNF-D01 a RNF-R01)**
1.  **Comunicaciones:** (NOTA: La configuración de despliegue HTTPS/SSL y TLS en servidor fue omitida por requerimiento actual, por lo que las iteraciones de RNF-D01 se aplican a nivel perimetral).
2.  **Puertos (RNF-D02):** El aplicativo se levanta bajo la configuración estricta en el puerto `8080`.
3.  **Manejo de Errores (RNF-R01):** El sistema muestra páginas customizadas y personalizadas (Thymeleaf/Tailwind) para los códigos `403` (Acceso denegado) y `404` (Recurso no encontrado).

### **Rendimiento, Usabilidad y Estabilidad Ui (RNF-P01, RNF-U01)**
1.  **Tiempos de Respuesta:** Diseño estructurado en API Rest para cumplir tiempos menores a 2 segundos bajo carga estándar.
2.  **Consistencia de UI y UX Dinámica:** Pantallas total y masivamente uniformes. El panel de proyectos públicos ahora cuenta con una barra de búsqueda inteligente en JavaScript que permite filtrar en tiempo real en la tabla por título o descripción.
3.  **Filtros Inteligentes en Backend:** Al asignar tareas (Panel del Líder), el sistema filtra proactivamente la base de datos para mostrar única y exclusivamente a los "Participantes" o "Usuarios", evitando que se asigne trabajo administrativo a jerarquías superiores (`LIDER` o `ADMINISTRADOR`).
4.  **Prevención de Errores de Renderizado e Inyección:** Se migró el contexto de sesión de Thymeleaf (basado en el inestable `HttpSession`) hacia el `Principal` puro de Security, garantizando que el usuario no pierda el acceso al recargar. Se solucionaron problemas de "Case Sensitivity" (Ej: 'Lider' vs 'LIDER').

### **Gestión de Base de Datos y Seed (MongoDB)**
Para inicializar la base de datos con la colección correcta de *Usuarios*, *Roles* y *Privilegios*, se ha proveído un script de automatización compatible con `mongosh` llamado **`setup_synergy_db.js`**. 
- Solo debes ejecutar en consola: `mongosh "mongodb://localhost:27017" setup_synergy_db.js`.
- Este archivo generará a los usuarios "Super Administrador" (`admin@synergy.com`), "Juan Lider Comunitario" y "Maria Voluntaria" con las configuraciones Hasheadas por BCrypt.

---

## 4. Estructura de Seguridad Interna (Spring Security)
Dentro del entorno se han configurado los siguientes compendios para la arquitectura técnica:

*   **UserDetails y PasswordEncoder:** La base se apoya en `UserDetailsService` cargando usuarios (y validando roles) directamente desde MongoDB (Spring Data Mongo). El hash se maneja vía `BCryptPasswordEncoder`.
*   **JWT y JwtCookieService:** El sistema emite y valida Tokens Json (`io.jsonwebtoken`) integrando un servicio personalizado `JwtCookieService` que adhiere el JWT de forma transparente en el navegador. Las rutas son validadas a través de nuestro filtro principal `JwtAuthenticationFilter`.
*   **Filtrado por AuthManager:** Uso pleno de `DaoAuthenticationProvider` y validación robusta de end-points (tanto rutas `/api/` como renderizados Thymeleaf) mediante `SecurityFilterChain`.

*(Fin del Documento)*
