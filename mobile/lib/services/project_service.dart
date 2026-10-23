import '../models/proyecto.dart';
import 'api_client.dart';

class ProjectService {
  ProjectService({ApiClient? client}) : _client = client ?? ApiClient();
  final ApiClient _client;

  Future<List<Proyecto>> getAll() async => _projects(await _client.get('/proyectos'));
  Future<Proyecto> getById(String id) async => Proyecto.fromJson(Map<String, dynamic>.from(await _client.get('/proyectos/$id') as Map));
  Future<List<Proyecto>> getMyProjects() async => _projects(await _client.get('/proyectos/mis-proyectos'));
  Future<List<Proyecto>> getMyLeadershipProjects() async => _projects(await _client.get('/proyectos/lider/mios'));

  Future<Proyecto> create(Proyecto project) async =>
      Proyecto.fromJson(Map<String, dynamic>.from(await _client.post('/proyectos', body: project.toJson()) as Map));

  Future<Proyecto> update(Proyecto project) async => Proyecto.fromJson(
        Map<String, dynamic>.from(await _client.put('/proyectos/${project.id}', body: project.toJson()) as Map),
      );

  Future<Proyecto> updateStatus(String id, String estado) async => Proyecto.fromJson(
        Map<String, dynamic>.from(await _client.put('/proyectos/$id/estado', body: {'estado': estado}) as Map),
      );

  Future<void> delete(String id) => _client.delete('/proyectos/$id');

  /// Riesgo calculado por el modelo predictivo: nivel (Bajo/Medio/Alto), probabilidad, motivos y sugerencia.
  Future<Map<String, dynamic>> getRisk(String id) async =>
      Map<String, dynamic>.from(await _client.get('/proyectos/$id/riesgo') as Map);

  List<Proyecto> _projects(dynamic json) => (json as List)
      .map((item) => Proyecto.fromJson(Map<String, dynamic>.from(item as Map)))
      .toList();
}
