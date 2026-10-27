package com.ecommerce.notificaciones.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

// Lee los usuarios definidos en application.properties (app.security.users[n].*)
@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(List<UserEntry> users) {
    public record UserEntry(String username, String password, List<String> roles) {}
}