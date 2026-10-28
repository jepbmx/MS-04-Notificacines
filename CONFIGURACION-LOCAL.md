# Configuracion local (una vez por equipo)

1. Copiar `config/local.example.properties` a `config/local.properties`.
2. Completar los cuatro valores. No usar comillas alrededor de ellos. El secreto debe tener al menos 32 bytes UTF-8; para tokens reales debe coincidir con Users-MS.
3. Preparar MySQL con `database/create_tables.sql` si la base es nueva. La aplicacion valida el esquema, no lo crea.
4. Recargar el proyecto Maven en IntelliJ. Elegir `Notificaciones local` para iniciar y `Generar JWT local` para obtener un token. Ambas ejecuciones deben usar la carpeta del proyecto como directorio de trabajo. Si IntelliJ solicita modulo, seleccionar el modulo Maven `notificaciones`.

El perfil local importa `config/local.properties`. Ese archivo esta ignorado por Git y fuera de los recursos empaquetados. La configuracion normal de otros entornos conserva sus variables de entorno.

Si quedaron variables DB_URL, DB_USERNAME, DB_PASSWORD o JWT_SECRET en configuraciones antiguas de IntelliJ, quitarlas de las nuevas ejecuciones para evitar discrepancias. Las variables de entorno tienen prioridad sobre el archivo.

El generador no inicia la base de datos. Mantiene el usuario de prueba 1 y rol CLIENTE. El token dura 15 minutos. En Postman: GET http://localhost:8080/api/notifications/me?page=0&size=10, Authorization > Bearer Token. Pegar solo el token. Un 200 con contenido vacio tambien es correcto.

Un token CLIENTE no permite POST: requiere SERVICE o ADMINISTRADOR. Generar un token nuevo cuando expire. Para diagnostico temporal, agregar --logging.level.org.springframework.security=TRACE a los argumentos de la aplicacion.

No subir config/local.properties ni compartir secretos o tokens en capturas. Para pruebas entre equipos, acordar el secreto por un canal privado. No reutilizar secretos de produccion.
