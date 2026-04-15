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
*   **Exploración Básica:** Ver listado básico de proyectos y filtrarlos por estado.
*   **Validación:** Al intentar "Participar" es redirigido obligatoriamente a la página de registro.

---

## 3. Requerimientos No Funcionales

El aplicativo fue configurado para satisfacer las siguientes políticas operativas y de seguridad:

### **Seguridad (RNF-S01 a RNF-S07)**
1.  **Protección de Sesión (RNF-S01):** Las cookies son configuradas mediante el estándar de Spring Security con los flag `HttpOnly` y `Secure`.
2.  **Inactividad (RNF-S02):** La sesión del usuario expira automáticamente tras de **13 minutos**.
3.  **Límite de Sesiones (RNF-S03):** Se limita el uso a un **máximo de 2 sesiones activas** de forma simultánea. Al superarse el límite, se finaliza la más antigua.
4.  **Gestor de Variables (RNF-S04) y Cifrado (RNF-S05 / RNF-S07):** La administración y protección confidencial se realiza a través de algoritmos BCrypt para contraseñas, y codificación en los JWT emitidos en el almacenamiento de variables propias (cookies con nombre custom `SYNERGY_SESSION`).
5.  **Auditoría (RNF-S06):** Quedará habilitado un Log Seguro de todas las transacciones críticas como inicio de sesión, cierre y eliminación/creación de proyectos.

### **Despliegue y Confiabilidad (RNF-D01 a RNF-R01)**
1.  **Comunicaciones:** (NOTA: La configuración de despliegue HTTPS/SSL y TLS en servidor fue omitida por requerimiento actual, por lo que las iteraciones de RNF-D01 se aplican a nivel perimetral).
2.  **Puertos (RNF-D02):** El aplicativo se levanta bajo la configuración estricta en el puerto `8080`.
3.  **Manejo de Errores (RNF-R01):** El sistema muestra páginas customizadas y personalizadas (Thymeleaf/Tailwind) para los códigos `403` (Acceso denegado) y `404` (Recurso no encontrado).

### **Rendimiento, Usabilidad y Estabilidad Ui (RNF-P01, RNF-U01)**
1.  **Tiempos de Respuesta:** Diseño estructurado en API Rest para cumplir tiempos menores a 2 segundos bajo carga estándar.
2.  **Consistencia de UI (SaaS Dark Theme):** Pantallas total y masivamente uniformes. El proyecto ahora cuenta con un Sistema de Diseño Único de Alto Calibre implementado a través de todos los paneles (Admin, Lider, Usuarios), con estética Deep Dark Slate, Glassmorphism, y degradados radiales nativos (sin dependencia externa de imágenes pesadas).
3.  **Prevención de Errores de Renderizado e Inyección:** Se han solucionado problemas de "Case Sensitivity" (Ej: 'Lider' vs 'LIDER') que boicoteaban la persistencia en el log-in, así como errores fatales en el renderizado interno de Thymeleaf (Error 500) por mapeo estricto del Modelo-Vista, lo que solidifica el 100% de la navegación.

### **Gestión de Base de Datos y Seed (MongoDB)**
Para inicializar la base de datos con la colección correcta de *Usuarios*, *Roles* y *Privilegios*, se ha proveído un script de automatización compatible con `mongosh` llamado **`setup_synergy_db.js`**. 
- Solo debes ejecutar en consola: `mongosh "mongodb://localhost:27017" setup_synergy_db.js`.
- Este archivo generará a los usuarios "Super Administrador" (`admin@synergy.com`), "Juan Lider Comunitario" y "Maria Voluntaria" con las configuraciones Hasheadas por BCrypt.

---

## 4. Estructura de Seguridad Interna (Spring Security)
Dentro del entorno se han configurado los siguientes compendios para la arquitectura técnica:

*   **UserDetails y PasswordEncoder:** La base se apoya en `UserDetailsService` cargando usuarios (y validando roles) directamente desde MongoDB (Spring Data Mongo). El hash se maneja vía `BCryptPasswordEncoder`.
*   **JWT y AuthFilter:** El sistema emite y valida Tokens Json (`io.jsonwebtoken`) resguardando las rutas en un Filtro de Autenticación personalizado `JwtAuthenticationFilter` incrustado como Middleware general.
*   **Filtrado por AuthManager:** Uso pleno de `DaoAuthenticationProvider` y validación robusta de end-points mediante `SecurityFilterChain`.

*(Fin del Documento)*
