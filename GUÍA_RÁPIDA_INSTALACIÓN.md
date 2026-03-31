# 🚀 Guía Rápida de Instalación - SYNERGY MongoDB + Security

## 📋 Pre-requisitos

- Java 21 o superior
- Maven 3.8+
- MongoDB (local o remoto)
- Git (opcional)

## ⚡ Instalación Rápida (5 minutos)

### 1️⃣ Verificar MongoDB

```bash
# Conectarse a MongoDB
mongosh

# Seleccionar base de datos
use synergy

# Crear índices (opcional)
load("synergy_mongo_setup.js")

# Salir
exit
```

### 2️⃣ Configurar application.properties

Editar `src/main/resources/application.properties`:

```properties
# Cambiar según tu MongoDB
spring.data.mongodb.uri=mongodb://usuario:contraseña@host:puerto/synergy
spring.data.mongodb.database=synergy

# Si es local:
# spring.data.mongodb.uri=mongodb://localhost:27017/synergy
```

### 3️⃣ Compilar Proyecto

```bash
mvn clean install -DskipTests
```

### 4️⃣ Ejecutar Aplicación

```bash
mvn spring-boot:run
```

**La app estará en:** `http://localhost:8081`

### 5️⃣ Probar Autenticación

```bash
# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo":"admin@synergy.com","contrasena":"123456"}'
```

**Respuesta:** Token JWT ✅

---

## 👥 Usuarios por Defecto

| Email | Contraseña | Rol |
|-------|-----------|-----|
| admin@synergy.com | 123456 | ADMINISTRADOR |
| lider@synergy.com | 123456 | LIDER |
| usuario@synergy.com | 123456 | PARTICIPANTE |

---

## 📝 Cambios Desde Migración

### ✅ Lo Nuevo

1. **MongoDB** en lugar de MySQL
2. **JWT** para autenticación sin estado
3. **Spring Security** con roles y privilegios
4. **Encriptación BCrypt** de contraseñas
5. **Validación de acceso** por endpoint
6. **Índices MongoDB** optimizados
7. **DataInitializer** para datos de ejemplo

### 🔄 Lo que Cambió

- IDs: `Long` → `String` (ObjectId)
- Relaciones JPA → Referencias por ID
- Entidades JPA → Documentos MongoDB
- Repositorios JPA → Repositorios MongoDB

### ❌ Lo que se Removió

- MySQL
- Sesiones HTTP tradicionales (ahora JWT)
- Queries JPQL

---

## 📚 Documentación Completa

- [MIGRACIÓN_MONGODB_SEGURIDAD.md](./MIGRACIÓN_MONGODB_SEGURIDAD.md) - Detalles técnicos
- [API_ENDPOINTS.md](./API_ENDPOINTS.md) - Ejemplos de API

---

## 🛠️ Troubleshooting

### Error: "Connection refused: localhost:27017"
```bash
# Iniciar MongoDB
mongod

# O en Windows
net start MongoDB
```

### Error: "Unknown host: mongodb.example.com"
- Verificar URI en `application.properties`
- Reemplazar con `mongodb://localhost:27017/synergy`

### Error: 401 Unauthorized
- Token expirado (24h)
- Hacer login de nuevo
- Verificar header: `Authorization: Bearer {token}`

### La app no inicia
```bash
mvn clean install -X
mvn spring-boot:run -X
```

---

## 🔑 JWT Token Expiration

El token JWT expira en **24 horas** por defecto.

Para cambiar en `application.properties`:
```properties
app.jwtExpirationMs=3600000  # 1 hora en milisegundos
```

---

## 💾 Respaldo de Datos MongoDB

```bash
# Exportar base de datos
mongodump --db synergy --out ./backup

# Importar base de datos
mongorestore ./backup/synergy
```

---

## 📞 Soporte

Para problemas:

1. Revisar logs: `mvn spring-boot:run | grep ERROR`
2. Revisar [GlobalExceptionHandler.java](./src/main/java/com/proaula/spring/synergy/Config/GlobalExceptionHandler.java)
3. Consultar [API_ENDPOINTS.md](./API_ENDPOINTS.md)

---

**¡Listo! SYNERGY con seguridad está operativo** 🎉
