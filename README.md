# Zenfi - App Flutter

Cliente móvil de **Zenfi**, la app de educación financiera. Consume la API en Quarkus
(`App-Edu.-Financiera`, backend en `edu.ucentral.zenfi`).

## Conexión con el backend

La URL se arma en `lib/config/api_config.dart`. **Por defecto la app usa el backend
desplegado en Render:**

`https://app-edu-financiera.onrender.com/zenfi`

Para trabajar con el backend local se compila con `--dart-define=BACKEND=local`
(en Android Studio: *Run > Edit Configurations > Additional run args*):

```shell
flutter run -d chrome --dart-define=BACKEND=local
```

| Backend | Dónde corre la app | URL base |
|---|---|---|
| Render (por defecto) | Cualquiera | `https://app-edu-financiera.onrender.com/zenfi` |
| Local | Emulador de Android | `http://10.0.2.2:8862/zenfi` |
| Local | Chrome (Flutter Web) | `http://localhost:8862/zenfi` |
| Local | Celular físico | `http://<IP del PC>:8862/zenfi` (ver comentarios en `api_config.dart`) |

En el plan gratuito de Render el servidor se apaga tras unos minutos sin uso: la primera
petición lo vuelve a encender y puede tardar cerca de un minuto. Por eso, contra Render la
app espera hasta 75 segundos por respuesta y, al arrancar, avisa que el servidor se está
activando.

Como el backend local usa `http://`, Android solo permite ese tráfico hacia los hosts
listados en `android/app/src/main/res/xml/network_security_config.xml`. Render usa
`https://`, así que no necesita esa excepción.

## Ejecución

- **Contra Render:** `flutter pub get` y luego **Run** (o `flutter run -d chrome`).
- **Contra el backend local:** levante el backend (`.\mvnw.cmd quarkus:dev`) y ejecute con
  `--dart-define=BACKEND=local`.

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
