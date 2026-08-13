<div align="center">

<img src="docs/screenshots/home.png" alt="Personal Comander" width="180" style="border-radius: 24px;">

# 🏰 Personal Comander

**Restaurant table & order management for waiters — fast, visual, voice-first.**

[Español](README.es.md) · English

[![Release](https://img.shields.io/github/v/release/jaminsmoke/PersonalComander?color=%23E9C349&label=release)](https://github.com/jaminsmoke/PersonalComander/releases)
[![Build](https://img.shields.io/github/actions/workflow/status/jaminsmoke/PersonalComander/ci.yml?label=build&color=%23E9C349)](https://github.com/jaminsmoke/PersonalComander/actions)
[![License](https://img.shields.io/github/license/jaminsmoke/PersonalComander?color=%23E9C349)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)

**v1.5** · Android 7.0+ (API 24)

</div>

---

## ✨ Features

- 🗺️ **Visual table board** — drag & drop tables across zones, zoom, rotate and fit-to-grid. Color-coded **traffic-light status**: free · occupied · in kitchen · reserved · blocked.
- 🛎️ **Voice-first orders** — take orders by voice ("dos cafés con leche y una tarta") with an on-device Spanish NLP parser and fuzzy product matching. No internet needed.
- 📋 **Touch orders** — full Comanda screen with category tabs, product search and quantities.
- 🍔 **Menu management** — products, prices, categories and emoji icons, fully offline.
- 📡 **POS sync** — pull the product catalog from your local TPV over the LAN.
- 💾 **Backup & restore** — JSON import/export of the whole database.
- 🌙 **Dark premium theme** — navy & gold design system, dynamic theming on Android 12+.
- 🌍 **Bilingual** — Spanish and English UI.

## 📱 Screenshots

| Home | Table board | Menu | Order |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/home.png" width="180"> | <img src="docs/screenshots/mesas_board.png" width="180"> | <img src="docs/screenshots/menu.png" width="180"> | <img src="docs/screenshots/comanda.png" width="180"> |

## 🚀 Getting started

### Requirements

- JDK 17+
- Android SDK with `platforms;android-37`
- An emulator or device running Android 7.0+ (API 24+)

### Build & run

```bash
# Install on a connected device / emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumentation tests (requires a running emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

The first launch seeds a demo menu and a default table layout, so you can explore every screen right away.

## 🧑‍🍳 Usage

1. **Home** — daily summary: occupied tables, active orders and revenue so far.
2. **Tables** — open the board, tap a table to open its order, drag to move it between zones, long-press for reserve/block actions.
3. **Order** — add products by tapping or by voice, review the ticket, send it to the kitchen and close it when paid.
4. **Menu** — manage products, prices, categories and icons.
5. **Settings** — configure the TPV server, import/export backups, and sync the catalog.

## 🏗️ Architecture

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | `androidx.navigation.compose` |
| State | ViewModel + StateFlow |
| Database | Room (SQLite) with KSP |
| Serialization | Gson (TPV sync, JSON backup) |
| Voice | Android `SpeechRecognizer` (on-device) |
| Min SDK / Target | 24 / 36 |

```
app/src/main/java/com/jaminsmoke/personalcomander/
├── data/     # Entities, DAOs, migrations, seed, TPV sync, backup
└── ui/       # Compose screens, ViewModels, theme, voice + NLP parser
```

## 🗺️ Roadmap

- **v1.6** — LAN room sync (Personal Bar as a node), repo documentation & CI/CD.
- Later — premium features (see licensing model).

## 🤝 Contributing

We welcome contributions! Check out the [contributing guide](CONTRIBUTING.md) to get started, and open issues for bugs or feature ideas.

## 📄 License

See [LICENSE](LICENSE) for details.
