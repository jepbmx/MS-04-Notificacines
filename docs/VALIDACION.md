# Validacion de la entrega final

Fecha: 28 de octubre de 2026. Base actualizada: develop con PR 16 integrado (5d1821e).

## Resultado local

Comando ejecutado en la carpeta real: `mvnw.cmd -B clean verify` con Microsoft OpenJDK 17.0.18.
BUILD SUCCESS: 77 pruebas, 0 fallos, 0 errores y 0 omitidas. Se genera el JAR ejecutable y JaCoCo sin exclusiones de codigo productivo para elevar los porcentajes.

| Cobertura global | Resultado |
|---|---:|
| Instrucciones | 98.55% |
| Lineas | 99.01% |
| Ramas | 93.16% |

Reporte: target/site/jacoco/index.html. Resultados individuales: target/surefire-reports.

## Contrato adoptado del PR 16

Se conserva literalmente NotificationService.retryNotification y el cuerpo del endpoint del companero: solo ADMINISTRADOR, HTTP 202, PENDING, incremento del contador y mismo ID. SERVICE recibe 403. La lectura permanece independiente. No se incorpora nuestro retry sincrono ni bloqueo pesimista.

Las pruebas cubren el contrato disponible, ausencia de otro intento, rechazo por estado o limite preexistente, JWT, propiedad, lectura repetida, filtros, sort, plantillas, persistencia e idempotencia concurrente de creacion. No se atribuye al retry una garantia de concurrencia o envio efectivo que no implementa.

## Validacion remota

La integracion anterior 1be237d paso H2 y MySQL en GitHub Actions. Para esta entrega revisar los checks del ultimo commit en el PR 15; la configuracion CI tambien ejecuta el SQL de datos y comprueba 30 notificaciones de prueba.

## Alcance y pendiente declarado

La creacion utiliza envio inicial simulado, permitido por MS-04. La solicitud de retry no tiene procesador posterior, otro envio ni historial nuevo. Esa parte de la demostracion integral sigue pendiente en la implementacion del responsable; no se altera aqui ni se presenta como cumplimiento total.

Preparacion: database/create_tables.sql, database/qa_notifications.sql, README, Swagger, docs/DEMOSTRACION.md y docs/EQUIPO.md. Cada integrante debe ensayar su parte y acreditar sus contribuciones reales.
