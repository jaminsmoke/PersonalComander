# Cuenta y turno

La release **v1.6** incluye cuenta de camarero (Identity en el VPS), ficha pública, visibilidad y turno LAN con Personal Bar.

## Oficios

**Commander** es la app del camarero (sala, comanda, cuenta). **Personal Bar** es el puesto del negocio (nodo LAN, colas, lista blanca). **Identity** es la fuente de verdad de cuentas, negocios y membresías.

El camarero crea y edita su cuenta en Commander. Bar **consulta** Identity y asigna cuentas que ya existen; no las crea. Room y las preferencias del teléfono son **caché y fallback** si Identity no responde.

## Entrar

1. En el header, pulsa **Entrar** (chip de sesión). No hace falta ir a Ajustes.
2. Correo y contraseña. Pestañas **Entrar** / **Registro**.
3. Identity por defecto: `https://camareros.siberia.solutions`.

Con sesión, el mismo chip (avatar + nick) abre **Mi perfil**: nick, foto, visibilidad de la ficha, QR, ver la ficha pública, cambiar contraseña, dirección y ciudad, renovar o revocar la clave, cerrar sesión.

Revocar el QR invalida la credencial actual. Renovarlo genera una nueva. Cerrar sesión no borra la cuenta; borrar la cuenta es otra acción y pide confirmación.

El nick que se ve en los locales se edita aquí, en registro o perfil.

## Turno en un establecimiento

El camarero se liga en **turno** al nodo Bar del local, no a una «sala» como si fuera un servidor. El registro canónico del negocio y de la membresía vive en Identity. Las **salas** (barra, interior, terraza…) son zonas del mapa.

1. En **Ajustes → Turno en el establecimiento**, busca Bares en la LAN.
2. Conecta con el QR / sesión de camarero.
3. Comprueba admisión y, si el nodo lo pide, inicia jornada.

Cuando el dispositivo está ligado:

- El mapa puede quedar en solo lectura.
- La carta puede quedar bloqueada para edición local.
- Las mesas siguen mostrando su identidad de red (`T3`, etc.).
- Al enviar una comanda, primero se actualiza el teléfono y después se publica una ronda a Bar.
- Desconectar el Bar vuelve al modo anterior **sin borrar** el estado local.

Si Bar no responde, la comanda enviada **se queda en el teléfono** y la app avisa.

El payload, los códigos HTTP y la idempotencia están en [Protocolo LAN](../sala-protocol.md). El catálogo TPV es otro canal: [Sync TPV](../sync-tpv.md).

## Qué hay en cada versión

| Flujo | v1.5 | v1.6 |
|---|---:|---:|
| Board, mesas y comandas locales | Sí | Sí |
| Voz on-device | Sí | Sí |
| TPV y backup JSON | Sí | Sí |
| Cuenta, perfil, QR y visibilidad | No | Sí |
| Establecimiento, salas y ronda a Bar | No | Sí |
