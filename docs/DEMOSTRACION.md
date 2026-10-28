# Demostracion con el retry del PR 16

1. Iniciar Notificaciones local y generar tokens 4 SERVICE, 4 CLIENTE, 5 CLIENTE y 4 ADMINISTRADOR. Solo el ultimo puede solicitar retry.
2. POST /api/notifications con SERVICE, una Idempotency-Key nueva y el siguiente JSON:

```json
{"idReceiver":"4","type":"DEMO_FAIL_ONCE","title":"Pedido de prueba","content":"Aviso de demostracion"}
```

3. Esperar 201 FAILED del envio inicial simulado y guardar el ID. GET /{id}/status con CLIENTE 4 devuelve FAILED.
4. PATCH /{id}/read dos veces con CLIENTE 4: read=true, misma readAt, entrega sigue FAILED. CLIENTE 5 recibe 404 al consultar o marcar ese ID.
5. POST /{id}/retry con CLIENTE o SERVICE devuelve 403. Con ADMINISTRADOR devuelve 202, mismo ID y PENDING.
6. Comprobar en SQL que retry_count aumento una vez, la lectura se conservo y notification_attempts sigue teniendo solo el intento inicial. No esperar SENT ni un intento nuevo: el PR 16 no ejecuta el procesamiento posterior.
7. Repetir retry con ADMINISTRADOR sobre PENDING devuelve 409 NOTIFICATION_NOT_RETRYABLE. Para probar el limite sin falsear envios, usar una fila del seed que ya este FAILED con retry_count=max_retries: 409.
8. Repetir el POST de creacion con mismo body y clave: 200 y mismo ID. Cambiar content con esa clave: 409.
9. GET /me?page=0&size=5 y GET ?status=PENDING&read=true&sort=createdAt,desc: 200. size=0: 400. Swagger: /swagger-ui.html.

Verificacion SQL (reemplazar 123 por el ID real):

```sql
SELECT id_notification, status, is_read, read_at, retry_count, max_retries
FROM notifications WHERE id_notification=123;
SELECT attempt_number, status, error_code FROM notification_attempts
WHERE id_notification=123 ORDER BY attempt_number;
```

El recorrido de creacion, consulta y lectura esta cubierto. El requisito de simular un reenvio efectivo sigue pendiente del responsable del retry. No cambiar estados manualmente para presentarlo como un envio ejecutado.
