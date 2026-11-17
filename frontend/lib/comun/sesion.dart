import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// La sesion del usuario en este dispositivo: el token que entrega el
/// backend al registrarse o iniciar sesion.
///
/// Se guarda con shared_preferences (SharedPreferences en Android,
/// localStorage en el navegador) para no pedir la contrasena cada vez que
/// se abre la app. El token vence a los 30 dias en el servidor.
class Sesion {
  Sesion._();

  static const _claveToken = 'zenfi_token';
  static const _claveCorreo = 'zenfi_correo';

  static String? _token;
  static String? _correo;

  /// Cambia cada vez que la sesion termina (cierre de sesion o token
  /// vencido): la pantalla de arranque lo escucha y vuelve al acceso.
  static final ValueNotifier<int> cerrada = ValueNotifier(0);

  static String? get token => _token;
  static String? get correo => _correo;
  static bool get activa => _token != null;

  /// Lee la sesion guardada. Se llama una vez al abrir la app.
  static Future<void> cargar() async {
    final datos = await SharedPreferences.getInstance();
    _token = datos.getString(_claveToken);
    _correo = datos.getString(_claveCorreo);
  }

  static Future<void> iniciar(String token, String correo) async {
    _token = token;
    _correo = correo;
    final datos = await SharedPreferences.getInstance();
    await datos.setString(_claveToken, token);
    await datos.setString(_claveCorreo, correo);
  }

  static Future<void> cerrar() async {
    if (_token == null) return;
    _token = null;
    _correo = null;
    final datos = await SharedPreferences.getInstance();
    await datos.remove(_claveToken);
    await datos.remove(_claveCorreo);
    cerrada.value++;
  }
}
