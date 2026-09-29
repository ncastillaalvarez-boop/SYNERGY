import '../models/participacion.dart';
import '../models/proyecto.dart';
import 'api_client.dart';

class ParticipationService {
  ParticipationService({ApiClient? client}) : _client = client ?? ApiClient();
  final ApiClient _client;

  Future<List<Proyecto>> getMine() async => (await _client.get('/participaciones/mias') as List)
      .map((item) => Proyecto.fromJson(Map<String, dynamic>.from(item as Map)))
      .toList();

  Future<List<Participacion>> getParticipants(String projectId) async =>
      (await _client.get('/participaciones/proyecto/$projectId') as List)
          .map((item) => Participacion.fromJson(Map<String, dynamic>.from(item as Map)))
          .toList();

  Future<bool> isEnrolled(String projectId) async {
    final json = Map<String, dynamic>.from(await _client.get('/participaciones/proyecto/$projectId/inscrito') as Map);
    return json['inscrito'] == true;
  }

  Future<bool> join(String projectId) async {
    final json = Map<String, dynamic>.from(await _client.post('/participaciones/proyecto/$projectId') as Map);
    return json['inscrito'] == true;
  }

  Future<void> leave(String projectId) => _client.delete('/participaciones/proyecto/$projectId');
}
