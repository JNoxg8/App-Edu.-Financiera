/// Espejo en Dart del DeudaRespuestaDto del backend (RQ-07).
///
/// Los campos calculados (cuotaMensual, saldoPendiente, costoIntereses...)
/// llegan ya resueltos desde Quarkus. Flutter NO recalcula nada:
/// la unica fuente de verdad es el backend.
class Deuda {
  final int id;
  final String acreedor;
  final double montoTotal;
  final double tasaInteresAnual;
  final int numeroCuotas;
  final DateTime fechaVencimiento;

  // --- calculados por el backend ---
  final double cuotaMensual;
  final double totalAPagar;
  final double costoIntereses;
  final double totalAbonado;
  final double saldoPendiente;
  final double porcentajePagado;
  final bool pagada;

  final List<Abono> abonos;

  Deuda({
    required this.id,
    required this.acreedor,
    required this.montoTotal,
    required this.tasaInteresAnual,
    required this.numeroCuotas,
    required this.fechaVencimiento,
    required this.cuotaMensual,
    required this.totalAPagar,
    required this.costoIntereses,
    required this.totalAbonado,
    required this.saldoPendiente,
    required this.porcentajePagado,
    required this.pagada,
    required this.abonos,
  });

  factory Deuda.desdeJson(Map<String, dynamic> json) {
    return Deuda(
      id: json['id'] as int,
      acreedor: json['acreedor'] as String,
      montoTotal: (json['montoTotal'] as num).toDouble(),
      tasaInteresAnual: (json['tasaInteresAnual'] as num).toDouble(),
      numeroCuotas: json['numeroCuotas'] as int,
      fechaVencimiento: DateTime.parse(json['fechaVencimiento'] as String),
      cuotaMensual: (json['cuotaMensual'] as num).toDouble(),
      totalAPagar: (json['totalAPagar'] as num).toDouble(),
      costoIntereses: (json['costoIntereses'] as num).toDouble(),
      totalAbonado: (json['totalAbonado'] as num).toDouble(),
      saldoPendiente: (json['saldoPendiente'] as num).toDouble(),
      porcentajePagado: (json['porcentajePagado'] as num).toDouble(),
      pagada: json['pagada'] as bool,
      abonos: (json['abonos'] as List<dynamic>? ?? [])
          .map((a) => Abono.desdeJson(a as Map<String, dynamic>))
          .toList(),
    );
  }

  /// Valor entre 0 y 1, que es lo que espera LinearProgressIndicator.
  double get progreso => (porcentajePagado / 100).clamp(0.0, 1.0);
}

class Abono {
  final int id;
  final double monto;
  final DateTime fecha;

  Abono({required this.id, required this.monto, required this.fecha});

  factory Abono.desdeJson(Map<String, dynamic> json) => Abono(
        id: json['id'] as int,
        monto: (json['monto'] as num).toDouble(),
        fecha: DateTime.parse(json['fecha'] as String),
      );
}
