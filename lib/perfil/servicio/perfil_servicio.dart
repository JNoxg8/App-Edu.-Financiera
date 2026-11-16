import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../comun/api_exception.dart';
import '../../comun/cliente_api.dart';
import '../../comun/moneda.dart';
import '../../config/api_config.dart';
import '../modelo/perfil.dart';

/// Comunicacion con /perfil (HU001).
class PerfilServicio {
  static Duration get _timeout => ApiConfig.timeout;

  final http.Client _cliente;

  PerfilServicio({http.Client? cliente}) : _cliente = cliente ?? ClienteApi();

  final String _base = '${ApiConfig.baseUrl}/perfil';

  /// Devuelve el perfil, o null si todavia no se ha creado.
  ///
  /// El 404 NO se trata como error: es la senal de que el usuario entra
  /// por primera vez y hay que mostrarle el formulario de creacion.
  Future<Perfil?> obtener() async {
    final res = await _cliente.get(Uri.parse(_base)).timeout(_timeout);
    if (res.statusCode == 404) return null;

    verificarRespuesta(res);
    return Perfil.desdeJson(
        jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>);
  }

  Future<Perfil> crear({
    required String nombre,
    required Moneda moneda,
    required NivelConocimiento nivel,
  }) async {
    final res = await _cliente
        .post(
          Uri.parse(_base),
          headers: {'Content-Type': 'application/json'},
          body: jsonEncode({
            'nombre': nombre,
            'moneda': moneda.codigo,
            'nivel': nivel.codigo,
          }),
        )
        .timeout(_timeout);

    verificarRespuesta(res);
    return Perfil.desdeJson(
        jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>);
  }
}
