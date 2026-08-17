---
hide:
  - navigation
  - toc
title: Personal Comander
---

<div class="pc-hero" markdown>

![Logo Personal Comander](assets/logo.png){ width="128" }

# 🛎️ Personal Comander

**Gestión de mesas y comandas para restaurantes — rápida, visual y por voz.**

El board visual de mesas, la toma de comandas por voz y el control de caja en una sola app Android, pensada para camareros.

<div class="pc-cta" markdown>

[📥 Descargar APK](https://github.com/jaminsmoke/PersonalComander/releases/latest){ .md-button .md-button--primary }

Manual en la [Wiki](https://github.com/jaminsmoke/PersonalComander/wiki) · [Documentación técnica](arquitectura.md)

</div>

</div>

<div class="pc-aviso" markdown>

!!! info "v1.6"
    El APK actual es **v1.6**: cuenta de camarero (Identity en el VPS), ficha pública, visibilidad y turno LAN con Personal Bar. [Detalle de flujos](flujos-v16.md).

</div>

## Para el camarero

<div class="grid cards" markdown>

- :material-map: **Board visual de mesas**

    Arrastra mesas entre zonas, haz zoom, rota y ajusta al grid. Semáforo de estados: libre · ocupada · en cocina · reservada · bloqueada.

- :material-message-processing: **Comandas por voz**

    Toma pedidos hablando — *"dos cafés con leche y una tarta"* — con un parser NLP en español y búsqueda difusa de productos. Sin internet.

- :material-notebook: **Comandas táctiles**

    Pantalla completa con pestañas de categorías, buscador de productos y cantidades.

- :material-hamburger: **Gestión de menú**

    Productos, precios, categorías e iconos emoji, totalmente offline.

</div>

## Para el local

<div class="grid cards" markdown>

- :material-server-network: **Sync TPV**

    Importa el catálogo de productos desde tu TPV local por LAN.

- :material-backup-restore: **Backup y restauración**

    Exportación e importación JSON de toda la base de datos.

- :material-weather-night: **Tema dark premium**

    Design system navy & gold, theming dinámico en Android 12+.

- :material-translate: **Bilingüe**

    Interfaz en español e inglés.

- :material-account-network: **Cuenta y sala**

    Perfil de camarero, QR de ficha pública, visibilidad y envío de rondas a Personal Bar.

</div>

## Así se ve en acción

<div class="pc-shots">

<figure>
![Resumen del día](screenshots/home.png)
<figcaption>Resumen: mesas ocupadas, pedidos activos y facturado.</figcaption>
</figure>

<figure>
![Board de mesas](screenshots/mesas_board.png)
<figcaption>El plano de la sala con el semáforo de estados.</figcaption>
</figure>

<figure>
![Gestión del menú](screenshots/menu.png)
<figcaption>Productos, precios y categorías.</figcaption>
</figure>

<figure>
![Comanda](screenshots/comanda.png)
<figcaption>Toma de pedidos táctil y por voz.</figcaption>
</figure>

<figure>
![Ajustes](screenshots/ajustes.png)
<figcaption>Cuenta, TPV, sincronización y backup.</figcaption>
</figure>

</div>

## Requisitos

- **Android 7.0+** (API 24)
- Un dispositivo o emulador con pantalla táctil (móvil vertical; AVD `Movil-Pixel10a`)
- Para la sincronización TPV: red local con el servidor del TPV

## Comunidad y ayuda

- [💬 Discussions](https://github.com/jaminsmoke/PersonalComander/discussions)
- [🐛 Reportar un bug](https://github.com/jaminsmoke/PersonalComander/issues/new)
- [🤝 Contribuir](https://github.com/jaminsmoke/PersonalComander/blob/main/CONTRIBUTING.md)
- [🔒 Reportar una vulnerabilidad](https://github.com/jaminsmoke/PersonalComander/security/policy)
