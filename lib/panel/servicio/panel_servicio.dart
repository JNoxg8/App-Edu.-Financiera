import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../comun/api_exception.dart';
import '../../config/api_config.dart';
import '../modelo/panel_principal.dart';

export '../../comun/api_exception.dart' show ApiException;

/// Unico punto del panel principal que habla HTTP (HU015).
class PanelServicio {
  static const Duration _timeout = Duration(seconds: 10);

  /// El cliente HTTP se puede inyectar para las pruebas (MockClient).
  final http.Client _cliente;

  PanelServicio({http.Client? cliente}) : _cliente = cliente ?? http.Client();

  /// RQ-15: una sola peticion trae las cuatro secciones del panel.
  Future<PanelPrincipal> consultar() async {
    final res = await _cliente
        .get(Uri.parse('${ApiConfig.baseUrl}/panel'))
        .timeout(_timeout);

    verificarRespuesta(res);
    return PanelPrincipal.desdeJson(
        jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>);
  }
}
