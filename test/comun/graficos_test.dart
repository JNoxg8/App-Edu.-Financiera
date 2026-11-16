import 'package:finanzas_app/comun/graficos.dart';
import 'package:finanzas_app/comun/tema.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('agruparPartes', () {
    List<({String nombre, double valor, double porcentaje})> partes(int n) => [
          for (var i = 0; i < n; i++)
            (nombre: 'C$i', valor: (n - i) * 10.0, porcentaje: 100.0 / n),
        ];

    test('con 5 o menos partes no agrupa y respeta el orden de colores', () {
      final r = agruparPartes(partes(3));

      expect(r.map((p) => p.nombre), ['C0', 'C1', 'C2']);
      expect(r.map((p) => p.color), PaletaCategorias.colores.take(3));
    });

    test('desde la sexta parte se suman en "Otras" en gris', () {
      final r = agruparPartes(partes(8));

      expect(r, hasLength(6));
      expect(r.last.nombre, 'Otras (3)');
      expect(r.last.color, PaletaCategorias.otras);
      expect(r.last.valor, 30 + 20 + 10); // C5 + C6 + C7
      expect(r.last.porcentaje, closeTo(3 * 100 / 8, 0.0001));
    });

    test('sin partes devuelve una lista vacia', () {
      expect(agruparPartes([]), isEmpty);
    });
  });

  group('coloresSeveridad', () {
    test('hasta 79 % es el color normal', () {
      expect(coloresSeveridad(0.79).relleno, ColoresZenfi.petroleo);
    });

    test('desde 80 % y hasta 100 % exacto es advertencia (no exceso)', () {
      expect(coloresSeveridad(0.8).relleno, ColoresZenfi.advertencia);
      expect(coloresSeveridad(1.0).relleno, ColoresZenfi.advertencia);
    });

    test('pasado el 100 % es peligro', () {
      expect(coloresSeveridad(1.01).relleno, ColoresZenfi.negativo);
    });
  });

  test('colorAvanceMeta distingue activa, cumplida y vencida', () {
    expect(colorAvanceMeta('ACTIVA'), ColoresZenfi.petroleo);
    expect(colorAvanceMeta('COMPLETADA'), ColoresZenfi.positivo);
    expect(colorAvanceMeta('VENCIDA'), ColoresZenfi.textoSecundario);
  });

  testWidgets('el anillo no falla con fracciones fuera de 0..1', (tester) async {
    await tester.pumpWidget(const MaterialApp(
      home: Column(children: [
        AnilloProgreso(fraccion: 1.7, color: Colors.red, colorPista: Colors.grey),
        AnilloProgreso(fraccion: -0.2, color: Colors.red, colorPista: Colors.grey),
      ]),
    ));
    expect(tester.takeException(), isNull);
  });
}
