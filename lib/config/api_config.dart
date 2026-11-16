import 'package:flutter/foundation.dart'
    show defaultTargetPlatform, kIsWeb, TargetPlatform;

/// Centraliza la URL del backend en un solo lugar.
///
/// El backend (plantilla gestortecno) escucha en el puerto 8862 y publica
/// todas sus rutas bajo /zenfi:  http://`<host>`:8862/zenfi/deudas
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
  /// Debe coincidir con quarkus.http.port en application.yml
  static const int _puerto = 8862;

  /// Debe coincidir con quarkus.http.root-path en application.yml
  static const String _rutaRaiz = '/zenfi';

  /// Para probar en un telefono fisico conectado a la misma red WiFi:
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

  static String get baseUrl => 'http://$_host:$_puerto$_rutaRaiz';
}

