import 'package:intl/intl.dart';

/// Monedas de preferencia (HU001). Espejo del enum Moneda del backend.
///
/// Solo cambian como se MUESTRAN los montos; no se convierten valores.
/// Esta en lib/comun porque todos los modulos que muestran dinero la usan.
enum Moneda {
  // En COP se usa un patron propio para poner el simbolo ANTES del numero
  // ($ 1.200.000), que es como se escribe en Colombia; el formato por
  // defecto de 'es_CO' lo pone al final (1.200.000 $).
  cop('COP', 'Peso colombiano', r'$', 'es_CO', 0, '\u00A4 #,##0'),
  usd('USD', 'Dólar estadounidense', r'US$', 'en_US', 2),
  eur('EUR', 'Euro', '€', 'es', 2);

  /// Codigo que viaja en el JSON ("COP", "USD", "EUR").
  final String codigo;
  final String nombre;
  final String simbolo;
  final String _locale;
  final int _decimales;
  final String? _patron;

  const Moneda(this.codigo, this.nombre, this.simbolo, this._locale,
      this._decimales,
      [this._patron]);

  /// Formato de montos para esta moneda: COP -> $ 1.200.000
  NumberFormat get formato => NumberFormat.currency(
        locale: _locale,
        symbol: simbolo,
        decimalDigits: _decimales,
        customPattern: _patron,
      );

  static Moneda desdeCodigo(String codigo) => Moneda.values.firstWhere(
        (m) => m.codigo == codigo,
        orElse: () => throw ArgumentError('Moneda desconocida: $codigo'),
      );
}
