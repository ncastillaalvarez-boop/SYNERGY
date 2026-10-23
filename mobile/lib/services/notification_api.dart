import '../models/notificacion.dart';
import 'api_client.dart';

/// Notificaciones del usuario (tareas asignadas, revisiones, mensajes del líder, riesgo alto).
class NotificationApi {
  NotificationApi({ApiClient? client}) : _client = client ?? ApiClient();
  final ApiClient _client;

  Future<({int noLeidas, List<Notificacion> lista})> listar() async {
    final json = Map<String, dynamic>.from(await _client.get('/notificaciones') as Map);
    final lista = (json['notificaciones'] as List? ?? [])
        .map((e) => Notificacion.fromJson(Map<String, dynamic>.from(e as Map)))
        .toList();
    final noLeidas = json['noLeidas'] is num ? (json['noLeidas'] as num).toInt() : 0;
    return (noLeidas: noLeidas, lista: lista);
  }

  Future<void> marcarLeidas() => _client.post('/notificaciones/leidas');
}
