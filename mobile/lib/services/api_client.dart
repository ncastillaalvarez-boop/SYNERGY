import 'dart:convert';

import 'package:http/http.dart' as http;

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

  Future<dynamic> get(String path) async => _handle(await _client.get(_uri(path), headers: await _headers()));

  Future<dynamic> post(String path, {Object? body}) async => _handle(
        await _client.post(_uri(path), headers: await _headers(), body: body == null ? null : jsonEncode(body)),
      );

  Future<dynamic> put(String path, {Object? body}) async => _handle(
        await _client.put(_uri(path), headers: await _headers(), body: body == null ? null : jsonEncode(body)),
      );

  Future<dynamic> delete(String path) async => _handle(await _client.delete(_uri(path), headers: await _headers()));

  Future<dynamic> upload(
    String path, {
    required String field,
    required String filename,
    List<int>? bytes,
    String? filePath,
  }) async {
    final request = http.MultipartRequest('POST', _uri(path));
    request.headers.addAll(await _headers(jsonContent: false));
    if (bytes != null) {
      request.files.add(http.MultipartFile.fromBytes(field, bytes, filename: filename));
    } else if (filePath != null) {
      request.files.add(await http.MultipartFile.fromPath(field, filePath, filename: filename));
    } else {
      throw const ApiException('No fue posible leer el archivo seleccionado.');
    }
    return _handle(await http.Response.fromStream(await request.send()));
  }

  Future<Map<String, String>> _headers({bool jsonContent = true}) async {
    final session = await _sessionStore.read();
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
      final message = body is Map ? (body['message'] ?? body['error'] ?? 'Error de comunicación') : '$body';
      throw ApiException(message.toString(), statusCode: response.statusCode);
    }
    return body;
  }
}
