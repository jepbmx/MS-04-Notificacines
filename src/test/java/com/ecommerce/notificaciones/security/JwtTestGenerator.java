
package com.ecommerce.notificaciones.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.StandardEnvironment;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class JwtTestGenerator {

    public static void main(String[] args) throws Exception {

        // Carga las mismas propiedades que la aplicacion, sin iniciar MySQL ni el servidor.
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles("local");
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        String secret = environment.getProperty("jwt.secret");

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Configura JWT_SECRET en config/local.properties o en el entorno"
            );
        }

        // Mismo tratamiento del secreto que Users-MS
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);

        if (key.length < 32) {
            throw new IllegalArgumentException(
                    "La clave JWT debe tener al menos 32 bytes"
            );
        }

        // Datos simulados del usuario autenticado
        Long userId = 1L;
        String email = "cliente@ejemplo.com";
        String role = "CLIENTE";


        Instant now = Instant.now();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(email)
                .issuer("Users-MS")
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(900)))
                .claim("userId", userId)
                .claim("role", role)
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claims
        );

        jwt.sign(new MACSigner(key));

        System.out.println("JWT generado:");
        System.out.println(jwt.serialize());
    }
}
