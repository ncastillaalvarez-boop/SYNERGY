import '../utils/json_utils.dart';

class Tarea {
  const Tarea({
    this.id,
    required this.titulo,
    required this.descripcion,
    this.estado = 'PENDIENTE',
    this.fechaEntrega,
    required this.idProyecto,
    this.usuariosAsignados = const [],
    this.entregas = const [],
    this.comentarios = const [],
    this.fechaCreacion,
    this.fechaActualizacion,
  });

  final String? id;
  final String titulo;
  final String descripcion;
  final String estado;
  final DateTime? fechaEntrega;
  final String idProyecto;
  final List<String> usuariosAsignados;
  final List<String> entregas;
  final List<String> comentarios;
  final DateTime? fechaCreacion;
  final DateTime? fechaActualizacion;

  factory Tarea.fromJson(Map<String, dynamic> json) => Tarea(
        id: JsonUtils.string(json['id']),
        titulo: JsonUtils.string(json['titulo']) ?? '',
        descripcion: JsonUtils.string(json['descripcion']) ?? '',
        estado: JsonUtils.string(json['estado']) ?? 'PENDIENTE',
        fechaEntrega: JsonUtils.dateTime(json['fechaEntrega']),
        idProyecto: JsonUtils.string(json['idProyecto']) ?? '',
        usuariosAsignados: JsonUtils.strings(json['usuariosAsignados']),
        entregas: JsonUtils.strings(json['entregas']),
        comentarios: JsonUtils.strings(json['comentarios']),
        fechaCreacion: JsonUtils.dateTime(json['fechaCreacion']),
        fechaActualizacion: JsonUtils.dateTime(json['fechaActualizacion']),
      );

  Map<String, dynamic> toJson() => JsonUtils.withoutNulls({
        'id': id,
        'titulo': titulo,
        'descripcion': descripcion,
        'estado': estado,
        'fechaEntrega': JsonUtils.dateOnly(fechaEntrega),
        'idProyecto': idProyecto,
        'usuariosAsignados': usuariosAsignados,
        'entregas': entregas,
        'comentarios': comentarios,
        'fechaCreacion': fechaCreacion?.toIso8601String(),
        'fechaActualizacion': fechaActualizacion?.toIso8601String(),
      });

  Tarea copyWith({String? estado}) => Tarea(
        id: id,
        titulo: titulo,
        descripcion: descripcion,
        estado: estado ?? this.estado,
        fechaEntrega: fechaEntrega,
        idProyecto: idProyecto,
        usuariosAsignados: usuariosAsignados,
        entregas: entregas,
        comentarios: comentarios,
        fechaCreacion: fechaCreacion,
        fechaActualizacion: fechaActualizacion,
      );
}
