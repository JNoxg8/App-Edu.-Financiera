import 'package:finanzas_app/movimientos/modelo/movimiento.dart';
import 'package:flutter_test/flutter_test.dart';

/// Conversion del JSON de movimientos, incluido el gasto de un aporte a una
/// meta de ahorro (categoria automatica "Metas de ahorro").
void main() {
  Map<String, dynamic> categoriaMetas() => {
        'id': 9,
        'nombre': 'Metas de ahorro',
        'tipo': 'GASTO',
        'predefinida': true,
        'requiereDeuda': false,
        'automatica': true,
      };

  test('el gasto de un aporte llega marcado y con su meta', () {
    final m = Movimiento.desdeJson({
      'id': 31,
      'tipo': 'GASTO',
      'monto': 500000.00,
      'fecha': '2026-11-16',
      'descripcion': 'Aporte a meta: Moto',
      'categoria': categoriaMetas(),
      'deudaId': null,
      'abonoDeuda': false,
      'metaAhorroId': 4,
      'aporteMeta': true,
    });

    expect(m.aporteMeta, isTrue);
    expect(m.abonoDeuda, isFalse);
    expect(m.metaAhorroId, 4);
    expect(m.categoria.automatica, isTrue);
  });

  test('sin los campos nuevos (backend anterior) todo queda en false', () {
    final c = Categoria.desdeJson({
      'id': 1,
      'nombre': 'Transporte',
      'tipo': 'GASTO',
      'predefinida': true,
      'requiereDeuda': false,
    });

    expect(c.automatica, isFalse);
  });
}
