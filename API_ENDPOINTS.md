# API Endpoints - Ejemplos de Uso

## 🔐 Autenticación

### 1. Login

**Request:**
```http
POST /api/auth/login HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{
  "correo": "admin@synergy.com",
  "contrasena": "123456"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbkBzeW5lcmd5LmNvbSIsInJvbCI6IkFETUlOSVNUUkFET1IiLCJpYXQiOjE3MTAyNDU2NDAsImV4cCI6MTcxMDMzMjA0MH0.abc123...",
  "tipo": "Bearer",
  "id": "65e8f3c4a1b2c3d4e5f6g7h8",
  "nombre": "Administrador",
  "correo": "admin@synergy.com",
  "rol": "ADMINISTRADOR",
  "privilegios": ["65e8f...", "65e9f...", ...],
  "expiresIn": 86400000
}
```

### 2. Registro

**Request:**
```http
POST /api/auth/registrar HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{
  "nombre": "Nuevo Usuario",
  "correo": "nuevousuario@synergy.com",
  "contrasena": "MiPassword123",
  "rol": "PARTICIPANTE"
}
```

**Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "tipo": "Bearer",
  "id": "65e8f3c4a1b2c3d4e5f6g7h9",
  "nombre": "Nuevo Usuario",
  "correo": "nuevousuario@synergy.com",
  "rol": "PARTICIPANTE",
  "privilegios": [...],
  "expiresIn": 86400000
}
```

### 3. Cambiar Contraseña

**Request:**
```http
POST /api/auth/cambiar-contrasena HTTP/1.1
Host: localhost:8081
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...

{
  "usuarioId": "65e8f3c4a1b2c3d4e5f6g7h8",
  "contrasenaActual": "123456",
  "nuevaContrasena": "NuevaPassword456"
}
```

**Response (200 OK):**
```json
"Contraseña cambiada exitosamente"
```

### 4. Logout

**Request:**
```http
POST /api/auth/logout HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response (200 OK):**
```json
"Sesión cerrada exitosamente"
```

---

## 👥 Administración de Usuarios (Requiere ADMINISTRADOR)

### 5. Listar Todos los Usuarios

**Request:**
```http
GET /api/admin/usuarios HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response (200 OK):**
```json
[
  {
    "id": "65e8f3c4a1b2c3d4e5f6g7h8",
    "nombre": "Administrador",
    "correo": "admin@synergy.com",
    "rol": "ADMINISTRADOR",
    "estado": "ACTIVO",
    "activo": true,
    "fechaCreacion": "2024-03-31T10:00:00",
    "ultimoAcceso": "2024-03-31T12:30:00"
  },
  {
    "id": "65e8f3c4a1b2c3d4e5f6g7h9",
    "nombre": "Líder de Proyecto",
    "correo": "lider@synergy.com",
    "rol": "LIDER",
    "estado": "ACTIVO",
    "activo": true,
    "fechaCreacion": "2024-03-31T10:05:00",
    "ultimoAcceso": "2024-03-31T12:25:00"
  }
]
```

### 6. Obtener Usuario por ID

**Request:**
```http
GET /api/admin/usuarios/65e8f3c4a1b2c3d4e5f6g7h8 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response (200 OK):**
```json
{
  "id": "65e8f3c4a1b2c3d4e5f6g7h8",
  "nombre": "Administrador",
  "correo": "admin@synergy.com",
  "rol": "ADMINISTRADOR",
  "estado": "ACTIVO",
  "activo": true,
  "privilegios": ["65e8f...", "65e9f...", ...],
  "proyectos": ["65e8f...", ...],
  "tareasAsignadas": [],
  "fechaCreacion": "2024-03-31T10:00:00",
  "ultimoAcceso": "2024-03-31T12:30:00"
}
```

### 7. Crear Nuevo Usuario

**Request:**
```http
POST /api/admin/usuarios HTTP/1.1
Host: localhost:8081
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...

{
  "nombre": "Nuevo Participante",
  "correo": "participante@synergy.com",
  "contrasena": "Password123",
  "rol": "PARTICIPANTE"
}
```

**Response (201 Created):**
```json
{
  "id": "65e8f3c4a1b2c3d4e5f6g7i0",
  "nombre": "Nuevo Participante",
  "correo": "participante@synergy.com",
  "rol": "PARTICIPANTE",
  "estado": "ACTIVO",
  "activo": true,
  "fechaCreacion": "2024-03-31T14:00:00"
}
```

### 8. Actualizar Usuario

**Request:**
```http
PUT /api/admin/usuarios/65e8f3c4a1b2c3d4e5f6g7i0 HTTP/1.1
Host: localhost:8081
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...

{
  "nombre": "Participante Actualizado",
  "correo": "participante_nuevo@synergy.com",
  "estado": "ACTIVO",
  "activo": true
}
```

**Response (200 OK):**
```json
{
  "id": "65e8f3c4a1b2c3d4e5f6g7i0",
  "nombre": "Participante Actualizado",
  "correo": "participante_nuevo@synergy.com",
  "rol": "PARTICIPANTE",
  "estado": "ACTIVO",
  "activo": true
}
```

### 9. Cambiar Rol de Usuario

**Request:**
```http
PUT /api/admin/usuarios/65e8f3c4a1b2c3d4e5f6g7i0/rol HTTP/1.1
Host: localhost:8081
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...

{
  "rol": "LIDER"
}
```

**Response (200 OK):**
```json
"Rol actualizado exitosamente"
```

### 10. Desactivar Usuario

