import 'dart:async';

import 'package:http/http.dart' as http;

import 'sesion.dart';

/// Cliente HTTP de la app: agrega el token de la sesion a cada peticion
/// (`Authorization: Bearer <token>`).
///
/// Si el servidor responde 401 a una peticion que SI llevaba token, la
/// sesion vencio o se cerro: se borra y la app vuelve a la pantalla de
/// acceso. Un 401 sin token (por ejemplo, una contrasena equivocada al
/// iniciar sesion) no cierra nada.
class ClienteApi extends http.BaseClient {
  final http.Client _interno;

  ClienteApi([http.Client? interno]) : _interno = interno ?? http.Client();

  @override
  Future<http.StreamedResponse> send(http.BaseRequest peticion) async {
    final token = Sesion.token;
    if (token != null) {
      peticion.headers['Authorization'] = 'Bearer $token';
    }
    final respuesta = await _interno.send(peticion);
    if (respuesta.statusCode == 401 && token != null) {
      unawaited(Sesion.cerrar());
    }
    return respuesta;
  }

  @override
  void close() => _interno.close();
}
