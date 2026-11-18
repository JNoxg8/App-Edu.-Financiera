# Zenfi API

Backend en **Quarkus** de la app de educación financiera **Zenfi** (cliente en Flutter).
La configuración sigue la plantilla `gestortecno` entregada por el profesor, y los
módulos siguen la arquitectura hexagonal del repositorio guía `quarkusgestiondocente`.

## Estructura del repositorio

| Carpeta | Contenido |
|---|---|
| `src/` | Backend en Quarkus (API REST) |
| `frontend/` | App en Flutter (Android y web). Ver `frontend/README.md` |
| `src/main/resources/META-INF/resources/` | Versión web compilada de la app y el APK, que el backend publica |

## Aplicación desplegada (Render)

| Medio | Enlace |
|---|---|
| Navegador | https://app-edu-financiera.onrender.com/zenfi/ |
| APK para Android | https://app-edu-financiera.onrender.com/zenfi/zenfi.apk |
| API (Swagger) | https://app-edu-financiera.onrender.com/zenfi/q/swagger-ui |

Render vuelve a desplegar cada vez que se actualiza la rama `develop`. Para publicar una
versión nueva de la app, desde `frontend/` ejecute `.\publicar.ps1` (compila la web y el APK y
los copia al backend) y luego haga commit y push a `develop`.

Cada usuario crea su cuenta (`POST /auth/registro`) y la API le exige el token de su sesión
(`Authorization: Bearer <token>`); en Swagger se ingresa con el botón **Authorize**.

## Requisitos

- JDK **21 o superior** (Eclipse Temurin). El proyecto compila para Java 21, la misma versión
  que usa Render; un JDK más nuevo (por ejemplo 25) también sirve.
- PostgreSQL con la base de datos y el usuario creados
- IntelliJ IDEA (Maven viene incluido en el proyecto con `mvnw`)
- Para la app: Flutter (ver `frontend/README.md`)

## Configuración local

Cree un archivo `.env` en la raíz del proyecto con estas variables. Quarkus lo lee al arrancar:

```properties
DB_URL=jdbc:postgresql://localhost:5432/zenfi
DB_USERNAME=zenfi
DB_PASSWORD=<su contraseña local>
# Opcional: update (por defecto) conserva los datos; drop-and-create borra y recrea las tablas
DB_ESQUEMA=update
```

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_URL` | Sí | URL JDBC de PostgreSQL (empieza por `jdbc:postgresql://`) |
| `DB_USERNAME` | Sí | Usuario de la base |
| `DB_PASSWORD` | Sí | Contraseña de la base |
| `DB_ESQUEMA` | No | `update` (por defecto) o `drop-and-create` |
| `PORT` | No | Puerto HTTP; por defecto `8862` (Render lo asigna solo) |

- El archivo `.env` está en `.gitignore`: **nunca se sube a GitHub**.
- **No ponga en su `.env` la URL de la base de Render.** Use siempre una base local, así las
  pruebas manuales nunca tocan los datos reales de los usuarios.

## Ejecución

En IntelliJ: panel **Maven → Plugins → quarkus → quarkus:dev**.
Desde la terminal (con `JAVA_HOME` apuntando al JDK):

```shell
./mvnw quarkus:dev        # Linux / macOS
.\mvnw.cmd quarkus:dev    # Windows (PowerShell)
```

| Recurso | URL |
|---|---|
| App web (si está compilada) | http://localhost:8862/zenfi/ |
| API | http://localhost:8862/zenfi |
| Versión | http://localhost:8862/zenfi/genericos/version |
| Swagger UI | http://localhost:8862/zenfi/q/swagger-ui |
| Dev UI | http://localhost:8862/zenfi/q/dev-ui |

Desde el emulador de Android la API se consume en `http://10.0.2.2:8862/zenfi`.

## Autenticación

Todas las rutas, salvo `/auth/registro`, `/auth/login` y `/genericos`, exigen el encabezado
`Authorization: Bearer <token>`. El token lo entrega el registro o el inicio de sesión.

- El token es un valor aleatorio de 256 bits; en la base solo se guarda su SHA-256.
- La contraseña se guarda cifrada con BCrypt.
- Las sesiones duran 30 días. Las vencidas se borran al arrancar y cada día a las 3 a. m.
- Cada usuario solo ve sus propios datos. Si pide un recurso de otro usuario, recibe 404,
  igual que si no existiera, para no revelar qué identificadores existen.

## Pruebas

```shell
./mvnw test
```

Las pruebas usan **H2 en memoria** (perfil `%test`): no necesitan PostgreSQL, Docker ni el archivo `.env`.

## Estructura del backend

```
edu.ucentral.zenfi
├── common
│   ├── excepcion         Excepciones de negocio y ExceptionMapper (formato ResponseApi)
│   ├── infraestructura   ResponseApi, ResponseApiError y migraciones de datos al arrancar
│   └── seguridad         UsuarioActual: el usuario dueño de la petición
├── genericos             GET /genericos/version
├── autenticacion         Registro, inicio y cierre de sesión, filtro del token
├── perfil                Datos del usuario y preferencias
├── movimientos           Ingresos, gastos y categorías
├── presupuesto           Presupuesto mensual y su estado
├── deudas                Deudas, cuotas y abonos
├── metasahorro           Metas de ahorro y aportes
├── notificaciones        Recordatorios de las metas y notificaciones
├── reporte               Reporte mensual
└── panel                 Resumen de la pantalla de inicio
```

Cada módulo sigue la arquitectura hexagonal:

```
modulo
├── dominio
│   ├── modelo        Java puro: reglas y cálculos financieros
│   ├── repositorio   Puertos de salida (interfaces) hacia la base de datos
│   └── puerto        Puertos hacia otros módulos
├── aplicacion        Servicio: orquesta los casos de uso
└── infraestructura   Recurso REST, DTOs (records) y adaptadores (Panache, otros módulos)
```

Los módulos se comunican con eventos de CDI (patrón *Observer*). Por ejemplo, un abono a una
deuda o un aporte a una meta generan un gasto en `movimientos`.

## Formato de errores

```json
{
  "mensaje": "El abono excede el saldo pendiente, que es 100000.00",
  "codigo": "Regla de negocio",
  "timestamp": "2026-09-22T15:04:05Z",
  "path": "/zenfi/deudas/1/abonos",
  "status": 409,
  "success": false,
  "error": { "mensaje": "...", "codigo": "NEG-409", "detalles": [] }
}
```

| Código interno | HTTP | Cuándo |
|---|---|---|
| `VAL-400` | 400 | Un campo no cumple la validación (`detalles` lista los campos) |
| `JSON-400` | 400 | Valor con tipo o formato equivocado en el JSON |
| `HTTP-4xx` | 400, 404, 405, 415… | Error del protocolo: JSON mal escrito, método no permitido, etc. |
| `AUT-401` | 401 | Falta el token, venció o las credenciales son incorrectas |
| `REC-404` | 404 | El recurso no existe o es de otro usuario |
| `NEG-409` | 409 | Se viola una regla de negocio o el registro ya existe |
| `ERR-500` | 500 | Error inesperado. El mensaje trae un código de referencia para buscar el detalle en el log |
