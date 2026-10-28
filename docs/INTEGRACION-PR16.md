# Integracion del retry del companero

Se adopta el PR 16 en nuestra rama sin aprobarlo ni fusionarlo en develop. El metodo retryNotification y el cuerpo del endpoint se mantienen iguales al commit 32ea6d6; se agrega solamente su descripcion OpenAPI.

Contrato: POST /api/notifications/{id}/retry, ADMINISTRADOR, HTTP 202, PENDING, incremento del contador y mismo ID. SERVICE recibe 403. Falta procesamiento posterior, historial de otro intento y proteccion concurrente.

Se retiran nuestra ruta duplicada, NotificationDeliveryService.retry y findLockedById. Se conserva el simulador de entrega inicial y las mejoras independientes: paginacion, lectura, JWT, errores, Swagger, SQL, pruebas de propiedad e idempotencia de creacion.

Las pruebas comprueban el comportamiento disponible y documentan la ausencia de otro intento. No convierten el flujo incompleto en cumplimiento total. El README y la demostracion deben reflejar esta limitacion.
