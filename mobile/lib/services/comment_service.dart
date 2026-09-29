import '../models/comentario.dart';
import 'api_client.dart';

class CommentService {
  CommentService({ApiClient? client}) : _client = client ?? ApiClient();
  final ApiClient _client;

  // Obtener comentarios de un proyecto
  Future<List<Comentario>> getForProject(String projectId) async =>
      (await _client.get('/comentarios/proyecto/$projectId') as List)
          .map((item) => Comentario.fromJson(Map<String, dynamic>.from(item as Map)))
          .toList();

  // Alias para compatibilidad
  Future<List<Comentario>> getByProjectId(String projectId) => getForProject(projectId);

  // Crear comentario en proyecto (método legacy)
  Future<Comentario> createForProject(String projectId, String contenido) async => Comentario.fromJson(
        Map<String, dynamic>.from(await _client.post('/comentarios/proyecto/$projectId', body: {'contenido': contenido}) as Map),
      );

  // Crear comentario con objeto Comentario
  Future<Comentario> create(String projectId, Comentario comment) async => Comentario.fromJson(
        Map<String, dynamic>.from(
          await _client.post('/comentarios/proyecto/$projectId', body: {'contenido': comment.contenido}) as Map,
        ),
      );

  // Eliminar comentario
  Future<void> delete(String id) => _client.delete('/comentarios/$id');
}
