plugins {
    java
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.accenture"
version = "0.1.0-SNAPSHOT"
description = "Acento APIS"

repositories {
    mavenCentral()
}

// Clase de arranque explicita: el JAR no depende de la deteccion automatica.
springBoot {
    mainClass.set("com.accenture.franchises.Main")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Persistencia reactiva: el runtime de requests no bloquea.
    implementation("org.springframework.boot:spring-boot-starter-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("io.r2dbc:r2dbc-pool")
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    // JDBC se conserva UNICAMENTE para ejecutar Flyway en el arranque.
    // Flyway no soporta R2DBC de forma nativa; ninguna ruta de request lo usa.
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    // Flyway 10+ separa el soporte por motor en modulos propios.
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-r2dbc-test")
    testImplementation("io.projectreactor:reactor-test")
    // El BOM de Boot 4 ya no gestiona Testcontainers: version explicita.
    testImplementation("org.testcontainers:junit-jupiter:1.21.3")
    testImplementation("org.testcontainers:postgresql:1.21.3")
}

// Sin toolchain: se compila con el JDK del entorno y se fija el bytecode a 25.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
