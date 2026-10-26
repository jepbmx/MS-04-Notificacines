package com.ecommerce.notificaciones.service;

public record DeliveryResult(boolean successful, String errorCode, String errorMessage) {

    public static DeliveryResult success() {
        return new DeliveryResult(true, null, null);
    }

    public static DeliveryResult failure(String errorCode, String errorMessage) {
        return new DeliveryResult(false, errorCode, errorMessage);
    }
}
