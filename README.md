# Zenfi - App Flutter

Cliente móvil de **Zenfi**, la app de educación financiera. Consume la API en Quarkus
(`App-Edu.-Financiera`, backend en `edu.ucentral.zenfi`).

## Conexión con el backend

La URL se arma en `lib/config/api_config.dart` y debe coincidir con `application.yml` del backend:

| Dónde corre la app | URL base |
|---|---|
| Emulador de Android | `http://10.0.2.2:8862/zenfi` |
| Chrome (Flutter Web) | `http://localhost:8862/zenfi` |
| Celular físico | `http://<IP del PC>:8862/zenfi` (ver comentarios en `api_config.dart`) |

Como el backend de desarrollo usa `http://`, Android solo permite ese tráfico hacia los
hosts listados en `android/app/src/main/res/xml/network_security_config.xml`.

## Ejecución

1. Levante el backend (`quarkus:dev`).
2. Encienda el emulador o elija Chrome como dispositivo.
3. `flutter pub get` y luego **Run** (o `flutter run`).

## Pruebas

```shell
flutter test
```

Las pruebas del servicio usan `MockClient`, así que no necesitan el backend encendido.

## Estructura

```
lib/
├── config/api_config.dart     URL del backend
├── main.dart
└── deudas/                    Un paquete por módulo, igual que el backend
    ├── modelo/                Clases que reflejan los DTO de la API
    ├── servicio/              Único punto que habla HTTP
    └── pantallas/             Widgets
```

Application ID: `edu.ucentral.finanzas_app`.
