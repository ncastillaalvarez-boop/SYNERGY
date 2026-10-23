package com.proaula.spring.synergy.Service;

import java.util.List;

/**
 * Ficha del modelo predictivo que está en ModeloRiesgo.java (se muestra en el panel del
 * administrador). Al reentrenar en Colab y reemplazar ModeloRiesgo.java, actualizar también
 * estos valores con lo que imprime el notebook (celdas de evaluación).
 */
public final class ModeloInfo {

    private ModeloInfo() {
    }

    /** Se guarda en cada proyecto evaluado (campo modeloVersion). */
    public static final String VERSION = "1.0";
    public static final String ALGORITMO = "Árbol de decisión (scikit-learn, profundidad máx. 5, hojas ≥ 8, clases balanceadas)";
    public static final String FECHA_ENTRENAMIENTO = "2026-10-21";
    public static final String DATASET = "600 proyectos terminados (450 entrenamiento / 150 prueba)";
    public static final String EXPORTACION = "m2cgen → Java (Service/ModeloRiesgo.java)";

    public static final double F1_MACRO_CV = 0.670;
    public static final double F1_MACRO_CV_DESV = 0.016;
    public static final double EXACTITUD_PRUEBA = 0.667;
    public static final double F1_MACRO_PRUEBA = 0.649;

    /** Clases en el orden de las filas y columnas de la matriz. */
    public static final List<String> CLASES = RiesgoCalculadora.NIVELES;

    /** Matriz de confusión en el conjunto de prueba: filas = nivel real, columnas = nivel predicho. */
    public static final int[][] MATRIZ_CONFUSION = {
        {20, 21, 0},
        {8, 30, 2},
        {1, 18, 50},
    };

    /** Precisión y exhaustividad (recall) por clase en el conjunto de prueba. */
    public static final double[] PRECISION = {0.690, 0.435, 0.962};
    public static final double[] RECALL = {0.488, 0.750, 0.725};

    /** Comparación con los otros modelos probados en el notebook (F1 macro CV, exactitud de prueba). */
    public static final List<String[]> COMPARACION = List.of(
        new String[] {"Árbol de decisión (elegido)", "0,670", "0,667"},
        new String[] {"Random forest", "0,672", "0,733"},
        new String[] {"Regresión logística", "0,675", "0,700"}
    );

    /** Total de filas de una clase real (para colorear la matriz). */
    public static int totalFila(int fila) {
        int t = 0;
        for (int v : MATRIZ_CONFUSION[fila]) {
            t += v;
        }
        return t;
    }

    /** Porcentaje (0-100) de la celda respecto a su fila. */
    public static int porcentajeCelda(int fila, int columna) {
        int total = totalFila(fila);
        return total == 0 ? 0 : Math.round(MATRIZ_CONFUSION[fila][columna] * 100f / total);
    }
}
