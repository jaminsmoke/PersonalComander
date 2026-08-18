# Ajustes

Ajustes es configuración: TPV, copias de seguridad, la URL de Identity (por si hay que cambiarla) y el **turno** en un Bar de la red local.

El login diario **no está aquí**. Para entrar o ver el perfil usa el chip **Entrar** / avatar del header (Resumen, Gestión o Ajustes). El flujo de cuenta está en [Cuenta y turno](cuenta.md).

<div class="pc-doc-shot" markdown>

<figure>
![Ajustes](../screenshots/ajustes.png)
<figcaption>TPV, turno Bar y copias de seguridad.</figcaption>
</figure>

</div>

## Sincronización con TPV

Commander puede importar el catálogo de un TPV por la red local:

1. Abre **Ajustes** y elige el programa de gestión compatible.
2. Usa **Buscar en red** o escribe IP, puerto y ruta.
3. Revisa la vista previa (nuevos, actualizados, ignorados).
4. Sincroniza.

El tráfico LAN **no va cifrado**. Úsalo solo en redes de confianza. Detalle técnico: [Sync TPV](../sync-tpv.md).

El TPV entrega productos. Personal Bar recibe **rondas** de comanda. No son el mismo canal.

## Copias de seguridad

- **Exportar:** guarda mesas, productos, pedidos y reservas en un JSON.
- **Importar:** restaura el estado desde ese fichero.

Exporta una copia antes de un cambio grande de carta o al cambiar de teléfono. Detalle: [Backup](../backup.md).

## URL del servicio camareros

El valor por defecto es `https://camareros.siberia.solutions` (Identity en el VPS). El campo en Ajustes es un **escape** para desarrollo o un fallo de red; no es el formulario de Entrar.

Commander no llama al servicio de negocio de Identity (`:8082`). Eso es de Personal Bar.

## Turno en el establecimiento

En **Ajustes → Turno en el establecimiento** buscas el Bar de la LAN (`:8787`) y ligas el teléfono al local. Qué implica el turno está en [Cuenta y turno](cuenta.md).
