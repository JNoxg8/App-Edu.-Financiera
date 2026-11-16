import 'package:finanzas_app/config/api_config.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';

/// Verifica que la URL base coincida con la configuracion del backend
/// (puerto 8862 y root-path /zenfi en application.yml).
void main() {
  tearDown(() => debugDefaultTargetPlatformOverride = null);

  test('En Android usa 10.0.2.2, el puerto 8862 y la ruta /zenfi', () {
    debugDefaultTargetPlatformOverride = TargetPlatform.android;

    expect(ApiConfig.baseUrl, 'http://10.0.2.2:8862/zenfi');
  });

  test('En escritorio usa localhost', () {
    debugDefaultTargetPlatformOverride = TargetPlatform.windows;

    expect(ApiConfig.baseUrl, 'http://localhost:8862/zenfi');
  });
}
