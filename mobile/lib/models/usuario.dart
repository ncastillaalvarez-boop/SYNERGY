import '../utils/json_utils.dart';

class Usuario {
  const Usuario({
    this.id,
    this.nombre,
    this.correo,
    this.contrasena,
    this.rol = 'PARTICIPANTE',
    this.estado = 'ACTIVO',
    this.activo = true,
    this.privilegios = const [],
    this.proyectos = const [],
    this.tareasAsignadas = const [],
    this.fechaCreacion,
    this.ultimoAcceso,
    this.ultimaCambioContrasena,
  });

  final String? id;
  final String? nombre;
  final String? correo;
  final String? contrasena;
  final String rol;
  final String estado;
  final bool activo;
  final List<String> privilegios;
  final List<String> proyectos;
  final List<String> tareasAsignadas;
  final DateTime? fechaCreacion;
  final DateTime? ultimoAcceso;
  final DateTime? ultimaCambioContrasena;

  factory Usuario.fromJson(Map<String, dynamic> json) => Usuario(
        id: JsonUtils.string(json['id']),
        nombre: JsonUtils.string(json['nombre']),
        correo: JsonUtils.string(json['correo']),
        rol: JsonUtils.string(json['rol']) ?? 'PARTICIPANTE',
        estado: JsonUtils.string(json['estado']) ?? 'ACTIVO',
        activo: JsonUtils.boolValue(json['activo'], fallback: true),
        privilegios: JsonUtils.strings(json['privilegios']),
        proyectos: JsonUtils.strings(json['proyectos']),
        tareasAsignadas: JsonUtils.strings(json['tareasAsignadas']),
        fechaCreacion: JsonUtils.dateTime(json['fechaCreacion']),
        ultimoAcceso: JsonUtils.dateTime(json['ultimoAcceso']),
        ultimaCambioContrasena: JsonUtils.dateTime(json['ultimaCambioContrasena']),
      );

  Map<String, dynamic> toJson() => JsonUtils.withoutNulls({
        'id': id,
        'nombre': nombre,
        'correo': correo,
        if (contrasena != null) 'contrasena': contrasena,
        'rol': rol,
        'estado': estado,
        'activo': activo,
        'privilegios': privilegios,
        'proyectos': proyectos,
        'tareasAsignadas': tareasAsignadas,
        'fechaCreacion': fechaCreacion?.toIso8601String(),
        'ultimoAcceso': ultimoAcceso?.toIso8601String(),
        'ultimaCambioContrasena': ultimaCambioContrasena?.toIso8601String(),
      });
}
