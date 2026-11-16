import 'dart:convert';

import 'package:finanzas_app/deudas/servicio/deuda_servicio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

/// Pruebas del servicio SIN backend: un MockClient simula las respuestas
/// de Quarkus. Es el equivalente al @InjectMock de las pruebas del backend.
void main() {
  const cabeceras = {'content-type': 'application/json; charset=utf-8'};

  final deudaJson = {
    'id': 1,
    'acreedor': 'Banco Demo',
    'montoTotal': 1200000.00,
    'tasaInteresAnual': 0.00,
    'numeroCuotas': 12,
    'fechaVencimiento': '2028-09-17',
    'cuotaMensual': 100000.00,
    'totalAPagar': 1200000.00,
    'costoIntereses': 0.00,
    'totalAbonado': 200000.00,
    'saldoPendiente': 1000000.00,
    'porcentajePagado': 16.67,
    'pagada': false,
    'abonos': [
      {'id': 1, 'monto': 200000.00, 'fecha': '2026-09-22'},
    ],
  };

  /// Arma el cuerpo de error con el formato ResponseApi del backend.
  String errorApi({
    required int status,
    required String mensaje,
    required String codigoInterno,
    List<String> detalles = const [],
  }) {
    return jsonEncode({
      'mensaje': mensaje,
      'codigo': 'Error',
      'timestamp': '2026-09-22T15:04:05Z',
      'path': '/zenfi/deudas',
      'status': status,
      'success': false,
      'error': {
        'mensaje': mensaje,
        'codigo': codigoInterno,
        'detalles': detalles,
      },
    });
  }

  setUp(() => debugDefaultTargetPlatformOverride = TargetPlatform.android);
  tearDown(() => debugDefaultTargetPlatformOverride = null);

  test('listar llama a /zenfi/deudas y convierte la respuesta', () async {
    Uri? urlLlamada;
    final servicio = DeudaServicio(
      cliente: MockClient((peticion) async {
        urlLlamada = peticion.url;
        return http.Response(jsonEncode([deudaJson]), 200, headers: cabeceras);
      }),
    );

    final deudas = await servicio.listar();

    expect(urlLlamada.toString(), 'http://10.0.2.2:8862/zenfi/deudas');
    expect(deudas, hasLength(1));
    expect(deudas.first.acreedor, 'Banco Demo');
    expect(deudas.first.saldoPendiente, 1000000.0);
    expect(deudas.first.abonos, hasLength(1));
  });

  test('un 400 de validacion trae los detalles desde error.detalles', () async {
    final servicio = DeudaServicio(
      cliente: MockClient((_) async => http.Response(
            errorApi(
              status: 400,
              mensaje: 'Error de validacion en los datos de entrada',
              codigoInterno: 'VAL-400',
              detalles: [
                'acreedor: El nombre del acreedor es requerido',
                'montoTotal: El monto total debe ser mayor a cero',
              ],
            ),
            400,
            headers: cabeceras,
          )),
    );

    await expectLater(
      servicio.crear(
        acreedor: '',
        montoTotal: -1,
        tasaInteresAnual: 0,
        numeroCuotas: 1,
        fechaVencimiento: DateTime(2028, 1, 1),
      ),
      throwsA(isA<ApiException>()
          .having((e) => e.codigo, 'codigo', 'VAL-400')
          .having((e) => e.mensaje, 'mensaje', contains('validacion'))
          .having((e) => e.detalles, 'detalles', hasLength(2))),
    );
  });

  test('un 409 de regla de negocio conserva el mensaje del backend', () async {
    final servicio = DeudaServicio(
      cliente: MockClient((_) async => http.Response(
            errorApi(
              status: 409,
              mensaje: 'El abono excede el saldo pendiente, que es 100000.00',
              codigoInterno: 'NEG-409',
            ),
            409,
            headers: cabeceras,
          )),
    );

    await expectLater(
      servicio.abonar(deudaId: 1, monto: 99999999),
      throwsA(isA<ApiException>()
          .having((e) => e.codigo, 'codigo', 'NEG-409')
          .having((e) => e.mensaje, 'mensaje', contains('excede'))
          .having((e) => e.detalles, 'detalles', isEmpty)),
    );
  });

  test('un error sin JSON devuelve un mensaje generico', () async {
    final servicio = DeudaServicio(
      cliente: MockClient((_) async => http.Response('Internal error', 500)),
    );

    await expectLater(
      servicio.obtener(1),
      throwsA(isA<ApiException>()
          .having((e) => e.mensaje, 'mensaje', contains('500'))),
    );
  });
}
