package com.ecommerce.notificaciones.service;

import com.ecommerce.notificaciones.model.Notifications;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Simulador determinista: no contrata ni llama a un proveedor externo. */
@Component
public class SimulatedNotificationSender {
    private final Set<String> failingReceivers;
    private final boolean demoTypes;

    public SimulatedNotificationSender(
            @Value("${app.notifications.simulator.fail-for-receivers:}") String receivers,
            @Value("${app.notifications.simulator.demo-types:false}") boolean demoTypes) {
        this.failingReceivers = Arrays.stream(receivers.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        this.demoTypes = demoTypes;
    }

    public boolean send(Notifications notification) {
        if (failingReceivers.contains(notification.getIdReceiver())) return false;
        if (!demoTypes) return true;
        if ("DEMO_ALWAYS_FAIL".equals(notification.getType())) return false;
        return !"DEMO_FAIL_ONCE".equals(notification.getType())
                || notification.getRetryCount() > 0;
    }
}
