# Ajustes

La pestaña **Ajustes** abre un hub de baldosas (el mismo patrón que Gestión): **TPV** y **Copias**. Cada baldosa lleva a su sección; la flecha vuelve al hub.

El login diario **no está aquí**. Para entrar o ver el perfil usa el chip **Entrar** / avatar del header (Resumen, Gestión o Ajustes). El flujo de cuenta está en [Cuenta y turno](cuenta.md).

El turno de sala no vive en Ajustes: los Bares de **esta Wi‑Fi** se ven en [Resumen](resumen.md). Las membresías de Identity están en [Gestión → Locales](locales.md).

<div class="pc-doc-shot" markdown>

![Ajustes](../screenshots/ajustes.png)

*Hub de Ajustes: TPV y Copias.*

</div>

## Sincronización con TPV

Commander puede importar el catálogo de un TPV por la red local:

1. Abre **Ajustes → TPV** y elige el programa de gestión compatible.
2. Usa **Buscar en red** o escribe IP, puerto y ruta.
3. Revisa la vista previa (nuevos, actualizados, ignorados).
4. Sincroniza.

El tráfico LAN **no va cifrado**. Úsalo solo en redes de confianza. Detalle técnico: [Sync TPV](../sync-tpv.md).

El TPV entrega productos. Personal Bar recibe **rondas** de comanda. No son el mismo canal.

## Copias de seguridad

En **Ajustes → Copias**:

- **Exportar:** guarda mesas, productos, pedidos y reservas en un JSON.
- **Importar:** restaura el estado desde ese fichero.

Exporta una copia antes de un cambio grande de carta o al cambiar de teléfono. Detalle: [Backup](../backup.md).

Identity (camareros) apunta al VPS por defecto; no hay campo de URL en la app. Commander no llama al servicio de negocio (`:8082`). Eso es de Personal Bar.
