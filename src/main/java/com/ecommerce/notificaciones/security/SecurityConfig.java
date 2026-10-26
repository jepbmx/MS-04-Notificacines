package com.ecommerce.notificaciones.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(NotificationSecurityProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/notifications/**").authenticated()
                        .anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            NotificationSecurityProperties properties,
            PasswordEncoder passwordEncoder) {
        List<UserDetails> users = new ArrayList<>();
        for (NotificationSecurityProperties.ConfiguredUser configuredUser : properties.getUsers()) {
            String username = configuredUser.getUsername();
            if (username == null || username.isBlank()) {
                continue;
            }
            if (configuredUser.getPassword() == null || configuredUser.getPassword().isBlank()) {
                throw new IllegalStateException("A password must be configured for each Basic-auth user.");
            }
            if (configuredUser.getRoles() == null || configuredUser.getRoles().isEmpty()) {
                throw new IllegalStateException("At least one role must be configured for each Basic-auth user.");
            }
            users.add(User.withUsername(username.trim())
                    .password(passwordEncoder.encode(configuredUser.getPassword()))
                    .roles(configuredUser.getRoles().toArray(String[]::new))
                    .build());
        }
        if (users.isEmpty()) {
            throw new IllegalStateException("Configure at least one user in app.security.users.");
        }
        return new InMemoryUserDetailsManager(users);
    }
}
