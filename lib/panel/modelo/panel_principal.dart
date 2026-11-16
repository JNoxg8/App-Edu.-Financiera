/// Espejo en Dart del PanelRespuestaDto del backend (HU015).
class PanelPrincipal {
  /// Mes actual en formato "AAAA-MM".
  final String mes;

  /// Ingresos totales menos gastos totales. Puede ser negativo.
  final double balanceGeneral;
  final double totalIngresos;
  final double totalGastos;

  /// false si el usuario todavia no registra ningun movimiento.
  final bool tieneMovimientos;

  /// Gastos del mes actual, incluidos los abonos a deudas.
  final double gastosMesActual;

  /// De la fecha limite mas cercana a la mas lejana.
  final List<ResumenMeta> metasActivas;

  /// Del vencimiento mas cercano al mas lejano.
  final List<ResumenDeuda> deudasPendientes;

  const PanelPrincipal({
    required this.mes,
    required this.balanceGeneral,
    required this.totalIngresos,
    required this.totalGastos,
    required this.tieneMovimientos,
    required this.gastosMesActual,
    required this.metasActivas,
    required this.deudasPendientes,
  });

  factory PanelPrincipal.desdeJson(Map<String, dynamic> json) => PanelPrincipal(
        mes: json['mes'] as String,
        balanceGeneral: (json['balanceGeneral'] as num).toDouble(),
        totalIngresos: (json['totalIngresos'] as num).toDouble(),
        totalGastos: (json['totalGastos'] as num).toDouble(),
        tieneMovimientos: json['tieneMovimientos'] as bool,
        gastosMesActual: (json['gastosMesActual'] as num).toDouble(),
        metasActivas: (json['metasActivas'] as List<dynamic>? ?? [])
            .map((m) => ResumenMeta.desdeJson(m as Map<String, dynamic>))
            .toList(),
        deudasPendientes: (json['deudasPendientes'] as List<dynamic>? ?? [])
            .map((d) => ResumenDeuda.desdeJson(d as Map<String, dynamic>))
            .toList(),
      );
}

class ResumenMeta {
  final int id;
  final String nombre;
  final double montoObjetivo;
  final double montoAhorrado;

  /// 0 a 100.
  final double porcentaje;
  final DateTime fechaLimite;

  const ResumenMeta({
    required this.id,
    required this.nombre,
    required this.montoObjetivo,
    required this.montoAhorrado,
    required this.porcentaje,
    required this.fechaLimite,
  });

  factory ResumenMeta.desdeJson(Map<String, dynamic> json) => ResumenMeta(
        id: json['id'] as int,
        nombre: json['nombre'] as String,
        montoObjetivo: (json['montoObjetivo'] as num).toDouble(),
        montoAhorrado: (json['montoAhorrado'] as num).toDouble(),
        porcentaje: (json['porcentaje'] as num).toDouble(),
        fechaLimite: DateTime.parse(json['fechaLimite'] as String),
      );
}

class ResumenDeuda {
  final int id;
  final String acreedor;
  final double saldoPendiente;

  /// 0 a 100.
  final double porcentajePagado;
  final DateTime fechaVencimiento;

  const ResumenDeuda({
    required this.id,
    required this.acreedor,
    required this.saldoPendiente,
    required this.porcentajePagado,
    required this.fechaVencimiento,
  });

  factory ResumenDeuda.desdeJson(Map<String, dynamic> json) => ResumenDeuda(
        id: json['id'] as int,
        acreedor: json['acreedor'] as String,
        saldoPendiente: (json['saldoPendiente'] as num).toDouble(),
        porcentajePagado: (json['porcentajePagado'] as num).toDouble(),
        fechaVencimiento: DateTime.parse(json['fechaVencimiento'] as String),
      );
}
