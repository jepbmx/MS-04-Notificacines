# MS-04 Notificaciones

Java 17, Spring Boot 4.1.1, Spring Data JPA, MySQL y JWT HS256. Registra avisos de eventos; no cobra pagos ni modifica ordenes. La entrega es **simulada y sincrona**, sin contratar proveedor externo.

## Preparacion local

1. Usar Java 17 y recargar Maven. Crear MySQL con `database/create_tables.sql`.
2. Copiar `config/local.example.properties` a `config/local.properties` y completar DB_URL, DB_USERNAME, DB_PASSWORD y JWT_SECRET. El secreto debe tener 32 bytes UTF-8 como minimo y coincidir con Users-MS. No se decodifica Base64. El archivo privado queda fuera de Git.
3. Ejecutar `Notificaciones local` en IntelliJ (perfil local y directorio raiz del proyecto). No requiere variables de entorno en IntelliJ. Las variables existentes del sistema tienen prioridad: evitar valores contradictorios.
4. Para otro entorno usar DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET. La aplicacion valida el esquema (`ddl-auto=validate`); no crea ni borra tablas.
5. Swagger: http://localhost:8080/swagger-ui.html. OpenAPI: http://localhost:8080/v3/api-docs. Solo la documentacion es publica; las operaciones requieren Bearer JWT.

## API y permisos

Base: `/api/notifications`. Se conserva el contrato sin `/v1` del documento MS-04; coordinar cualquier versionado entre equipos.

| Metodo | Ruta relativa | Permiso y resultado |
|---|---|---|
| POST | / | SERVICE o ADMINISTRADOR; 201 con Location; 200 si repite la clave |
| GET | /me | Autenticado; solo sus notificaciones |
| GET | / | Autenticado; filtros type, status, read; solo sus notificaciones |
| GET | /{id} | Propietario; 404 para ajenas o inexistentes |
| GET | /{id}/status | Propietario; entrega, lectura, fechas y elegibilidad de reintento |
| PATCH | /{id}/read | Propietario; 200, idempotente y sin body |
| POST | /{id}/retry | Solo ADMINISTRADOR; operacion sobre cualquier destinatario; 202 y PENDING |

JWT: `iss=Users-MS`, `userId` entero numerico positivo y rol sin prefijo ROLE_. Tokens locales: ejecutar `JwtTestGenerator` con argumentos `4 CLIENTE` o `4 SERVICE`. El tercer argumento opcional es email; sin argumentos conserva usuario 4 CLIENTE. Usa la misma configuracion local; expira en 15 minutos. En Bruno pegar solo el JWT en Auth > Bearer Token.

Ejemplo POST (Content-Type: application/json):

```json
{"idReceiver":"4","type":"ORDER_CONFIRMED","title":"Pedido PED-4001 confirmado","content":"Tu pedido esta listo para entrega."}
```

Header opcional `Idempotency-Key: evento-pedido-4001`. Una repeticion compatible devuelve el mismo ID y no vuelve a enviar. Cambiar destinatario, tipo o texto explicito con esa clave devuelve 409. La clave es global entre servicios y admite 120 caracteres. Ante dos creaciones concurrentes la restriccion unica impide duplicados: la perdedora puede recibir 409 y repetir para recuperar el recurso con 200.

Si falta title o content se usa la plantilla activa del type con `params`, por ejemplo `{"pedido":"PED-4001","nombre":"Ana"}`. Falta de plantilla o parametro devuelve 422. Titulo maximo 200, contenido maximo 5000; tambien se validan valores renderizados.

Listados: `page=0&size=10&sort=createdAt,desc`. `page >= 0`, `1 <= size <= 100`. `/me` usa orden fijo fecha/ID descendente; la busqueda permite repetir `sort` y solo admite createdAt, idNotification, type, status, read. Desempate por ID para paginacion estable. Fechas ISO 8601 UTC.

## Entrega y reintentos

La creacion nueva registra el intento inicial (numero 1) dentro de la transaccion. El simulador guarda SENT o FAILED; un fallo de entrega no es un fallo HTTP de creacion. No hay trabajador de fondo ni promesa de reintento automatico. `willRetry` significa que un operador autorizado puede reintentar.

