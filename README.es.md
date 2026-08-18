<div align="center">

<img src="docs/screenshots/home.png" alt="Personal Comander" width="180">

# Personal Comander

**La herramienta del camarero** en la familia PersonalHostel: mesas, comandas táctiles y por voz, en un móvil en vertical. Personal Bar es el puesto del **negocio** (colas, nodo LAN). Identity es el registro de cuentas (VPS).

Español · [English](README.md)

[![Release](https://img.shields.io/github/v/release/jaminsmoke/PersonalComander?color=%23E9C349&label=release)](https://github.com/jaminsmoke/PersonalComander/releases)
[![Build](https://img.shields.io/github/actions/workflow/status/jaminsmoke/PersonalComander/ci.yml?label=build&color=%23E9C349)](https://github.com/jaminsmoke/PersonalComander/actions)
[![License](https://img.shields.io/github/license/jaminsmoke/PersonalComander?color=%23E9C349)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)

**v1.6** · Android 7.0+ (API 24) · [🌐 Sitio](https://jaminsmoke.github.io/PersonalComander/) · [📖 Manual](https://jaminsmoke.github.io/PersonalComander/manual/)

</div>

---

La ficha de producto, el manual y la documentación técnica están en el [sitio](https://jaminsmoke.github.io/PersonalComander/). Este README es para clonar y compilar la app.

## Primeros pasos

- JDK 17+
- Android SDK con `platforms;android-37`
- Emulador o dispositivo Android 7.0+ (API 24+). Objetivo de UI: móvil vertical, AVD `Movil-Pixel10a`

```bash
./gradlew installDebug
./gradlew test
./gradlew connectedAndroidTest
./gradlew lint
```

El primer arranque crea un menú y un plano de demostración.

El login diario es **Entrar** en el header, no Ajustes. Cuenta, perfil y turno Bar: [manual](https://jaminsmoke.github.io/PersonalComander/manual/cuenta/).

## Docs y capturas

```bash
python scripts/generate_assets.py
bash scripts/capture_screens.sh
# Teléfono y tablet a la vez: ADB_DEVICE=emulator-5554 bash scripts/capture_screens.sh
python scripts/check_docs_links.py
python scripts/check_docs_structure.py
python scripts/check_docs_cobertura.py
python scripts/check_docs_shots.py --selftest
```

Regenera los assets públicos tras un cambio de marca o de UI para no desincronizar el README y el sitio. Arquitectura para humanos: [docs/arquitectura.md](docs/arquitectura.md). Mapa para agentes: [`AGENTS.md`](AGENTS.md).

## Roadmap

- **v1.7** — ciclo actual del kanban.
- Más adelante — funcionalidades premium (ver modelo de licencia).

## Contribuir

[CONTRIBUTING.md](CONTRIBUTING.md). Seguridad: [SECURITY.md](SECURITY.md).

## Licencia

[MIT](LICENSE).
