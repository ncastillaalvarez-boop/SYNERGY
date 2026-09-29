class ParticipacionId {
  const ParticipacionId({required this.usuarioId, required this.proyectoId});

  final String usuarioId;
  final String proyectoId;

  factory ParticipacionId.fromJson(Map<String, dynamic> json) => ParticipacionId(
        usuarioId: json['usuarioId']?.toString() ?? '',
        proyectoId: json['proyectoId']?.toString() ?? '',
      );

  Map<String, dynamic> toJson() => {'usuarioId': usuarioId, 'proyectoId': proyectoId};
}
