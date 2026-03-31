# SYNERGY - Sistema de Gestión de Proyectos con Seguridad

## 📋 Descripción

SYNERGY es una aplicación web de gestión de proyectos migrada de SQL (MySQL) a **MongoDB** con implementación completa de seguridad basada en:

- ✅ **Autenticación** con JWT (JSON Web Tokens)
- ✅ **Autorización** basada en roles y privilegios
- ✅ **Control de acceso** por recurso (CRUD)
- ✅ **Gestión de sesiones** con expiración
- ✅ **Encriptación** de contraseñas con BCrypt
- ✅ **Manejo de errores** centralizado

## 🏗️ Arquitectura

### Estructura del Proyecto

```
src/main/java/com/proaula/spring/synergy/
├── Model/              # Entidades MongoDB
│   ├── Usuarios.java
│   ├── Proyecto.java
│   ├── Tarea.java
│   ├── Comentario.java
│   ├── EntregaTarea.java
│   ├── Rol.java
│   ├── Privilegio.java
│   └── Participacion.java
├── Repository/         # Interfaces MongoDB
│   ├── UsuarioRepository.java
│   ├── ProyectoRepository.java
│   ├── TareaRepository.java
│   ├── RolRepository.java
│   ├── PrivilegioRepository.java
│   └── ...
├── Service/           # Lógica de negocio
│   ├── AuthService.java
│   ├── UsuarioService.java
│   ├── RolPrivilegioService.java
│   └── ...
├── Controller/        # Endpoints REST
│   ├── AuthController.java
│   ├── AdminUsuariosController.java
│   ├── RolPrivilegioController.java
│   └── ...
├── Security/          # Autenticación & Autorización
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── UserDetailsServiceImpl.java
│   └── LoginRequest.java
└── Config/           # Configuración
    ├── SecurityConfig.java
    ├── GlobalExceptionHandler.java
    └── DataInitializer.java
```

## 🔐 Seguridad Implementada

### 1. **Autenticación (Login)**

**Endpoint:** `POST /api/auth/login`

```json
{
  "correo": "admin@synergy.com",
  "contrasena": "123456"
}
```

