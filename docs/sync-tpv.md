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

## Roadmap relacionado

El item #18 del kanban plantea **Personal Bar como nodo de sala**: sincronización multi-dispositivo por LAN con identidad permanente. Cuando se implemente, esta sección se ampliará con el protocolo de sala.
