# Sync TPV

La app puede importar el catálogo de productos desde un **servidor TPV en la red local** (LAN), sin pasar por la nube.

## Cómo funciona

1. **Escaneo de la red**: el cliente busca el servidor TPV en la subred local (escaneo TCP /24).
2. **Conexión**: se conecta al host configurado en Ajustes (IP + puerto).
3. **Descarga**: hace una petición HTTP GET y recibe el catálogo como JSON.
4. **Importación**: los productos se insertan con `replace`, actualizando el menú local.

## Ficheros

| Fichero | Función |
|---|---|
| `Tpv.kt` | Adaptador del modelo TPV |
| `TpvCliente.kt` | Cliente de red: escaneo, conexión y descarga |

## Configuración

- **IP del servidor TPV**: campo en Ajustes con `KeyboardType.Uri` (corregido en el item #2 — antes el teclado numérico impedía escribir puntos).
- **Cleartext**: la app usa `cleartextTraffic=true` para el tráfico HTTP local.

!!! warning "Seguridad"
    El tráfico LAN **no está cifrado**. Usa la app solo en redes de confianza. Consulta la [política de seguridad](https://github.com/jaminsmoke/PersonalComander/blob/main/SECURITY.md) para más detalles.

## Flujo desde Ajustes

1. Abre **Ajustes → TPV** y selecciona el programa de gestión compatible.
2. Usa **Buscar en red** para localizar servidores o introduce IP, puerto y ruta manualmente.
3. Revisa el servidor encontrado y pulsa **Sincronizar**.
4. La app muestra una vista previa de productos nuevos, actualizados e ignorados antes de aplicar el catálogo.

La sincronización TPV solo actualiza el catálogo local. No es el mismo canal que la conexión a Personal Bar: el TPV entrega productos y el Bar recibe rondas de comandas.

En modo Establecimiento la carta puede ser de solo lectura, porque el catálogo del local se considera la fuente compartida. El flujo de cuenta, establecimiento y Bar está en [Cuenta y turno](manual/cuenta.md).

## Protocolo de sala

El catálogo TPV y el nodo Bar son canales distintos. Las rondas hacia Personal Bar están en [Protocolo LAN](sala-protocol.md) (issue #44). El establecimiento es el negocio; las salas son las zonas del mapa.
