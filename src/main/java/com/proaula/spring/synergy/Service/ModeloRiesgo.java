// Generado automáticamente desde Google Colab con m2cgen. No editar a mano.
// Modelo: Árbol de decisión. Clases: 0=Bajo, 1=Medio, 2=Alto.
// Entrada (en este orden): dias_retraso, pct_tareas_pendientes, participantes, tasa_participacion, duracion_dias, avance_real, avance_esperado, brecha_avance, actividades, cat_Limpieza de playas, cat_Reforestación, cat_Manglares y ciénagas, cat_Reciclaje y residuos, cat_Huertas comunitarias, cat_Educación ambiental, cat_Fauna y aves, cat_Agua y saneamiento, cat_Energía limpia, cat_Espacios verdes, cat_Calidad del aire, cat_Canales y caños
package com.proaula.spring.synergy.Service;
public class ModeloRiesgo {
    public static double[] score(double[] input) {
        double[] var0;
        if (input[0] <= 1.5) {
            if (input[3] <= 0.6834999918937683) {
                if (input[3] <= 0.38750000298023224) {
                    if (input[4] <= 127.5) {
                        if (input[4] <= 98.0) {
                            var0 = new double[] {0.1086402266288952, 0.11416430594900852, 0.7771954674220962};
                        } else {
                            var0 = new double[] {0.0, 0.514003294892916, 0.48599670510708404};
                        }
                    } else {
                        var0 = new double[] {0.0, 0.0, 1.0};
                    }
                } else {
                    if (input[10] <= 0.5) {
                        if (input[4] <= 109.5) {
                            var0 = new double[] {0.030480050866316968, 0.896836750914004, 0.07268319821967892};
                        } else {
                            var0 = new double[] {0.28578525740545246, 0.6006334223436626, 0.11358132025088491};
                        }
                    } else {
                        var0 = new double[] {0.6707896051974014, 0.10069965017491254, 0.22851074462768617};
                    }
                }
            } else {
                if (input[7] <= -0.06799999997019768) {
                    if (input[6] <= 0.49650000035762787) {
                        if (input[3] <= 0.7750000059604645) {
                            var0 = new double[] {0.2547643761677393, 0.6692962424745693, 0.07593938135769152};
                        } else {
                            var0 = new double[] {1.0, 0.0, 0.0};
                        }
                    } else {
                        if (input[5] <= 0.2680000066757202) {
                            var0 = new double[] {0.3846378870826193, 0.5389276609971164, 0.0764344519202641};
                        } else {
                            var0 = new double[] {0.09434774586382925, 0.7931607109908357, 0.11249154314533488};
                        }
                    }
                } else {
                    if (input[7] <= 0.1380000039935112) {
                        if (input[4] <= 126.0) {
                            var0 = new double[] {0.8696046530084693, 0.10965862065055948, 0.020736726340971186};
                        } else {
                            var0 = new double[] {0.45801678665030027, 0.4813057758020104, 0.06067743754768935};
                        }
                    } else {
                        if (input[2] <= 6.5) {
                            var0 = new double[] {0.8756628460694919, 0.09686190268743708, 0.027475251243071097};
                        } else {
                            var0 = new double[] {1.0, 0.0, 0.0};
                        }
                    }
                }
            }
        } else {
            if (input[0] <= 10.5) {
                if (input[8] <= 5.5) {
                    if (input[7] <= -0.35499998927116394) {
                        if (input[4] <= 111.5) {
                            var0 = new double[] {0.0, 0.0, 1.0};
                        } else {
                            var0 = new double[] {0.19330855018587365, 0.0, 0.8066914498141264};
                        }
                    } else {
                        if (input[8] <= 3.5) {
                            var0 = new double[] {0.12480676918070135, 0.13115287608819462, 0.744040354731104};
                        } else {
                            var0 = new double[] {0.0, 0.46846846846846846, 0.5315315315315315};
                        }
                    }
                } else {
                    if (input[5] <= 0.3540000021457672) {
                        if (input[4] <= 104.0) {
                            var0 = new double[] {0.13483936184239445, 0.1416956005801433, 0.7234650375774623};
                        } else {
                            var0 = new double[] {0.0, 0.7460545193687231, 0.25394548063127687};
                        }
                    } else {
                        var0 = new double[] {0.23546868604175195, 0.5773638968481376, 0.18716741711011053};
                    }
                }
            } else {
                if (input[16] <= 0.5) {
                    var0 = new double[] {0.0, 0.0, 1.0};
                } else {
                    var0 = new double[] {0.0, 0.16377952755905514, 0.8362204724409449};
                }
            }
        }
        return var0;
    }
}
