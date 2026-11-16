import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../comun/api_exception.dart';
import '../../comun/cliente_api.dart';
import '../../config/api_config.dart';
import '../../presupuesto/modelo/mes.dart';
import '../modelo/reporte_mensual.dart';

export '../../comun/api_exception.dart' show ApiException;

/// Unico punto del modulo de reporte que habla HTTP (HU025).
class ReporteServicio {
  static Duration get _timeout => ApiConfig.timeout;

  /// El cliente HTTP se puede inyectar para las pruebas (MockClient).
  final http.Client _cliente;

  ReporteServicio({http.Client? cliente})
      : _cliente = cliente ?? ClienteApi();

  final String _base = '${ApiConfig.baseUrl}/reportes';

  /// RQ-25: reporte del mes. Siempre trae datos (200): si el mes no tiene
  /// presupuesto, `presupuesto` llega en null pero el desglose por
  /// categoria igual viene.
  Future<ReporteMensual> consultar(DateTime mes) async {
    final res = await _cliente
        .get(Uri.parse('$_base/mensual?mes=${codigoMes(mes)}'))
        .timeout(_timeout);

    verificarRespuesta(res);
    return ReporteMensual.desdeJson(
        jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>);
  }
}
