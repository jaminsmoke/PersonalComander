# Modelo de datos

La base de datos Room (`personal_comander.db`) contiene la estructura central de la app.

## Entidades y relaciones

```
Sala (room)  1──* Mesa (table)  1──* Pedido (order)  1──* LineaPedido (line items)
                        │
                        └── linked via comandaActivaId on Mesa

Producto (product) ─── referenced by LineaPedido.productoId
```

- **Sala**: recinto del mapa del establecimiento (barra, interior, terraza…). No es el modo de sesión.
- **Establecimiento / local**: cuenta del Bar a la que el camarero se liga (`ModoSesion.Establecimiento`).
- **Mesa**: la mesa física con posición en el board, `salaId`, forma y alias. El ID visible y de red (B1, T2) sale del **nombre** de la sala (`idZona`). No es el id Room.
- **Pedido**: la comanda de una mesa (abierta, enviada a cocina o cerrada).
- **LineaPedido**: cada línea del pedido (producto + cantidad + importe).
- **Producto**: ítem del menú con precio, categoría e icono.

## Enums

| Enum | Valores | Uso |
|---|---|---|
| `MesaEstado` | `LIBRE`, `OCUPADA`, `EN_COCINA` | Ciclo de vida de la comanda |
| `MesaForma` | `REDONDA`, `CUADRADA`, `RECTANGULAR`, `RECTANGULAR_XL` | Render del board |
| `PedidoEstado` | `ABIERTA`, `ENVIADA`, `CERRADA` | Estados del pedido |

## Hold de sala (reservas y bloqueos)

Desde v1.5, la reserva y el bloqueo **viven aparte del ciclo de comanda**:

- Entidad `Reserva` (nombre, mesa, fecha) + flag `bloqueada` en Mesa.
- El ciclo de comanda sigue siendo `LIBRE / OCUPADA / EN_COCINA`.
- El menú de mesa permite: reservar (con nombre), cancelar reserva, bloquear y desbloquear.
- El **primer producto** convertido convierte la reserva en mesa ocupada.

## Migraciones

- El schema se exporta a `app/schemas/com.jaminsmoke.personalcomander.data.AppDatabase/` (ficheros `N.json`).
- Cada versión nueva incrementa `version` en `AppDatabase` y añade una migración `MIGRATION_N_M+1`.
- Las migraciones se verifican con tests de instrumentación en `androidTest` (`MigracionesTest`).
- **Regla**: nunca modificar una migración ya publicada; crear una nueva.

## Integridad

- Operaciones multi-tabla: `@Transaction` o `db.withTransaction {}`.
- La normalización de posiciones del board se ejecuta en el ViewModel con `distinctUntilChanged` para evitar bucles.
