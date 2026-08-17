<div align="center">

<img src="docs/screenshots/home.png" alt="Personal Comander" width="180">

# Personal Comander

**The waiter’s tool** in the PersonalHostel family: table board, touch and voice orders, on a phone in portrait. Personal Bar is the **venue** station (queues, LAN node). Identity is the account registry (VPS).

[Español](README.es.md) · English

[![Release](https://img.shields.io/github/v/release/jaminsmoke/PersonalComander?color=%23E9C349&label=release)](https://github.com/jaminsmoke/PersonalComander/releases)
[![Build](https://img.shields.io/github/actions/workflow/status/jaminsmoke/PersonalComander/ci.yml?label=build&color=%23E9C349)](https://github.com/jaminsmoke/PersonalComander/actions)
[![License](https://img.shields.io/github/license/jaminsmoke/PersonalComander?color=%23E9C349)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)

**v1.6** · Android 7.0+ (API 24) · [🌐 Site](https://jaminsmoke.github.io/PersonalComander/) · [📖 Manual](https://jaminsmoke.github.io/PersonalComander/manual/)

</div>

---

Product page, user manual and technical docs live on the [site](https://jaminsmoke.github.io/PersonalComander/). This README is for cloning and building the app.

## Getting started

- JDK 17+
- Android SDK with `platforms;android-37`
- Emulator or device on Android 7.0+ (API 24+). UI target: portrait phone, AVD `Movil-Pixel10a`

```bash
./gradlew installDebug
./gradlew test
./gradlew connectedAndroidTest
./gradlew lint
```

First launch seeds a demo menu and table layout.

Daily login is **Entrar** in the header, not Settings. Account, profile and Bar shift: [manual](https://jaminsmoke.github.io/PersonalComander/manual/cuenta/).

## Docs and screenshots

```bash
python scripts/generate_assets.py
bash scripts/capture_screens.sh
# Phone + tablet emulators: ADB_DEVICE=emulator-5556 bash scripts/capture_screens.sh
python scripts/check_docs_links.py
python scripts/check_docs_structure.py
```

Regenerate public assets after a rebrand or UI redesign so the README and the site stay in sync. Architecture for humans: [docs/arquitectura.md](docs/arquitectura.md). Agent map: [`AGENTS.md`](AGENTS.md).

## Roadmap

- **v1.7** — current kanban cycle.
- Later — premium features (see licensing model).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Security: [SECURITY.md](SECURITY.md).

## License

[MIT](LICENSE).
