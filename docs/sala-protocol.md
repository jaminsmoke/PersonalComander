# Protocolo LAN de sala (Bar 0.1)

Contrato entre **Personal Comander** (cliente) y **Personal Bar** (nodo). Puerto fijo **8787**. HTTP en claro solo en LAN de confianza.

No confundir con Identity: el **servicio camareros** (`:8080`, HTTPS en producción) es el DNI del camarero y la fuente de verdad de cuentas y membresías. El **servicio negocio** (`:8082`) es de Bar / Identity Web. El nodo de sala sigue siendo Bar LAN.

Rutas Identity que Commander llama: [`identity-contract-paths.txt`](identity-contract-paths.txt).
Rutas Bar que Commander llama: [`bar-contract-paths.txt`](bar-contract-paths.txt). El job CI `Family contracts` las contrasta con Identity `main` (OpenAPI camareros y negocio) y Bar `main` (`BarLanModule.kt`); el informe queda en el summary del job. Aún no es check requerido del ruleset.

## Glosario

| Término | Qué es | Qué no es |
|---|---|---|
| **Establecimiento** | Negocio / local. Registro canónico en Identity. En turno, el camarero se **liga al nodo Bar** (`ModoSesion.Establecimiento`). | Una sala del mapa. |
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
| `POST /v1/sesion` | `{ "qr": "phid1:…" }` → `{ admitido, camareroId, nombre }` | Sí (candado carta/mapa/TPV al ligar; **no** inicia jornada) |
| `POST /v1/sesion/iniciar` | `{ "qr": "phid1:…" }` → estado de jornada | Sí (gesto Empiezo; 404 = nodo viejo) |
| `POST /v1/sesion/cortar` | `{ "qr": "phid1:…" }` | Sí (terminar jornada o salir del nodo) |
| `POST /v1/heartbeat` | `{ "camareroId" }` | Sí (~10 s mientras hay jornada) |
| `POST /v1/rondas` | recibe una ronda → 201/200 + **lista de tickets**; 403 sin jornada | Sí (enviar; se guarda `ticketId` por línea) |
| `POST /v1/tickets/{id}/preparado` | ticket preparado | No (UI de expo en Bar) |
| `POST /v1/tickets/{id}/recogido` | ticket recogido en expo | No (UI de expo en Bar) |
| `GET /v1/estado` | establecimiento, salas, colas, mesas | Sí (realinear tickets al conectar SSE; **réplica de layout** al ligar si `admitido`) |
| `GET /v1/carta` | catálogo `{productos:[{id,nombre,categoria,precio,disponible}]}` | Sí (espejo al ligar) |
| `SSE /v1/eventos` | `ticket.preparado` / `ticket.recogido` / `sesion.cortada` | Sí (aviso recoger y corte de jornada) |

Handshake al ligar: `POST /v1/sesion` con el QR. Si el camarero está ACTIVA en la lista blanca, `admitido=true` y candan carta, mapa y TPV. 404 (nodo viejo) o no admitido: el ligue sigue; `admitido=false`.

**Contratado ≠ jornada.** Ligar al nodo no autoriza comandas. El camarero pide `POST /v1/sesion/iniciar` (gesto Empiezo). Bar concede `sesionActiva`. Commander manda `POST /v1/heartbeat` ~10 s. Corte: `POST /v1/sesion/cortar`, SSE `sesion.cortada`, 403 de ronda/heartbeat, o pérdida de lista blanca. 404 en iniciar = Bar 0.1: se trata como jornada implícita.

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

Al reconectar: `GET /v1/estado` y marcar `PREPARADO` (y `servidos`) como `LISTA` en líneas con ese `ticketId`. El bucle SSE **no** reescribe el mapa.

## Réplica de mapa

Bar es la fuente de verdad del layout. Si el camarero está **admitido**, al ligar Commander lee `salas` y `mesas` de `GET /v1/estado` y hace upsert por `codigoBar` (el `id` string de Bar). Conserva `id` Room, `estado`, `comandaActivaId` y `reservaActivaId` locales. **No borra** el seed ni salas/mesas sin código. Si no está admitido, 404 o el nodo no manda mapa, el layout local no se toca. `idZona` sigue siendo función (`zonaPrefijo(nombreSala)+indiceZona`), no un campo JSON.

## Flujo de usuario

1. El camarero inicia sesión contra Identity (servicio camareros) desde **Ajustes** o **Entrar**. La cuenta puede estar **registrada** en varios establecimientos; eso no activa un turno.
2. **Standalone** (Local o Identidad): carta y mapa locales. El header lo indica. Ligarse a un nodo no es jornada.
3. Activa el turno buscando el Bar en LAN (puerto 8787) o por host. Tras el health, Commander consulta `POST /v1/sesion`. Si Identity lista locales y el `health` no coincide, se avisa pero **no se bloquea**.
4. Si está en la lista blanca, carta, mapa y TPV pasan a solo lectura y se replica el layout. El header dice **En nodo** hasta **Empezar jornada** (`POST /v1/sesion/iniciar`); entonces dice **Activo**. Al volver a Home se **revalida** `admitido`. Si se pierde la lista blanca, la jornada se corta.
5. Sin jornada no se llama a `POST /v1/rondas`. Si Bar corta (SSE `sesion.cortada`, 403 o latido), el nodo puede seguir ligado.
6. Al enviar con jornada, Comander manda **solo líneas `PENDIENTE`**, las marca `ENVIADA` y guarda los `ticketId` del body.
7. Bar marca preparado → SSE → líneas `LISTA` + snackbar/notificación.
8. El camarero marca **servido en mesa** (`SERVIDA`). Recogido de bandeja sigue en Bar.

Pendiente de lista blanca **no** es una invitación de cuenta. Las invitaciones (Bar invita, camarero acepta) van en ítems de Bar e Identity; la aceptación in-app aún no está.

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
- `productoId`: id de red de Bar (`cana`) si Commander espejó `GET /v1/carta` (`codigoBar`); si no, el Long de Room en string. Sin match, Bar manda la línea a BARRA.
- Respuesta: array de tickets (`id` = `{rondaId}-barra` / `-cocina`).

## Comportamiento en Comander

| Modo | Al enviar a cocina |
|---|---|
| Local / Identidad | Solo Room: pedido `ENVIADA`, mesa `EN_COCINA`, líneas `LISTA` (para recoger sin expo). |
| Establecimiento **con jornada** | Delta `PENDIENTE` + `POST /v1/rondas` → `ENVIADA` + `ticketId`. SSE + `/estado`. Servido solo desde `LISTA`. |
| Establecimiento **sin jornada** | No se llama a `/v1/rondas`. El camarero ve el aviso y empieza la jornada en Ajustes. |

No se recorta el un-tablet. `esActivo` en código sigue significando «ligado al nodo»; el permiso de comandar es `sesionTrabajo`.
