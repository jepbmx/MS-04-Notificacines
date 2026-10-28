# Validacion realizada

Fecha local: 28 de octubre de 2026. Rama: feat/cumplimiento-evaluacion-final.

## Verificado en la carpeta real del proyecto

Comando: `mvnw.cmd -B clean verify` con Microsoft OpenJDK 17.0.18.
Resultado: BUILD SUCCESS; 73 pruebas, 0 fallos, 0 errores, 0 omitidas.
Se genero el JAR ejecutable y el reporte JaCoCo, sin excluir codigo productivo para elevar el porcentaje.

| JaCoCo global | Cubierto |
|---|---:|
| Instrucciones | 98.65% |
| Lineas | 99.01% |
| Ramas | 93.68% |
| Metodos | 99.38% |

Reporte navegable: target/site/jacoco/index.html. Resultados: target/surefire-reports.

## Comprobacion del paquete y HTTP real

Se arranco el JAR con una base H2 temporal y puerto 18084, separado de la instancia personal. Se comprobaron por HTTP real las siete rutas, Swagger UI y OpenAPI, con resultados 201/200 para creacion, consulta, filtros, lectura y reintento; 200 para repeticion idempotente; 409 para reintentar SENT; 403 para CLIENTE y 400 para size=0. El proceso temporal fue detenido.

Las pruebas automatizadas incluyen fallos simulados, agotamiento del limite, propiedad, autenticacion real JWT, parametros invalidos, plantillas, reintentos concurrentes y eventos concurrentes. No usan las credenciales personales ni borran ecommerce.

## Limites que no deben ocultarse

- La comprobacion adicional con MySQL local fue rechazada con SQLState 42000, codigo 1044 (acceso a base denegado). No se obtuvo un resultado de pruebas MySQL local. El trabajo CI mysql esta preparado para validar el SQL y los contratos en una instancia aislada.
- No se demuestra correo externo: el documento permite el simulador. El envio es sincrono; willRetry representa elegibilidad para operacion manual.
- El ensayo con la configuracion local de cada companero y su dominio individual siguen siendo responsabilidad del equipo. La guia no acredita conocimientos ni contribuciones reales.
- Los resultados CI deben comprobarse en el PR; configurar el trabajo no equivale a una ejecucion exitosa.
