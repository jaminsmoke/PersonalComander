# Board de mesas

El board es el plano visual de la sala. Cada mesa se dibuja según su forma (redonda, cuadrada, rectangular) y su estado se muestra con un color (semáforo).

La pestaña **Mesas** de la barra inferior no lleva el chip de sesión: la cuenta se gestiona desde Resumen, Gestión o Ajustes.

<div class="pc-doc-shot" markdown>

<figure>
![Board de mesas](../screenshots/mesas_board.png)
<figcaption>Plano de la sala y semáforo de estados.</figcaption>
</figure>

</div>

## Semáforo de estados

| Estado | Color | Significado |
|---|---|---|
| Libre | Verde | Sin comanda activa |
| Ocupada | Amarillo | Comanda abierta |
| En cocina | Naranja | Comanda enviada |
| Reservada | Morado | Tiene una reserva |
| Bloqueada | Rojo | Mesa fuera de servicio |

La reserva y el bloqueo no forman parte del ciclo de comanda (`LIBRE` / `OCUPADA` / `EN_COCINA`). El primer producto de una mesa reservada la pasa a ocupada.

## Acciones

- **Tocar** una mesa → abre su comanda.
- **Arrastrar** una mesa → la mueve de sala.
- **Mantener pulsado** → reservar, cancelar reserva, bloquear o desbloquear.
- **Zoom** con botones +/− o pellizco.
- **Ver todo el plano** con el botón de ajuste al grid.

Si te equivocas al mover una mesa, hay deshacer.

## Salas

Las mesas se organizan por **salas** del mapa (barra, interior, terraza…). Una sala es una zona física del local. No es una cuenta, un modo de sesión ni un servidor LAN.

El identificador de red de una mesa sale del nombre de la sala más el índice: `T3` es la mesa 3 de Terraza. No es el id interno de la base de datos.

Cuando el dispositivo está en turno con un establecimiento, el mapa puede quedar en **solo lectura**: el plano lo marca el local (Personal Bar), no el teléfono.
