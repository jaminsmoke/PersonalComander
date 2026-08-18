# Ajustes

La pestaña **Ajustes** abre un hub de baldosas (el mismo patrón que Gestión): **Turno**, **TPV**, **Copias** y **Avanzado**. Cada baldosa lleva a su sección; la flecha vuelve al hub.

El login diario **no está aquí**. Para entrar o ver el perfil usa el chip **Entrar** / avatar del header (Resumen, Gestión o Ajustes). El flujo de cuenta está en [Cuenta y turno](cuenta.md).

El indicador de turno en Resumen y en Gestión → Locales abre **Ajustes → Turno** (`ajustes?abrir=turno`), no el hub.

<div class="pc-doc-shot" markdown>

![Ajustes](../screenshots/ajustes.png)

*Hub de Ajustes: Turno, TPV, Copias y Avanzado.*

</div>

## Turno

En **Ajustes → Turno** buscas el Bar de la LAN (`:8787`) y ligas el teléfono al local. Las membresías de Identity siguen en esta sección. Qué implica el turno está en [Cuenta y turno](cuenta.md).

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

## Avanzado

**Ajustes → Avanzado** guarda la URL del servicio camareros. El valor por defecto es `https://camareros.siberia.solutions` (Identity en el VPS). El campo es un **escape** para desarrollo o un fallo de red; no es el formulario de Entrar.

Commander no llama al servicio de negocio de Identity (`:8082`). Eso es de Personal Bar.
