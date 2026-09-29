import '../utils/json_utils.dart';

class Rol {
  const Rol({
    this.id,
    required this.nombre,
    this.descripcion,
    this.privilegios = const [],
    this.activo = true,
    this.fechaCreacion,
  });

  final String? id;
  final String nombre;
  final String? descripcion;
  final List<String> privilegios;
  final bool activo;
  final DateTime? fechaCreacion;

  factory Rol.fromJson(Map<String, dynamic> json) => Rol(
        id: JsonUtils.string(json['id']),
        nombre: JsonUtils.string(json['nombre']) ?? '',
        descripcion: JsonUtils.string(json['descripcion']),
        privilegios: JsonUtils.strings(json['privilegios']),
        activo: JsonUtils.boolValue(json['activo'], fallback: true),
        fechaCreacion: JsonUtils.dateTime(json['fechaCreacion']),
      );

  Map<String, dynamic> toJson() => JsonUtils.withoutNulls({
        'id': id,
        'nombre': nombre,
        'descripcion': descripcion,
        'privilegios': privilegios,
        'activo': activo,
        'fechaCreacion': fechaCreacion?.toIso8601String(),
      });
}
