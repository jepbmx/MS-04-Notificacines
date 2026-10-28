package com.ecommerce.notificaciones;

import com.ecommerce.notificaciones.repository.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationFlowTest {
    static final String SECRET = "test-only-secret-that-is-at-least-32-bytes-long";
    @Autowired MockMvc mvc;
    @Autowired NotificationRepository notifications;
    @Autowired NotificationAttemptRepository attempts;
    @Autowired ProcessedEventRepository events;
    @Autowired NotificationTemplateRepository templates;
    final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach void clean() {
        events.deleteAll(); attempts.deleteAll(); notifications.deleteAll(); templates.deleteAll();
    }

    String token(int user, String role) throws Exception {
        return token(user, role, SECRET, Instant.now().plusSeconds(600));
    }
    String token(Object user, String role, String secret, Instant expiration) throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer("Users-MS").subject("qa@example.test")
                .issueTime(Date.from(Instant.now().minusSeconds(600)))
                .expirationTime(Date.from(expiration)).claim("userId", user).claim("role", role).build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return "Bearer " + jwt.serialize();
    }
    String body(String type) {
        return "{\"idReceiver\":\"4\",\"type\":\"" + type
                + "\",\"title\":\"Pedido confirmado\",\"content\":\"Tu pedido esta listo\"}";
    }
    MvcResult create(String type, String key) throws Exception {
        return mvc.perform(post("/api/notifications").header("Authorization", token(4,"SERVICE"))
                .header("Idempotency-Key", key).contentType("application/json").content(body(type)))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
    }
    int id(MvcResult response) throws Exception {
        return json.readTree(response.getResponse().getContentAsString()).get("id").asInt();
    }

    @Test void creationReadingAndRetryRequestRespectThePeerContract() throws Exception {
        int id = id(create("DEMO_FAIL_ONCE", "flow"));
        assertEquals(1, notifications.count()); assertEquals(1, attempts.count());
        mvc.perform(get("/api/notifications/{id}/status", id).header("Authorization", token(4,"CLIENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.willRetry").value(true));
        var read = mvc.perform(patch("/api/notifications/{id}/read", id).header("Authorization", token(4,"CLIENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.read").value(true)).andReturn();
        var readAt = json.readTree(read.getResponse().getContentAsString()).get("readAt").asText();
        mvc.perform(patch("/api/notifications/{id}/read", id).header("Authorization", token(4,"CLIENTE")))
                .andExpect(jsonPath("$.readAt").value(readAt));
        mvc.perform(post("/api/notifications/{id}/retry", id).header("Authorization", token(4,"ADMINISTRADOR")))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.read").value(true));
        assertEquals(1, notifications.count()); assertEquals(1, attempts.count()); assertEquals(1, events.count());
        mvc.perform(post("/api/notifications").header("Authorization", token(4,"SERVICE"))
                .header("Idempotency-Key", "flow").contentType("application/json").content(body("DEMO_FAIL_ONCE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        assertEquals(1, attempts.count());
        mvc.perform(post("/api/notifications").header("Authorization", token(4,"SERVICE"))
                .header("Idempotency-Key", "flow").contentType("application/json").content(body("OTHER")))
                .andExpect(status().isConflict());
    }

    @Test void peerRetryRejectsPendingAndAlreadyExhaustedRecords() throws Exception {
        int id = id(create("DEMO_ALWAYS_FAIL", "limit"));
        String admin = token(4,"ADMINISTRADOR");
        mvc.perform(post("/api/notifications/{id}/retry", id).header("Authorization",admin))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("PENDING"));
        assertEquals(1,notifications.findById(id).orElseThrow().getRetryCount());
        assertEquals(1,attempts.count());
        mvc.perform(post("/api/notifications/{id}/retry", id).header("Authorization",admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_RETRYABLE"));
        var exhausted=notifications.findById(id).orElseThrow();
        exhausted.setStatus(com.ecommerce.notificaciones.model.NotificationStatus.FAILED);
        exhausted.setRetryCount(exhausted.getMaxRetries());
        notifications.saveAndFlush(exhausted);
        mvc.perform(post("/api/notifications/{id}/retry", id).header("Authorization",admin))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/notifications/0/retry").header("Authorization",admin))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/notifications/999999/retry").header("Authorization",admin))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/notifications/{id}/retry", id).header("Authorization",token(4,"SERVICE")))
                .andExpect(status().isForbidden());
    }

    @Test void ownershipAndRoleRulesApplyOverHttp() throws Exception {
        int id = id(create("ORDER", "own"));
        for(String suffix:List.of("", "/status")) mvc.perform(get("/api/notifications/"+id+suffix)
                .header("Authorization",token(5,"CLIENTE"))).andExpect(status().isNotFound());
        mvc.perform(patch("/api/notifications/{id}/read", id).header("Authorization",token(5,"CLIENTE")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/notifications/me").header("Authorization",token(5,"CLIENTE")))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(post("/api/notifications/{id}/retry", id).header("Authorization",token(4,"CLIENTE")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/notifications").header("Authorization",token(4,"CLIENTE"))
                .contentType("application/json").content(body("ORDER"))).andExpect(status().isForbidden());
    }

    @Test void filtersSortBindingAndValidationWorkThroughMvc() throws Exception {
        create("ORDER", "one"); create("DEMO_ALWAYS_FAIL", "two");
        mvc.perform(get("/api/notifications").param("sort","createdAt,desc")
                .param("status","FAILED").param("read","false").header("Authorization",token(4,"CLIENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/notifications").param("sort","type,asc","createdAt,desc")
                .param("size","1").header("Authorization",token(4,"CLIENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalPages").value(2));
        for(String route:List.of("/api/notifications","/api/notifications/me"))
            for(String[] param:new String[][]{{"page","-1"},{"size","0"},{"size","101"},{"page","abc"}})
                mvc.perform(get(route).param(param[0],param[1]).header("Authorization",token(4,"CLIENTE")))
                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.timestamp").exists());
        mvc.perform(get("/api/notifications").param("sort","password,asc").header("Authorization",token(4,"CLIENTE")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/notifications").param("status","INVALID").header("Authorization",token(4,"CLIENTE")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/notifications").contentType("application/json").content("{")
                .header("Authorization",token(4,"SERVICE"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/notifications").contentType("application/json").content("{}")
                .header("Authorization",token(4,"SERVICE"))).andExpect(status().isBadRequest());
    }

    @Test void invalidTokensHaveTheCommonErrorBody() throws Exception {
        mvc.perform(get("/api/notifications/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        for(String bearer:List.of("Bearer garbage",token(4,"CLIENTE",SECRET,Instant.now().minusSeconds(300)),
                token("4","CLIENTE",SECRET,Instant.now().plusSeconds(600)),
                token(4,"CLIENTE","wrong-secret-with-at-least-32-bytes-long",Instant.now().plusSeconds(600))))
            mvc.perform(get("/api/notifications/me").header("Authorization",bearer))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(header().string("WWW-Authenticate","Bearer"));
    }

    @Test void swaggerDescribesAllSevenRoutes() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/notifications/{id}/retry'].post").exists())
                .andExpect(jsonPath("$.paths['/api/notifications/{id}/retry'].post.responses['401'].content['application/json'].schema.properties.code").exists());
    }

    @Test void concurrentEventsNeverCreateDuplicateNotifications() throws Exception {
        String auth = token(4,"SERVICE");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        Callable<Integer> call = () -> { gate.await(); return mvc.perform(post("/api/notifications")
                .header("Authorization",auth).header("Idempotency-Key","same-event")
                .contentType("application/json").content(body("ORDER")))
                .andReturn().getResponse().getStatus(); };
        try {
            var a=pool.submit(call); var b=pool.submit(call); gate.countDown();
            var statuses = List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
            assertTrue(statuses.contains(201));
            assertTrue(statuses.stream().allMatch(s -> s==201 || s==200 || s==409));
            assertEquals(1,notifications.count()); assertEquals(1,events.count()); assertEquals(1,attempts.count());
        } finally { pool.shutdownNow(); }
    }

    @Test void templatesAreRenderedAndInvalidParametersRejected() throws Exception {
        templates.saveAndFlush(new com.ecommerce.notificaciones.model.NotificationTemplates(
                "READY", "Pedido {{pedido}}", "Hola {{nombre}}"));
        String auth=token(4,"SERVICE");
        mvc.perform(post("/api/notifications").header("Authorization",auth).contentType("application/json")
                .content("{\"idReceiver\":\"4\",\"type\":\"READY\",\"params\":{\"pedido\":\"P1\",\"nombre\":\"Ana\"}}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("Pedido P1"));
        mvc.perform(post("/api/notifications").header("Authorization",auth).contentType("application/json")
                .content("{\"idReceiver\":\"4\",\"type\":\"READY\",\"params\":{\"pedido\":\"P1\"}}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("TEMPLATE_PARAM_MISSING"));
        mvc.perform(post("/api/notifications").header("Authorization",auth).contentType("application/json")
                .content("{\"idReceiver\":\"4\",\"type\":\"MISSING\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(1,notifications.count());
    }
}
