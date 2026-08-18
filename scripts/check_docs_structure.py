#!/usr/bin/env python3
"""Comprueba organización del sitio MkDocs: nav, versión, assets, anti-copia.

Uso:
    python scripts/check_docs_structure.py
    python scripts/check_docs_structure.py --selftest
"""

from __future__ import annotations

import argparse
import re
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

NAV_MD = re.compile(r"([A-Za-z0-9_./-]+\.md)")
VERSION_NAME = re.compile(r'versionName\s*=\s*"([^"]+)"')
WIKI_URL = "github.com/jaminsmoke/PersonalComander/wiki"
STALE = (
    "release descargable actual es v1.5",
    "apk público actual es v1.5",
    "en desarrollo para v1.6",
    "v1.6 en desarrollo",
)
ANTI_COPY_IN_ARQUITECTURA = (
    "RMS_UMBRAL_CERCANIA",
    "dos cafés con leche y una tarta",
)
REQUIRED_ASSETS = (
    "docs/assets/logo.png",
    "docs/assets/favicon.png",
    "docs/assets/og-image.png",
    "docs/screenshots/home.png",
    "docs/screenshots/auth.png",
    "docs/screenshots/mesas_board.png",
    "docs/screenshots/gestion.png",
    "docs/screenshots/menu.png",
    "docs/screenshots/locales.png",
    "docs/screenshots/invitaciones.png",
    "docs/screenshots/comanda.png",
    "docs/screenshots/ajustes.png",
)
# Captura junto al texto que la explica (el Inicio solo lleva el hero).
CONTEXT_SHOTS = (
    ("docs/index.md", "screenshots/home.png"),
    ("docs/manual/index.md", "screenshots/auth.png"),
    ("docs/manual/instalacion.md", "screenshots/home.png"),
    ("docs/manual/resumen.md", "screenshots/home.png"),
    ("docs/manual/mesas.md", "screenshots/mesas_board.png"),
    ("docs/manual/comandas.md", "screenshots/comanda.png"),
    ("docs/manual/carta.md", "screenshots/gestion.png"),
    ("docs/manual/carta.md", "screenshots/menu.png"),
    ("docs/manual/locales.md", "screenshots/locales.png"),
    ("docs/manual/invitaciones.md", "screenshots/invitaciones.png"),
    ("docs/manual/ajustes.md", "screenshots/ajustes.png"),
    ("docs/manual/cuenta.md", "screenshots/auth.png"),
)
VERSION_FILES = (
    "docs/index.md",
    "docs/manual/instalacion.md",
    "docs/manual/faq.md",
    "README.md",
    "README.es.md",
)


def nav_pages(mkdocs_text: str) -> list[str]:
    pages: list[str] = []
    in_nav = False
    for line in mkdocs_text.splitlines():
        if line.startswith("nav:"):
            in_nav = True
            continue
        if not in_nav:
            continue
        if line.strip() and not line[0].isspace() and not line.startswith("-"):
            break
        match = NAV_MD.search(line)
        if match:
            pages.append(match.group(1).replace("\\", "/"))
    return pages


def version_name(gradle_text: str) -> str | None:
    match = VERSION_NAME.search(gradle_text)
    return match.group(1) if match else None


def comprobar(root: Path) -> list[str]:
    fallos: list[str] = []
    mkdocs = root / "mkdocs.yml"
    gradle = root / "app" / "build.gradle.kts"
    docs = root / "docs"

    if not mkdocs.is_file():
        return ["falta mkdocs.yml"]
    mkdocs_text = mkdocs.read_text(encoding="utf-8")
    if WIKI_URL in mkdocs_text.replace("https://", "").replace("http://", ""):
        fallos.append("mkdocs.yml no debe enlazar la wiki; el manual vive en Pages")

    pages = nav_pages(mkdocs_text)
    if "index.md" not in pages:
        fallos.append("nav no incluye index.md")

    md_files = sorted(
        p.relative_to(docs).as_posix()
        for p in docs.rglob("*.md")
    )
    nav_set = set(pages)
    for rel in md_files:
        if rel not in nav_set:
            fallos.append(f"página huérfana (no está en nav): docs/{rel}")
    for rel in pages:
        if not (docs / rel).is_file():
            fallos.append(f"nav apunta a un fichero que no existe: {rel}")

    if not gradle.is_file():
        fallos.append("falta app/build.gradle.kts")
        return fallos
    ver = version_name(gradle.read_text(encoding="utf-8"))
    if not ver:
        fallos.append("no se encontró versionName en app/build.gradle.kts")
        return fallos
    pin = f"v{ver}"
    for rel in VERSION_FILES:
        path = root / rel
        if not path.is_file():
            fallos.append(f"falta {rel} (debe mencionar {pin})")
            continue
        text = path.read_text(encoding="utf-8")
        if pin not in text:
            fallos.append(f"{rel} no menciona la versión pública {pin}")

    for rel in REQUIRED_ASSETS:
        if not (root / rel).is_file():
            fallos.append(f"falta asset {rel}")

    for rel, needle in CONTEXT_SHOTS:
        path = root / rel
        if not path.is_file():
            fallos.append(f"falta {rel} (debe mostrar {needle})")
            continue
        if needle not in path.read_text(encoding="utf-8"):
            fallos.append(f"{rel} no referencia {needle}")

    for md in docs.rglob("*.md"):
        text = md.read_text(encoding="utf-8")
        rel = md.relative_to(root).as_posix()
        if WIKI_URL in text:
            fallos.append(f"{rel} enlaza la wiki; el manual debe vivir en docs/manual/")
        lower = text.lower()
        for phrase in STALE:
            if phrase in lower:
                fallos.append(f"{rel} conserva copy obsoleto: {phrase!r}")

    arq = docs / "arquitectura.md"
    if arq.is_file():
        arq_text = arq.read_text(encoding="utf-8")
        for needle in ANTI_COPY_IN_ARQUITECTURA:
            if needle.lower() in arq_text.lower() or needle in arq_text:
                fallos.append(
                    f"docs/arquitectura.md duplica material de voz ({needle!r}); "
                    "debe enlazar voz.md",
                )

    return fallos


