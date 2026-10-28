USE ecommerce;

INSERT INTO notification_templates (
  type, title_template, content_template, is_active, created_at, updated_at
)
VALUES
  ('ORDER_CREATED', 'Orden {{orderId}} confirmada',
   'Hola {{customerName}}, recibimos tu orden {{orderId}}.', 1,
   UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  ('PAYMENT_CONFIRMED', 'Pago confirmado para {{orderId}}',
   'Hola {{customerName}}, confirmamos el pago de {{amount}} para la orden {{orderId}}.', 1,
   UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  ('SHIPMENT_UPDATED', 'Actualización de tu envío {{trackingNumber}}',
   'Hola {{customerName}}, el estado de tu envío es: {{shipmentStatus}}.', 1,
   UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
  title_template = VALUES(title_template),
  content_template = VALUES(content_template),
  is_active = VALUES(is_active),
  updated_at = UTC_TIMESTAMP(6);

INSERT INTO notifications (
  id_receiver, title, content, type, status, is_read, read_at,
  idempotency_key, retry_count, max_retries, last_error_code, created_at, sent_at
)
VALUES
  ('customer-1001', 'Orden ORD-2026-001 confirmada',
   'Hola Ana, recibimos tu orden ORD-2026-001.', 'ORDER_CREATED',
   'SENT', 1, UTC_TIMESTAMP(6), 'seed-event-order-2026-001',
   0, 3, NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  ('customer-1002', 'Pago confirmado para ORD-2026-002',
   'Hola Luis, confirmamos el pago de la orden ORD-2026-002.', 'PAYMENT_CONFIRMED',
   'FAILED', 0, NULL, 'seed-event-payment-2026-002',
   1, 3, 'PAYMENT_PROVIDER_UNAVAILABLE', UTC_TIMESTAMP(6), NULL)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  content = VALUES(content),
  status = VALUES(status),
  is_read = VALUES(is_read),
  read_at = VALUES(read_at),
  retry_count = VALUES(retry_count),
  max_retries = VALUES(max_retries),
  last_error_code = VALUES(last_error_code),
  created_at = VALUES(created_at),
  sent_at = VALUES(sent_at);

INSERT INTO notification_attempts (
  id_notification, attempt_number, status, error_code, error_message, attempted_at
)
SELECT id_notification, 1, 'SUCCESS', NULL, NULL, UTC_TIMESTAMP(6)
FROM notifications
WHERE idempotency_key = 'seed-event-order-2026-001'
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO notification_attempts (
  id_notification, attempt_number, status, error_code, error_message, attempted_at
)
SELECT id_notification, 1, 'FAILED', 'PAYMENT_PROVIDER_UNAVAILABLE',
       'El proveedor de pagos no estuvo disponible.', UTC_TIMESTAMP(6)
FROM notifications
WHERE idempotency_key = 'seed-event-payment-2026-002'
ON DUPLICATE KEY UPDATE
  status = VALUES(status),
  error_code = VALUES(error_code),
  error_message = VALUES(error_message);

INSERT INTO processed_events (event_id, event_type, id_notification, processed_at)
SELECT 'seed-event-order-2026-001', 'ORDER_CREATED', id_notification, UTC_TIMESTAMP(6)
FROM notifications
WHERE idempotency_key = 'seed-event-order-2026-001'
  AND NOT EXISTS (
    SELECT 1 FROM processed_events
    WHERE event_id = 'seed-event-order-2026-001'
  );

INSERT INTO processed_events (event_id, event_type, id_notification, processed_at)
SELECT 'seed-event-payment-2026-002', 'PAYMENT_CONFIRMED', id_notification, UTC_TIMESTAMP(6)
FROM notifications
WHERE idempotency_key = 'seed-event-payment-2026-002'
  AND NOT EXISTS (
    SELECT 1 FROM processed_events
    WHERE event_id = 'seed-event-payment-2026-002'
  );