# Demostracion para la evaluacion

1. Iniciar MySQL, ejecutar database/create_tables.sql si es nuevo y completar config/local.properties una sola vez. Iniciar Notificaciones local. Verificar el mensaje de arranque.
2. Generar JWT con argumentos `4 SERVICE`, otro con `4 CLIENTE` y otro con `5 CLIENTE`. En Bruno usar Auth > Bearer Token y solo el JWT. No guardar tokens en Git ni capturas.
3. POST http://localhost:8080/api/notifications con token SERVICE, header Idempotency-Key nuevo y body:

```json
{"idReceiver":"4","type":"DEMO_FAIL_ONCE","title":"Pedido PED-DEMO confirmado","content":"Demostracion de fallo y reintento sin duplicados."}
```

4. Esperado 201 y status FAILED (demo-types activo en perfil local). Guardar id y Location. Consultar GET /api/notifications/{id}/status con CLIENTE 4: FAILED y willRetry=true.
5. PATCH /api/notifications/{id}/read con CLIENTE 4, sin body: read=true y readAt. Repetir: mismo readAt.
6. POST /api/notifications/{id}/retry con SERVICE, sin body: 200, mismo id, SENT; lectura conservada. GET /{id}/status: finalState=true, willRetry=false.
7. Repetir creacion con mismo body y clave: 200, mismo id, no otro intento. Cambiar content con la misma clave: 409.
8. Con CLIENTE 5, GET /{id}, GET /{id}/status y PATCH /{id}/read: 404. Con CLIENTE 4, POST /{id}/retry: 403. Sin JWT: 401 con formato comun.
9. Crear otra con tipo DEMO_ALWAYS_FAIL y clave nueva. Reintentar 3 veces: 200 y FAILED. Cuarto reintento: 409. Total: 4 intentos (inicial + 3); una sola notificacion.
10. Probar GET /me?page=0&size=5 y GET ?status=FAILED&read=false&sort=createdAt,desc. Probar size=0 y sort=password,asc: 400. Revisar Swagger /swagger-ui.html.

Verificar en MySQL (reemplazar 123 por el ID real):

```sql
SELECT id_notification, status, is_read, read_at, retry_count, max_retries
FROM notifications WHERE id_notification = 123;
SELECT attempt_number, status, error_code, attempted_at
FROM notification_attempts WHERE id_notification = 123 ORDER BY attempt_number;
SELECT event_id, id_notification FROM processed_events WHERE id_notification = 123;
```

Para automatizar el recorrido desde PowerShell 7: `./scripts/demo.ps1`; pide ambos tokens sin imprimirlos. Requiere usuario 4 y perfil local, y crea dos registros con claves nuevas.

Antes de evaluar: ejecutar `./mvnw.cmd clean verify`, abrir target/site/jacoco/index.html, tener los IDs a mano y explicar cada cambio de estado. Los inserts sinteticos no sustituyen la demostracion del envio simulado desde la API.
