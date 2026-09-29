import '../models/usuario.dart';
import 'api_client.dart';

class UserService {
  UserService({ApiClient? client}) : _client = client ?? ApiClient();
  final ApiClient _client;

  Future<Usuario> getById(String id) async => Usuario.fromJson(Map<String, dynamic>.from(await _client.get('/usuarios/$id') as Map));

  Future<List<Usuario>> getAllForAdmin() async => (await _client.get('/admin/usuarios') as List)
      .map((item) => Usuario.fromJson(Map<String, dynamic>.from(item as Map)))
      .toList();

  Future<Usuario> updateForAdmin(Usuario user) async => Usuario.fromJson(
        Map<String, dynamic>.from(await _client.put('/admin/usuarios/${user.id}', body: user.toJson()) as Map),
      );

  Future<void> setActive(String id, bool active) => _client.put('/admin/usuarios/$id/${active ? 'activar' : 'desactivar'}');
  Future<void> changeRole(String id, String role) => _client.put('/admin/usuarios/$id/rol', body: {'rol': role});
  Future<void> delete(String id) => _client.delete('/admin/usuarios/$id');
}
