import 'dart:convert';

import 'package:http/http.dart' as http;

/// Error que conserva el mensaje enviado por el backend, para
/// mostrarselo tal cual al usuario.
///
/// Vive en lib/comun porque lo usan todos los modulos (deudas, perfil...).
/// Si estuviera dentro de deudas, el perfil dependeria del modulo de deudas.
class ApiException implements Exception {
  final String mensaje;
  final List<String> detalles;

  /// Codigo interno del backend: VAL-400, JSON-400, REC-404 o NEG-409.
  final String? codigo;

  /// Codigo HTTP de la respuesta (400, 404, 409...). Null si no hubo respuesta.
  final int? status;

  ApiException(this.mensaje,
      [this.detalles = const [], this.codigo, this.status]);

  @override
  String toString() =>
      detalles.isEmpty ? mensaje : '$mensaje\n${detalles.join('\n')}';
}

/// Traduce la respuesta de error del backend a una ApiException.
///
/// El backend responde todos los errores con el formato ResponseApi:
///
///   {
///     "mensaje": "Error de validacion en los datos de entrada",
///     "status": 400,
///     "success": false,
///     "error": {
///       "codigo": "VAL-400",
///       "detalles": ["nombre: El nombre es obligatorio"]
///     }
///   }
///
/// Si el codigo HTTP es 2xx no hace nada.
void verificarRespuesta(http.Response res) {
  if (res.statusCode >= 200 && res.statusCode < 300) return;

  try {
    final cuerpo =
        jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
    final error = cuerpo['error'] as Map<String, dynamic>?;

    throw ApiException(
      cuerpo['mensaje'] as String? ??
          error?['mensaje'] as String? ??
          'Error ${res.statusCode}',
      (error?['detalles'] as List<dynamic>? ?? []).cast<String>(),
      error?['codigo'] as String?,
      res.statusCode,
    );
  } on ApiException {
    rethrow;
  } catch (_) {
    throw ApiException('Error ${res.statusCode} al comunicarse con el servidor',
        const [], null, res.statusCode);
  }
}
