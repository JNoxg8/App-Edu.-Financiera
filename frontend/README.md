# Zenfi - App Flutter

Cliente de **Zenfi**, la app de educación financiera, para Android y web. Consume la API en
Quarkus que está en la raíz de este mismo repositorio (`src/`, paquete `edu.ucentral.zenfi`).

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

## Publicar una versión nueva

Desde esta carpeta (`frontend/`):

```powershell
.\publicar.ps1            # versión web + APK
.\publicar.ps1 -SoloWeb   # solo la versión web
```

El script compila la app y copia el resultado a `src/main/resources/META-INF/resources/` del
backend. Después haga commit y push a `develop`: Render vuelve a desplegar y la app queda en
https://app-edu-financiera.onrender.com/zenfi/ (el APK en `/zenfi/zenfi.apk`).

## Ícono

El ícono (anillo de progreso con una Z, en los colores de la app) se dibuja con
`tool/generar_icono.py` (Python + Pillow) y `flutter_launcher_icons` lo convierte a todos los
tamaños de Android y web. Para regenerarlo:

```shell
python tool/generar_icono.py assets/icono
dart run flutter_launcher_icons
```

## Estructura

```
lib/
├── main.dart
├── config/api_config.dart     URL del backend (Render o local)
├── comun/                     Tema, cliente HTTP con el token, sesión, gráficos, menú lateral
├── inicio/                    Arranque (pantalla de acceso o inicio) y menú principal
├── autenticacion/             Registro e inicio de sesión
├── panel/                     Resumen de la pantalla de inicio
├── movimientos/  presupuesto/  deudas/  metas_ahorro/
├── notificaciones/  reporte/  perfil/
│   ├── modelo/                Clases que reflejan los DTO de la API
│   ├── servicio/              Único punto que habla HTTP
│   └── pantallas/             Widgets
```

Application ID: `edu.ucentral.finanzas_app`.
