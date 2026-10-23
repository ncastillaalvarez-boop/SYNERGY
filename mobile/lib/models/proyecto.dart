import '../utils/json_utils.dart';

class Proyecto {
  const Proyecto({
    this.id,
    required this.nombre,
    required this.descripcion,
    this.fechaInicio,
    this.fechaFin,
    this.idLider,
    this.participantes = const [],
    this.estado = 'ACTIVO',
    this.comentarios = const [],
    this.tareas = const [],
    this.fechaCreacion,
    this.fechaActualizacion,
    this.codigo,
    this.categoria,
    this.nivelRiesgo,
    this.probabilidadRiesgo,
    this.riesgoFactor,
  });

  final String? id;
  final String nombre;
  final String descripcion;
  final DateTime? fechaInicio;
  final DateTime? fechaFin;
  final String? idLider;
  final List<String> participantes;
  final String estado;
  final List<String> comentarios;
  final List<String> tareas;
  final DateTime? fechaCreacion;
  final DateTime? fechaActualizacion;
  final String? codigo;
  final String? categoria;

  /// Bajo, Medio o Alto según el modelo predictivo (null = sin datos o no activo).
  final String? nivelRiesgo;
  final double? probabilidadRiesgo;
  final String? riesgoFactor;

  factory Proyecto.fromJson(Map<String, dynamic> json) => Proyecto(
        id: JsonUtils.string(json['id']),
        nombre: JsonUtils.string(json['nombre']) ?? '',
        descripcion: JsonUtils.string(json['descripcion']) ?? '',
        fechaInicio: JsonUtils.dateTime(json['fechaInicio']),
        fechaFin: JsonUtils.dateTime(json['fechaFin']),
        idLider: JsonUtils.string(json['idLider']),
        participantes: JsonUtils.strings(json['participantes']),
        estado: JsonUtils.string(json['estado']) ?? 'ACTIVO',
        comentarios: JsonUtils.strings(json['comentarios']),
        tareas: JsonUtils.strings(json['tareas']),
        fechaCreacion: JsonUtils.dateTime(json['fechaCreacion']),
        fechaActualizacion: JsonUtils.dateTime(json['fechaActualizacion']),
        codigo: JsonUtils.string(json['codigo']),
        categoria: JsonUtils.string(json['categoria']),
        nivelRiesgo: JsonUtils.string(json['nivelRiesgo']),
        probabilidadRiesgo: json['probabilidadRiesgo'] is num ? (json['probabilidadRiesgo'] as num).toDouble() : null,
        riesgoFactor: JsonUtils.string(json['riesgoFactor']),
      );

  Map<String, dynamic> toJson() => JsonUtils.withoutNulls({
        'id': id,
        'nombre': nombre,
        'descripcion': descripcion,
        'fechaInicio': JsonUtils.dateOnly(fechaInicio),
        'fechaFin': JsonUtils.dateOnly(fechaFin),
        'idLider': idLider,
        'participantes': participantes,
        'estado': estado,
        'comentarios': comentarios,
        'tareas': tareas,
        'fechaCreacion': fechaCreacion?.toIso8601String(),
        'fechaActualizacion': fechaActualizacion?.toIso8601String(),
      });

  Proyecto copyWith({
    String? nombre,
    String? descripcion,
    DateTime? fechaInicio,
    DateTime? fechaFin,
    String? estado,
  }) =>
      Proyecto(
        id: id,
        nombre: nombre ?? this.nombre,
        descripcion: descripcion ?? this.descripcion,
        fechaInicio: fechaInicio ?? this.fechaInicio,
        fechaFin: fechaFin ?? this.fechaFin,
        idLider: idLider,
        participantes: participantes,
        estado: estado ?? this.estado,
        comentarios: comentarios,
        tareas: tareas,
        fechaCreacion: fechaCreacion,
        fechaActualizacion: fechaActualizacion,
        codigo: codigo,
        categoria: categoria,
        nivelRiesgo: nivelRiesgo,
        probabilidadRiesgo: probabilidadRiesgo,
        riesgoFactor: riesgoFactor,
      );
}
