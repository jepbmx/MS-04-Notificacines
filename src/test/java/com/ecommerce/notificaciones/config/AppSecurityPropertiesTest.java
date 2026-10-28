package com.ecommerce.notificaciones.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppSecurityPropertiesTest {

    @Test
    void exposesConfiguredUsersAndRecordValueSemantics() {
        AppSecurityProperties.UserEntry user =
                new AppSecurityProperties.UserEntry("service", "secret", List.of("SERVICE"));
        AppSecurityProperties properties = new AppSecurityProperties(List.of(user));

        assertEquals(List.of(user), properties.users());
        assertEquals("service", user.username());
        assertEquals("secret", user.password());
        assertEquals(List.of("SERVICE"), user.roles());
        assertEquals(user, new AppSecurityProperties.UserEntry(
                "service", "secret", List.of("SERVICE")
        ));
        assertEquals(user.hashCode(), new AppSecurityProperties.UserEntry(
                "service", "secret", List.of("SERVICE")
        ).hashCode());
        assertTrue(user.toString().contains("service"));
        assertEquals(properties, new AppSecurityProperties(List.of(user)));
        assertEquals(properties.hashCode(), new AppSecurityProperties(List.of(user)).hashCode());
        assertTrue(properties.toString().contains("service"));
    }
}