def _write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def _fixture_ok(root: Path) -> None:
    _write(
        root / "mkdocs.yml",
        "site_name: t\n"
        "nav:\n"
        "  - Inicio: index.md\n"
        "  - Manual: manual/index.md\n"
        "  - Instalación: manual/instalacion.md\n"
        "  - FAQ: manual/faq.md\n"
        "  - Resumen: manual/resumen.md\n"
        "  - Mesas: manual/mesas.md\n"
        "  - Comandas: manual/comandas.md\n"
        "  - Carta: manual/carta.md\n"
        "  - Locales: manual/locales.md\n"
        "  - Invitaciones: manual/invitaciones.md\n"
        "  - Ajustes: manual/ajustes.md\n"
        "  - Cuenta: manual/cuenta.md\n"
        "  - Arquitectura: arquitectura.md\n",
    )
    _write(root / "app" / "build.gradle.kts", 'versionName = "1.6"\n')
    for rel in VERSION_FILES:
        _write(root / rel, "La release es v1.6.\n")
    _write(
        root / "docs" / "index.md",
        "v1.6\nscreenshots/home.png\n",
    )
    _write(
        root / "docs" / "manual" / "index.md",
        "Manual screenshots/home.png screenshots/auth.png\n",
    )
    _write(root / "docs" / "manual" / "instalacion.md", "Instalación v1.6 screenshots/home.png\n")
    _write(root / "docs" / "manual" / "resumen.md", "Resumen screenshots/home.png\n")
    _write(root / "docs" / "manual" / "mesas.md", "Mesas screenshots/mesas_board.png\n")
    _write(root / "docs" / "manual" / "comandas.md", "Comanda screenshots/comanda.png\n")
    _write(
        root / "docs" / "manual" / "carta.md",
        "Carta screenshots/gestion.png screenshots/menu.png\n",
    )
    _write(root / "docs" / "manual" / "locales.md", "Locales screenshots/locales.png\n")
    _write(
        root / "docs" / "manual" / "invitaciones.md",
        "Invitaciones screenshots/invitaciones.png\n",
    )
    _write(root / "docs" / "manual" / "ajustes.md", "Ajustes screenshots/ajustes.png\n")
    _write(root / "docs" / "manual" / "cuenta.md", "Cuenta screenshots/auth.png\n")
    _write(root / "docs" / "arquitectura.md", "Ver [Voz](voz.md).\n")
    for rel in REQUIRED_ASSETS:
        _write(root / rel, "x")


def selftest() -> int:
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        _fixture_ok(root)
        fallos = comprobar(root)
        if fallos:
            print("SELFTEST FAIL: fixture verde no debía fallar", fallos, file=sys.stderr)
            return 1

        (root / "mkdocs.yml").write_text(
            "nav:\n  - Inicio: index.md\n  - Wiki: https://github.com/jaminsmoke/PersonalComander/wiki\n",
            encoding="utf-8",
        )
        fallos = comprobar(root)
        if not any("wiki" in f.lower() for f in fallos):
            print("SELFTEST FAIL: debía detectar tab wiki en mkdocs.yml", file=sys.stderr)
            return 1

        _fixture_ok(root)
        _write(root / "docs" / "huerfana.md", "hola\n")
        fallos = comprobar(root)
        if not any("huérfana" in f for f in fallos):
            print("SELFTEST FAIL: debía detectar markdown huérfano", file=sys.stderr)
            return 1

        _fixture_ok(root)
        (root / "README.md").write_text("sin version\n", encoding="utf-8")
        fallos = comprobar(root)
        if not any("README.md" in f and "v1.6" in f for f in fallos):
            print("SELFTEST FAIL: debía exigir v1.6 en README.md", file=sys.stderr)
            return 1

        _fixture_ok(root)
        arq = root / "docs" / "arquitectura.md"
        arq.write_text('Ejemplo: "dos cafés con leche y una tarta"\n', encoding="utf-8")
        fallos = comprobar(root)
        if not any("voz" in f.lower() for f in fallos):
            print("SELFTEST FAIL: debía detectar copia de voz en arquitectura", file=sys.stderr)
            return 1

        _fixture_ok(root)
        faq = root / "docs" / "manual" / "faq.md"
        faq.write_text("El APK público actual es v1.5. v1.6\n", encoding="utf-8")
        fallos = comprobar(root)
        if not any("obsoleto" in f for f in fallos):
            print("SELFTEST FAIL: debía detectar copy v1.5", file=sys.stderr)
            return 1

        _fixture_ok(root)
        (root / "docs" / "manual" / "mesas.md").write_text("sin captura\n", encoding="utf-8")
        fallos = comprobar(root)
        if not any("mesas_board.png" in f for f in fallos):
            print("SELFTEST FAIL: debía exigir captura en mesas.md", file=sys.stderr)
            return 1

    print("Docs structure selftest OK")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--selftest", action="store_true")
    args = parser.parse_args()
    if args.selftest:
        return selftest()
    fallos = comprobar(ROOT)
    if fallos:
        for f in fallos:
            print(f"STRUCTURE {f}")
        return 1
    print("Documentation structure OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
