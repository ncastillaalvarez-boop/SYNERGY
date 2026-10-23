import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

import '../models/auth_session.dart';
import '../utils/api_exception.dart';
import '../utils/app_config.dart';
import 'session_store.dart';

class ApiClient {
  ApiClient({SessionStore? sessionStore, http.Client? client})
      : _sessionStore = sessionStore ?? SessionStore(),
        _client = client ?? http.Client();

  final SessionStore _sessionStore;
  final http.Client _client;

  Uri _uri(String path) => Uri.parse('${AppConfig.apiBaseUrl}$path');

  /// Login y registro no deben enviar un token viejo guardado en el teléfono.
  bool _esPublica(String path) =>
      path.startsWith('/auth/login') || path.startsWith('/auth/registrar') || path.startsWith('/auth/refresh');

  Future<dynamic> get(String path) => _send(path, (headers) => _client.get(_uri(path), headers: headers));

  Future<dynamic> post(String path, {Object? body}) => _send(
        path,
        (headers) => _client.post(_uri(path), headers: headers, body: body == null ? null : jsonEncode(body)),
      );

  Future<dynamic> put(String path, {Object? body}) => _send(
        path,
        (headers) => _client.put(_uri(path), headers: headers, body: body == null ? null : jsonEncode(body)),
      );

  Future<dynamic> delete(String path) => _send(path, (headers) => _client.delete(_uri(path), headers: headers));

  Future<dynamic> upload(
    String path, {
    required String field,
    required String filename,
    List<int>? bytes,
    String? filePath,
  }) async {
    final request = http.MultipartRequest('POST', _uri(path));
    request.headers.addAll(await _headers(path, jsonContent: false));
    if (bytes != null) {
      request.files.add(http.MultipartFile.fromBytes(field, bytes, filename: filename));
    } else if (filePath != null) {
      request.files.add(await http.MultipartFile.fromPath(field, filePath, filename: filename));
    } else {
      throw const ApiException('No fue posible leer el archivo seleccionado.');
    }
    return _conRed(() async {
      final streamed = await request.send().timeout(const Duration(seconds: 60));
      return _handle(await http.Response.fromStream(streamed));
    });
  }

  Future<dynamic> _send(String path, Future<http.Response> Function(Map<String, String> headers) llamada) async {
    return _conRed(() async {
      var response = await llamada(await _headers(path)).timeout(AppConfig.requestTimeout);
      // Token de acceso vencido: se renueva una vez con el refresh token y se repite la petición
      if (response.statusCode == 401 && !_esPublica(path) && await _renovarToken()) {
        response = await llamada(await _headers(path)).timeout(AppConfig.requestTimeout);
      }
      return _handle(response);
    });
  }

  /// Pide un token nuevo. Devuelve false si no hay refresh token o ya venció (hay que iniciar sesión).
  Future<bool> _renovarToken() async {
    final session = await _sessionStore.read();
    final refresh = session?.refreshToken;
    if (session == null || refresh == null || refresh.isEmpty) return false;
    try {
      final r = await _client
          .post(_uri('/auth/refresh'),
              headers: {'Content-Type': 'application/json; charset=UTF-8', 'Accept': 'application/json'},
              body: jsonEncode({'refreshToken': refresh}))
          .timeout(AppConfig.requestTimeout);
      if (r.statusCode != 200) return false;
      final nueva = AuthSession.fromJson(Map<String, dynamic>.from(jsonDecode(utf8.decode(r.bodyBytes)) as Map));
      if (nueva.token.isEmpty) return false;
      await _sessionStore.save(nueva);
      return true;
    } catch (_) {
      return false;
    }
  }

  /// Convierte los fallos de red (servidor apagado, IP equivocada, sin Wi-Fi) en un mensaje claro.
  /// Antes estos errores no se mostraban y parecía que el botón no hacía nada.
  Future<dynamic> _conRed(Future<dynamic> Function() accion) async {
    try {
      return await accion();
    } on ApiException {
      rethrow;
    } on TimeoutException {
      throw ApiException('El servidor no respondió a tiempo (${AppConfig.apiBaseUrl}). '
          'Revisa que Synergy esté encendido y que el teléfono esté en la misma red Wi-Fi.');
    } on SocketException {
      throw ApiException(_sinConexion());
    } on http.ClientException {
      throw ApiException(_sinConexion());
    } on HandshakeException {
      throw ApiException('No se pudo establecer una conexión segura con ${AppConfig.apiBaseUrl}.');
    }
  }

  String _sinConexion() => 'No se pudo conectar con el servidor (${AppConfig.apiBaseUrl}). '
      'Si usas un teléfono real, cambia la dirección en "Servidor" por la IP de tu PC.';

  Future<Map<String, String>> _headers(String path, {bool jsonContent = true}) async {
    final session = _esPublica(path) ? null : await _sessionStore.read();
    return {
      if (jsonContent) 'Content-Type': 'application/json; charset=UTF-8',
      'Accept': 'application/json',
      if (session != null && session.token.isNotEmpty) 'Authorization': 'Bearer ${session.token}',
    };
  }

  dynamic _handle(http.Response response) {
    dynamic body;
    if (response.body.isNotEmpty) {
      try {
        body = jsonDecode(utf8.decode(response.bodyBytes));
      } catch (_) {
        body = response.body;
      }
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message = body is Map
          ? (body['message'] ?? body['error'] ?? body['mensaje'] ?? 'Error de comunicación (${response.statusCode})')
          : (body == null || '$body'.isEmpty ? 'Error de comunicación (${response.statusCode})' : '$body');
      throw ApiException(message.toString(), statusCode: response.statusCode);
    }
    return body;
  }
}
