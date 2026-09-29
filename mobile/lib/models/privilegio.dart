import '../utils/json_utils.dart';

class Privilegio {
  const Privilegio({
    this.id,
    required this.nombre,
    this.descripcion,
    this.recurso,
    this.accion,
    this.activo = true,
    this.fechaCreacion,
  });

  final String? id;
  final String nombre;
  final String? descripcion;
  final String? recurso;
  final String? accion;
  final bool activo;
  final DateTime? fechaCreacion;

  factory Privilegio.fromJson(Map<String, dynamic> json) => Privilegio(
        id: JsonUtils.string(json['id']),
        nombre: JsonUtils.string(json['nombre']) ?? '',
        descripcion: JsonUtils.string(json['descripcion']),
        recurso: JsonUtils.string(json['recurso']),
        accion: JsonUtils.string(json['accion']),
        activo: JsonUtils.boolValue(json['activo'], fallback: true),
        fechaCreacion: JsonUtils.dateTime(json['fechaCreacion']),
      );

  Map<String, dynamic> toJson() => JsonUtils.withoutNulls({
        'id': id,
        'nombre': nombre,
        'descripcion': descripcion,
        'recurso': recurso,
        'accion': accion,
        'activo': activo,
        'fechaCreacion': fechaCreacion?.toIso8601String(),
      });
}
