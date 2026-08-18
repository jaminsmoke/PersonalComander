# Resumen

La pestaña **Resumen** (barra inferior) es la pantalla de inicio. Muestra el día en curso. Mesas, Gestión y Ajustes se abren por la barra inferior, no por atajos en esta pantalla.

## Qué ves

- **Facturado hoy** — total de las líneas de comanda del teléfono en el día (Room). El Server no tiene facturado.
- **Mesas ocupadas** respecto al total del plano.
- **Pedidos activos** — comandas abiertas o enviadas.
- **Oficio** — horas de jornada y **rondas servidas** del libro canónico del Server (`GET /v1/camareros/me/resumen`). Chips **Día / Semana / Mes**. La gráfica son horas por día a partir de `GET /v1/camareros/me/jornadas` (intervalos reales, no un reloj local).

Sin cuenta (modo Local) el oficio no se rellena: no se inventan horas ni rondas. El hero de facturado sigue.

Las rondas las produce Bar al completar una ronda (todos los tickets **RECOGIDO**) y las proyecta a `POST /v1/negocio/estadisticas/servicio`. El campo del Server se llama `mesas_servidas`; en pantalla se etiqueta **rondas servidas**. Las horas se abren al **Empezar jornada** (dual-write LAN + `POST /v1/camareros/me/jornadas/iniciar` si el nombre de health coincide con una membresía).

<div class="pc-doc-shot" markdown>

![Resumen del día](../screenshots/home.png)

*Facturado, mesas ocupadas, pedidos activos y panel de oficio.*

</div>

El chip **Entrar** / avatar del header abre la cuenta. Bajo el título, el indicador de turno (**Standalone** o el nombre del local) lleva a Entrar o a Ajustes; el detalle está en [Cuenta y turno](cuenta.md).
