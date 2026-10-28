-- MySQL. Ejecutar el archivo completo en una misma conexion.
-- Datos sinteticos; no borra ni actualiza registros existentes.
-- Cada ejecucion crea un lote nuevo con IDs asignados por MySQL.
USE ecommerce;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
START TRANSACTION;
SET @lote = CONCAT('qa-final-', UUID());
SET @ahora = UTC_TIMESTAMP(6);

INSERT INTO notifications
(id_receiver, title, content, type, status, is_read, read_at,
 idempotency_key, retry_count, max_retries, last_error_code, created_at, sent_at)
VALUES
('4', 'QA 01 - Pedido PED-4001', 'Aviso de ORDER_CONFIRMED del pedido PED-4001. Escenario de prueba PENDING.', 'ORDER_CONFIRMED', 'PENDING', 0, NULL, CONCAT(@lote, '-01'), 0, 3, NULL, TIMESTAMPADD(HOUR, -29, @ahora), NULL),
('4', 'QA 02 - Pedido PED-4002', 'Aviso de PAYMENT_RECEIVED del pedido PED-4002. Escenario de prueba PROCESSING.', 'PAYMENT_RECEIVED', 'PROCESSING', 0, NULL, CONCAT(@lote, '-02'), 0, 3, NULL, TIMESTAMPADD(HOUR, -28, @ahora), NULL),
('4', 'QA 03 - Pedido PED-4003', 'Aviso de ORDER_SHIPPED del pedido PED-4003. Escenario de prueba SENT.', 'ORDER_SHIPPED', 'SENT', 0, NULL, CONCAT(@lote, '-03'), 0, 3, NULL, TIMESTAMPADD(HOUR, -27, @ahora), TIMESTAMPADD(MINUTE, 1, TIMESTAMPADD(HOUR, -27, @ahora))),
('4', 'QA 04 - Pedido PED-4004', 'Aviso de ORDER_DELIVERED del pedido PED-4004. Escenario de prueba SENT.', 'ORDER_DELIVERED', 'SENT', 1, TIMESTAMPADD(MINUTE, 5, TIMESTAMPADD(HOUR, -26, @ahora)), CONCAT(@lote, '-04'), 0, 3, NULL, TIMESTAMPADD(HOUR, -26, @ahora), TIMESTAMPADD(MINUTE, 1, TIMESTAMPADD(HOUR, -26, @ahora))),
('4', 'QA 05 - Pedido PED-4005', 'Aviso de ORDER_CONFIRMED del pedido PED-4005. Escenario de prueba FAILED.', 'ORDER_CONFIRMED', 'FAILED', 0, NULL, CONCAT(@lote, '-05'), 0, 3, 'DELIVERY_TIMEOUT', TIMESTAMPADD(HOUR, -25, @ahora), NULL),
('4', 'QA 06 - Pedido PED-4006', 'Aviso de ORDER_SHIPPED del pedido PED-4006. Escenario de prueba FAILED.', 'ORDER_SHIPPED', 'FAILED', 0, NULL, CONCAT(@lote, '-06'), 1, 3, 'DELIVERY_TIMEOUT', TIMESTAMPADD(HOUR, -24, @ahora), NULL),
('4', 'QA 07 - Pedido PED-4007', 'Aviso de PAYMENT_RECEIVED del pedido PED-4007. Escenario de prueba FAILED.', 'PAYMENT_RECEIVED', 'FAILED', 0, NULL, CONCAT(@lote, '-07'), 2, 3, 'DELIVERY_TIMEOUT', TIMESTAMPADD(HOUR, -23, @ahora), NULL),
('4', 'QA 08 - Pedido PED-4008', 'Aviso de ORDER_CONFIRMED del pedido PED-4008. Escenario de prueba FAILED.', 'ORDER_CONFIRMED', 'FAILED', 0, NULL, CONCAT(@lote, '-08'), 3, 3, 'DELIVERY_TIMEOUT', TIMESTAMPADD(HOUR, -22, @ahora), NULL),
('4', 'QA 09 - Pedido PED-4009', 'Aviso de PROMOTION del pedido PED-4009. Escenario de prueba CANCELLED.', 'PROMOTION', 'CANCELLED', 0, NULL, CONCAT(@lote, '-09'), 0, 3, NULL, TIMESTAMPADD(HOUR, -21, @ahora), NULL),
('4', 'QA 10 - Pedido PED-40010', 'Aviso de REFUND_APPROVED del pedido PED-40010. Escenario de prueba SENT.', 'REFUND_APPROVED', 'SENT', 1, TIMESTAMPADD(MINUTE, 5, TIMESTAMPADD(HOUR, -20, @ahora)), CONCAT(@lote, '-10'), 0, 3, NULL, TIMESTAMPADD(HOUR, -20, @ahora), TIMESTAMPADD(MINUTE, 1, TIMESTAMPADD(HOUR, -20, @ahora))),
('4', 'QA 11 - Pedido PED-40011', 'Aviso de ORDER_CONFIRMED del pedido PED-40011. Escenario de prueba SENT.', 'ORDER_CONFIRMED', 'SENT', 0, NULL, CONCAT(@lote, '-11'), 1, 3, NULL, TIMESTAMPADD(HOUR, -19, @ahora), TIMESTAMPADD(MINUTE, 1, TIMESTAMPADD(HOUR, -19, @ahora))),
('4', 'QA 12 - Pedido PED-40012', 'Aviso de ORDER_CONFIRMED del pedido PED-40012. Escenario de prueba SENT.', 'ORDER_CONFIRMED', 'SENT', 1, TIMESTAMPADD(MINUTE, 5, TIMESTAMPADD(HOUR, -18, @ahora)), CONCAT(@lote, '-12'), 0, 3, NULL, TIMESTAMPADD(HOUR, -18, @ahora), TIMESTAMPADD(MINUTE, 1, TIMESTAMPADD(HOUR, -18, @ahora))),
('4', 'QA 13 - Pedido PED-40013', 'Aviso de ORDER_SHIPPED del pedido PED-40013. Escenario de prueba PENDING.', 'ORDER_SHIPPED', 'PENDING', 0, NULL, CONCAT(@lote, '-13'), 0, 3, NULL, TIMESTAMPADD(HOUR, -17, @ahora), NULL),
('4', 'QA 14 - Pedido PED-40014', 'Aviso de PAYMENT_RECEIVED del pedido PED-40014. Escenario de prueba FAILED.', 'PAYMENT_RECEIVED', 'FAILED', 0, NULL, CONCAT(@lote, '-14'), 3, 3, 'DELIVERY_TIMEOUT', TIMESTAMPADD(HOUR, -16, @ahora), NULL),
('4', 'QA 15 - Pedido PED-40015', 'Aviso de REFUND_APPROVED del pedido PED-40015. Escenario de prueba PROCESSING.', 'REFUND_APPROVED', 'PROCESSING', 0, NULL, CONCAT(@lote, '-15'), 0, 3, NULL, TIMESTAMPADD(HOUR, -15, @ahora), NULL),
('4', 'QA 16 - Pedido PED-40016', 'Aviso de PROMOTION del pedido PED-40016. Escenario de prueba SENT.', 'PROMOTION', 'SENT', 0, NULL, CONCAT(@lote, '-16'), 0, 3, NULL, TIMESTAMPADD(HOUR, -14, @ahora), TIMESTAMPADD(MINUTE, 1, TIMESTAMPADD(HOUR, -14, @ahora))),
('5', 'QA 17 - Pedido PED-50017', 'Aviso de ORDER_CONFIRMED del pedido PED-50017. Escenario de prueba PENDING.', 'ORDER_CONFIRMED', 'PENDING', 0, NULL, CONCAT(@lote, '-17'), 0, 3, NULL, TIMESTAMPADD(HOUR, -13, @ahora), NULL),
('5', 'QA 18 - Pedido PED-50018', 'Aviso de ORDER_SHIPPED del pedido PED-50018. Escenario de prueba SENT.', 'ORDER_SHIPPED', 'SENT', 0, NULL, CONCAT(@lote, '-18'), 0, 3, NULL, TIMESTAMPADD(HOUR, -12, @ahora), TIMESTAMPADD(MINUTE, 1, TIMESTAMPADD(HOUR, -12, @ahora))),
('5', 'QA 19 - Pedido PED-50019', 'Aviso de PAYMENT_RECEIVED del pedido PED-50019. Escenario de prueba FAILED.', 'PAYMENT_RECEIVED', 'FAILED', 0, NULL, CONCAT(@lote, '-19'), 1, 3, 'DELIVERY_TIMEOUT', TIMESTAMPADD(HOUR, -11, @ahora), NULL),
('5', 'QA 20 - Pedido PED-50020', 'Aviso de ORDER_DELIVERED del pedido PED-50020. Escenario de prueba SENT.', 'ORDER_DELIVERED', 'SENT', 1, TIMESTAMPADD(MINUTE, 5, TIMESTAMPADD(HOUR, -10, @ahora)), CONCAT(@lote, '-20'), 0, 3, NULL, TIMESTAMPADD(HOUR, -10, @ahora), TIMESTAMPADD(MINUTE, 1, TIMESTAMPADD(HOUR, -10, @ahora))),
('5', 'QA 21 - Pedido PED-50021', 'Aviso de PROMOTION del pedido PED-50021. Escenario de prueba CANCELLED.', 'PROMOTION', 'CANCELLED', 0, NULL, CONCAT(@lote, '-21'), 0, 3, NULL, TIMESTAMPADD(HOUR, -9, @ahora), NULL),
('5', 'QA 22 - Pedido PED-50022', 'Aviso de ORDER_CONFIRMED del pedido PED-50022. Escenario de prueba FAILED.', 'ORDER_CONFIRMED', 'FAILED', 0, NULL, CONCAT(@lote, '-22'), 3, 3, 'DELIVERY_TIMEOUT', TIMESTAMPADD(HOUR, -8, @ahora), NULL),
('5', 'QA 23 - Pedido PED-50023', 'Aviso de REFUND_APPROVED del pedido PED-50023. Escenario de prueba PROCESSING.', 'REFUND_APPROVED', 'PROCESSING', 0, NULL, CONCAT(@lote, '-23'), 0, 3, NULL, TIMESTAMPADD(HOUR, -7, @ahora), NULL),
('6', 'QA 24 - Pedido PED-60024', 'Aviso de ORDER_CONFIRMED del pedido PED-60024. Escenario de prueba PENDING.', 'ORDER_CONFIRMED', 'PENDING', 0, NULL, CONCAT(@lote, '-24'), 0, 3, NULL, TIMESTAMPADD(HOUR, -6, @ahora), NULL),
('6', 'QA 25 - Pedido PED-60025', 'Aviso de ORDER_SHIPPED del pedido PED-60025. Escenario de prueba SENT.', 'ORDER_SHIPPED', 'SENT', 1, TIMESTAMPADD(MINUTE, 5, TIMESTAMPADD(HOUR, -5, @ahora)), CONCAT(@lote, '-25'), 0, 3, NULL, TIMESTAMPADD(HOUR, -5, @ahora), TIMESTAMPADD(MINUTE, 1, TIMESTAMPADD(HOUR, -5, @ahora))),
('6', 'QA 26 - Pedido PED-60026', 'Aviso de PAYMENT_RECEIVED del pedido PED-60026. Escenario de prueba FAILED.', 'PAYMENT_RECEIVED', 'FAILED', 0, NULL, CONCAT(@lote, '-26'), 2, 3, 'DELIVERY_TIMEOUT', TIMESTAMPADD(HOUR, -4, @ahora), NULL),
('6', 'QA 27 - Pedido PED-60027', 'Aviso de PROMOTION del pedido PED-60027. Escenario de prueba CANCELLED.', 'PROMOTION', 'CANCELLED', 0, NULL, CONCAT(@lote, '-27'), 0, 3, NULL, TIMESTAMPADD(HOUR, -3, @ahora), NULL),
('6', 'QA 28 - Pedido PED-60028', 'Aviso de ORDER_DELIVERED del pedido PED-60028. Escenario de prueba SENT.', 'ORDER_DELIVERED', 'SENT', 0, NULL, CONCAT(@lote, '-28'), 0, 3, NULL, TIMESTAMPADD(HOUR, -2, @ahora), TIMESTAMPADD(MINUTE, 1, TIMESTAMPADD(HOUR, -2, @ahora))),
('6', 'QA 29 - Pedido PED-60029', 'Aviso de REFUND_APPROVED del pedido PED-60029. Escenario de prueba PROCESSING.', 'REFUND_APPROVED', 'PROCESSING', 0, NULL, CONCAT(@lote, '-29'), 0, 3, NULL, TIMESTAMPADD(HOUR, -1, @ahora), NULL),
('6', 'QA 30 - Pedido PED-60030', 'Aviso de ORDER_CONFIRMED del pedido PED-60030. Escenario de prueba FAILED.', 'ORDER_CONFIRMED', 'FAILED', 0, NULL, CONCAT(@lote, '-30'), 3, 3, 'DELIVERY_TIMEOUT', TIMESTAMPADD(HOUR, -0, @ahora), NULL);

