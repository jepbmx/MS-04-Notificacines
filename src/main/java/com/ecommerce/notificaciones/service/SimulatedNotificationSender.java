package com.ecommerce.notificaciones.service;

import com.ecommerce.notificaciones.model.Notification;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulatedNotificationSender implements NotificationSender {

    private final Set<String> failingReceivers;

    public SimulatedNotificationSender(
            @Value("${app.notifications.simulator.fail-for-receivers:}") String configuredReceivers) {
        this.failingReceivers = Arrays.stream(configuredReceivers.split(","))
                .map(String::trim)
                .filter(receiver -> !receiver.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public DeliveryResult send(Notification notification) {
        if (failingReceivers.contains(notification.getIdReceiver())) {
            return DeliveryResult.failure("SIMULATED_DELIVERY_FAILURE", "Simulated delivery failed.");
        }
        return DeliveryResult.success();
    }
}
