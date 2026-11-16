import 'dart:convert';
import 'package:http/http.dart' as http;

import '../../config/api_config.dart';
import '../../comun/api_exception.dart';
import '../modelo/deuda.dart';

// Las pantallas de deudas importan ApiException desde este archivo;
// el export evita tener que cambiar esos imports.
export '../../comun/api_exception.dart' show ApiException;

/// Unico punto de la app que habla HTTP.
/// Las pantallas nunca llaman a http directamente: solo usan este servicio.
/// Es el mismo principio del repositorio en el backend.
class DeudaServicio {
  static const Duration _timeout = Duration(seconds: 10);

  /// El cliente HTTP se puede inyectar para las pruebas (MockClient).
  /// En la app se usa el cliente real.
  final http.Client _cliente;

  DeudaServicio({http.Client? cliente}) : _cliente = cliente ?? http.Client();

  final String _base = '${ApiConfig.baseUrl}/deudas';

  Map<String, String> get _headers => {'Content-Type': 'application/json'};

  Future<List<Deuda>> listar() async {
    final res = await _cliente.get(Uri.parse(_base)).timeout(_timeout);
    _verificar(res);

    final lista = jsonDecode(utf8.decode(res.bodyBytes)) as List<dynamic>;
    return lista
        .map((d) => Deuda.desdeJson(d as Map<String, dynamic>))
        .toList();
  }

  Future<Deuda> obtener(int id) async {
    final res = await _cliente.get(Uri.parse('$_base/$id')).timeout(_timeout);
    _verificar(res);
    return Deuda.desdeJson(
        jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>);
  }

  Future<Deuda> crear({
    required String acreedor,
    required double montoTotal,
    required double tasaInteresAnual,
    required int numeroCuotas,
    required DateTime fechaVencimiento,
  }) async {
    final res = await _cliente.post(
          Uri.parse(_base),
          headers: _headers,
          body: jsonEncode({
            'acreedor': acreedor,
            'montoTotal': montoTotal,
            'tasaInteresAnual': tasaInteresAnual,
            'numeroCuotas': numeroCuotas,
            // El backend espera formato ISO-8601: 2028-09-17
            'fechaVencimiento':
                fechaVencimiento.toIso8601String().split('T').first,
          }),
        )
        .timeout(_timeout);

    _verificar(res);
    return Deuda.desdeJson(
        jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>);
  }

  /// RQ-22: modificar una deuda. El montoTotal no se envia porque
  /// el requisito no permite cambiarlo.
  Future<Deuda> actualizar({
    required int id,
    required String acreedor,
    required double tasaInteresAnual,
    required int numeroCuotas,
    required DateTime fechaVencimiento,
  }) async {
    final res = await _cliente.put(
      Uri.parse('$_base/$id'),
      headers: _headers,
      body: jsonEncode({
        'acreedor': acreedor,
        'tasaInteresAnual': tasaInteresAnual,
        'numeroCuotas': numeroCuotas,
        'fechaVencimiento':
        fechaVencimiento.toIso8601String().split('T').first,
      }),
    )
        .timeout(_timeout);

    _verificar(res);
    return Deuda.desdeJson(
        jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>);
  }


  Future<Deuda> abonar({required int deudaId, required double monto}) async {
    final res = await _cliente.post(
          Uri.parse('$_base/$deudaId/abonos'),
          headers: _headers,
          body: jsonEncode({'monto': monto}),
        )
        .timeout(_timeout);

    _verificar(res);
    return Deuda.desdeJson(
        jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>);
  }

  Future<void> eliminar(int deudaId) async {
    final res =
        await _cliente.delete(Uri.parse('$_base/$deudaId')).timeout(_timeout);
    _verificar(res);
  }

  /// La traduccion de errores esta en lib/comun/api_exception.dart,
  /// compartida con los demas modulos.
  void _verificar(http.Response res) => verificarRespuesta(res);
}
