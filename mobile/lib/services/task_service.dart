import '../models/tarea.dart';
import 'api_client.dart';

class TaskService {
  TaskService({ApiClient? client}) : _client = client ?? ApiClient();
  final ApiClient _client;

  Future<List<Tarea>> getMine() async => _tasks(await _client.get('/tareas/mias'));
  Future<List<Tarea>> getManaged() async => _tasks(await _client.get('/tareas/gestion'));
  Future<List<Tarea>> getForProject(String projectId) async => _tasks(await _client.get('/tareas/proyecto/$projectId'));
  Future<Tarea> getById(String id) async => Tarea.fromJson(Map<String, dynamic>.from(await _client.get('/tareas/$id') as Map));
  Future<Tarea> create(Tarea task) async => Tarea.fromJson(Map<String, dynamic>.from(await _client.post('/tareas', body: task.toJson()) as Map));
  Future<Tarea> update(Tarea task) async => Tarea.fromJson(Map<String, dynamic>.from(await _client.put('/tareas/${task.id}', body: task.toJson()) as Map));
  Future<Tarea> updateStatus(String id, String estado) async => Tarea.fromJson(Map<String, dynamic>.from(await _client.put('/tareas/$id/estado', body: {'estado': estado}) as Map));
  Future<void> delete(String id) => _client.delete('/tareas/$id');

  List<Tarea> _tasks(dynamic json) => (json as List)
      .map((item) => Tarea.fromJson(Map<String, dynamic>.from(item as Map)))
      .toList();
}
