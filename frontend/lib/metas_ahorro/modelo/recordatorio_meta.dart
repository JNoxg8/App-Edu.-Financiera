class RecordatorioMeta {
  final int id;
  final int metaAhorroId;
  final String frecuencia;
  final DateTime proximoRecordatorio;
  final bool activo;
  final DateTime fechaCreacion;

  const RecordatorioMeta({
    required this.id,
    required this.metaAhorroId,
    required this.frecuencia,
    required this.proximoRecordatorio,
    required this.activo,
    required this.fechaCreacion,
  });

  factory RecordatorioMeta.desdeJson(Map<String, dynamic> json) {
    return RecordatorioMeta(
      id: (json['id'] as num).toInt(),
      metaAhorroId: (json['metaAhorroId'] as num).toInt(),
      frecuencia: json['frecuencia'] as String,
      proximoRecordatorio: DateTime.parse(
        json['proximoRecordatorio'] as String,
      ),
      activo: json['activo'] as bool,
      fechaCreacion: DateTime.parse(json['fechaCreacion'] as String),
    );
  }
}
