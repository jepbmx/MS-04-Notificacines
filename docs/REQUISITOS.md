# Matriz de requisitos

Fuentes: MS-04 Notificaciones (5 paginas) y RALLY JAVA 2026 Evaluacion Final (1 pagina), proporcionados por el equipo.

| Requisito | Implementacion / evidencia |
|---|---|
| Siete endpoints | NotificationController; documentados en Swagger y README |
| Destinatario, tipo, contenido y estado | DTO con Bean Validation, servicio y entidad |
| Propiedad | Consultas por usuario JWT y 404 para recursos ajenos; NotificationFlowTest |
| Idempotencia | Clave unica, evento procesado, retorno de mismo ID; prueba secuencial y concurrente |
| Envio y fallo simulados | SimulatedNotificationSender; tipos demo solo habilitados en local/test |
| Reintento autorizado y limitado | NotificationDeliveryService, bloqueo pesimista, limite 3, historial y sin duplicar ID |
| Lectura separada de entrega | read/readAt independientes, repetir lectura conserva fecha |
| Errores y JWT | ErrorResponse comun, entry points Bearer, validacion y roles |
| Base minima y relaciones | database/create_tables.sql; PK, FK, restricciones unicas e indices |
| Plantillas y eventos opcionales | notification_templates y processed_events implementados |
| JSON y DTOs | Registros de entrada/salida; no se expone entidad |
| page, size, sort | Validacion en ambos listados; sort permitido con desempate |
| Fechas | Instant UTC, precision de microsegundos para lectura y entrega |
| Dinero | No aplica: no hay operaciones monetarias |
| Swagger/OpenAPI | /swagger-ui.html y /v3/api-docs; parametros, ejemplos y errores |
| Pruebas y cobertura | Maven verify, JUnit/Mockito, integracion HTTP/persistencia, JaCoCo |
| Preparacion de datos | database/qa_notifications.sql; no borra filas existentes |
| Compilacion y arranque | Maven verify y pruebas Spring; validar tambien config local de cada equipo |
| Git y trabajo en equipo | Rama de implementacion; completar commits y PR reales segun flujo del equipo |
| Dominio individual | docs/EQUIPO.md y ensayo de docs/DEMOSTRACION.md; requiere participacion humana |

Limites explicitos: no se envia correo real; permitido por MS-04. No hay reintento automatico. No se altera el estado de ordenes ni pagos. No se exige /api/v1 salvo acuerdo entre todos los equipos. La cobertura es evidencia de ejecucion, no prueba de ausencia de defectos. Consultar docs/VALIDACION.md para resultados y limitaciones verificadas.
