import '../utils/json_utils.dart';

class EntregaTarea {
  const EntregaTarea({
    this.id,
    required this.idTarea,
    this.idUsuario,
    this.nombreArchivo,
    this.tipoArchivo,
    this.fechaEntrega,
    this.estado = 'ENTREGADO',
    this.calificacion,
    this.comentariosRevisor,
  });

  final String? id;
  final String idTarea;
  final String? idUsuario;
  final String? nombreArchivo;
  final String? tipoArchivo;
  final DateTime? fechaEntrega;
  final String estado;
  final String? calificacion;
  final String? comentariosRevisor;

  factory EntregaTarea.fromJson(Map<String, dynamic> json) => EntregaTarea(
        id: JsonUtils.string(json['id']),
        idTarea: JsonUtils.string(json['idTarea']) ?? '',
        idUsuario: JsonUtils.string(json['idUsuario']),
        nombreArchivo: JsonUtils.string(json['nombreArchivo']),
        tipoArchivo: JsonUtils.string(json['tipoArchivo']),
        fechaEntrega: JsonUtils.dateTime(json['fechaEntrega']),
        estado: JsonUtils.string(json['estado']) ?? 'ENTREGADO',
        calificacion: JsonUtils.string(json['calificacion']),
        comentariosRevisor: JsonUtils.string(json['comentariosRevisor']),
      );

  Map<String, dynamic> toJson() => JsonUtils.withoutNulls({
        'id': id,
        'idTarea': idTarea,
        'idUsuario': idUsuario,
        'nombreArchivo': nombreArchivo,
        'tipoArchivo': tipoArchivo,
        'fechaEntrega': fechaEntrega?.toIso8601String(),
        'estado': estado,
        'calificacion': calificacion,
        'comentariosRevisor': comentariosRevisor,
      });
}
