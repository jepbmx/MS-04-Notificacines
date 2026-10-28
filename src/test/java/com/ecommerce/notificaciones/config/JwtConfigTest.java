package com.ecommerce.notificaciones.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtConfigTest {

    private static final String TEST_SECRET =
            "test-only-secret-that-is-at-least-32-bytes-long";

    private final JwtDecoder decoder = new JwtConfig().jwtDecoder(TEST_SECRET);

    @Test
    void acceptsTokenWithExpectedIssuerNumericUserIdAndJti() throws Exception {
        String token = createToken(
                "Users-MS",
                "\"userId\":3,\"jti\":\"2886d1f1-f75b-4883-88b9-2b959668d45c\""
        );

        Jwt jwt = decoder.decode(token);

        assertEquals(3, ((Number) jwt.getClaim("userId")).intValue());
        assertEquals(
                "2886d1f1-f75b-4883-88b9-2b959668d45c",
                jwt.getId()
        );
    }

    @Test
    void rejectsTokenWithoutNumericUserId() throws Exception {
        String token = createToken("Users-MS", "\"userId\":\"3\"");

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void rejectsTokenWithUnexpectedIssuer() throws Exception {
        String token = createToken("other-service", "\"userId\":3");

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    private String createToken(String issuer, String extraClaims)
            throws Exception {
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encode(
                "{\"iss\":\"" + issuer + "\",\"sub\":\"test@example.com\","
                        + "\"iat\":" + now.getEpochSecond() + ","
                        + "\"exp\":" + now.plusSeconds(60).getEpochSecond() + ","
                        + extraClaims + "}"
        );
        String content = header + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                TEST_SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));
        return content + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(content.getBytes(StandardCharsets.US_ASCII)));
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }
}
