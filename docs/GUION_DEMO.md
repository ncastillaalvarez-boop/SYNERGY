# Guion de la demostración (≈ 10 minutos)

Antes: MongoDB encendido y `mvnw clean spring-boot:run` (o `docker compose up --build`). Tener abiertas dos ventanas (una normal y otra de incógnito) y el emulador con la app.

| Min | Quién | Qué mostrar | Qué decir |
|---|---|---|---|
| 0:00 | — | Página de inicio | El problema: proyectos ambientales de Cartagena que no terminan por falta de seguimiento. |
| 0:45 | Invitado | *Ver proyectos* → filtros → *Participar* → registro → *Volver* | Cualquiera puede explorar; al registrarse vuelve donde estaba. La contraseña exige mayúscula, minúscula y número. |
| 2:00 | Voluntario (`voluntario@synergy.com`) | *Inicio*: próximas tareas, calendario, insignias | Panel pensado para el voluntario. |
| 2:45 | Voluntario | *Mis tareas* → entregar un PDF | Solo acepta tipos permitidos y revisa el contenido real del archivo. |
| 3:30 | Líder (`lider@synergy.com`) | *Panel* → *Avance por proyecto* ordenado por riesgo; campana con la entrega nueva | El líder ve primero lo que necesita atención. |
| 4:15 | Líder | Detalle de un proyecto en riesgo alto: lo que más influye, fecha estimada, mini gráfica | **Modelo predictivo**: no solo dice el nivel, explica por qué. |
| 5:00 | Líder | *¿Qué pasa si…?*: completar 3 tareas y activar 2 voluntarios | El modelo se usa para decidir, no solo para mirar. |
| 5:45 | Líder | Tablero kanban (arrastrar una tarea), *Mensaje al equipo*, *Reporte* PDF | Herramientas del día a día. |
| 6:30 | Administrador (`admin@synergy.com`) | Gráficas, tendencia de 12 meses, mapa, ranking, aviso de propuestas antiguas | Visión de toda la plataforma. |
| 7:30 | Administrador | *Modelo de riesgo*: matriz de confusión y comparación de modelos; *Dataset CSV* | Cómo se entrenó en Colab (pandas, numpy, matplotlib, scikit-learn) y se exportó a Java con m2cgen. |
| 8:30 | App móvil | Login → lista con semáforo → notificaciones → modo sin conexión (apagar el Wi-Fi) | Misma API con JWT y renovación automática del token. |
| 9:15 | — | GitHub Actions en verde, Swagger (`/swagger-ui.html`), Jira | Calidad: pruebas automáticas, documentación de la API y sprints. |

Preguntas probables:

- *¿Por qué árbol de decisión y no random forest?* Se explica regla por regla y se exporta a Java sin librerías; random forest dio 0,733 de exactitud frente a 0,667, y se puede cambiar reentrenando.
- *¿Los datos son reales?* Son simulados con reglas realistas (600 proyectos terminados); el flujo para reentrenar con datos reales está listo (CSV → Colab → `ModeloRiesgo.java`).
- *¿Cómo evitan que el modelo "copie" la regla?* Las variables se miden a la mitad del proyecto y la etiqueta es cómo terminó.
