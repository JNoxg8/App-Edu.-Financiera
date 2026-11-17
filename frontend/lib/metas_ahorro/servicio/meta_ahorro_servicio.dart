import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../comun/api_exception.dart';
import '../../comun/cliente_api.dart';
import '../../config/api_config.dart';
import '../modelo/meta_ahorro.dart';

/// Cliente HTTP para la HU003. Las pantallas no llaman HTTP directamente.
class MetaAhorroServicio {
  static Duration get _timeout => ApiConfig.timeout;

  final http.Client _cliente;

  MetaAhorroServicio({http.Client? cliente})
    : _cliente = cliente ?? ClienteApi();

  String get _url => '${ApiConfig.baseUrl}/metas-ahorro';

  Future<MetaAhorro> crear({
    required String nombre,
    required double montoObjetivo,
    required DateTime fechaLimite,
    required double montoInicial,
  }) async {
    final res = await _cliente
        .post(
          Uri.parse(_url),
          headers: const {'Content-Type': 'application/json'},
          body: jsonEncode({
            'nombre': nombre.trim(),
            'montoObjetivo': montoObjetivo,
            'fechaLimite': fechaLimite.toIso8601String().split('T').first,
            'montoInicial': montoInicial,
          }),
        )
        .timeout(_timeout);

    verificarRespuesta(res);
    return MetaAhorro.desdeJson(
      jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>,
    );
  }

  Future<List<MetaAhorro>> listar() async {
    final res = await _cliente.get(Uri.parse(_url)).timeout(_timeout);

    verificarRespuesta(res);

    final datos = jsonDecode(utf8.decode(res.bodyBytes)) as List<dynamic>;

    return datos
        .map((item) => MetaAhorro.desdeJson(item as Map<String, dynamic>))
        .toList();
  }

  /// Elimina la meta (y sus aportes y recordatorios) en el backend.
  /// DELETE /metas-ahorro/{id} responde 204 sin cuerpo.
  Future<void> eliminar(int id) async {
    final res = await _cliente.delete(Uri.parse('$_url/$id')).timeout(_timeout);

    verificarRespuesta(res);
  }
}
