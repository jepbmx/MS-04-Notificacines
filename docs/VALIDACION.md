# Validacion de la integracion

Esta revision adopta el retry del PR 16. Los resultados anteriores de 73 pruebas y las pruebas de concurrencia del reintento corresponden a nuestra implementacion anterior, no a la del companero.

La nueva suite verifica ADMINISTRADOR/202/PENDING, mismo ID, contador, lectura conservada, rechazo de SERVICE, limites preexistentes y ausencia de otro intento. Conserva pruebas de creacion concurrente, propiedad, JWT, filtros, paginacion y plantillas. Consultar la ultima ejecucion de CI del PR para resultados actuales.

No se afirma que pase la demostracion de reenvio efectivo: falta el procesador del retry. Esto se mantiene como pendiente del responsable sin modificar su codigo.
