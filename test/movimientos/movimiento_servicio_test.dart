import 'dart:convert';

import 'package:finanzas_app/movimientos/modelo/movimiento.dart';
import 'package:finanzas_app/movimientos/servicio/movimiento_servicio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

/// Pruebas del servicio de HU009 SIN backend: MockClient simula Quarkus.
void main() {
  const cabeceras = {'content-type': 'application/json; charset=utf-8'};

  final deudasJson = {
    'id': 8,
    'nombre': 'Deudas',
    'tipo': 'GASTO',
    'predefinida': true,
    'requiereDeuda': true,
  };

  final movimientoJson = {
    'id': 1,
    'tipo': 'GASTO',
    'monto': 150000.00,
    'fecha': '2026-09-27',
    'descripcion': 'Abono a deuda: Banco Demo',
    'categoria': deudasJson,
    'deudaId': 3,
    'abonoDeuda': true,
  };

  setUp(() => debugDefaultTargetPlatformOverride = TargetPlatform.android);
  tearDown(() => debugDefaultTargetPlatformOverride = null);

  test('listar envia los filtros, la busqueda y el orden', () async {
    Uri? url;
    final servicio = MovimientoServicio(
      cliente: MockClient((peticion) async {
        url = peticion.url;
        return http.Response(jsonEncode([movimientoJson]), 200,
            headers: cabeceras);
      }),
    );

    final lista = await servicio.listar(
      tipo: TipoMovimiento.gasto,
      categoriaId: 8,
      buscar: ' banco ',
      desde: DateTime(2026, 9, 1),
      hasta: DateTime(2026, 9, 30),
      orden: OrdenMovimientos.monto,
      ascendente: true,
    );

    expect(url!.path, '/zenfi/movimientos');
    expect(url!.queryParameters, {
      'tipo': 'GASTO',
      'categoriaId': '8',
      'buscar': 'banco',
      'desde': '2026-09-01',
      'hasta': '2026-09-30',
      'ordenarPor': 'MONTO',
      'direccion': 'ASC',
    });
    expect(lista.single.abonoDeuda, isTrue);
    expect(lista.single.categoria.requiereDeuda, isTrue);
    expect(lista.single.deudaId, 3);
  });

  test('crear un gasto en Deudas envia el deudaId', () async {
    Map<String, dynamic>? cuerpo;
    final servicio = MovimientoServicio(
      cliente: MockClient((peticion) async {
        cuerpo = jsonDecode(peticion.body) as Map<String, dynamic>;
        return http.Response(jsonEncode(movimientoJson), 201,
            headers: cabeceras);
      }),
    );

    await servicio.crear(
      tipo: TipoMovimiento.gasto,
      monto: 150000,
      fecha: DateTime(2026, 9, 27),
      categoriaId: 8,
      deudaId: 3,
    );

    expect(cuerpo, {
      'tipo': 'GASTO',
      'monto': 150000,
      'fecha': '2026-09-27',
      'descripcion': null,
      'categoriaId': 8,
      'deudaId': 3,
    });
  });

  test('un nombre de categoria repetido llega como ApiException 409', () async {
    final servicio = MovimientoServicio(
      cliente: MockClient((_) async => http.Response(
            jsonEncode({
              'mensaje': "Ya existe una categoria de gastos llamada 'Transporte'",
              'status': 409,
              'success': false,
              'error': {'codigo': 'NEG-409', 'detalles': []},
            }),
            409,
            headers: cabeceras,
          )),
    );

    await expectLater(
      servicio.crearCategoria(nombre: 'transporte', tipo: TipoMovimiento.gasto),
      throwsA(isA<ApiException>()
          .having((e) => e.status, 'status', 409)
          .having((e) => e.mensaje, 'mensaje', contains('Ya existe'))),
    );
  });

  test('resumen convierte totales y categorias', () async {
    final servicio = MovimientoServicio(
      cliente: MockClient((_) async => http.Response(
            jsonEncode({
              'desde': null,
              'hasta': null,
              'totalIngresos': 1000000.00,
              'totalGastos': 400000.00,
              'balance': 600000.00,
              'porcentajeGastado': 40.00,
              'ingresosPorCategoria': [],
              'gastosPorCategoria': [
                {
                  'categoriaId': 8,
                  'categoria': 'Deudas',
                  'total': 400000.00,
                  'porcentaje': 100.00,
                  'cantidadMovimientos': 2,
                },
              ],
            }),
            200,
            headers: cabeceras,
          )),
    );

    final r = await servicio.resumen();

    expect(r.balance, 600000.0);
    expect(r.gastosPorCategoria.single.categoria, 'Deudas');
  });
}
