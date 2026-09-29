# Informe del rediseño CSS de Synergy

Fecha: 28/09/2026

## 1. Qué se hizo

Se reescribió todo el estilo de la web (Spring Boot + Thymeleaf) con **la misma paleta**:
fondo `#0B0E14`, superficies `#111827`, verde esmeralda `#10B981 / #34D399 / #059669`,
textos slate (`#F8FAFC`, `#94A3B8`…) y los acentos azul, ámbar y rojo que ya existían.

### Nueva organización de `static/css`

| Archivo | Para qué sirve |
|---|---|
| `core.css` | Sistema de diseño compartido: variables (tokens), reset, tipografía, botones, formularios, tablas, badges, avatares, alertas, toasts, estados vacíos, animaciones y accesibilidad. |
| `landing.css` | Página de inicio (`index.html`). |
| `auth.css` | Login, Registro, Registro de líder y Selección de rol. |
| `dashboard.css` | Todos los paneles internos (admin, líder, participante, historial, jerarquía, credenciales…). |
| `error.css` | `Error.html`, `error/403.html`, `error/404.html`. |

`static/js/script.js` ahora es un script global de mejoras (FAQ, menú móvil, animaciones al hacer
scroll, mostrar/ocultar contraseña, nombre del archivo subido, toasts). **Si no carga, todo sigue funcionando.**

### Iconos
Se usa **Font Awesome 6.5.1** desde cdnjs (se carga en el `<head>` de cada plantilla).
Se reemplazaron todos los emojis por iconos vectoriales coherentes.

### Animaciones (seguras)
- Entrada suave de tarjetas y paneles, aparición escalonada de listas.
- Revelado al hacer scroll en la página de inicio (solo si JS está activo; sin JS todo es visible).
- Hover con elevación, brillo en botón primario, luz que sigue al cursor en las tarjetas de características.
- FAQ con acordeón animado, fondo ambiental con manchas de luz lentas.
- Todo se desactiva automáticamente si el usuario tiene activado "reducir movimiento" en su sistema.

## 2. Problemas encontrados y corregidos

1. **9 archivos CSS idénticos** (`Administrador_Dashboard.css`, `Usuarios_Dashboard.css`, `Admin_Usuarios_Dashboard.css`,
   `Tareas_Proyecto.css`, `Participacion_Proyecto.css`, `Mis_Proyectos_Participacion.css`, `Lista_Proyectos.css`,
   `Lider_Asignar_Tareas.css`, `Comentarios_Proyectos.css`) → unificados en `dashboard.css`.
2. **4 archivos CSS de error idénticos** (`error.css`, `ErrorM.css`, `merror.css`, `rror.css`) → un solo `error.css`.
3. **`Lider_Dashboard.css` no existía** pero lo pedían `Lider_Crear_Proyecto.html` y `Lider_Editar_Proyecto.html`.
4. **`Index.css` no tenía estilos** para Beneficios, Equipo ni Preguntas Frecuentes: el FAQ se veía siempre abierto y sin formato.
5. **La sección "Sobre Synergy" estaba mal maquetada**: el título y el contenido quedaban lado a lado.
6. **El hero de inicio no ocupaba todo el ancho** (heredaba `max-width` y `padding` de `section`).
7. **`Lider_Proyecto_Dashboard.html` tenía 3 `</div>` sin cerrar** (HTML roto).
8. **Páginas con otra paleta** (morado/rosa/blanco): `Historial.html`, `jerarquia.html`, `usuario_credenciales.html`,
   `registro_proyectos.html`, `Lider_Asignar_Tareas.html`, `Lider_Detalle_Proyecto.html`, `Participacion_Proyecto.html`,
   y los 403/404 con Tailwind → todas unificadas al tema oscuro esmeralda.
9. **Estilos en línea con colores claros** que chocaban con el tema oscuro (alertas, selects, botones azules `#007bff`, etc.) → eliminados.
10. **Imágenes rotas**: `Comentarios_Proyectos.html`, `Mis_Proyectos_Participacion.html` y `Participacion_Proyecto.html`
    usaban `src="Logo.png"` (ruta relativa inexistente).
11. **Etiquetas flotantes** del login/registro se superponían al texto en correos incompletos (usaban `:valid`) → ahora usan `:placeholder-shown`.
12. **`showToast()`** en `Participacion_Proyecto_Usuario.html` buscaba un `#toast` que no existía; el toast no tenía CSS.
13. Clases sin estilo: `.steps`, `.info-box`, `.task-info`, `.btn-participar`, `.btn-cancelar`, `.form-group`, tablas sin clase.
14. CSS inválido: `title: attr(data-nombre)` dentro de `.avatar`.
15. `Historial.html`, `jerarquia.html`, `Lider_Tareas_Recibidas.html`, `Lider_Usuarios_Proyecto.html` no tenían `viewport` (se veían mal en móvil) ni `lang`.
16. La URL de `th:action` en `Participacion_Proyecto_Salir.html` tenía un salto de línea dentro de `@{...}`; se dejó en una sola línea.
17. `seleccionar-rol.html`: el cuadro de info ahora también funciona con teclado (focus/blur).

