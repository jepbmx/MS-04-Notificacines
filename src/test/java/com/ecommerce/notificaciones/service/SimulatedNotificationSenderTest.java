package com.ecommerce.notificaciones.service;

import com.ecommerce.notificaciones.model.Notifications;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimulatedNotificationSenderTest {
    @Test void respectsConfiguredFailuresAndDemoFlag() {
        var n = new Notifications("4", "title", "content", "ORDER");
        assertFalse(new SimulatedNotificationSender(" 4, ,5 ", false).send(n));
        assertTrue(new SimulatedNotificationSender("", false).send(n));
        n.setType("DEMO_ALWAYS_FAIL");
        assertTrue(new SimulatedNotificationSender("", false).send(n));
        var sender = new SimulatedNotificationSender("", true);
        assertFalse(sender.send(n));
        n.setType("DEMO_FAIL_ONCE");
        assertFalse(sender.send(n));
        n.setRetryCount(1);
        assertTrue(sender.send(n));
        n.setType("ORDER");
        assertTrue(sender.send(n));
    }
}
