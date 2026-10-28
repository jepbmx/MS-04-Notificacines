# Preparacion del equipo

La evaluacion requiere dominio individual y seguimiento real de ramas, commits y PR. Cada integrante debe ensayar su parte y poder explicar el flujo completo. No atribuir contribuciones que no esten en el historial.

| Asignacion del documento MS-04 | Temas a explicar |
|---|---|
| Diana: POST registro | DTO, validacion, plantillas, Idempotency-Key, transaccion, 201/200/409 |
| Gerardo: consulta ID y filtros | Propiedad, 404, consultas JPA, paginacion y sort permitido |
| Miguel: /me y /read | JWT userId, lectura independiente, idempotencia de readAt, seguridad |
| Jose Eduardo: /status y /retry | Estados, limite, intentos, bloqueo y reutilizacion del ID |

Preguntas para practicar:
- Por que id_receiver no tiene FK a la base de otro microservicio?
- Que evita dos notificaciones con la misma clave y que devuelve una carrera concurrente?
- Por que el reintento conserva el ID y por que requiere un rol privilegiado?
- Diferencia entre FAILED/read=false y SENT/read=true. Puede leerse una fallida?
- Por que max_retries=3 permite cuatro intentos totales?
- Que significa willRetry si no hay proceso automatico?
- Diferencia entre 401, 403, 404, 409 y 422. Por que un envio fallido puede devolver HTTP 201?
- Que prueba Mockito, que prueba MockMvc con JWT y que prueba la base de datos?
- Diferencias entre H2 y MySQL; por que hay un trabajo CI MySQL separado?
- Que NO garantiza un porcentaje de cobertura alto?

Checklist humano: cada persona revisa sus commits y PR, ensaya peticiones exitosas y fallidas, abre JaCoCo, y registra tareas reales pendientes. No se puede garantizar preparacion individual con una modificacion de codigo.