**Respuesta:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tipo": "Bearer",
  "id": "usuario_id",
  "nombre": "Administrador",
  "correo": "admin@synergy.com",
  "rol": "ADMINISTRADOR",
  "privilegios": ["priv1_id", "priv2_id", ...],
  "expiresIn": 86400000
}
```

### 2. **Roles y Privilegios**

#### Tres Roles Definidos:

| Rol | Descripción | Privilegios |
|-----|-------------|------------|
| **ADMINISTRADOR** | Acceso total al sistema | Todos los privilegios |
| **LIDER** | Gestión de proyectos y tareas | Crear/editar tareas, asignar usuarios, ver entregas |
| **PARTICIPANTE** | Acceso limitado | Ver proyectos, ver tareas, entregar, comentar |

#### Privilegios por Recurso:

- **PROYECTO**: CREAR_PROYECTO, VER_PROYECTO, EDITAR_PROYECTO, ELIMINAR_PROYECTO
- **TAREA**: CREAR_TAREA, VER_TAREA, EDITAR_TAREA, ELIMINAR_TAREA, ASIGNAR_TAREA
- **USUARIO**: CREAR_USUARIO, VER_USUARIO, EDITAR_USUARIO, ELIMINAR_USUARIO, CAMBIAR_ROL
- **COMENTARIO**: CREAR_COMENTARIO, EDITAR_COMENTARIO, ELIMINAR_COMENTARIO
- **ENTREGA**: CREAR_ENTREGA, VER_ENTREGA, CALIFICAR_ENTREGA
- **ADMIN**: ACCESO_ADMIN, GENERAR_REPORTES

### 3. **Restricción de Acceso**

#### Rutas Protegidas:

```
GET    /api/admin/usuarios              → @PreAuthorize("hasRole('ADMINISTRADOR')")
POST   /api/admin/usuarios/{id}/rol     → @PreAuthorize("hasRole('ADMINISTRADOR')")
GET    /api/admin/roles-privilegios     → @PreAuthorize("hasRole('ADMINISTRADOR')")
GET    /api/usuarios/**                 → @PreAuthorize("isAuthenticated()")
```

**Headers requeridos:**
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

### 4. **Manejo de Sesiones**

- **Timeout**: 30 minutos (configurable en `application.properties`)
- **Tipo de almacenamiento**: MongoDB
- **Expiración de tokens**: 24 horas

### 5. **Encriptación de Contraseñas**

- Algoritmo: **BCrypt** (10 rondas)
- Implementado en: `SecurityConfig.passwordEncoder()`

### 6. **Manejo de Errores Centralizado**

**GlobalExceptionHandler** maneja:

- `UsernameNotFoundException` → 404 Not Found
- `BadCredentialsException` → 401 Unauthorized
- `AccessDeniedException` → 403 Forbidden
- `RuntimeException` → 400 Bad Request
- `Exception` → 500 Internal Server Error

## 🚀 Configuración e Instalación

### Requisitos

- Java 21+
- MongoDB Local o Remoto
- Maven
- Spring Boot 3.5.8+

### Pasos de Instalación

#### 1. **Configurar MongoDB**

```bash
# Conectarse a MongoDB
mongosh

# Crear índices (Opcional - se crea automáticamente)
use synergy
load("synergy_mongo_setup.js")
```

#### 2. **Actualizar `application.properties`**

```properties
# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/synergy
spring.data.mongodb.database=synergy

# JWT
app.jwtSecret=tu_secret_key_muy_largo
app.jwtExpirationMs=86400000

# Sesiones
server.servlet.session.timeout=30m
```

#### 3. **Compilar y Ejecutar**

```bash
mvn clean install
mvn spring-boot:run
```

**La aplicación se iniciará en:** `http://localhost:8081`

## 📚 Endpoints API

### Autenticación

```
POST   /api/auth/login                  # Login
POST   /api/auth/registrar              # Registro
POST   /api/auth/cambiar-contrasena     # Cambiar contraseña
POST   /api/auth/logout                 # Logout
```

### Administración de Usuarios (Admin)

```
GET    /api/admin/usuarios              # Listar todos
GET    /api/admin/usuarios/{id}         # Obtener por ID
POST   /api/admin/usuarios              # Crear
PUT    /api/admin/usuarios/{id}         # Actualizar
DELETE /api/admin/usuarios/{id}         # Eliminar
PUT    /api/admin/usuarios/{id}/rol     # Cambiar rol
PUT    /api/admin/usuarios/{id}/activar
PUT    /api/admin/usuarios/{id}/desactivar
GET    /api/admin/usuarios/rol/{rol}    # Por rol
GET    /api/admin/usuarios/estado/activos
```

### Gestión de Roles y Privilegios (Admin)

```
GET    /api/admin/roles-privilegios/roles
GET    /api/admin/roles-privilegios/roles/{id}
POST   /api/admin/roles-privilegios/roles
PUT    /api/admin/roles-privilegios/roles/{id}
DELETE /api/admin/roles-privilegios/roles/{id}

GET    /api/admin/roles-privilegios/privilegios
GET    /api/admin/roles-privilegios/privilegios/{id}
POST   /api/admin/roles-privilegios/privilegios
PUT    /api/admin/roles-privilegios/privilegios/{id}
DELETE /api/admin/roles-privilegios/privilegios/{id}
```

## 👥 Usuarios de Prueba

La aplicación se inicializa automáticamente con 3 usuarios:

| Email | Contraseña | Rol |
|-------|-----------|-----|
| admin@synergy.com | 123456 | ADMINISTRADOR |
| lider@synergy.com | 123456 | LIDER |
| usuario@synergy.com | 123456 | PARTICIPANTE |

## 🔄 Flujo de Autenticación

```
1. Usuario envía email + contraseña al /api/auth/login
2. SecurityConfig.authenticationProvider() valida las credenciales
3. JwtTokenProvider.generateToken() crea JWT
4. Usuario recibe token en AuthResponse
5. Cliente envía token en header: Authorization: Bearer {token}
6. JwtAuthenticationFilter valida el token
7. SecurityContext establece el usuario autenticado
8. @PreAuthorize verifica permisos antes de ejecutar el método
```

## 🗄️ Modelo de Datos MongoDB

### Colecciones Principales

#### usuarios
```json
{
  "_id": "ObjectId",
  "nombre": "string",
  "correo": "string (unique)",
  "contrasena": "string (bcrypt)",
  "rol": "string",
  "estado": "ACTIVO | INACTIVO",
  "activo": "boolean",
  "privilegios": ["privilegio_id"],
  "proyectos": ["proyecto_id"],
  "tareasAsignadas": ["tarea_id"],
  "fechaCreacion": "datetime",
  "ultimoAcceso": "datetime"
}
```

#### roles
```json
{
  "_id": "ObjectId",
  "nombre": "string (unique)",
  "descripcion": "string",
  "privilegios": ["privilegio_id"],
  "activo": "boolean",
  "fechaCreacion": "datetime"
}
```

#### privilegios
```json
{
  "_id": "ObjectId",
  "nombre": "string (unique)",
  "descripcion": "string",
  "recurso": "PROYECTO | TAREA | USUARIO | ...",
  "accion": "CREATE | READ | UPDATE | DELETE",
  "activo": "boolean",
  "fechaCreacion": "datetime"
}
```

## 🛠️ Personalización

### Cambiar Expiración de Sesión

En `application.properties`:
```properties
server.servlet.session.timeout=60m  # 60 minutos
app.jwtExpirationMs=3600000         # 1 hora en milisegundos
```

### Agregar Nuevo Rol

```java
Rol nuevoRol = new Rol("REVISOR", "Revisor de tareas");
nuevoRol.setPrivilegios(Arrays.asList(privilegioId1, privilegioId2));
rolRepository.save(nuevoRol);
```

### Crear Nuevo Privilege

```java
Privilegio priv = new Privilegio(
    "VER_REPORTES",
    "Ver reportes del sistema",
    "REPORTE",
    "READ"
);
privilegioRepository.save(priv);
```

## 📝 Logging

La configuración de logging está en `application.properties`:

```properties
logging.level.root=INFO
logging.level.com.proaula.spring.synergy=DEBUG
logging.level.org.springframework.security=DEBUG
```

## 🐛 Troubleshooting

### "MongoDB connection refused"
- Verificar que MongoDB esté ejecutándose: `mongosh`
- Cambiar URI en `application.properties`

### "Invalid JWT token"
- Verificar que el token no haya expirado (24 horas por defecto)
- Verificar que el `app.jwtSecret` sea el mismo en el servidor

### "Access Denied"
- Verificar que el usuario tenga el rol correcto
- Verificar que el rol tenga el privilegio necesario

## 📖 Referencias

- [Spring Security](https://spring.io/projects/spring-security)
- [Spring Data MongoDB](https://spring.io/projects/spring-data-mongodb)
- [JWT.io](https://jwt.io/)
- [MongoDB Documentation](https://docs.mongodb.com/)

## 📄 Licencia

Este proyecto es parte del sistema SYNERGY de gestión de proyectos escolares.

---

**Última actualización**: Marzo 2026
**Versión**: 2.0.0 (MongoDB + Security)
