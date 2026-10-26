# MS-04 Notificaciones

Servicio Spring Boot para registrar y consultar notificaciones generadas por eventos. El servicio simula la entrega; no procesa pagos ni modifica órdenes.

## Configuración

Configura estas variables de entorno antes de iniciar la aplicación:

| Variable | Uso |
|---|---|
| `DB_URL` | URL JDBC; por defecto apunta a `localhost:3306/ms_notifications` |
| `DB_USERNAME` | Usuario de MySQL; por defecto `root` |
| `DB_PASSWORD` | Contraseña de MySQL |
| `NOTIFICATIONS_USER` / `NOTIFICATIONS_PASSWORD` | Usuario receptor con rol `USER`; el username debe coincidir con `idReceiver` |
| `NOTIFICATIONS_ADMIN_USER` / `NOTIFICATIONS_ADMIN_PASSWORD` | Usuario con permiso para reintentar entregas |
| `NOTIFICATIONS_SERVICE_USER` / `NOTIFICATIONS_SERVICE_PASSWORD` | Cuenta de servicio autorizada para registrar notificaciones |
| `NOTIFICATION_FAILURE_RECEIVERS` | IDs separados por coma para simular fallos de entrega; vacío significa entrega exitosa |

El esquema se encuentra en [ms04_bd_final.sql](src/main/java/com/ecommerce/notificaciones/model/ms04_bd_final.sql). La base usada por defecto es `ms_notifications`. La aplicación actualiza el esquema con Hibernate (`ddl-auto=update`).

Las contraseñas Basic se codifican con BCrypt en memoria al iniciar. Configura credenciales distintas para cada rol. HTTP Basic debe usarse detrás de HTTPS fuera de un entorno local/de demostración.

Para probar un fallo, incluye el `idReceiver` en `NOTIFICATION_FAILURE_RECEIVERS` y reinicia la aplicación. Ese destinatario fallará en cada intento simulado. Si quitas el ID y reinicias, un reintento podrá completarse correctamente.

## API

Todas las rutas requieren HTTP Basic salvo que se indique lo contrario:

| Método y ruta | Permiso | Acción |
|---|---|---|
| `POST /api/notifications` | `SERVICE` o `ADMIN` | Registra y simula el envío |
| `GET /api/notifications/{id}` | Autenticado; solo destinatario | Consulta una notificación |
| `GET /api/notifications/{id}/status` | Autenticado; solo destinatario | Consulta estado de entrega y lectura |
| `GET /api/notifications/me` | Autenticado | Lista las del usuario autenticado |
| `GET /api/notifications` | Autenticado | Lista las propias con filtros/paginación |
| `PATCH /api/notifications/{id}/read` | Autenticado; solo destinatario | Marca como leída sin cambiar estado de entrega |
| `POST /api/notifications/{id}/retry` | `ADMIN` | Reintenta solo estados `FAILED`, hasta el límite |

El listado acepta `status`, `type`, `read`, `page` (desde 0) y `size` (de 1 a 100). `POST` acepta una cabecera `Idempotency-Key` opcional (máximo 120 caracteres); repetirla devuelve la notificación existente sin generar otro intento.

Ejemplo de alta:

```http
POST /api/notifications
Authorization: Basic <credenciales de SERVICE>
Idempotency-Key: order-created-42
Content-Type: application/json

{
  "idReceiver": "cliente-17",
  "title": "Orden creada",
  "content": "Tu orden fue registrada.",
  "type": "ORDER_CREATED"
}
```

Los errores de validación y negocio devuelven un objeto con `status`, `code`, `message` y `fieldErrors`. Los mensajes de errores de entrega no contienen datos internos del proveedor.

## Pruebas

`mvnw.cmd test` usa H2 en memoria y credenciales exclusivas de pruebas; no necesita una instancia MySQL.
