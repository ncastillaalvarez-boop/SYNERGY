import '../models/privilegio.dart';
import '../models/rol.dart';
import 'api_client.dart';

class RolePrivilegeService {
  RolePrivilegeService({ApiClient? client}) : _client = client ?? ApiClient();
  final ApiClient _client;

  Future<List<Rol>> getRoles() async => (await _client.get('/admin/roles-privilegios/roles') as List)
      .map((item) => Rol.fromJson(Map<String, dynamic>.from(item as Map)))
      .toList();

  Future<List<Privilegio>> getPrivileges() async => (await _client.get('/admin/roles-privilegios/privilegios') as List)
      .map((item) => Privilegio.fromJson(Map<String, dynamic>.from(item as Map)))
      .toList();
}
