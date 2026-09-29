import '../utils/json_utils.dart';

class AuthSession {
  const AuthSession({
    required this.token,
    required this.id,
    required this.nombre,
    required this.correo,
    required this.rol,
    this.tipo = 'Bearer',
    this.privilegios = const [],
    this.expiresIn,
  });

  final String token;
  final String id;
  final String nombre;
  final String correo;
  final String rol;
  final String tipo;
  final List<String> privilegios;
  final int? expiresIn;

  bool get isAdmin => rol.toUpperCase() == 'ADMINISTRADOR';
  bool get isLeader => rol.toUpperCase() == 'LIDER';

  factory AuthSession.fromJson(Map<String, dynamic> json) => AuthSession(
        token: json['token']?.toString() ?? '',
        id: json['id']?.toString() ?? '',
        nombre: json['nombre']?.toString() ?? '',
        correo: json['correo']?.toString() ?? '',
        rol: json['rol']?.toString() ?? 'PARTICIPANTE',
        tipo: json['tipo']?.toString() ?? 'Bearer',
        privilegios: JsonUtils.strings(json['privilegios']),
        expiresIn: json['expiresIn'] is num ? (json['expiresIn'] as num).toInt() : null,
      );

  Map<String, dynamic> toJson() => {
        'token': token,
        'id': id,
        'nombre': nombre,
        'correo': correo,
        'rol': rol,
        'tipo': tipo,
        'privilegios': privilegios,
        'expiresIn': expiresIn,
      };
}
