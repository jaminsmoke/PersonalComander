---
hide:
  - navigation
  - toc
title: Personal Comander
---

<div class="pc-hero" markdown>

<div class="pc-hero-copy" markdown>

# Personal Comander

**La herramienta del camarero** — mesas, comanda táctil y por voz, en un móvil en vertical.

En la familia PersonalHostel, Commander es el teléfono de sala. **Personal Bar** es el puesto del negocio (colas y nodo LAN). **Identity** es el registro de cuentas, en el VPS. No son la misma app.

<div class="pc-cta" markdown>

[📥 Descargar APK](https://github.com/jaminsmoke/PersonalComander/releases/latest){ .md-button .md-button--primary }
[📖 Manual](manual/index.md){ .md-button }

</div>

</div>

<div class="pc-hero-shot" markdown>

![Resumen del día en Personal Comander](screenshots/home.png)

</div>

</div>

<div class="pc-aviso" markdown>

!!! info "v1.6"
    La release actual incluye cuenta de camarero, ficha pública, visibilidad y turno LAN con Personal Bar. [Cuenta y turno](manual/cuenta.md).

</div>

## En sala

<div class="grid cards" markdown>

- :material-map: **Board de mesas**

    Arrastra mesas entre salas, zoom, giro y ajuste al grid. Semáforo: libre · ocupada · en cocina · reservada · bloqueada.

- :material-message-processing: **Comanda por voz**

    *Dos cafés con leche y una tarta* — parser en el dispositivo, sin internet.

- :material-notebook: **Comanda táctil**

    Categorías, buscador y cantidades a pantalla completa.

- :material-hamburger: **Carta**

    Productos y precios desde Gestión, también offline.

</div>

## En el local

<div class="grid cards" markdown>

- :material-account-network: **Cuenta y turno**

    Identity en el header (**Entrar**), QR de ficha y rondas a Personal Bar.

- :material-server-network: **Sync TPV**

    Catálogo desde el TPV de la red local.

- :material-backup-restore: **Backup**

    Exportar e importar toda la base en JSON.

- :material-translate: **Bilingüe**

    Interfaz en español e inglés. Tema navy y oro, dinámico en Android 12+.

</div>

## Así se ve

<div class="pc-shots">

<figure class="pc-shot-lead">
![Resumen del día](screenshots/home.png)
<figcaption>Resumen: mesas ocupadas, pedidos activos y facturado.</figcaption>
</figure>

<figure>
![Board de mesas](screenshots/mesas_board.png)
<figcaption>Plano de la sala y semáforo de estados.</figcaption>
</figure>

<figure>
![Gestión del menú](screenshots/menu.png)
<figcaption>Carta: productos, precios y categorías.</figcaption>
</figure>

<figure>
![Comanda](screenshots/comanda.png)
<figcaption>Pedido táctil y por voz.</figcaption>
</figure>

<figure>
![Ajustes](screenshots/ajustes.png)
<figcaption>TPV, turno Bar y copias de seguridad.</figcaption>
</figure>

</div>

## Requisitos

- **Android 7.0+** (API 24)
- Móvil en vertical (AVD de referencia: `Movil-Pixel10a`)
- TPV y Bar: red local. Cuenta Identity: red hacia el VPS

## Comunidad

- [💬 Discussions](https://github.com/jaminsmoke/PersonalComander/discussions)
- [🐛 Reportar un bug](https://github.com/jaminsmoke/PersonalComander/issues/new)
- [🤝 Contribuir](https://github.com/jaminsmoke/PersonalComander/blob/main/CONTRIBUTING.md)
- [🔒 Vulnerabilidad](https://github.com/jaminsmoke/PersonalComander/security/policy)
- [Técnica](arquitectura.md)
