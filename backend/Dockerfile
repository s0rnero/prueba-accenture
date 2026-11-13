# syntax=docker/dockerfile:1.7
# Etapa de compilacion con el JDK completo.
FROM eclipse-temurin:25-jdk AS build
# Directorio de trabajo de la etapa de build.
WORKDIR /workspace
# Copia el wrapper y los archivos de Gradle primero para cachear dependencias.
COPY gradlew settings.gradle.kts build.gradle.kts ./
# Copia el directorio del wrapper de Gradle.
COPY gradle ./gradle
# Descarga las dependencias sin compilar, en capas que Docker puede cachear.
RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    chmod +x gradlew && ./gradlew dependencies --no-daemon
# Copia el codigo fuente de la aplicacion.
COPY src ./src
# Compila el jar ejecutable reutilizando las caches de Gradle.
RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    ./gradlew bootJar --no-daemon

# Etapa de runtime: solo el JRE, la imagen final queda mas chica.
FROM eclipse-temurin:25-jre AS runtime
# Copia busybox desde su imagen para tener wget sin instalar paquetes.
COPY --from=busybox:1.36.1-musl /bin/busybox /usr/local/bin/busybox
# Publica wget como enlace al busybox copiado, lo usa el healthcheck.
RUN ln -s /usr/local/bin/busybox /usr/local/bin/wget
# Directorio de trabajo de la aplicacion.
WORKDIR /app
# Crea el usuario sin privilegios y el directorio temporal de la aplicacion.
RUN groupadd --system spring && \
    useradd --system --gid spring --home-dir /nonexistent --shell /usr/sbin/nologin spring && \
    mkdir -p /tmp/franchises && chown -R spring:spring /app /tmp/franchises
# Copia el jar compilado con el usuario final como duenio.
COPY --from=build --chown=spring:spring /workspace/build/libs/franchises-*.jar ./app.jar
# Corre el proceso con el usuario sin privilegios.
USER spring:spring
# Puerto en el que escucha la API.
EXPOSE 8080
# Opciones de JVM: memoria segun el limite del contenedor y apagado ordenado.
ENV JAVA_TOOL_OPTIONS="-Xmx256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=25 -Djava.io.tmpdir=/tmp/franchises -Dserver.shutdown=graceful"
# Comando de arranque de la aplicacion.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
