# Manual de usuario de Synergy

Synergy organiza proyectos ambientales de Cartagena: el **administrador** aprueba propuestas, el **líder** organiza su equipo y el **voluntario** participa y entrega evidencias. Se usa en el navegador (<http://localhost:8080>) o en la app Android.

## Invitado (sin cuenta)

1. En la página de inicio, *Ver proyectos* muestra los proyectos activos con buscador y filtros por categoría y barrio.
2. *Participar* pide crear una cuenta; al registrarse, *Volver* regresa a la misma lista.

## Voluntario

| Quiero… | Dónde |
|---|---|
| Ver mis próximas tareas y el calendario del mes | *Inicio* |
| Unirme a un proyecto | *Explorar proyectos* → *Participar* |
| Entregar una tarea (PDF, imagen, Office, TXT, CSV o ZIP, máx. 10 MB) | *Mis tareas* → *Entregar* |
| Ver si el líder aprobó o rechazó mi entrega y su comentario | *Inicio* → *Tus últimas entregas* |
| Comentar en un proyecto | *Mis proyectos* → *Comentarios* |
| Ver mis insignias (primera entrega, puntual, en equipo…) | *Inicio* → *Tus insignias* |
| Descargar el certificado de un proyecto completado | *Inicio* → *Certificados* → *Descargar* (se guarda como PDF desde el navegador) |
| Proponer un proyecto | *Proponer proyecto* (lo revisa el administrador) |
| Ver avisos (tareas nuevas, revisiones, mensajes del líder) | Campana *Notificaciones* |

## Líder de proyecto

| Quiero… | Dónde |
|---|---|
| Ver qué proyectos necesitan atención | *Panel* → *Avance por proyecto* (ordenado por riesgo; la marca indica dónde debería ir según las fechas) |
| Crear un proyecto | *Nuevo proyecto* (nombre, descripción, categoría y fechas obligatorios; avisa si el nombre ya existe) |
| Reutilizar un proyecto anterior | Detalle → *Usar como plantilla* (copia datos y tareas, sin voluntarios) |
| Asignar tareas | *Proyectos y tareas* → *Nueva tarea* (la fecha debe estar dentro del proyecto) |
| Mover tareas entre Pendiente / En progreso / Completada | Detalle → *Tablero de tareas* (arrastrar, o el selector en el celular) |
| Revisar entregas | *Entregas* → pestaña *Por revisar* → *Aprobar* / *Rechazar* con comentario |
| Saber el riesgo y qué hacer | Detalle → panel de riesgo: nivel, lo que más influye, sugerencia, fecha estimada de fin y mini gráfica |
| Probar acciones antes de hacerlas | Detalle → *¿Qué pasa si…?* |
| Escribir a todo el equipo | Detalle → *Mensaje al equipo* (llega como notificación) |
| Recordar a quienes no han participado | Detalle → *Enviarles un recordatorio* |
| Invitar voluntarios con experiencia en la categoría | Detalle → *Voluntarios recomendados* → *Invitar* |
| Reporte para la entidad o el profesor | Detalle → *Reporte* → *Imprimir o guardar como PDF* |

## Administrador

| Quiero… | Dónde |
|---|---|
| Aprobar o rechazar propuestas | *Proyectos* → filtro *Por aprobar* (aviso si alguna lleva más de 3 días) |
| Ver gráficas por estado, riesgo, categoría y mes | *Proyectos* (arriba). El filtro *Creados desde / hasta* cambia las gráficas |
| Ver el mapa de proyectos activos por barrio | *Proyectos* → *Proyectos activos por barrio* |
| Exportar a Excel | *Exportar a Excel* en *Proyectos* (con los filtros actuales, p. ej. solo riesgo alto) o en *Usuarios* |
| Consultar el modelo predictivo | *Modelo de riesgo*: métricas, matriz de confusión, dataset CSV/ARFF y *Recalcular* |
| Gestionar cuentas | *Usuarios*: cambiar rol, activar/desactivar, *contraseña temporal* si alguien la olvidó |
| Ver quién hizo qué | *Auditoría* |

## App móvil

1. Instalar el APK o `flutter run` (ver README, sección 9).
2. En el inicio de sesión, *Servidor* → escribir la IP del PC (o `10.0.2.2:8080` en el emulador) → *Probar conexión*.
3. Mismas cuentas que la web. La campana muestra las notificaciones; deslizar hacia abajo recarga. Si se pierde la conexión, la lista de proyectos muestra la última copia guardada.
