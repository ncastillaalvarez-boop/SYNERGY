import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

/// Copia local de las últimas respuestas de la API (modo sin conexión).
/// Funciona en Android, iOS y web porque usa SharedPreferences.
class OfflineCache {
  OfflineCache._();

  static const _prefijo = 'synergy_cache_';

  /// true si la última lectura vino de la copia local (para mostrar el aviso "sin conexión").
  static bool ultimaLecturaDesdeCache = false;

  static Future<void> guardar(String clave, dynamic json) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('$_prefijo$clave', jsonEncode(json));
      await prefs.setString('$_prefijo${clave}_fecha', DateTime.now().toIso8601String());
    } catch (_) {
      // la copia es opcional
    }
  }

  static Future<dynamic> leer(String clave) async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString('$_prefijo$clave');
    return raw == null ? null : jsonDecode(raw);
  }

  static Future<DateTime?> fecha(String clave) async {
    final raw = (await SharedPreferences.getInstance()).getString('$_prefijo${clave}_fecha');
    return raw == null ? null : DateTime.tryParse(raw);
  }

  /// Borra la copia (al cerrar sesión, para que otro usuario no vea datos ajenos).
  static Future<void> limpiar() async {
    final prefs = await SharedPreferences.getInstance();
    for (final k in prefs.getKeys().where((k) => k.startsWith(_prefijo)).toList()) {
      await prefs.remove(k);
    }
  }
}
