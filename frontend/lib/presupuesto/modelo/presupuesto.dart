/// Espejo en Dart del PresupuestoRespuestaDto del backend (HU011).
///
/// Lo gastado, el porcentaje y el disponible llegan ya calculados desde
/// Quarkus. Flutter NO recalcula nada: la unica fuente de verdad es el
/// backend.
class Presupuesto {
  final int id;

  /// Mes en formato "AAAA-MM", igual que lo envia el backend.
  final String mes;
  final double montoLimite;

  // --- calculados por el backend ---
  final double totalGastado;
  final double porcentajeUtilizado;

  /// Negativo cuando lo gastado supera el limite.
  final double disponible;
  final bool excedido;

  Presupuesto({
    required this.id,
    required this.mes,
    required this.montoLimite,
    required this.totalGastado,
    required this.porcentajeUtilizado,
    required this.disponible,
    required this.excedido,
  });

  factory Presupuesto.desdeJson(Map<String, dynamic> json) {
    return Presupuesto(
      id: json['id'] as int,
      mes: json['mes'] as String,
      montoLimite: (json['montoLimite'] as num).toDouble(),
      totalGastado: (json['totalGastado'] as num).toDouble(),
      porcentajeUtilizado: (json['porcentajeUtilizado'] as num).toDouble(),
      disponible: (json['disponible'] as num).toDouble(),
      excedido: json['excedido'] as bool,
    );
  }

  /// Valor entre 0 y 1, que es lo que espera LinearProgressIndicator.
  /// Al exceder el limite la barra se queda llena.
  double get progreso => (porcentajeUtilizado / 100).clamp(0.0, 1.0);
}
