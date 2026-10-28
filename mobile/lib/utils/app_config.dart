import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:shared_preferences/shared_preferences.dart';

/// Configuración de la app.
///
/// La dirección del servidor se puede cambiar desde la pantalla de inicio de sesión
/// (botón "Servidor"). Así funciona igual en el emulador y en un teléfono real:
///  - Emulador de Android: http://10.0.2.2:8080/api  (10.0.2.2 es el PC desde el emulador)
///  - Teléfono real:       http://IP-DEL-PC:8080/api (misma red Wi-Fi que el PC)
class AppConfig {
  AppConfig._();

  static const String appName = 'Synergy';

  /// Valor inicial. También se puede fijar al compilar:
  /// flutter run --dart-define=API_BASE_URL=http://192.168.1.10:8080/api
  static const String _apiBaseUrlCompilada = String.fromEnvironment('API_BASE_URL');

  /// Emulador de Android: 10.0.2.2 (es el PC). Navegador (flutter run -d edge): localhost.
  static String get defaultApiBaseUrl => _apiBaseUrlCompilada.isNotEmpty
      ? _apiBaseUrlCompilada
      : (kIsWeb ? 'http://localhost:8080/api' : 'http://10.0.2.2:8080/api');

  /// Tiempo máximo de espera de cada petición.
  static const Duration requestTimeout = Duration(seconds: 15);

  static const String _prefsKey = 'synergy_api_base_url';
  static String _apiBaseUrl = defaultApiBaseUrl;

  static String get apiBaseUrl => _apiBaseUrl;

  /// Carga la dirección guardada (se llama al abrir la app).
  static Future<void> load() async {
    try {
      final saved = (await SharedPreferences.getInstance()).getString(_prefsKey);
      if (saved != null && saved.isNotEmpty) _apiBaseUrl = saved;
    } catch (_) {
      _apiBaseUrl = defaultApiBaseUrl;
    }
  }

  /// Guarda una nueva dirección. Acepta "192.168.1.10", "192.168.1.10:8080" o la URL completa.
  static Future<String> setApiBaseUrl(String value) async {
    final url = normalizar(value);
    _apiBaseUrl = url;
    final prefs = await SharedPreferences.getInstance();
    if (url == defaultApiBaseUrl) {
      await prefs.remove(_prefsKey);
    } else {
      await prefs.setString(_prefsKey, url);
    }
    return url;
  }

  /// Completa lo que falte: el protocolo, el puerto y la ruta /api.
  /// Sin protocolo: una IP o localhost usan http://...:8080 (servidor en el PC);
  /// un dominio (por ejemplo synergy-expoferia.onrender.com) usa https://.
  static String normalizar(String value) {
    var url = value.trim();
    if (url.isEmpty) return defaultApiBaseUrl;
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      final host = url.split(RegExp(r'[:/]')).first;
      final esLocal = host == 'localhost' || RegExp(r'^\d{1,3}(\.\d{1,3}){3}$').hasMatch(host);
      url = '${esLocal ? 'http' : 'https'}://$url';
    }
    while (url.endsWith('/')) {
      url = url.substring(0, url.length - 1);
    }
    final uri = Uri.tryParse(url);
    if (uri == null || uri.host.isEmpty) return defaultApiBaseUrl;
    final puerto = uri.hasPort ? uri.port : (uri.scheme == 'https' ? 443 : 8080);
    var ruta = uri.path;
    if (!ruta.endsWith('/api')) ruta = '$ruta/api';
    return Uri(scheme: uri.scheme, host: uri.host, port: puerto, path: ruta).toString();
  }
}
