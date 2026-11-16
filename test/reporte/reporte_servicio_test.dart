import 'dart:convert';

import 'package:finanzas_app/reporte/servicio/reporte_servicio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

/// Pruebas del reporte mensual (HU025) SIN backend: un MockClient simula
/// las respuestas de Quarkus.
void main() {
  const cabeceras = {'content-type': 'application/json; charset=utf-8'};

  final categorias = [
    {
      'categoriaId': 4,
      'categoria': 'Alimentación',
      'total': 450000.00,
      'porcentaje': 75.00,
      'cantidadMovimientos': 6,
    },
    {
      'categoriaId': 2,
      'categoria': 'Transporte',
      'total': 150000.00,
      'porcentaje': 25.00,
      'cantidadMovimientos': 1,
    },
  ];

  setUp(() => debugDefaultTargetPlatformOverride = TargetPlatform.android);
  tearDown(() => debugDefaultTargetPlatformOverride = null);

  test('consultar llama a /zenfi/reportes/mensual?mes=AAAA-MM', () async {
    Uri? urlLlamada;
    final servicio = ReporteServicio(
      cliente: MockClient((peticion) async {
        urlLlamada = peticion.url;
        return http.Response(
            jsonEncode({
              'mes': '2026-10',
              'totalGastado': 0,
              'tieneGastos': false,
              'presupuesto': null,
              'gastosPorCategoria': [],
            }),
            200,
            headers: cabeceras);
      }),
    );

    await servicio.consultar(DateTime(2026, 10));

    expect(urlLlamada.toString(),
        'http://10.0.2.2:8862/zenfi/reportes/mensual?mes=2026-10');
  });

  test('con presupuesto convierte la comparacion y el desglose', () async {
    final servicio = ReporteServicio(
      cliente: MockClient((_) async => http.Response(
          jsonEncode({
            'mes': '2026-10',
            'totalGastado': 600000.00,
            'tieneGastos': true,
            'presupuesto': {
              'id': 1,
              'mes': '2026-10',
              'montoLimite': 500000.00,
              'totalGastado': 600000.00,
              'porcentajeUtilizado': 120.00,
              'disponible': -100000.00,
              'excedido': true,
            },
            'gastosPorCategoria': categorias,
          }),
          200,
          headers: cabeceras)),
    );

    final reporte = await servicio.consultar(DateTime(2026, 10));

    expect(reporte.presupuesto, isNotNull);
    expect(reporte.presupuesto!.excedido, isTrue);
    expect(reporte.presupuesto!.disponible, -100000.0);
    expect(reporte.gastosPorCategoria, hasLength(2));
    // Las tildes deben llegar bien: el cuerpo se decodifica como UTF-8
    expect(reporte.gastosPorCategoria.first.categoria, 'Alimentación');
    expect(reporte.gastosPorCategoria.first.porcentaje, 75.0);
  });

  test('sin presupuesto (null) igual trae los gastos por categoria', () async {
    final servicio = ReporteServicio(
      cliente: MockClient((_) async => http.Response(
          jsonEncode({
            'mes': '2026-09',
            'totalGastado': 600000.00,
            'tieneGastos': true,
            'presupuesto': null,
            'gastosPorCategoria': categorias,
          }),
          200,
          headers: cabeceras)),
    );

    final reporte = await servicio.consultar(DateTime(2026, 9));

    expect(reporte.presupuesto, isNull);
    expect(reporte.tieneGastos, isTrue);
    expect(reporte.totalGastado, 600000.0);
  });

  test('un mes con formato invalido (400) es un error con el mensaje del backend',
      () async {
    final servicio = ReporteServicio(
      cliente: MockClient((_) async => http.Response(
          jsonEncode({
            'mensaje': 'El mes debe tener el formato AAAA-MM',
            'status': 400,
            'success': false,
            'error': {
              'mensaje': 'El mes debe tener el formato AAAA-MM',
              'codigo': 'VAL-400',
              'detalles': [],
            },
          }),
          400,
          headers: cabeceras)),
    );

    await expectLater(
      servicio.consultar(DateTime(2026, 10)),
      throwsA(isA<ApiException>().having((e) => e.status, 'status', 400)),
    );
  });
}
