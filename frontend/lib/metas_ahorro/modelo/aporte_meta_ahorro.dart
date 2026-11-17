/// Modelo Dart correspondiente a un aporte de una meta de ahorro.
class AporteMetaAhorro {
  final int id;
  final int metaAhorroId;
  final double monto;
  final DateTime fecha;

  const AporteMetaAhorro({
    required this.id,
    required this.metaAhorroId,
    required this.monto,
    required this.fecha,
  });

  factory AporteMetaAhorro.desdeJson(Map<String, dynamic> json) {
    return AporteMetaAhorro(
      id: (json['id'] as num).toInt(),
      metaAhorroId: (json['metaAhorroId'] as num).toInt(),
      monto: (json['monto'] as num).toDouble(),
      fecha: DateTime.parse(json['fecha'] as String),
    );
  }
}
