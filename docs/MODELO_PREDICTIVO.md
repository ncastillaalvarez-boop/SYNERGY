# Documento del modelo predictivo de riesgo (US-21)

## 1. Problema

Muchos proyectos ambientales no cumplen sus objetivos y se nota tarde. El modelo estima, para cada proyecto **activo**, el riesgo de **no cumplir** (Bajo, Medio o Alto) con lo que ha pasado hasta hoy, para que el líder y el administrador actúen a tiempo.

## 2. Variable objetivo (reglas de la propuesta)

| Nivel | Condición (cualquiera) |
|---|---|
| Alto | más de 15 días de retraso, más del 50 % de tareas pendientes o participación menor al 40 % |
| Medio | 5 a 15 días de retraso, 25–50 % de tareas pendientes o participación de 40–70 % |
| Bajo | menos de 5 días de retraso, menos del 25 % pendientes y participación mayor al 70 % |

## 3. Variables de entrada (`RiesgoCalculadora`)

| Variable | Significado |
|---|---|
| dias_retraso | días de la tarea vencida más atrasada (máx. 120) |
| pct_tareas_pendientes | tareas no completadas / tareas del proyecto |
| participantes | voluntarios inscritos (sin el líder) |
| tasa_participacion | voluntarios con al menos una entrega o comentario / inscritos |
| duracion_dias | días entre inicio y fin |
| avance_real | 1 − pct_tareas_pendientes |
| avance_esperado | tiempo transcurrido / duración |
| brecha_avance | avance_real − avance_esperado |
| actividades | entregas + comentarios |
| categoría | 12 columnas 0/1 (una por categoría) |

## 4. Datos

- 600 proyectos **terminados** generados con un "compromiso" oculto por proyecto (`GeneradorDatos`), con tareas, fechas de cumplimiento, entregas y comentarios coherentes.
- **Para que el modelo prediga y no copie la regla:** las variables se miden a la **mitad** de cada proyecto y la etiqueta se calcula con cómo **terminó**.
- Descarga: panel del administrador → *Dataset CSV* / *ARFF (Weka)*. Distribución: Alto 277, Bajo 165, Medio 158.

## 5. Entrenamiento (Google Colab)

Notebook `docs/modelo-predictivo/Synergy_modelo_predictivo.ipynb`:

1. **pandas**: carga, limpieza y descripción del CSV.
2. **matplotlib / numpy**: distribución de clases, histogramas y correlaciones.
3. División 75 / 25 estratificada y validación cruzada de 5 pliegues (F1 macro).
4. Modelos comparados:

| Modelo | F1 macro (CV) | Exactitud prueba |
|---|---|---|
| **Árbol de decisión (elegido)** | 0,670 ± 0,016 | 0,667 |
| Random forest | 0,672 | 0,733 |
| Regresión logística | 0,675 | 0,700 |

Matriz de confusión del árbol (150 de prueba; filas = real, columnas = predicho):

| | Bajo | Medio | Alto |
|---|---|---|---|
| Bajo | 20 | 21 | 0 |
| Medio | 8 | 30 | 2 |
| Alto | 1 | 18 | 50 |

Lo más importante: casi nunca confunde un proyecto de riesgo alto con uno bajo (1 de 69).

Se eligió el árbol porque se puede explicar regla por regla a líderes y profesores y se exporta a Java sin dependencias.

## 6. Exportación a Java

`m2cgen` convierte el árbol en `Service/ModeloRiesgo.java` (`double[] score(double[])` → probabilidades Bajo/Medio/Alto). `prueba_modelo.csv` compara 25 casos: Java y Python dan lo mismo. La ficha (versión, fecha, métricas) está en `Service/ModeloInfo.java` y se ve en *Administración → Modelo de riesgo*.

## 7. Uso en Synergy

- Se recalcula al arrancar, cada noche (2:30 a. m.), cuando cambian las tareas de un proyecto y con *Recalcular*.
- Cada proyecto guarda nivel, probabilidad, motivos, sugerencia, **lo que más influye**, **fecha estimada de fin** y la versión del modelo.
- Historial diario (colección `historial_riesgo`) con mini gráfica y texto del último cambio.
- Aviso al líder y a los administradores cuando un proyecto **pasa** a Alto.
- Simulador "¿qué pasa si…?" para el líder.
- API: `GET /api/proyectos/{id}/riesgo` (app móvil).

## 8. Limitaciones y próximos pasos

- Los datos de entrenamiento son simulados. Cuando haya proyectos reales terminados: descargar el CSV, volver a correr el notebook, reemplazar `ModeloRiesgo.java` y actualizar `ModeloInfo.java`.
- Comparar de nuevo con random forest (mejor exactitud) y revisar la **calibración** (que un 70 % de probabilidad acierte cerca del 70 % de las veces).
- Variables candidatas: tiempo de respuesta a comentarios, entregas rechazadas y tareas reasignadas.
