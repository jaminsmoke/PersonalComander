# Comandas

Abre una mesa desde el [board](mesas.md) para tomar el pedido. La comanda es a pantalla completa: categorías, buscador, cantidades y envío.

## Por voz

1. Abre la mesa.
2. Toca el micrófono.
3. Habla con naturalidad, por ejemplo *dos cafés con leche y una tarta*.
4. Revisa las líneas y confirma.

El reconocimiento va **en el dispositivo**: no hace falta internet. Entiende cantidades y nombres del catálogo, con algo de tolerancia a errores de transcripción. El detalle técnico está en [Voz](../voz.md).

## Táctil

1. Busca el producto por nombre o recorre las pestañas de categoría.
2. Toca el producto para añadirlo.
3. Ajusta la cantidad si hace falta.

## Ciclo

1. **Abierta** — se van añadiendo productos.
2. **Enviada** — se guarda en el teléfono. Si hay turno con un establecimiento, además se publica una **ronda** a Personal Bar (barra / cocina).
3. **Cerrada** — se cobra y la mesa vuelve a libre.

En modo **Local** o **Identidad**, enviar a cocina no habla con Bar: solo actualiza la base local. En modo **Establecimiento**, Bar reparte la ronda. Si Bar no responde, la comanda **se conserva enviada en el teléfono** y la app avisa; no se pierde el pedido.

## Para recoger y servido

Con turno en un Bar, cuando barra o cocina marca el ticket listo las líneas pasan a **para recoger** (`LISTA`). Un aviso (snackbar o notificación) te lo dice. Al dárselo al cliente, márcalas **servido**.

Sin Bar, enviar a cocina deja las líneas en enviada; no hay cola de expo.

El contrato de esa ronda está en [Protocolo LAN](../sala-protocol.md).

## Consejos

- El primer producto de una mesa reservada la convierte en ocupada.
- Cerrar la mesa pide confirmación; si te equivocas, puedes reabrirla.
