package com.ecommerce.notificaciones;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class NotificacionesApplicationTest {

    @Test
    void mainDelegatesStartupToSpringApplication() {
        String[] args = {"--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> springApplication =
                     mockStatic(SpringApplication.class)) {
            NotificacionesApplication.main(args);

            springApplication.verify(
                    () -> SpringApplication.run(NotificacionesApplication.class, args)
            );
        }
    }
}
