import '../../movimientos/modelo/movimiento.dart';
import '../../presupuesto/modelo/presupuesto.dart';

/// Espejo en Dart del ReporteMensualRespuestaDto del backend (HU025).
///
/// Reutiliza los modelos que ya existen: el presupuesto llega con el mismo
/// formato que GET /presupuestos (HU011) y cada categoria con el mismo que
/// el resumen de movimientos (HU009).
class ReporteMensual {
  /// Mes en formato "AAAA-MM".
  final String mes;
  final double totalGastado;
  final bool tieneGastos;

  /// null si el mes no tiene presupuesto definido.
  final Presupuesto? presupuesto;

  /// De la categoria con mas gasto a la de menos.
  final List<TotalCategoria> gastosPorCategoria;

  const ReporteMensual({
    required this.mes,
    required this.totalGastado,
    required this.tieneGastos,
    required this.presupuesto,
    required this.gastosPorCategoria,
  });

  factory ReporteMensual.desdeJson(Map<String, dynamic> json) {
    final presupuesto = json['presupuesto'] as Map<String, dynamic>?;
    return ReporteMensual(
      mes: json['mes'] as String,
      totalGastado: (json['totalGastado'] as num).toDouble(),
      tieneGastos: json['tieneGastos'] as bool,
      presupuesto:
          presupuesto == null ? null : Presupuesto.desdeJson(presupuesto),
      gastosPorCategoria: (json['gastosPorCategoria'] as List<dynamic>? ?? [])
          .map((t) => TotalCategoria.desdeJson(t as Map<String, dynamic>))
          .toList(),
    );
  }
}
