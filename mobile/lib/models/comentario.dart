import '../utils/json_utils.dart';

class Comentario {
  const Comentario({
    this.id,
    required this.contenido,
    this.fecha,
    this.idProyecto,
    this.idUsuario,
    this.idTarea,
  });

  final String? id;
  final String contenido;
  final DateTime? fecha;
  final String? idProyecto;
  final String? idUsuario;
  final String? idTarea;

  factory Comentario.fromJson(Map<String, dynamic> json) => Comentario(
        id: JsonUtils.string(json['id']),
        contenido: JsonUtils.string(json['contenido']) ?? '',
        fecha: JsonUtils.dateTime(json['fecha']),
        idProyecto: JsonUtils.string(json['idProyecto']),
        idUsuario: JsonUtils.string(json['idUsuario']),
        idTarea: JsonUtils.string(json['idTarea']),
      );

  Map<String, dynamic> toJson() => JsonUtils.withoutNulls({
        'id': id,
        'contenido': contenido,
        'fecha': fecha?.toIso8601String(),
        'idProyecto': idProyecto,
        'idUsuario': idUsuario,
        'idTarea': idTarea,
      });
}
