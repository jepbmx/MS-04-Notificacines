package com.ecommerce.notificaciones.repository;

import com.ecommerce.notificaciones.model.ProcessedEvents;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvents, String> {
}