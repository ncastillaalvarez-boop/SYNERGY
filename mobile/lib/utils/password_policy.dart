/// Mismas reglas que el servidor (Security/PoliticaContrasena.java) para las contraseñas nuevas.
class PasswordPolicy {
  PasswordPolicy._();

  static const int minimo = 8;
  static const String descripcion = 'Mínimo 8 caracteres, con mayúscula, minúscula y número.';

  /// Devuelve el problema o null si la contraseña sirve.
  static String? validar(String? value) {
    final v = value ?? '';
    if (v.length < minimo) return 'Mínimo $minimo caracteres';
    if (v.length > 72) return 'Máximo 72 caracteres';
    if (!RegExp(r'[A-ZÁÉÍÓÚÑ]').hasMatch(v)) return 'Incluye al menos una mayúscula';
    if (!RegExp(r'[a-záéíóúñ]').hasMatch(v)) return 'Incluye al menos una minúscula';
    if (!RegExp(r'[0-9]').hasMatch(v)) return 'Incluye al menos un número';
    return null;
  }
}