-- Un evento procesado por notificacion con clave de idempotencia.
INSERT INTO processed_events (event_id, event_type, id_notification, processed_at)
SELECT idempotency_key, type, id_notification, created_at
FROM notifications WHERE idempotency_key LIKE CONCAT(@lote, '-%');

-- Historial: intento inicial + retry_count reintentos.
-- Para SENT, solo el ultimo intento es SUCCESS.
INSERT INTO notification_attempts
(id_notification, attempt_number, status, error_code, error_message, attempted_at)
SELECT n.id_notification, a.num,
       CASE WHEN n.status = 'SENT' AND a.num = n.retry_count + 1
            THEN 'SUCCESS' ELSE 'FAILED' END,
       CASE WHEN n.status = 'SENT' AND a.num = n.retry_count + 1
            THEN NULL ELSE 'DELIVERY_TIMEOUT' END,
       CASE WHEN n.status = 'SENT' AND a.num = n.retry_count + 1
            THEN NULL ELSE 'El proveedor no respondio dentro del tiempo permitido.' END,
       TIMESTAMPADD(SECOND, a.num * 10, n.created_at)
FROM notifications n
JOIN (SELECT 1 num UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) a
  ON a.num <= n.retry_count + 1
WHERE n.idempotency_key LIKE CONCAT(@lote, '-%')
  AND n.status IN ('FAILED', 'SENT');

