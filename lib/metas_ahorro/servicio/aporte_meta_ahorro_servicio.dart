import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../comun/api_exception.dart';
import '../../comun/cliente_api.dart';
import '../../config/api_config.dart';
import '../modelo/aporte_meta_ahorro.dart';

/// Cliente HTTP para gestionar los aportes de las metas de ahorro.
class AporteMetaAhorroServicio {
  static Duration get _timeout => ApiConfig.timeout;

  final http.Client _cliente;

  AporteMetaAhorroServicio({http.Client? cliente})
    : _cliente = cliente ?? ClienteApi();

  String _url(int metaAhorroId) =>
      '${ApiConfig.baseUrl}/metas-ahorro/$metaAhorroId/aportes';

  /// Consulta el historial de aportes de una meta.
  Future<List<AporteMetaAhorro>> listar(int metaAhorroId) async {
    final res = await _cliente
        .get(Uri.parse(_url(metaAhorroId)))
        .timeout(_timeout);

    verificarRespuesta(res);

    final datos = jsonDecode(utf8.decode(res.bodyBytes)) as List<dynamic>;

    return datos
        .map((item) => AporteMetaAhorro.desdeJson(item as Map<String, dynamic>))
        .toList();
  }

  /// Registra un nuevo aporte en una meta.
  /// La fecha es obligatoria (HU004): el backend responde 400 si falta.
  Future<AporteMetaAhorro> registrar({
    required int metaAhorroId,
    required double monto,
    required DateTime fecha,
  }) async {
    final cuerpo = <String, dynamic>{
      'monto': monto,
      'fecha': fecha.toIso8601String().split('T').first,
    };

    final res = await _cliente
        .post(
          Uri.parse(_url(metaAhorroId)),
          headers: const {'Content-Type': 'application/json'},
          body: jsonEncode(cuerpo),
        )
        .timeout(_timeout);

    verificarRespuesta(res);

    return AporteMetaAhorro.desdeJson(
      jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>,
    );
  }

  /// Elimina un aporte de una meta.
  Future<void> eliminar({
    required int metaAhorroId,
    required int aporteId,
  }) async {
    final res = await _cliente
        .delete(Uri.parse('${_url(metaAhorroId)}/$aporteId'))
        .timeout(_timeout);

    verificarRespuesta(res);
  }
}
