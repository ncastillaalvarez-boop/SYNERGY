import 'dart:async';

import '../models/auth_session.dart';
import '../models/usuario.dart';
import 'api_client.dart';
import 'session_store.dart';

class AuthService {
  AuthService({ApiClient? client, SessionStore? sessionStore})
      : _client = client ?? ApiClient(),
        _sessionStore = sessionStore ?? SessionStore();

  final ApiClient _client;
  final SessionStore _sessionStore;

  Future<AuthSession?> restoreSession() => _sessionStore.read();

  Future<AuthSession> login(String correo, String contrasena) async {
    final json = Map<String, dynamic>.from(await _client.post('/auth/login', body: {
      'correo': correo.trim(),
      'contrasena': contrasena,
    }) as Map);
    final session = AuthSession.fromJson(json);
    await _sessionStore.save(session);
    return session;
  }

  Future<AuthSession> register(Usuario usuario) async {
    final json = Map<String, dynamic>.from(await _client.post('/auth/registrar', body: usuario.toJson()) as Map);
    final session = AuthSession.fromJson(json);
    await _sessionStore.save(session);
    return session;
  }

  Future<void> changePassword({
    required String usuarioId,
    required String actual,
    required String nueva,
  }) async {
    await _client.post('/auth/cambiar-contrasena', body: {
      'usuarioId': usuarioId,
      'contrasenaActual': actual,
      'nuevaContrasena': nueva,
    });
  }

  /// Cierra la sesión en el teléfono SIEMPRE. Avisar al servidor es opcional:
  /// si no hay conexión, antes la excepción impedía salir de la app.
  Future<void> logout() async {
    await _sessionStore.clear();
    try {
      await _client.post('/auth/logout').timeout(const Duration(seconds: 3));
    } catch (_) {
      // sin conexión: el token JWT simplemente deja de usarse
    }
  }
}
