import 'dart:convert';

import 'package:finanzas_app/config/api_config.dart';
import 'package:finanzas_app/panel/servicio/panel_servicio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

/// Pruebas del panel principal (HU015) SIN backend: un MockClient simula
/// las respuestas de Quarkus.
void main() {
  const cabeceras = {'content-type': 'application/json; charset=utf-8'};

  setUp(() => debugDefaultTargetPlatformOverride = TargetPlatform.android);
  tearDown(() => debugDefaultTargetPlatformOverride = null);

  test('consultar llama a /zenfi/panel y convierte las cuatro secciones', () async {
    Uri? url;
    final servicio = PanelServicio(
      cliente: MockClient((peticion) async {
        url = peticion.url;
        return http.Response(
            jsonEncode({
              'mes': '2026-11',
              'balanceGeneral': -150000.00,
              'totalIngresos': 850000.00,
              'totalGastos': 1000000.00,
              'tieneMovimientos': true,
              'gastosMesActual': 420000.50,
              'gastosMesPorCategoria': [
                {'categoria': 'Alimentación', 'total': 300000.50, 'porcentaje': 71.43},
                {'categoria': 'Transporte', 'total': 120000.00, 'porcentaje': 28.57},
              ],
              'presupuesto': {
                'id': 5,
                'mes': '2026-11',
                'montoLimite': 400000.00,
                'totalGastado': 420000.50,
                'porcentajeUtilizado': 105.00,
                'disponible': -20000.50,
                'excedido': true,
              },
              'metasActivas': [
                {
                  'id': 3,
                  'nombre': 'Viaje a Cartagena',
                  'montoObjetivo': 2000000.00,
                  'montoAhorrado': 500000.00,
                  'porcentaje': 25.00,
                  'fechaLimite': '2027-03-01',
                },
              ],
              'deudasPendientes': [
                {
                  'id': 8,
                  'acreedor': 'Banco Ñ',
                  'saldoPendiente': 400000.00,
                  'porcentajePagado': 33.33,
                  'fechaVencimiento': '2027-11-16',
                },
              ],
            }),
            200,
            headers: cabeceras);
      }),
    );

    final panel = await servicio.consultar();

    expect(url.toString(), '${ApiConfig.baseUrl}/panel');
    expect(panel.balanceGeneral, -150000.0);
    expect(panel.gastosMesActual, 420000.5);
    expect(panel.metasActivas.single.porcentaje, 25.0);
    expect(panel.metasActivas.single.fechaLimite, DateTime(2027, 3, 1));
    // Las tildes y la Ñ deben llegar bien: el cuerpo se decodifica como UTF-8
    expect(panel.deudasPendientes.single.acreedor, 'Banco Ñ');
    expect(panel.gastosMesPorCategoria.first.categoria, 'Alimentación');
    expect(panel.presupuesto!.excedido, isTrue);
    expect(panel.presupuesto!.totalGastado, panel.gastosMesActual);
  });

  test('sin informacion llegan listas vacias y tieneMovimientos en false', () async {
    final servicio = PanelServicio(
      cliente: MockClient((_) async => http.Response(
          jsonEncode({
            'mes': '2026-11',
            'balanceGeneral': 0,
            'totalIngresos': 0,
            'totalGastos': 0,
            'tieneMovimientos': false,
            'gastosMesActual': 0,
            'metasActivas': [],
            'deudasPendientes': [],
          }),
          200,
          headers: cabeceras)),
    );

    final panel = await servicio.consultar();

    expect(panel.tieneMovimientos, isFalse);
    expect(panel.metasActivas, isEmpty);
    expect(panel.deudasPendientes, isEmpty);
    expect(panel.gastosMesPorCategoria, isEmpty);
    expect(panel.presupuesto, isNull);
  });

  test('un error del servidor se convierte en ApiException', () async {
    final servicio = PanelServicio(
      cliente: MockClient((_) async => http.Response(
          jsonEncode({
            'mensaje': 'Error interno',
            'status': 500,
            'success': false,
          }),
          500,
          headers: cabeceras)),
    );

    await expectLater(
      servicio.consultar(),
      throwsA(isA<ApiException>().having((e) => e.status, 'status', 500)),
    );
  });
}
