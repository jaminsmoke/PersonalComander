# Protocolo LAN de sala (Bar 0.1)

Contrato entre **Personal Comander** (cliente) y **Personal Bar** (nodo). Puerto fijo **8787**. HTTP en claro solo en LAN de confianza.

No confundir con Identity (`:8080`, HTTPS en producción): eso es el DNI del camarero, no el nodo del local.

## Glosario

| Término | Qué es | Qué no es |
|---|---|---|
| **Establecimiento** | Negocio / local. Cuenta del Bar. El camarero se **liga al establecimiento** (`ModoSesion.Establecimiento`). | Una sala del mapa. |
| **Sala** | Zona del mapa del establecimiento (barra, interior, terraza…). En Comander: entidad Room `Sala` + `Mesa.salaId` (antes «zona»). | El modo de sesión ni el host LAN. |
| **idZona** | Identidad de mesa **en red**: prefijo del **nombre de la sala** + `indiceZona` (`T3` = Terraza 3). | El `id` autoincrement de Room ni el alias visible. |
| **Ronda** | Lo que Comander envía al Bar al «enviar a cocina» ligado. Bar la parte en tickets BARRA / COCINA. | El pedido Room completo como modelo de red. |
| **Preparado** | Ticket listo en expo (Bar). Evento SSE `ticket.preparado`. En Comander: líneas `LISTA` («para recoger»). | Servido en mesa. |
| **Recogido** | El camarero de barra sacó el ticket de la cola (`POST /recogido`). Evento `ticket.recogido`. | Servido en mesa (eso es Comander, `LineaEstado.SERVIDA`). |

`GET /health` trae `establecimiento` (nombre del negocio) y `sala` como **alias deprecado del mismo valor**. `sala` en health **no** es una sala del mapa.

## Endpoints (Bar)

| Método | Ruta | Uso en Comander |
|---|---|---|
| `GET /health` | liveness `{ok, role:"bar", establecimiento, sala, version}` | Sí (ligar) |
| `POST /v1/rondas` | recibe una ronda → 201/200 + **lista de tickets** | Sí (enviar; se guarda `ticketId` por línea) |
| `POST /v1/tickets/{id}/preparado` | ticket preparado | No (UI de expo en Bar) |
| `POST /v1/tickets/{id}/recogido` | ticket recogido en expo | No (UI de expo en Bar) |
| `GET /v1/estado` | establecimiento, salas, colas, mesas | Sí (realinear al conectar SSE; Bar no persiste eventos) |
| `SSE /v1/eventos` | `ticket.preparado` / `ticket.recogido` | Sí (aviso recoger) |

Sin autenticación en 0.1 (lista blanca QR de Bar aún no).

## SSE

`event` = tipo. `data` = JSON `SalaEvent` v1 (Bar [#37](https://github.com/jaminsmoke/PersonalBar/issues/37) / PR [#38](https://github.com/jaminsmoke/PersonalBar/pull/38)).

`mesaId` va en la raíz. `destino`, `numeroCola` y `rondaId` van **dentro de `ticket`**. Commander los hidrata a la raíz al parsear. Si faltan, **no inventa la mesa**.

```json
{
  "version": 1,
  "tipo": "ticket.preparado",
  "ticketId": "p42-t1730000000000-barra",
  "preparadoPor": "Anita",
  "mesaId": "T3",
  "camarero": "Lucía García",
  "resumen": "2× Caña",
  "ticket": {
    "id": "p42-t1730000000000-barra",
    "rondaId": "p42-t1730000000000",
    "destino": "BARRA",
    "estado": "PREPARADO",
    "preparadoPor": "Anita",
    "numeroCola": 1,
    "lineas": [
      { "productoId": "12", "nombreProducto": "Caña", "cantidad": 2 }
    ]
  }
}
```

Aviso en sala: `T3 · Cola 1 Bebida lista` (`destino` `BARRA` → Bebida, `COCINA` → Comida).

Al reconectar: `GET /v1/estado` y marcar `PREPARADO` (y `servidos`) como `LISTA` en líneas con ese `ticketId`.

## Flujo de usuario

1. El camarero inicia sesión desde **Ajustes** y abre su **Perfil** para revisar la credencial QR.
2. Busca el establecimiento en la sección de sala y conecta el Bar por host y puerto.
3. La app muestra las salas del local y puede bloquear la edición local del mapa o la carta.
4. Al enviar, Comander manda **solo líneas `PENDIENTE`**, las marca `ENVIADA` y guarda los `ticketId` del body.
5. Bar marca preparado → SSE → líneas `LISTA` + snackbar/notificación.
6. El camarero marca **servido en mesa** (`SERVIDA`). Recogido de bandeja sigue en Bar.

Si Bar falla el POST, la comanda local permanece enviada y el tablet muestra un aviso.

Este flujo pertenece a v1.6 en desarrollo. La release pública v1.5 funciona en modo Local y no incluye la integración completa de establecimiento.

## Payload de ronda

```json
{
  "id": "p42-t1730000000000",
  "mesaId": "T3",
  "numero": 1,
  "camarero": "Lucía García",
  "creadoEn": 1730000000000,
  "lineas": [
    { "productoId": "12", "nombreProducto": "Caña", "cantidad": 2 },
    { "productoId": "3", "nombreProducto": "Croquetas", "cantidad": 1 }
  ]
}
```

- `id`: único por envío. Si se repite, Bar responde 200 y no duplica.
- `mesaId`: **idZona**, p. ej. `T3`. Nunca el id Room.
- `productoId`: en Comander es el Long de Room en string. El catálogo de Bar usa ids propios (`cana`…): sin sync de carta el destino puede ser siempre BARRA.
- Respuesta: array de tickets (`id` = `{rondaId}-barra` / `-cocina`).

## Comportamiento en Comander

| Modo | Al enviar a cocina |
|---|---|
| Local / Identidad | Solo Room: pedido `ENVIADA`, mesa `EN_COCINA`, líneas `LISTA` (para recoger sin expo). |
| Establecimiento | Delta `PENDIENTE` + `POST /v1/rondas` → `ENVIADA` + `ticketId`. SSE + `/estado`. Servido solo desde `LISTA`. |

No se exige `admitido=true`. No se recorta el un-tablet.
