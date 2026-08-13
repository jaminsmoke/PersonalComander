<div align="center">

<img src="docs/screenshots/home.png" alt="Personal Comander" width="180">

# 🛎️ Personal Comander

**Gestión de mesas y comandas para restaurantes — rápida, visual y por voz.**

Español · [English](README.md)

[![Release](https://img.shields.io/github/v/release/jaminsmoke/PersonalComander?color=%23E9C349&label=release)](https://github.com/jaminsmoke/PersonalComander/releases)
[![Build](https://img.shields.io/github/actions/workflow/status/jaminsmoke/PersonalComander/ci.yml?label=build&color=%23E9C349)](https://github.com/jaminsmoke/PersonalComander/actions)
[![License](https://img.shields.io/github/license/jaminsmoke/PersonalComander?color=%23E9C349)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)

**v1.5** · Android 7.0+ (API 24)

</div>

---

## ✨ Funcionalidades

- 🗺️ **Board visual de mesas** — arrastra mesas entre zonas, zoom, giro y ajuste al grid. **Semáforo de estados**: libre · ocupada · en cocina · reservada · bloqueada.
- 🛎️ **Comandas por voz** — toma pedidos hablando ("dos cafés con leche y una tarta") con un parser NLP en español y búsqueda difusa de productos. Sin conexión a internet.
- 📋 **Comandas táctiles** — pantalla de comanda completa con pestañas de categorías, buscador de productos y cantidades.
- 🍔 **Gestión de menú** — productos, precios, categorías e iconos emoji, totalmente offline.
- 📡 **Sync TPV** — importa el catálogo de productos desde tu TPV local por LAN.
- 💾 **Copia de seguridad** — exportación/importación JSON de toda la base de datos.
- 🌙 **Tema dark premium** — design system navy & gold, theming dinámico en Android 12+.
- 🌍 **Bilingüe** — interfaz en español e inglés.

## 📱 Capturas

| Resumen | Board de mesas | Menú | Comanda |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/home.png" width="180"> | <img src="docs/screenshots/mesas_board.png" width="180"> | <img src="docs/screenshots/menu.png" width="180"> | <img src="docs/screenshots/comanda.png" width="180"> |

## 🚀 Primeros pasos

### Requisitos

- JDK 17+
- Android SDK con `platforms;android-37`
- Un emulador o dispositivo con Android 7.0+ (API 24+)

### Compilar y ejecutar

```bash
# Instalar en un dispositivo/emulador conectado
./gradlew installDebug

# Tests unitarios
./gradlew test

# Tests de instrumentación (requiere emulador activo)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

En el primer arranque la app crea un menú de demostración y un plano de mesas por defecto, así puedes explorar todas las pantallas desde el minuto uno.

## 🧑‍🍳 Uso

1. **Resumen** — resumen del día: mesas ocupadas, pedidos activos y facturado.
2. **Mesas** — abre el board, toca una mesa para su comanda, arrástrala para moverla de zona, mantén pulsado para reservar/bloquear.
3. **Comanda** — añade productos por voz o con el dedo, revisa la cuenta, envíala a cocina y ciérrala al cobrar.
4. **Menú** — gestiona productos, precios, categorías e iconos.
5. **Ajustes** — configura el servidor TPV, importa/exporta copias y sincroniza el catálogo.

## 🏗️ Arquitectura

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navegación | `androidx.navigation.compose` |
| Estado | ViewModel + StateFlow |
| Base de datos | Room (SQLite) con KSP |
| Serialización | Gson (sync TPV, backup JSON) |
| Voz | Android `SpeechRecognizer` (en el dispositivo) |
| Min SDK / Target | 24 / 36 |

```
app/src/main/java/com/jaminsmoke/personalcomander/
├── data/     # Entidades, DAOs, migraciones, seed, sync TPV, backup
└── ui/       # Pantallas Compose, ViewModels, tema, voz + parser NLP
```

## 🗺️ Roadmap

- **v1.6** — sincronización de sala por LAN (Personal Bar como nodo), documentación del repo y CI/CD.
- Más adelante — funcionalidades premium (ver modelo de licencia).

## 🤝 Contribuir

¡Las contribuciones son bienvenidas! Consulta la [guía de contribución](CONTRIBUTING.md) para empezar, y abre issues para bugs o ideas.

## 📄 Licencia

Publicado bajo la [licencia MIT](LICENSE).
