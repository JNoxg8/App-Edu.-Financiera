import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../comun/api_exception.dart';
import '../../comun/cliente_api.dart';
import '../../config/api_config.dart';
import '../modelo/notificacion.dart';

class NotificacionServicio {
  static Duration get _timeout => ApiConfig.timeout;

  final http.Client _cliente;

  NotificacionServicio({http.Client? cliente})
      : _cliente = cliente ?? ClienteApi();

  String get _url => '${ApiConfig.baseUrl}/notificaciones';

  Future<List<Notificacion>> listar() async {
    final res = await _cliente
        .get(Uri.parse(_url))
        .timeout(_timeout);

    verificarRespuesta(res);

    final datos = jsonDecode(utf8.decode(res.bodyBytes)) as List<dynamic>;

    return datos
        .map((item) => Notificacion.desdeJson(
              item as Map<String, dynamic>,
            ))
        .toList();
  }

  Future<List<Notificacion>> listarNoLeidas() async {
    final res = await _cliente
        .get(Uri.parse('$_url/no-leidas'))
        .timeout(_timeout);

    verificarRespuesta(res);

    final datos = jsonDecode(utf8.decode(res.bodyBytes)) as List<dynamic>;

    return datos
        .map((item) => Notificacion.desdeJson(
              item as Map<String, dynamic>,
            ))
        .toList();
  }

  Future<Notificacion> marcarComoLeida(int id) async {
    final res = await _cliente
        .patch(Uri.parse('$_url/$id/leida'))
        .timeout(_timeout);

    verificarRespuesta(res);

    return Notificacion.desdeJson(
      jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>,
    );
  }
}
