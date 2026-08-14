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

[📥 Descargar APK](https://github.com/jaminsmoke/PersonalComander/releases/latest){ .md-button .md-button--primary }
[📖 Wiki](https://github.com/jaminsmoke/PersonalComander/wiki){ .md-button }
[📚 Documentación](arquitectura.md){ .md-button }

!!! warning "v1.6 en desarrollo"
    El APK descargable actual es **v1.5**. Los flujos de cuenta de camarero, establecimiento, salas y Personal Bar están evolucionando en `main` para v1.6. Consulta el [detalle de flujos](flujos-v16.md) antes de probarlos desde código.

</div>

---

## Características

<div class="grid cards" markdown>

- :material-map: **Board visual de mesas**

    Arrastra mesas entre zonas, haz zoom, rota y ajusta al grid. Semáforo de estados: libre · ocupada · en cocina · reservada · bloqueada.

- :material-message-processing: **Comandas por voz**

    Toma pedidos hablando — *"dos cafés con leche y una tarta"* — con un parser NLP en español y búsqueda difusa de productos. Sin internet.

- :material-notebook: **Comandas táctiles**

    Pantalla completa con pestañas de categorías, buscador de productos y cantidades.

- :material-hamburger: **Gestión de menú**

    Productos, precios, categorías e iconos emoji, totalmente offline.

- :material-server-network: **Sync TPV**

    Importa el catálogo de productos desde tu TPV local por LAN.

- :material-backup-restore: **Backup y restauración**

    Exportación e importación JSON de toda la base de datos.

- :material-weather-night: **Tema dark premium**

    Design system navy & gold, theming dinámico en Android 12+.

- :material-translate: **Bilingüe**

    Interfaz en español e inglés.

- :material-account-network: **Cuenta y sala (v1.6)**

    Perfil de camarero, QR, establecimiento, salas y envío de rondas a Personal Bar. En desarrollo.

</div>

## Capturas

<div class="grid cards" markdown>

- ![Resumen](screenshots/home.png){ width="200" }

    **Resumen del día** — mesas ocupadas, pedidos activos y facturado.

- ![Board de mesas](screenshots/mesas_board.png){ width="200" }

    **Board de mesas** — el plano de la sala con el semáforo de estados.

- ![Menú](screenshots/menu.png){ width="200" }

    **Gestión del menú** — productos, precios y categorías.

- ![Comanda](screenshots/comanda.png){ width="200" }

    **Comanda** — toma de pedidos táctil y por voz.

- ![Ajustes](screenshots/ajustes.png){ width="200" }

    **Ajustes** — cuenta, conexión TPV, sincronización y backup.

</div>

## ¿Qué puedes hacer con Personal Comander?

- Tomar comandas **por voz o con el dedo** en segundos
- Ver la **sala completa** en un board visual con estados por colores
- Mover mesas **entre zonas** con arrastrar y soltar
- **Reservar y bloquear** mesas sin mezclarlo con el estado de la comanda
- Gestionar el **menú** (productos, precios, categorías) sin conexión
- **Sincronizar el catálogo** desde tu TPV por la red local
- Llevar el **control de caja**: facturado hoy, pedidos activos
- **Exportar e importar** copias de seguridad JSON

Los flujos de cuenta, establecimiento y Personal Bar están documentados como **v1.6 en desarrollo** y no se incluyen todavía en el APK v1.5 descargable.

## Requisitos

- **Android 7.0+** (API 24)
- Un dispositivo o emulador con pantalla táctil (tablet recomendada para el board)
- Para la sincronización TPV: red local con el servidor del TPV

## Comunidad y ayuda

- [💬 Discussions](https://github.com/jaminsmoke/PersonalComander/discussions)
- [🐛 Reportar un bug](https://github.com/jaminsmoke/PersonalComander/issues/new)
- [🤝 Contribuir](https://github.com/jaminsmoke/PersonalComander/blob/main/CONTRIBUTING.md)
- [🔒 Reportar una vulnerabilidad](https://github.com/jaminsmoke/PersonalComander/security/policy)
