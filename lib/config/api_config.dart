import 'package:flutter/foundation.dart'
    show defaultTargetPlatform, kIsWeb, TargetPlatform;

/// Centraliza la URL del backend en un solo lugar.
///
/// Por defecto la app usa el backend desplegado en Render. Para trabajar
/// con el backend local (`.\mvnw.cmd quarkus:dev`) se compila con:
///
///     flutter run -d chrome --dart-define=BACKEND=local
///
/// En Android Studio: Run > Edit Configurations > "Additional run args".
///
/// El backend local escucha en el puerto 8862 y publica todas sus rutas
/// bajo /zenfi:  http://`<host>`:8862/zenfi/deudas
///
/// El emulador de Android no puede usar "localhost": para la app dentro
/// del Android virtual, localhost es el propio emulador. La direccion
/// 10.0.2.2 es un alias que el emulador ofrece para llegar a la maquina
/// anfitriona (tu PC).
///
/// Nota: se usa defaultTargetPlatform de flutter/foundation en lugar de
/// Platform de dart:io, porque dart:io no existe en Flutter Web y
/// romperia la compilacion para esa plataforma.
class ApiConfig {
  /// Backend desplegado (HTTPS, sin puerto: Render lo publica en el 443).
  static const String urlRender =
      'https://app-edu-financiera.onrender.com/zenfi';

  /// Se fija al compilar con --dart-define=BACKEND=local; si no se indica,
  /// se usa Render. Es const: no se puede cambiar con la app corriendo.
  static const String _backend =
      String.fromEnvironment('BACKEND', defaultValue: 'render');

  static bool get usaRender => _backend != 'local';

  /// Debe coincidir con quarkus.http.port en application.yml
  static const int _puerto = 8862;

  /// Debe coincidir con quarkus.http.root-path en application.yml
  static const String _rutaRaiz = '/zenfi';

  /// Para probar en un telefono fisico contra el backend LOCAL, conectado
  /// a la misma red WiFi:
  ///   1. Averigua la IP de tu PC con `ipconfig` (Windows), en
  ///      "Direccion IPv4" del adaptador WiFi.
  ///   2. Ponla en _ipManual y cambia _usarIpManual a true.
  ///   3. Agrega esa misma IP en
  ///      android/app/src/main/res/xml/network_security_config.xml
  static const bool _usarIpManual = false;
  static const String _ipManual = '192.168.1.10';

  static String get _host {
    if (_usarIpManual) return _ipManual;

    // En web el navegador si puede resolver localhost normalmente
    if (kIsWeb) return 'localhost';

    if (defaultTargetPlatform == TargetPlatform.android) return '10.0.2.2';

    // Linux, Windows, macOS, iOS
    return 'localhost';
  }

  /// URL del backend local segun la plataforma.
  static String get urlLocal => 'http://$_host:$_puerto$_rutaRaiz';

  static String get baseUrl => usaRender ? urlRender : urlLocal;

  /// Cuanto esperar una respuesta. En el plan gratuito de Render el
  /// servidor se apaga sin trafico y la primera peticion lo vuelve a
  /// encender: puede tardar cerca de un minuto.
  static Duration get timeout =>
      usaRender ? const Duration(seconds: 75) : const Duration(seconds: 10);
}
