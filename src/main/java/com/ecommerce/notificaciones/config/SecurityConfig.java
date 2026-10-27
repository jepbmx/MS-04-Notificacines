package com.ecommerce.notificaciones.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(AppSecurityProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // Crea los usuarios en memoria desde las propiedades; omite los que no tengan usuario/contraseña
    @Bean
    public UserDetailsService userDetailsService(AppSecurityProperties props, PasswordEncoder encoder) {
        List<UserDetails> users = new ArrayList<>();
        if (props.users() != null) {
            for (AppSecurityProperties.UserEntry u : props.users()) {
                if (u.username() == null || u.username().isBlank()
                        || u.password() == null || u.password().isBlank()) continue;
                String[] roles = u.roles() == null ? new String[0] : u.roles().toArray(String[]::new);
                users.add(User.withUsername(u.username())
                        .password(encoder.encode(u.password()))
                        .roles(roles).build());
            }
        }
        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 401 con el formato común de error (JSON)
        AuthenticationEntryPoint entryPoint = (req, res, ex) ->
                writeError(req, res, 401, "UNAUTHORIZED", "Se requiere autenticación válida");

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(b -> b.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(a -> a
                        // Registrar notificación: solo otros servicios o administradores
                        .requestMatchers(HttpMethod.POST, "/api/notifications").hasAnyRole("SERVICE", "ADMIN")
                        // El resto de endpoints: autenticado (los compañeros afinan roles por endpoint)
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        // 403 con el formato común de error (JSON)
                        .accessDeniedHandler((req, res, ex) ->
                                writeError(req, res, 403, "FORBIDDEN", "No tienes permisos para esta operación")));
        return http.build();
    }

    // Escribe el error en el formato común del PDF
    private void writeError(HttpServletRequest req, HttpServletResponse res,
                            int status, String code, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"code\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now().truncatedTo(ChronoUnit.SECONDS), status, code, message, req.getRequestURI()));
    }
}