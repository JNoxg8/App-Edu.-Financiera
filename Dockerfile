# ============================================================
#  Zenfi API - imagen para Render
#  A diferencia de src/main/docker/Dockerfile.jvm, compila el
#  proyecto dentro de Docker, porque Render construye la imagen
#  directamente desde el repositorio (no existe target/).
# ============================================================

# ---------------- Etapa 1: compilar ----------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src/main ./src/main
# Las pruebas se ejecutan en local (mvnw verify), no en el despliegue.
RUN mvn -B package -DskipTests

# ---------------- Etapa 2: ejecutar ----------------
FROM eclipse-temurin:21-jre
WORKDIR /deployments
COPY --from=build /app/target/quarkus-app/lib/ lib/
COPY --from=build /app/target/quarkus-app/*.jar ./
COPY --from=build /app/target/quarkus-app/app/ app/
COPY --from=build /app/target/quarkus-app/quarkus/ quarkus/

# update conserva los datos entre reinicios (drop-and-create los borra).
# Los logs van a la consola, que es lo que muestra Render.
ENV DB_ESQUEMA=update \
    QUARKUS_LOG_FILE_ENABLED=false \
    QUARKUS_HIBERNATE_ORM_LOG_SQL=false

# No ejecutar la aplicacion como root.
RUN useradd --system zenfi
USER zenfi

EXPOSE 8862
# Los contenedores corren en UTC: sin la zona de Colombia, desde las 7 p. m.
# LocalDate.now() ya devolveria el dia siguiente (aportes, metas, presupuesto).
CMD ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=America/Bogota", "-jar", "quarkus-run.jar"]
