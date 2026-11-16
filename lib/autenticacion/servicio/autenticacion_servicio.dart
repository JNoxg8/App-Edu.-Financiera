import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../comun/api_exception.dart';
import '../../comun/cliente_api.dart';
import '../../comun/sesion.dart';
import '../../config/api_config.dart';

export '../../comun/api_exception.dart' show ApiException;

/// Comunicacion con /auth: crear cuenta, iniciar y cerrar sesion.
class AutenticacionServicio {
  static Duration get _timeout => ApiConfig.timeout;

  final http.Client _cliente;

  AutenticacionServicio({http.Client? cliente})
      : _cliente = cliente ?? ClienteApi();

  String get _base => '${ApiConfig.baseUrl}/auth';

  /// Crea la cuenta y deja la sesion iniciada.
  Future<void> registrar(String correo, String contrasena) =>
      _entrar('registro', correo, contrasena);

  Future<void> iniciarSesion(String correo, String contrasena) =>
      _entrar('login', correo, contrasena);

  /// Invalida el token en el servidor y lo borra del dispositivo. Aunque
  /// no haya conexion, la sesion se cierra en el dispositivo igual.
  Future<void> cerrarSesion() async {
    try {
      await _cliente.post(Uri.parse('$_base/logout')).timeout(_timeout);
    } catch (_) {
      // Sin conexion: el token vencera solo en el servidor
    }
    await Sesion.cerrar();
  }

  Future<void> _entrar(String accion, String correo, String contrasena) async {
    final res = await _cliente
        .post(
          Uri.parse('$_base/$accion'),
          headers: const {'Content-Type': 'application/json'},
          body: jsonEncode({'correo': correo.trim(), 'contrasena': contrasena}),
        )
        .timeout(_timeout);

    verificarRespuesta(res);
    final datos = jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
    await Sesion.iniciar(datos['token'] as String, datos['correo'] as String);
  }
}
