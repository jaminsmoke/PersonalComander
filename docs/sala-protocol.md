# Protocolo LAN de sala (Bar 0.1)

Contrato entre **Personal Comander** (cliente) y **Personal Bar** (nodo). Puerto fijo **8787**. HTTP en claro solo en LAN de confianza.

No confundir con Identity (`:8080`, HTTPS en producción): eso es el DNI del camarero, no el nodo del local.

## Glosario

| Término | Qué es | Qué no es |
|---|---|---|
| **Establecimiento** | Negocio / local. Cuenta del Bar. El camarero se **liga al establecimiento** (`ModoSesion.Establecimiento`). | Una sala del mapa. |
| **Sala** | Zona del mapa del establecimiento (barra, interior, terraza…). En Commander: entidad Room `Sala` + `Mesa.salaId` (antes «zona»). | El modo de sesión ni el host LAN. |
| **idZona** | Identidad de mesa **en red**: prefijo del **nombre de la sala** + `indiceZona` (`T3` = Terraza 3). | El `id` autoincrement de Room ni el alias visible. |
| **Ronda** | Lo que Commander envía al Bar al «enviar a cocina» ligado. Bar la parte en tickets BARRA / COCINA. | El pedido Room completo como modelo de red. |

`GET /health` trae `establecimiento` (nombre del negocio) y `sala` como **alias deprecado del mismo valor**. `sala` en health **no** es una sala del mapa.

## Endpoints (Bar)

| Método | Ruta | Uso en Commander 0.1 |
|---|---|---|
| `GET /health` | liveness `{ok, role:"bar", establecimiento, sala, version}` | Sí (ligar) |
| `POST /v1/rondas` | recibe una ronda → 201 o 200 idempotente | Sí (enviar) |
| `POST /v1/tickets/{id}/listo` | ticket listo | No (ítem recoger) |
| `POST /v1/tickets/{id}/servido` | ticket servido | No (ítem recoger) |
| `GET /v1/estado` | establecimiento, **salas**, colas, mesas | No (ítem mapa) |
| `SSE /v1/eventos` | `ticket.listo` / `ticket.servido` | No (ítem recoger) |

Sin autenticación en 0.1 (lista blanca QR de Bar aún no).

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
- `productoId`: en Commander es el Long de Room en string. El catálogo de Bar usa ids propios (`cana`…): sin sync de carta el destino puede ser siempre BARRA.

## Comportamiento en Commander

| Modo | Al enviar a cocina |
|---|---|
| Local / Identidad | Solo Room: pedido `ENVIADA`, mesa `EN_COCINA`. Sin HTTP. |
| Establecimiento | Lo mismo **y** `POST /v1/rondas` al `barHost`:`barPuerto`. Si Bar falla, lo local se queda; snackbar. |

No se exige `admitido=true`. No se recorta el un-tablet.
