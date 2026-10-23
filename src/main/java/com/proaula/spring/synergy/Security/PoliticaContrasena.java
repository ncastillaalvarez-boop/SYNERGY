package com.proaula.spring.synergy.Security;

/**
 * Reglas para las contraseñas NUEVAS (registro, cambio de contraseña, cuentas creadas por el
 * administrador). Las cuentas existentes con contraseñas más cortas pueden seguir entrando.
 *
 * Mínimo 8 caracteres, al menos una mayúscula, una minúscula y un número.
 * Las mismas reglas las muestra el indicador de fortaleza de los formularios (script.js).
 */
public final class PoliticaContrasena {

    public static final int MINIMO = 8;
    public static final int MAXIMO = 72; // BCrypt ignora lo que pase de 72 bytes

    public static final String DESCRIPCION =
            "Mínimo " + MINIMO + " caracteres, con al menos una mayúscula, una minúscula y un número.";

    private PoliticaContrasena() {
    }

    /** Devuelve el motivo por el que no sirve, o null si la contraseña cumple las reglas. */
    public static String validar(String contrasena) {
        if (contrasena == null || contrasena.length() < MINIMO) {
            return "La contraseña debe tener al menos " + MINIMO + " caracteres.";
        }
        if (contrasena.length() > MAXIMO) {
            return "La contraseña no puede tener más de " + MAXIMO + " caracteres.";
        }
        if (contrasena.chars().noneMatch(Character::isUpperCase)) {
            return "La contraseña debe tener al menos una letra mayúscula.";
        }
        if (contrasena.chars().noneMatch(Character::isLowerCase)) {
            return "La contraseña debe tener al menos una letra minúscula.";
        }
        if (contrasena.chars().noneMatch(Character::isDigit)) {
            return "La contraseña debe tener al menos un número.";
        }
        if (contrasena.isBlank()) {
            return "La contraseña no puede estar en blanco.";
        }
        return null;
    }

    public static boolean esValida(String contrasena) {
        return validar(contrasena) == null;
    }
}