En perfil local, `app.notifications.simulator.demo-types=true` habilita DEMO_FAIL_ONCE (falla inicialmente y luego envia) y DEMO_ALWAYS_FAIL (siempre falla). Fuera de local esta desactivado por defecto. `app.notifications.simulator.fail-for-receivers` acepta IDs separados por comas y fuerza fallo permanente, con prioridad sobre los tipos demo.

La solicitud de reintento pertenece al PR 16 y se conserva sin modificar: solo ADMINISTRADOR, HTTP 202, mismo ID, retry_count incrementado y estado PENDING. Se rechaza con 409 NOTIFICATION_NOT_RETRYABLE si el estado no es FAILED o el limite ya esta agotado. Maximo predeterminado: 3 reintentos.

**Pendiente del flujo de retry:** no existe procesador posterior, no se realiza otra entrega y no se agrega un intento al historial. Una segunda solicitud sobre PENDING responde 409. No se conserva el bloqueo pesimista ni el reintento sincrono de nuestra implementacion anterior. La demostracion integral de fallo y reenvio del documento MS-04 permanece parcial hasta que el responsable complete ese flujo. No se atribuye a las pruebas una garantia de concurrencia del retry.

PENDING y PROCESSING son estados transitorios; CANCELLED esta soportado para consulta pero no se expone una operacion de cancelacion (no solicitada). Los registros PENDING insertados manualmente no se procesan en segundo plano.

## Errores

Formato comun: `timestamp`, `status`, `code`, `message`, `path`. No se devuelven trazas, contrasenas, secretos ni tokens. Fallos de entrega se registran solo con ID y numero de intento.

| HTTP | Uso |
|---|---|
| 200 / 201 | Consulta/actualizacion/repeticion; creacion nueva |
| 202 | Solicitud administrativa de reintento aceptada; procesamiento posterior pendiente |
| 400 | JSON, parametros, tamano de pagina o identificador invalidos |
| 401 | JWT ausente, invalido, expirado o sin userId numerico valido |
| 403 | Rol insuficiente para crear o reintentar |
| 404 | Notificacion inexistente o consulta de otra persona |
| 409 | Clave incompatible, concurrencia o reintento no permitido |
| 422 | Plantilla o contenido generado invalido |
| 500 | Error inesperado con mensaje publico generico |
| 503 | Persistencia temporalmente no disponible, cuando es traducida por Spring a fallo de recurso |

## Base de datos

`notifications`: PK autoincremental, clave de idempotencia unica, indices por destinatario/fecha, estado y tipo. `notification_attempts`: FK a notificacion y unicidad (id_notification, attempt_number). `notification_templates`: tipo unico. `processed_events`: event_id como PK y referencia unica a notificacion. Sin FK a Users-MS: el destinatario es un identificador externo.

Las transacciones agrupan notificacion, evento e intento. La lectura usa dirty checking de JPA. No se requiere dinero numerico en este MS; no realiza calculos monetarios.

## Pruebas, cobertura y demostracion

```powershell
.\mvnw.cmd clean verify
```

Perfil test usa H2 aislada compatible con MySQL, sin credenciales locales y sin tocar la base personal. Incluye contratos HTTP con JWT firmado, filtros y sort reales, persistencia, idempotencia y concurrencia de creacion, propiedad y contrato de solicitud de retry. JaCoCo: `target/site/jacoco/index.html`; no se promete 100% ni se excluyen reglas para inflar cobertura. CI ejecuta tambien contra MySQL con el esquema SQL y publica cobertura.

Para datos variados ejecutar `database/qa_notifications.sql` completo en una misma conexion; agrega un lote nuevo sin borrar datos. Ver `docs/DEMOSTRACION.md`, `docs/REQUISITOS.md` y `docs/EQUIPO.md`.

## Git

Trabajar en ramas, commits concretos y PR revisados hacia develop; no guardar credenciales, JWT ni target. No fusionar sin revisar pruebas y diferencias. El equipo debe registrar sus contribuciones reales: una guia no sustituye historial de Git ni conocimiento individual.
