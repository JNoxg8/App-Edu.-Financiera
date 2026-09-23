# Zenfi API

Backend en **Quarkus** de la app de educación financiera **Zenfi** (cliente en Flutter).
La configuración sigue la plantilla `gestortecno` entregada por el profesor, y los
módulos siguen la arquitectura hexagonal del repositorio guía `quarkusgestiondocente`.

## Requisitos

- JDK **25** (Eclipse Temurin)
- PostgreSQL con la base de datos y el usuario creados
- IntelliJ IDEA (Maven viene incluido en el proyecto con `mvnw`)

## Configuración local

1. Copie `.env.example` como `.env` en la raíz del proyecto.
2. Ajuste `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` según su PostgreSQL.
3. El archivo `.env` está en `.gitignore`: **nunca se sube a GitHub**.

## Ejecución

En IntelliJ: panel **Maven → Plugins → quarkus → quarkus:dev**.
Desde la terminal (con `JAVA_HOME` apuntando al JDK 25):

```shell
./mvnw quarkus:dev        # Linux / macOS
mvnw.cmd quarkus:dev      # Windows
```

| Recurso | URL |
|---|---|
| API | http://localhost:8862/zenfi |
| Versión | http://localhost:8862/zenfi/genericos/version |
| Swagger UI | http://localhost:8862/zenfi/q/swagger-ui |
| Dev UI | http://localhost:8862/zenfi/q/dev-ui |

Desde el emulador de Android la API se consume en `http://10.0.2.2:8862/zenfi`.

## Pruebas

```shell
./mvnw test
```

Las pruebas usan **H2 en memoria** (perfil `%test`): no necesitan PostgreSQL, Docker ni el archivo `.env`.

## Estructura

```
edu.ucentral.zenfi
├── common
│   ├── excepcion         Excepciones de negocio y ExceptionMapper (400, 404, 409)
│   └── infraestructura   ResponseApi y ResponseApiError (formato estándar de respuesta)
├── genericos             GET /genericos/version
└── deudas                Un paquete por módulo (hexagonal)
    ├── dominio
    │   ├── modelo        Java puro: reglas y cálculos financieros
    │   └── repositorio   Puerto (interfaz)
    ├── aplicacion        Servicio: orquesta los casos de uso
    └── infraestructura   Recurso REST, DTOs (records) y persistencia con Panache
```

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

Códigos internos: `VAL-400` (validación), `REC-404` (no encontrado), `NEG-409` (regla de negocio).

## Decisiones respecto a la plantilla `gestortecno`

Se adoptó la plantilla (paquete `edu.ucentral`, Java 25, Quarkus 3.39.4, `application.yml`
con variables en `.env`, `ResponseApi` con Lombok, OpenAPI, logs con rotación y endpoint de
versión) con estos ajustes justificados:

1. **CORS solo en `application.yml`.** No se incluye el `CorsFilter` porque fija el origen en
   `http://localhost:4200` (Angular), mientras que Flutter Web usa un puerto aleatorio. Además,
   tener dos mecanismos de CORS a la vez puede duplicar cabeceras, lo que los navegadores rechazan.
2. **Pruebas con H2.** La prueba de la plantilla consulta `/hello`, que no existe. Se reemplazó por
   `GenericoRecursoTest` y las pruebas usan H2 en memoria para ejecutarse en cualquier equipo.
3. **Sin `quarkus-hibernate-orm-rest-data-panache`.** Esa extensión genera endpoints CRUD directamente
   desde las entidades, saltando las capas de dominio y aplicación donde viven las reglas de negocio.
4. **Sin `quarkus-logging-json`.** Convierte la consola en JSON (pensado para producción), lo que
   dificulta leer los logs durante el desarrollo.
5. **DTOs como `record`.** Lombok se usa en `ResponseApi`; los DTOs son records de Java (inmutables
   y sin dependencias externas).
6. **Claves de configuración corregidas.** Se usan las claves oficiales de Quarkus para CORS
   (`quarkus.http.cors.*`) y OpenAPI (`quarkus.smallrye-openapi.info-*`), y el campo `success`
   (en la plantilla aparece como `succes`).
7. **`logs/` en `.gitignore`**, para no subir los archivos de log al repositorio.
