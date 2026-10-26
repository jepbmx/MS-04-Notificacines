package com.ecommerce.notificaciones;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.notificaciones.repository.NotificationAttemptRepository;
import com.ecommerce.notificaciones.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationAttemptRepository attemptRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void cleanDatabase() {
        attemptRepository.deleteAll();
        notificationRepository.deleteAll();
    }

    @Test
    void createsNotificationOnceForRepeatedIdempotencyKey() throws Exception {
        String request = """
                {"idReceiver":"alice","title":"Order created","content":"Order 42 was created","type":"ORDER_CREATED"}
                """;

        mockMvc.perform(post("/api/notifications")
                        .with(httpBasic("notifier", "notifier-test-password"))
                        .header("Idempotency-Key", "order-event-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SENT"));
        int firstId = notificationRepository.findAll().get(0).getIdNotification();

        mockMvc.perform(post("/api/notifications")
                        .with(httpBasic("notifier", "notifier-test-password"))
                        .header("Idempotency-Key", "order-event-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(firstId));

        mockMvc.perform(get("/api/notifications/me")
                        .with(httpBasic("alice", "alice-test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        org.junit.jupiter.api.Assertions.assertEquals(1, attemptRepository.count());
    }

    @Test
    void preventsReadingAnotherUsersNotificationAndSeparatesReadFromDeliveryStatus() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .with(httpBasic("notifier", "notifier-test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idReceiver":"alice","title":"Payment confirmed","content":"Payment received","type":"PAYMENT_CONFIRMED"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SENT"));
        int id = notificationRepository.findAll().get(0).getIdNotification();

        mockMvc.perform(get("/api/notifications/{id}", id)
                        .with(httpBasic("bob", "bob-test-password")))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/notifications/{id}/read", id)
                        .with(httpBasic("alice", "alice-test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.status").value("SENT"));

        mockMvc.perform(get("/api/notifications/{id}/status", id)
                        .with(httpBasic("alice", "alice-test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryStatus").value("SENT"))
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(get("/api/notifications/me")
                        .with(httpBasic("alice", "alice-test-password"))
                        .param("status", "SENT")
                        .param("type", "PAYMENT_CONFIRMED")
                        .param("read", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/notifications/me")
                        .with(httpBasic("alice", "alice-test-password"))
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void retriesFailedNotificationOnlyUpToConfiguredLimitWithoutCreatingDuplicates() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .with(httpBasic("notifier", "notifier-test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idReceiver":"retry-target","title":"Shipping update","content":"Package dispatched","type":"SHIPPING_UPDATED"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"));
        int id = notificationRepository.findAll().get(0).getIdNotification();

        mockMvc.perform(post("/api/notifications/{id}/retry", id)
                        .with(httpBasic("retry-target", "retry-target-test-password")))
                .andExpect(status().isForbidden());

        for (int retryCount = 1; retryCount <= 3; retryCount++) {
            mockMvc.perform(post("/api/notifications/{id}/retry", id)
                            .with(httpBasic("admin", "admin-test-password")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.retryCount").value(retryCount))
                    .andExpect(jsonPath("$.status").value("FAILED"));
        }
        mockMvc.perform(post("/api/notifications/{id}/retry", id)
                        .with(httpBasic("admin", "admin-test-password")))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/notifications/me")
                        .with(httpBasic("retry-target", "retry-target-test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        org.junit.jupiter.api.Assertions.assertEquals(4, attemptRepository.count());
    }

    @Test
    void requiresAuthenticationAndValidatesCreateRequest() throws Exception {
        mockMvc.perform(get("/api/notifications/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/notifications")
                        .with(httpBasic("alice", "alice-test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idReceiver":"alice","title":"Order created","content":"Order registered","type":"ORDER_CREATED"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/notifications")
                        .with(httpBasic("notifier", "notifier-test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idReceiver":"","title":"","content":"","type":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
