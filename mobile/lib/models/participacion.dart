import '../utils/json_utils.dart';

class Participacion {
  const Participacion({this.id, required this.idUsuario, required this.idProyecto, this.rol, this.fechaUnion});

  final String? id;
  final String idUsuario;
  final String idProyecto;
  final String? rol;
  final DateTime? fechaUnion;

  factory Participacion.fromJson(Map<String, dynamic> json) => Participacion(
        id: JsonUtils.string(json['id']),
        idUsuario: JsonUtils.string(json['idUsuario']) ?? '',
        idProyecto: JsonUtils.string(json['idProyecto']) ?? '',
        rol: JsonUtils.string(json['rol']),
        fechaUnion: JsonUtils.dateTime(json['fechaUnion']),
      );

  Map<String, dynamic> toJson() => JsonUtils.withoutNulls({
        'id': id,
        'idUsuario': idUsuario,
        'idProyecto': idProyecto,
        'rol': rol,
        'fechaUnion': fechaUnion?.toIso8601String(),
      });
}
