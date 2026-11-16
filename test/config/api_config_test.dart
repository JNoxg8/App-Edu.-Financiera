import 'package:finanzas_app/config/api_config.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';

/// Verifica la URL del backend: Render por defecto y, con
/// --dart-define=BACKEND=local, el backend local (puerto 8862 y root-path
/// /zenfi, como en application.yml).
void main() {
  tearDown(() => debugDefaultTargetPlatformOverride = null);

  test('Por defecto usa el backend de Render por HTTPS y espera hasta 75 s', () {
    expect(ApiConfig.usaRender, isTrue);
    expect(ApiConfig.baseUrl, 'https://app-edu-financiera.onrender.com/zenfi');
    expect(ApiConfig.timeout, const Duration(seconds: 75));
  });

  test('El backend local en Android usa 10.0.2.2, el puerto 8862 y /zenfi', () {
    debugDefaultTargetPlatformOverride = TargetPlatform.android;

    expect(ApiConfig.urlLocal, 'http://10.0.2.2:8862/zenfi');
  });

  test('El backend local en escritorio usa localhost', () {
    debugDefaultTargetPlatformOverride = TargetPlatform.windows;

    expect(ApiConfig.urlLocal, 'http://localhost:8862/zenfi');
  });
}
