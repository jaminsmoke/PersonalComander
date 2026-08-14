# Flujos v1.6

!!! warning "Estado de esta página"
    Estos flujos están en desarrollo en `main` y no forman parte todavía del APK público v1.5. La [release actual](https://github.com/jaminsmoke/PersonalComander/releases/latest) sigue siendo v1.5.

Esta página conecta la documentación técnica con el [manual de usuario de la wiki](https://github.com/jaminsmoke/PersonalComander/wiki). Describe qué ocurre en la aplicación cuando se usa identidad de camarero y cuando el dispositivo se liga a un establecimiento.

## Oficios

**Commander** es la app del camarero (sala, comanda, cuenta). **Personal Bar** es el puesto del negocio (nodo LAN, colas, lista blanca). **Identity** (VPS) es la fuente de verdad de cuentas de camarero, negocios y membresías.

La UX visual sigue partida: el camarero crea su cuenta en Commander; el negocio se da de alta en Bar. Bar **consulta** Identity; no es el registro. Room y las prefs de sesión en el teléfono son **fallback** si Identity no responde.

Bar no crea cuentas de camarero: las asigna al establecimiento. El nick visible en los locales se define aquí, en registro/perfil (ítem kanban `PVTI_lAHOBM87Yc4BgJWOzg2gWTY`).

## Cuenta del camarero

1. Desde **Ajustes**, abre **Cuenta**.
2. Regístrate o inicia sesión contra Identity.
3. Abre **Perfil** para revisar nombre, email, teléfono y avatar.
4. El perfil muestra el QR de credencial, que se puede renovar o revocar.

Revocar el QR invalida la credencial actual. Renovarlo genera una nueva credencial. Cerrar sesión no elimina la cuenta; borrar la cuenta es una acción separada y requiere confirmación.

## Establecimiento y salas

El camarero se liga en **turno** al nodo Bar del **establecimiento**. El registro canónico del local y de la membresía vive en Identity (`GET /v1/camareros/me/establecimientos`). Las **salas** son las zonas físicas del mapa, como barra, interior o terraza. No son modos de sesión ni servidores distintos.

Cuando el dispositivo está ligado a un establecimiento:

- El mapa puede quedar en solo lectura si la configuración pertenece al local.
- La carta puede quedar bloqueada para edición local.
- Las mesas siguen mostrando su identidad de red, por ejemplo `T3` para la mesa 3 de Terraza.
- Desconectar el Bar devuelve el dispositivo al modo anterior sin borrar el estado local.

## Comanda y ronda LAN

En modo **Local** o **Identidad**, enviar a cocina actualiza Room localmente: la comanda pasa a `ENVIADA` y la mesa a `EN_COCINA`, sin HTTP.

En modo **Establecimiento**, además se envía una ronda a Personal Bar mediante `POST /v1/rondas`. Si Bar no responde, la operación local se conserva y la aplicación muestra un aviso; no se pierde la comanda del tablet.

El contrato de payload, los códigos HTTP y la idempotencia están documentados en [Protocolo LAN](sala-protocol.md). La sincronización de catálogo TPV es un canal diferente y se explica en [Sync TPV](sync-tpv.md).

## Estado de publicación

| Flujo | v1.5 | v1.6 en `main` |
|---|---:|---:|
| Board, mesas y comandas locales | Disponible | Disponible |
| Voz y parser on-device | Disponible | Disponible |
| TPV y backup JSON | Disponible | Disponible |
| Cuenta, perfil y QR | No incluido | En desarrollo |
| Establecimiento, salas y ronda a Bar | No incluido | En desarrollo |
