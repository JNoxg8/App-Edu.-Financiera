import 'dart:convert';

import 'package:finanzas_app/autenticacion/servicio/autenticacion_servicio.dart';
import 'package:finanzas_app/comun/cliente_api.dart';
import 'package:finanzas_app/comun/sesion.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Sesion, cliente con token e inicio de sesion, SIN backend: MockClient
/// simula a Quarkus y shared_preferences usa valores en memoria.
void main() {
  const cabeceras = {'content-type': 'application/json; charset=utf-8'};

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    await Sesion.cargar();
  });

  test('iniciar sesion guarda el token y queda guardado para la proxima vez', () async {
    final servicio = AutenticacionServicio(
      cliente: MockClient((peticion) async {
        expect(peticion.url.path, endsWith('/auth/login'));
        expect(jsonDecode(peticion.body), {'correo': 'ana@mail.com', 'contrasena': 'clave-segura'});
        return http.Response(
            jsonEncode({'token': 'tok-123', 'usuarioId': 4, 'correo': 'ana@mail.com'}), 200,
            headers: cabeceras);
      }),
    );

    await servicio.iniciarSesion(' ana@mail.com ', 'clave-segura');

    expect(Sesion.token, 'tok-123');
    // Al reabrir la app la sesion se recupera del almacenamiento
    await Sesion.cargar();
    expect(Sesion.activa, isTrue);
    expect(Sesion.correo, 'ana@mail.com');
  });

  test('credenciales incorrectas: ApiException y no se abre sesion', () async {
    final servicio = AutenticacionServicio(
      cliente: MockClient((_) async => http.Response(
          jsonEncode({
            'mensaje': 'Correo o contrasena incorrectos',
            'status': 401,
            'error': {'codigo': 'AUT-401', 'detalles': []},
          }),
          401,
          headers: cabeceras)),
    );

    await expectLater(servicio.iniciarSesion('ana@mail.com', 'mala'),
        throwsA(isA<ApiException>().having((e) => e.status, 'status', 401)));
    expect(Sesion.activa, isFalse);
  });

  test('ClienteApi agrega el token a cada peticion', () async {
    await Sesion.iniciar('tok-abc', 'ana@mail.com');
    String? autorizacion;
    final cliente = ClienteApi(MockClient((peticion) async {
      autorizacion = peticion.headers['Authorization'];
      return http.Response('[]', 200);
    }));

    await cliente.get(Uri.parse('https://servidor/zenfi/deudas'));

    expect(autorizacion, 'Bearer tok-abc');
  });

  test('un 401 con token cierra la sesion y avisa a la app', () async {
    await Sesion.iniciar('tok-vencido', 'ana@mail.com');
    final avisosAntes = Sesion.cerrada.value;
    final cliente = ClienteApi(MockClient((_) async => http.Response('{}', 401)));

    await cliente.get(Uri.parse('https://servidor/zenfi/panel'));
    await Future<void>.delayed(Duration.zero);

    expect(Sesion.activa, isFalse);
    expect(Sesion.cerrada.value, avisosAntes + 1);
  });

  test('cerrar sesion borra el token aunque no haya conexion', () async {
    await Sesion.iniciar('tok-xyz', 'ana@mail.com');
    final servicio = AutenticacionServicio(
      cliente: MockClient((_) async => throw http.ClientException('sin red')),
    );

    await servicio.cerrarSesion();

    expect(Sesion.activa, isFalse);
  });
}
