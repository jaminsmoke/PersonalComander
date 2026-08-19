# Resumen

La pestaña **Resumen** (barra inferior) es la pantalla de inicio. Muestra el día en curso. Mesas, Gestión y Ajustes se abren por la barra inferior, no por atajos en esta pantalla.

## Qué ves

- **Facturado hoy** — total de las líneas de comanda del teléfono en el día (Room). El Server no tiene facturado.
- **Mesas ocupadas** respecto al total del plano.
- **Pedidos activos** — comandas abiertas o enviadas.
- **Oficio** — horas de jornada y **rondas servidas** del libro canónico del Server (`GET /v1/camareros/me/resumen`). Chips **Día / Semana / Mes**. La gráfica son horas por día a partir de `GET /v1/camareros/me/jornadas` (intervalos reales, no un reloj local).

Sin cuenta (modo Local) el oficio no se rellena: no se inventan horas ni rondas. El hero de facturado sigue.

**En esta red** lista solo los Bares **descubiertos ahora** en la Wi‑Fi del teléfono (nombres, nunca IP). Bar anuncia al activar o cortar Local activo (UDP); si no llega el anuncio, el radar reintenta en esta pantalla. Gris = el local no te tiene de camarero; amarillo = admitido sin jornada; verde = jornada en curso; rojo = fallo de red. Un local de Identity que no está en esta red **no sale**. El detalle está en [Cuenta y turno](cuenta.md).

Las rondas las produce Bar al completar una ronda (todos los tickets **RECOGIDO**) y las proyecta a `POST /v1/negocio/estadisticas/servicio`. El campo del Server se llama `mesas_servidas`; en pantalla se etiqueta **rondas servidas**. Las horas se abren al **Empezar jornada** (dual-write LAN + `POST /v1/camareros/me/jornadas/iniciar` si el `establecimiento_id` del health coincide con una membresía; si el nodo no manda UUID, se usa el nombre).

<div class="pc-doc-shot" markdown>

![Resumen del día](../screenshots/home.png)

*Facturado, mesas ocupadas, pedidos activos y panel de oficio.*

</div>

El chip **Entrar** / avatar del header abre la cuenta. Bajo el título, el indicador de turno (**Standalone** o el nombre del local) lleva a Entrar si estás en Local; con sesión, el radar de locales está en esta misma pantalla.
