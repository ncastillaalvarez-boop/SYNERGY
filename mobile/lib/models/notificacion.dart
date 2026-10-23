import '../utils/json_utils.dart';

class Notificacion {
  const Notificacion({required this.id, required this.titulo, this.mensaje, this.tipo, this.leida = false, this.fecha});

  final String id;
  final String titulo;
  final String? mensaje;
  final String? tipo;
  final bool leida;
  final DateTime? fecha;

  factory Notificacion.fromJson(Map<String, dynamic> json) => Notificacion(
        id: JsonUtils.string(json['id']) ?? '',
        titulo: JsonUtils.string(json['titulo']) ?? '',
        mensaje: JsonUtils.string(json['mensaje']),
        tipo: JsonUtils.string(json['tipo']),
        leida: JsonUtils.boolValue(json['leida']),
        fecha: JsonUtils.dateTime(json['fecha']),
      );
}
