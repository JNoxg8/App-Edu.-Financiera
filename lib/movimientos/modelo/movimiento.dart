/// Espejos en Dart de los DTO de HU009 (ingresos y gastos).
///
/// Igual que en deudas, Flutter NO recalcula nada: totales, balance y
/// porcentajes llegan resueltos desde Quarkus.
library;

/// Espejo del enum TipoMovimiento del backend.
enum TipoMovimiento {
  ingreso('INGRESO', 'Ingreso'),
  gasto('GASTO', 'Gasto');

  /// Valor que viaja en el JSON.
  final String codigo;
  final String nombre;

  const TipoMovimiento(this.codigo, this.nombre);

  static TipoMovimiento desdeCodigo(String codigo) =>
      TipoMovimiento.values.firstWhere(
        (t) => t.codigo == codigo,
        orElse: () => throw ArgumentError('Tipo desconocido: $codigo'),
      );
}

/// Campo por el que se ordena el listado (parametro ordenarPor).
enum OrdenMovimientos {
  fecha('FECHA', 'Fecha'),
  monto('MONTO', 'Monto'),
  categoria('CATEGORIA', 'Categoría');

  final String codigo;
  final String nombre;

  const OrdenMovimientos(this.codigo, this.nombre);
}

/// Espejo de CategoriaRespuestaDto.
class Categoria {
  final int id;
  final String nombre;
  final TipoMovimiento tipo;
  final bool predefinida;

  /// true solo en "Deudas": al elegirla hay que escoger la deuda a abonar.
  final bool requiereDeuda;

  const Categoria({
    required this.id,
    required this.nombre,
    required this.tipo,
    required this.predefinida,
    required this.requiereDeuda,
  });

  factory Categoria.desdeJson(Map<String, dynamic> json) => Categoria(
        id: json['id'] as int,
        nombre: json['nombre'] as String,
        tipo: TipoMovimiento.desdeCodigo(json['tipo'] as String),
        predefinida: json['predefinida'] as bool,
        requiereDeuda: json['requiereDeuda'] as bool? ?? false,
      );

  // Dos categorias son la misma si tienen el mismo id. Lo necesita el
  // DropdownButtonFormField para reconocer el valor seleccionado.
  @override
  bool operator ==(Object other) => other is Categoria && other.id == id;

  @override
  int get hashCode => id.hashCode;
}

/// Espejo de MovimientoRespuestaDto.
class Movimiento {
  final int id;
  final TipoMovimiento tipo;
  final double monto;
  final DateTime fecha;
  final String? descripcion;
  final Categoria categoria;
  final int? deudaId;

  /// true si el gasto viene de un abono a una deuda: en ese caso solo se
  /// puede editar la descripcion.
  final bool abonoDeuda;

  const Movimiento({
    required this.id,
    required this.tipo,
    required this.monto,
    required this.fecha,
    required this.descripcion,
    required this.categoria,
    required this.deudaId,
    required this.abonoDeuda,
  });

  factory Movimiento.desdeJson(Map<String, dynamic> json) => Movimiento(
        id: json['id'] as int,
        tipo: TipoMovimiento.desdeCodigo(json['tipo'] as String),
        monto: (json['monto'] as num).toDouble(),
        fecha: DateTime.parse(json['fecha'] as String),
        descripcion: json['descripcion'] as String?,
        categoria:
            Categoria.desdeJson(json['categoria'] as Map<String, dynamic>),
        deudaId: json['deudaId'] as int?,
        abonoDeuda: json['abonoDeuda'] as bool? ?? false,
      );

  bool get esIngreso => tipo == TipoMovimiento.ingreso;
}

/// Total de una categoria dentro del resumen.
class TotalCategoria {
  final int categoriaId;
  final String categoria;
  final double total;
  final double porcentaje;
  final int cantidadMovimientos;

  const TotalCategoria({
    required this.categoriaId,
    required this.categoria,
    required this.total,
    required this.porcentaje,
    required this.cantidadMovimientos,
  });

  factory TotalCategoria.desdeJson(Map<String, dynamic> json) => TotalCategoria(
        categoriaId: json['categoriaId'] as int,
        categoria: json['categoria'] as String,
        total: (json['total'] as num).toDouble(),
        porcentaje: (json['porcentaje'] as num).toDouble(),
        cantidadMovimientos: json['cantidadMovimientos'] as int,
      );
}

/// Espejo de ResumenRespuestaDto.
class ResumenMovimientos {
  final double totalIngresos;
  final double totalGastos;
  final double balance;
  final double porcentajeGastado;
  final List<TotalCategoria> ingresosPorCategoria;
  final List<TotalCategoria> gastosPorCategoria;

  const ResumenMovimientos({
    required this.totalIngresos,
    required this.totalGastos,
    required this.balance,
    required this.porcentajeGastado,
    required this.ingresosPorCategoria,
    required this.gastosPorCategoria,
  });

  factory ResumenMovimientos.desdeJson(Map<String, dynamic> json) {
    List<TotalCategoria> lista(String campo) =>
        (json[campo] as List<dynamic>? ?? [])
            .map((t) => TotalCategoria.desdeJson(t as Map<String, dynamic>))
            .toList();

    return ResumenMovimientos(
      totalIngresos: (json['totalIngresos'] as num).toDouble(),
      totalGastos: (json['totalGastos'] as num).toDouble(),
      balance: (json['balance'] as num).toDouble(),
      porcentajeGastado: (json['porcentajeGastado'] as num).toDouble(),
      ingresosPorCategoria: lista('ingresosPorCategoria'),
      gastosPorCategoria: lista('gastosPorCategoria'),
    );
  }
}