**Request:**
```http
PUT /api/admin/usuarios/65e8f3c4a1b2c3d4e5f6g7i0/desactivar HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response (200 OK):**
```json
"Usuario desactivado"
```

### 11. Activar Usuario

**Request:**
```http
PUT /api/admin/usuarios/65e8f3c4a1b2c3d4e5f6g7i0/activar HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response (200 OK):**
```json
"Usuario activado"
```

### 12. Eliminar Usuario

**Request:**
```http
DELETE /api/admin/usuarios/65e8f3c4a1b2c3d4e5f6g7i0 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response (200 OK):**
```json
"Usuario eliminado"
```

### 13. Listar Usuarios por Rol

**Request:**
```http
GET /api/admin/usuarios/rol/LIDER HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response (200 OK):**
```json
[
  {
    "id": "65e8f3c4a1b2c3d4e5f6g7h9",
    "nombre": "Líder de Proyecto",
    "correo": "lider@synergy.com",
    "rol": "LIDER",
    "estado": "ACTIVO",
    "activo": true
  }
]
```

### 14. Listar Usuarios Activos

**Request:**
```http
GET /api/admin/usuarios/estado/activos HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response (200 OK):**
```json
[
  { ... },
  { ... }
]
```

---

## 🎭 Gestión de Roles y Privilegios

### 15. Listar Todos los Roles

**Request:**
```http
GET /api/admin/roles-privilegios/roles HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response (200 OK):**
```json
[
  {
    "id": "66f8f3c4a1b2c3d4e5f6g7h1",
    "nombre": "ADMINISTRADOR",
    "descripcion": "Acceso total al sistema",
    "privilegios": ["65e8f...", "65e9f...", ...],
    "activo": true,
    "fechaCreacion": "2024-03-31T10:00:00"
  },
  {
    "id": "66f8f3c4a1b2c3d4e5f6g7h2",
    "nombre": "LIDER",
    "descripcion": "Líder de proyecto",
    "privilegios": ["65e8f...", ...],
    "activo": true,
    "fechaCreacion": "2024-03-31T10:05:00"
  }
]
```

### 16. Crear Nuevo Rol

**Request:**
```http
POST /api/admin/roles-privilegios/roles HTTP/1.1
Host: localhost:8081
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...

{
  "nombre": "REVISOR",
  "descripcion": "Revisor de entregas y tareas",
  "activo": true,
  "privilegios": ["65e8f3c4a1b2c3d4e5f6g7h3", "65e8f3c4a1b2c3d4e5f6g7h4"]
}
```

**Response (201 Created):**
```json
{
  "id": "66f8f3c4a1b2c3d4e5f6g7h3",
  "nombre": "REVISOR",
  "descripcion": "Revisor de entregas y tareas",
  "privilegios": ["65e8f3c4a1b2c3d4e5f6g7h3", "65e8f3c4a1b2c3d4e5f6g7h4"],
  "activo": true,
  "fechaCreacion": "2024-03-31T14:00:00"
}
```

### 17. Listar Todos los Privilegios

**Request:**
```http
GET /api/admin/roles-privilegios/privilegios HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response (200 OK):**
```json
[
  {
    "id": "65e8f3c4a1b2c3d4e5f6g7h3",
    "nombre": "CREAR_PROYECTO",
    "descripcion": "Crear nuevos proyectos",
    "recurso": "PROYECTO",
    "accion": "CREATE",
    "activo": true,
    "fechaCreacion": "2024-03-31T10:00:00"
  },
  { ... }
]
```

### 18. Agregar Privilegio a Rol

**Request:**
```http
POST /api/admin/roles-privilegios/roles/66f8f3c4a1b2c3d4e5f6g7h3/privilegios/65e8f3c4a1b2c3d4e5f6g7h3 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response (200 OK):**
```json
"Privilegio agregado al rol"
```

---

## 📊 Códigos de Error

### Error 400 - Bad Request
```json
{
  "timestamp": "2024-03-31T14:30:00",
  "status": 400,
  "error": "Error en la solicitud",
  "message": "El correo ya está registrado",
  "path": "/api/admin/usuarios"
}
```

### Error 401 - Unauthorized
```json
{
  "timestamp": "2024-03-31T14:30:00",
  "status": 401,
  "error": "Credenciales inválidas",
  "message": "El correo o la contraseña son incorrectos",
  "path": "/api/auth/login"
}
```

### Error 403 - Forbidden
```json
{
  "timestamp": "2024-03-31T14:30:00",
  "status": 403,
  "error": "Acceso denegado",
  "message": "No tienes permisos para acceder a este recurso",
  "path": "/api/admin/usuarios"
}
```

### Error 404 - Not Found
```json
{
  "timestamp": "2024-03-31T14:30:00",
  "status": 404,
  "error": "Usuario no encontrado",
  "message": "Usuario no encontrado",
  "path": "/api/admin/usuarios/65e8f3c4a1b2c3d4e5f6g7i1"
}
```

---

## 🧪 Testing con cURL

```bash
# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo":"admin@synergy.com","contrasena":"123456"}'

# Listar usuarios (con token)
curl -X GET http://localhost:8081/api/admin/usuarios \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."

# Crear usuario
curl -X POST http://localhost:8081/api/admin/usuarios \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -d '{
    "nombre":"Nuevo Usuario",
    "correo":"nuevo@synergy.com",
    "contrasena":"Pass123",
    "rol":"PARTICIPANTE"
  }'
```

---

**Última actualización**: Marzo 2026
