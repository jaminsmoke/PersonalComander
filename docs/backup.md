# Backup y restauración

Personal Comander permite **exportar e importar** la base de datos completa en formato JSON desde Ajustes.

## Exportación

- Serializa todas las entidades (mesas, productos, pedidos, líneas, reservas) a un único JSON.
- Se guarda en el almacenamiento del dispositivo para compartirlo o archivarlo.

## Importación

- Lee el JSON de backup y restaura el estado completo de la base de datos.
- Útil para migrar de dispositivo o recuperar tras un reset.

## Modelos

Los modelos de import/export viven en `BackupJson.kt` (data classes de Gson).

## Buenas prácticas

- Haz copias de seguridad **antes** de actualizar el menú a gran escala o de cambiar de dispositivo.
- El fichero de backup contiene datos de negocio (precios, pedidos): protégelo.
- La integridad de la restauración se valida con tests (ver [Modelo de datos](data-model.md)).

!!! tip
    La sincronización de sala por LAN (#18) reducirá la dependencia de backups manuales entre dispositivos del mismo local.