## 3. Cambios pequeños de comportamiento (revisar)

- Se añadió confirmación antes de **eliminar usuario** (Admin) y antes de **salir de un proyecto**.
- Se añadieron mensajes de "lista vacía" en tablas que no los tenían.
- `Registro.html`: el botón volver ahora va a `/seleccionar-rol` (antes a `/`), igual que el registro de líder. También muestra `${error}` si el controlador lo envía.
- `registro_proyectos.html`: se añadió botón volver a `/proyectos/dashboard`.
- Botones con `onclick="location.href=..."` se cambiaron por enlaces `<a th:href>` con el mismo destino.
- **No se cambió ninguna expresión Thymeleaf** (`th:action`, `th:field`, `th:each`, `th:if`…) ni ninguna ruta.

## 4. Pendientes que NO se tocaron (recomendado revisar)

1. **Mayúsculas en la carpeta de imágenes**: la carpeta se llama `static/IMG/` pero las plantillas piden `/img/Logo.png`.
   En Windows funciona, pero **al empaquetar en .jar o desplegar en Linux el logo se romperá**. Solución: renombrar la carpeta a `img`
   (en dos pasos: `IMG` → `img_tmp` → `img`) y verificar que `SecurityConfig` permita `/img/**`.
2. **Archivos CSS obsoletos**: ya no los usa ninguna plantilla y se pueden borrar:
   `Admin_Usuarios_Dashboard.css, Administrador_Dashboard.css, Comentarios_Proyectos.css, ErrorM.css, merror.css, rror.css,
   Index.css, Lider_Asignar_Tareas.css, Lista_Proyectos.css, Login.css, Mis_Proyectos_Participacion.css, Participacion_Proyecto.css,
   registro_proyecto.css, Registro.css, RegistroLider.css, SeleccionarRol.css, Tareas_Proyecto.css, Usuarios_Dashboard.css`.
   También `update_css.py` (script viejo que sobrescribía esos 9 CSS) y `style/` + `views/` (plantilla de Live Server sin uso).
3. **Rutas a verificar** en los controladores: `/inicio` (Mis_Proyectos_Participacion), `/menu` (Participacion_Proyecto),
   `/proyectos` (Historial, jerarquía), `/index`. Si no existen darán 404.
4. `application.properties`: `server.servlet.session.cookie.secure=true` con SSL desactivado hace que el navegador no envíe la
   cookie de sesión por `http://` (puede romper el login en local). Además la clave JWT está escrita en el archivo; mejor moverla a una variable de entorno.
5. `Comentarios_Proyectos.html` usa `#dates.format(...)`; si `fecha` es `LocalDateTime` debe ser `#temporals.format(...)`.
6. Font Awesome y la fuente Inter se cargan desde internet; sin conexión la página funciona pero sin iconos.

## 5. Actualización 2: por qué la página salía sin estilos

Spring sirve la web desde `target/classes`. El IDE solo copió allí los archivos **modificados**
(`error.css`, `script.js`, las plantillas), pero **no los CSS nuevos** (`core.css`, `landing.css`,
`auth.css`, `dashboard.css`), así que el HTML nuevo pedía hojas de estilo que no existían.

Corrección aplicada:
- Los CSS/JS nuevos se copiaron también a `target/classes/static`.
- Cada plantilla tiene ahora una ruta de respaldo (`href="../static/css/..."` + `th:href`), así que también
  se ve con estilos si abres el HTML con Live Server o directamente desde la carpeta.
- Se añadió favicon con el logo.

Mejoras visuales nuevas: hero de dos columnas con vista previa animada del panel (inclinación 3D con el
cursor, tarjetas flotantes, barra de progreso y anillo animados), palabra rotativa, franja de desplazamiento
continuo, cifras con contador animado, barra de progreso de lectura, botón volver arriba, borde luminoso
giratorio en tarjetas, efecto de onda al hacer clic y línea de luz animada en los paneles.

## 6. Cómo probar

Las plantillas se sirven desde `target/classes`, así que recompila antes de probar:

```
mvnw clean spring-boot:run
```

Los archivos originales quedaron respaldados en `_respaldo_diseno_original/`.
