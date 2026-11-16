class Notificacion {
  final int id;
  final String titulo;
  final String mensaje;
  final bool leida;
  final DateTime fechaCreacion;
  final int? metaAhorroId;

  Notificacion({
    required this.id,
    required this.titulo,
    required this.mensaje,
    required this.leida,
    required this.fechaCreacion,
    this.metaAhorroId,
  });

  factory Notificacion.desdeJson(Map<String, dynamic> json) {
    return Notificacion(
      id: (json['id'] as num).toInt(),
      titulo: json['titulo'] as String,
      mensaje: json['mensaje'] as String,
      leida: json['leida'] as bool,
      fechaCreacion: DateTime.parse(json['fechaCreacion'] as String),
      metaAhorroId: (json['metaAhorroId'] as num?)?.toInt(),
    );
  }
}
