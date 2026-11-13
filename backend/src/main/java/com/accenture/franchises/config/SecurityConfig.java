package com.accenture.franchises.config;

import com.accenture.franchises.security.CookieAuthWebFilter;
import com.accenture.franchises.security.JwtService;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, JwtService jwtService) {
        return http
                // CSRF se resuelve en CsrfCookieWebFilter (doble cookie), no en el filtro estandar.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(exchanges -> exchanges
                        // Preflight CORS: solo para la API, no para el resto del servidor.
                        .pathMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        // Lista exacta a proposito: "/api/v1/auth/**" dejaria publico /auth/me.
                        .pathMatchers("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/csrf").permitAll()
                        .pathMatchers("/actuator/health").permitAll()
                        // Editar el propio perfil: permiso propio, sin UPDATE global.
                        .pathMatchers(HttpMethod.PATCH, "/api/v1/auth/me").hasAuthority("UPDATE_OWN")
                        .pathMatchers("/api/v1/users/**").hasRole("ADMIN")
                        // Permisos por verbo sobre el dominio de franquicias.
                        .pathMatchers(HttpMethod.GET, "/api/v1/franchises/**").hasAuthority("READ")
                        .pathMatchers(HttpMethod.POST, "/api/v1/franchises/**").hasAuthority("CREATE")
                        .pathMatchers(HttpMethod.PATCH, "/api/v1/franchises/**").hasAuthority("UPDATE")
                        .pathMatchers(HttpMethod.PUT, "/api/v1/franchises/**").hasAuthority("UPDATE")
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/franchises/**").hasAuthority("DELETE")
                        .pathMatchers("/api/v1/**").authenticated()
                        // Denegar por defecto: toda ruta no declarada arriba se rechaza.
                        .anyExchange().denyAll())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN)))
                .addFilterAt(new CookieAuthWebFilter(jwtService), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.corsAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Accept", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
