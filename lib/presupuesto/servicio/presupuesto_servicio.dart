import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../comun/api_exception.dart';
import '../../config/api_config.dart';
import '../modelo/mes.dart';
import '../modelo/presupuesto.dart';

export '../../comun/api_exception.dart' show ApiException;

/// Unico punto del modulo de presupuesto que habla HTTP (HU011).
/// Las pantallas nunca llaman a http directamente: solo usan este servicio.
class PresupuestoServicio {
  static const Duration _timeout = Duration(seconds: 10);

  /// El cliente HTTP se puede inyectar para las pruebas (MockClient).
  final http.Client _cliente;

  PresupuestoServicio({http.Client? cliente})
      : _cliente = cliente ?? http.Client();

  final String _base = '${ApiConfig.baseUrl}/presupuestos';

  Map<String, String> get _headers => {'Content-Type': 'application/json'};

  /// Presupuesto del mes con lo gastado y el porcentaje utilizado.
  ///
  /// Devuelve null si ese mes todavia no tiene presupuesto, para que la
  /// pantalla invite a crearlo en vez de mostrar un error.
  Future<Presupuesto?> consultar(DateTime mes) async {
    final res = await _cliente
        .get(Uri.parse('$_base?mes=${codigoMes(mes)}'))
        .timeout(_timeout);

    try {
      verificarRespuesta(res);
    } on ApiException catch (e) {
      // Solo REC-404 significa "el mes no tiene presupuesto". Un 404 sin ese
      // codigo (ruta inexistente, backend desactualizado) si es un error.
      if (e.codigo == 'REC-404') return null;
      rethrow;
    }

    return Presupuesto.desdeJson(
        jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>);
  }

  /// RQ-11: definir el presupuesto de un mes. El backend responde 409 si
  /// el mes ya tiene presupuesto o si ya paso.
  Future<Presupuesto> crear({
    required DateTime mes,
    required double montoLimite,
  }) async {
    final res = await _cliente
        .post(
          Uri.parse(_base),
          headers: _headers,
          body: jsonEncode({
            'mes': codigoMes(mes),
            'montoLimite': montoLimite,
          }),
        )
        .timeout(_timeout);

    verificarRespuesta(res);
    return Presupuesto.desdeJson(
        jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>);
  }
}
