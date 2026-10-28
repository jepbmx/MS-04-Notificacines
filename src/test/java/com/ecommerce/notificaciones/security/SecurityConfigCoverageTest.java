package com.ecommerce.notificaciones.security;

import com.ecommerce.notificaciones.config.SecurityConfig;
import com.ecommerce.notificaciones.controller.NotificationController;
import com.ecommerce.notificaciones.service.NotificationService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@TestPropertySource(properties = "jwt.secret=test-only-secret-that-is-at-least-32-bytes-long")
class SecurityConfigCoverageTest {

    private static final String TEST_SECRET =
            "test-only-secret-that-is-at-least-32-bytes-long";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void rejectsPostWhenRoleIsMissingOrBlank() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken(null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken(" "))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private String createToken(String role) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer("Users-MS")
                .subject("user@example.test")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(60)))
                .claim("userId", 42L);
        if (role != null) {
            claims.claim("role", role);
        }

        SignedJWT token = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claims.build()
        );
        token.sign(new MACSigner(TEST_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return token.serialize();
    }
}
