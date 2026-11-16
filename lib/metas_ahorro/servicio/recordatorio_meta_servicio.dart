import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../comun/api_exception.dart';
import '../../comun/cliente_api.dart';
import '../../config/api_config.dart';
import '../modelo/recordatorio_meta.dart';

class RecordatorioMetaServicio {
  static Duration get _timeout => ApiConfig.timeout;

  final http.Client _cliente;

  RecordatorioMetaServicio({http.Client? cliente})
    : _cliente = cliente ?? ClienteApi();

  String _url(int metaAhorroId) =>
      '${ApiConfig.baseUrl}/metas-ahorro/$metaAhorroId/recordatorio';

  Future<RecordatorioMeta> consultar(int metaAhorroId) async {
    final res = await _cliente
        .get(Uri.parse(_url(metaAhorroId)))
        .timeout(_timeout);

    verificarRespuesta(res);

    return RecordatorioMeta.desdeJson(
      jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>,
    );
  }

  Future<RecordatorioMeta> configurar({
    required int metaAhorroId,
    required String frecuencia,
    required bool activo,
  }) async {
    final res = await _cliente
        .put(
          Uri.parse(_url(metaAhorroId)),
          headers: const {'Content-Type': 'application/json'},
          body: jsonEncode({'frecuencia': frecuencia, 'activo': activo}),
        )
        .timeout(_timeout);

    verificarRespuesta(res);

    return RecordatorioMeta.desdeJson(
      jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>,
    );
  }
}
