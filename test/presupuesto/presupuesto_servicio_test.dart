import 'dart:convert';

import 'package:finanzas_app/config/api_config.dart';
import 'package:finanzas_app/presupuesto/modelo/mes.dart';
import 'package:finanzas_app/presupuesto/modelo/presupuesto.dart';
import 'package:finanzas_app/presupuesto/servicio/presupuesto_servicio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

/// Pruebas del modulo de presupuesto (HU011) SIN backend: un MockClient
/// simula las respuestas de Quarkus.
void main() {
  const cabeceras = {'content-type': 'application/json; charset=utf-8'};

  final presupuestoJson = {
    'id': 1,
    'mes': '2026-10',
    'montoLimite': 1000000.00,
    'totalGastado': 750000.00,
    'porcentajeUtilizado': 75.00,
    'disponible': 250000.00,
    'excedido': false,
  };

  /// Arma el cuerpo de error con el formato ResponseApi del backend.
  String errorApi(int status, String mensaje, String codigoInterno) {
    return jsonEncode({
      'mensaje': mensaje,
      'status': status,
      'success': false,
      'error': {'mensaje': mensaje, 'codigo': codigoInterno, 'detalles': []},
    });
  }

  setUp(() => debugDefaultTargetPlatformOverride = TargetPlatform.android);
  tearDown(() => debugDefaultTargetPlatformOverride = null);

  group('Mes', () {
    test('codigoMes usa el formato AAAA-MM que espera el backend', () {
      expect(codigoMes(DateTime(2026, 10)), '2026-10');
      expect(codigoMes(DateTime(2027, 1, 31)), '2027-01');
    });

    test('nombreMes muestra el mes en espanol', () {
      expect(nombreMes(DateTime(2026, 10)), 'Octubre 2026');
    });

    test('esMesPasado: solo el mes anterior al actual es pasado', () {
      final actual = mesActual();
      expect(esMesPasado(DateTime(actual.year, actual.month - 1)), isTrue);
      expect(esMesPasado(actual), isFalse);
      expect(esMesPasado(DateTime(actual.year, actual.month + 1)), isFalse);
    });
  });

  group('Presupuesto', () {
    test('progreso queda entre 0 y 1 aunque se exceda el limite', () {
      final excedido = Presupuesto.desdeJson({
        ...presupuestoJson,
        'porcentajeUtilizado': 115.00,
        'disponible': -150000.00,
        'excedido': true,
      });

      expect(Presupuesto.desdeJson(presupuestoJson).progreso, 0.75);
      expect(excedido.progreso, 1.0);
    });
  });

  group('PresupuestoServicio', () {
    test('consultar llama a /zenfi/presupuestos?mes=AAAA-MM y convierte la respuesta',
        () async {
      Uri? urlLlamada;
      final servicio = PresupuestoServicio(
        cliente: MockClient((peticion) async {
          urlLlamada = peticion.url;
          return http.Response(jsonEncode(presupuestoJson), 200,
              headers: cabeceras);
        }),
      );

      final presupuesto = await servicio.consultar(DateTime(2026, 10));

      expect(urlLlamada.toString(),
          '${ApiConfig.baseUrl}/presupuestos?mes=2026-10');
      expect(presupuesto, isNotNull);
      expect(presupuesto!.porcentajeUtilizado, 75.0);
      expect(presupuesto.disponible, 250000.0);
      expect(presupuesto.excedido, isFalse);
    });

    test('un mes sin presupuesto (REC-404) devuelve null, no un error', () async {
      final servicio = PresupuestoServicio(
        cliente: MockClient((_) async => http.Response(
            errorApi(404, 'No hay un presupuesto definido para 2026-10',
                'REC-404'),
            404,
            headers: cabeceras)),
      );

      expect(await servicio.consultar(DateTime(2026, 10)), isNull);
    });

    test('un 404 sin REC-404 (ruta inexistente) si es un error', () async {
      final servicio = PresupuestoServicio(
        cliente: MockClient((_) async => http.Response('Not Found', 404)),
      );

      await expectLater(
        servicio.consultar(DateTime(2026, 10)),
        throwsA(isA<ApiException>()
            .having((e) => e.status, 'status', 404)),
      );
    });

    test('crear envia el mes y el monto limite en el formato del backend',
        () async {
      Map<String, dynamic>? cuerpo;
      String? metodo;
      final servicio = PresupuestoServicio(
        cliente: MockClient((peticion) async {
          metodo = peticion.method;
          cuerpo = jsonDecode(peticion.body) as Map<String, dynamic>;
          return http.Response(jsonEncode(presupuestoJson), 201,
              headers: cabeceras);
        }),
      );

      await servicio.crear(mes: DateTime(2026, 10), montoLimite: 1000000);

      expect(metodo, 'POST');
      expect(cuerpo, {'mes': '2026-10', 'montoLimite': 1000000});
    });

    test('un 409 (mes con presupuesto) conserva el mensaje del backend',
        () async {
      final servicio = PresupuestoServicio(
        cliente: MockClient((_) async => http.Response(
            errorApi(409, 'Ya existe un presupuesto para 2026-10', 'NEG-409'),
            409,
            headers: cabeceras)),
      );

      await expectLater(
        servicio.crear(mes: DateTime(2026, 10), montoLimite: 500000),
        throwsA(isA<ApiException>()
            .having((e) => e.codigo, 'codigo', 'NEG-409')
            .having((e) => e.mensaje, 'mensaje', contains('Ya existe'))),
      );
    });
  });
}
