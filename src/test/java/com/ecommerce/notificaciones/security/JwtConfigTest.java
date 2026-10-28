package com.ecommerce.notificaciones.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtConfigTest {

    private static final String TEST_SECRET =
            "test-only-secret-that-is-at-least-32-bytes-long";

    private final JwtDecoder decoder = new JwtConfig().jwtDecoder(TEST_SECRET);

    @Test
    void acceptsValidTokenWithNumericUserIdAndJti() throws Exception {
        String jti = UUID.randomUUID().toString();
        Jwt jwt = decoder.decode(createToken("Users-MS", 3L, jti));

        assertEquals(3L, ((Number) jwt.getClaim("userId")).longValue());
        assertEquals(jti, jwt.getId());
    }

    @Test
    void rejectsTokenWithoutNumericUserId() throws Exception {
        assertThrows(
                JwtException.class,
                () -> decoder.decode(createToken("Users-MS", "3", UUID.randomUUID().toString()))
        );
    }

    @Test
    void rejectsTokenWithUnexpectedIssuer() throws Exception {
        assertThrows(
                JwtException.class,
                () -> decoder.decode(createToken("other-service", 3L, UUID.randomUUID().toString()))
        );
    }

    @Test
    void rejectsSecretShorterThanThirtyTwoBytes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtConfig().jwtDecoder("too-short")
        );
    }

    private String createToken(String issuer, Object userId, String jti)
            throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("test@example.com")
                .jwtID(jti)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(60)))
                .claim("userId", userId)
                .build();

        SignedJWT token = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claims
        );
        token.sign(new MACSigner(TEST_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return token.serialize();
    }
}
