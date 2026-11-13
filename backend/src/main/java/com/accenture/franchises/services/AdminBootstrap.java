package com.accenture.franchises.services;

import com.accenture.franchises.config.SecurityProperties;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final int ATTEMPTS = 3;

    private final SecurityProperties properties;
    private final UserService users;

    public AdminBootstrap(SecurityProperties properties, UserService users) {
        this.properties = properties;
        this.users = users;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = properties.adminEmail();
        String password = properties.adminPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("ADMIN_EMAIL/ADMIN_PASSWORD no configurados: no se crea administrador inicial");
            return;
        }
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                ensure(email, password);
                return;
            } catch (RuntimeException error) {
                log.warn("No se pudo verificar el administrador inicial (intento {} de {}): {}",
                        attempt, ATTEMPTS, error.getMessage());
                pause();
            }
        }
        log.error("No se pudo crear el administrador inicial: la aplicacion arranca igual");
    }

    private void ensure(String email, String password) {
        boolean exists = users.findByEmail(email).blockOptional(Duration.ofSeconds(30)).isPresent();
        if (exists) {
            return;
        }
        users.create(email, password, "Administrador", List.of("ADMIN"))
                .block(Duration.ofSeconds(30));
        log.info("Administrador inicial creado: {}", email);
    }

    private void pause() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
