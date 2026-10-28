
package com.ecommerce.notificaciones.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;
import com.ecommerce.notificaciones.exception.ErrorResponse;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        AuthenticationEntryPoint unauthorized = (req, res, ex) -> {
            res.setHeader("WWW-Authenticate", "Bearer");
            writeError(req, res, 401, "UNAUTHORIZED", "Se requiere autenticacion valida");
        };
        AccessDeniedHandler forbidden = (req, res, ex) ->
                writeError(req, res, 403, "FORBIDDEN", "No tienes permisos para esta operacion");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();

        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");

            if (role == null || role.isBlank()) {
                return List.of();
            }

            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/notifications/*/retry")
                        .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/notifications/*/status")
                        .hasRole("ADMINISTRADOR")

                        // POST: únicamente SERVICE o ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/notifications")
                        .hasAnyRole("SERVICE", "ADMINISTRADOR")

                        // El estado es administrativo; otras consultas requieren autenticación.
                        .requestMatchers(
                                HttpMethod.GET, "/api/notifications/me"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.PATCH, "/api/notifications/*/read"
                        ).authenticated()

                        // Resto de endpoints: JWT obligatorio
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden)
                )
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden)
                )
                .build();
    }

    private void writeError(
            HttpServletRequest req,
            HttpServletResponse res,
            int status,
            String code,
            String message
    ) throws IOException {

        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        res.getWriter().write(JsonMapper.builder().build().writeValueAsString(
                new ErrorResponse(Instant.now().truncatedTo(ChronoUnit.SECONDS),
                        status, code, message, req.getRequestURI())));
    }
}