-- Tipos exclusivos del lote: no sobrescriben plantillas previas.
SET @tipo_activo = CONCAT('QA_ACTIVE_', RIGHT(@lote, 12));
SET @tipo_inactivo = CONCAT('QA_INACTIVE_', RIGHT(@lote, 12));
INSERT INTO notification_templates
(type, title_template, content_template, is_active)
VALUES
(@tipo_activo, 'Pedido {{pedido}} listo',
 'Hola {{nombre}}, tu pedido {{pedido}} esta listo para entrega.', 1),
(@tipo_inactivo, 'Promocion terminada',
 'Esta plantilla no esta disponible para nuevos avisos.', 0);

-- Plantilla activa que produce titulo demasiado largo: probar 422.
SET @tipo_largo = CONCAT('QA_LONG_', RIGHT(@lote, 12));
INSERT INTO notification_templates
(type, title_template, content_template, is_active)
VALUES (@tipo_largo, '{{texto}}', 'Aviso para {{nombre}}.', 1);

COMMIT;

-- Guardar estos valores en Bruno.
SELECT @lote lote, @tipo_activo tipo_activo,
       @tipo_inactivo tipo_inactivo, @tipo_largo tipo_titulo_largo;

-- Mapa de casos a IDs REALES. El numero QA del titulo NO es el ID.
SELECT title caso, id_notification id, id_receiver usuario, type, status,
       is_read, retry_count, max_retries,
       (status = 'FAILED' AND retry_count < max_retries) puede_reintentar,
       idempotency_key
FROM notifications WHERE idempotency_key LIKE CONCAT(@lote, '-%')
ORDER BY id_notification;

SELECT id_receiver usuario, status, COUNT(*) cantidad
FROM notifications WHERE idempotency_key LIKE CONCAT(@lote, '-%')
GROUP BY id_receiver, status ORDER BY id_receiver, status;

-- ID inexistente para pruebas 404 (usar antes de crear mas datos).
SELECT COALESCE(MAX(id_notification), 0) + 100000 AS id_inexistente
FROM notifications;

SELECT a.*
FROM notification_attempts a
JOIN notifications n ON n.id_notification = a.id_notification
WHERE n.idempotency_key LIKE CONCAT(@lote, '-%')
ORDER BY a.id_notification, a.attempt_number;
