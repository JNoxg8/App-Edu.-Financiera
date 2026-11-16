import 'dart:convert';

import 'package:finanzas_app/comun/api_exception.dart';
import 'package:finanzas_app/metas_ahorro/servicio/aporte_meta_ahorro_servicio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

/// Pruebas de los aportes a metas (HU004) SIN backend: un MockClient simula
/// las respuestas de Quarkus.
void main() {
  const cabeceras = {'content-type': 'application/json; charset=utf-8'};

  setUp(() => debugDefaultTargetPlatformOverride = TargetPlatform.android);
  tearDown(() => debugDefaultTargetPlatformOverride = null);

  test('registrar siempre envia la fecha como AAAA-MM-DD', () async {
    Map<String, dynamic>? cuerpo;
    Uri? url;
    final servicio = AporteMetaAhorroServicio(
      cliente: MockClient((peticion) async {
        url = peticion.url;
        cuerpo = jsonDecode(peticion.body) as Map<String, dynamic>;
        return http.Response(
            jsonEncode({
              'id': 7,
              'metaAhorroId': 3,
              'monto': 150000.0,
              'fecha': '2026-11-02',
            }),
            201,
            headers: cabeceras);
      }),
    );

    final aporte = await servicio.registrar(
      metaAhorroId: 3,
      monto: 150000,
      fecha: DateTime(2026, 11, 2),
    );

    expect(url.toString(), 'http://10.0.2.2:8862/zenfi/metas-ahorro/3/aportes');
    expect(cuerpo, {'monto': 150000, 'fecha': '2026-11-02'});
    expect(aporte.id, 7);
  });

  test('una meta vencida (409) es un error con el mensaje del backend', () async {
    final servicio = AporteMetaAhorroServicio(
      cliente: MockClient((_) async => http.Response(
          jsonEncode({
            'mensaje': 'No se pueden realizar aportes a una meta vencida',
            'status': 409,
            'success': false,
            'error': {
              'mensaje': 'No se pueden realizar aportes a una meta vencida',
              'codigo': 'NEG-409',
              'detalles': [],
            },
          }),
          409,
          headers: cabeceras)),
    );

    await expectLater(
      servicio.registrar(
          metaAhorroId: 3, monto: 100, fecha: DateTime(2026, 11, 2)),
      throwsA(isA<ApiException>()
          .having((e) => e.status, 'status', 409)
          .having((e) => e.mensaje, 'mensaje', contains('vencida'))),
    );
  });
}
