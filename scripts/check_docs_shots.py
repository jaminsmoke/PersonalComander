#!/usr/bin/env python3
"""Comprueba que el HTML generado por MkDocs pinta las capturas.

El grep del markdown no basta: #95 referenció PNG que MkDocs dejó como
`![alt](../screenshots/…)` crudo dentro de <figure>. Este script mira el
`site/` ya construido.

Uso:
    python scripts/check_docs_shots.py --site-dir site
    python scripts/check_docs_shots.py --selftest
"""

from __future__ import annotations

import argparse
import re
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

IMG_SRC = re.compile(r"<img\b[^>]*\bsrc=\"([^\"]+)\"", re.IGNORECASE)
RAW_MD_SHOT = re.compile(r"!\[[^\]]*\]\([^)]*screenshots/[^)]+\)")

# Página HTML (relativa a site/) → ficheros de captura que deben aparecer en <img>.
PAGE_SHOTS: tuple[tuple[str, tuple[str, ...]], ...] = (
    ("index.html", ("home.png",)),
    (
        "manual/index.html",
        (
            "home.png",
            "mesas_board.png",
            "comanda.png",
            "menu.png",
            "locales.png",
            "invitaciones.png",
            "ajustes.png",
            "auth.png",
        ),
    ),
    ("manual/instalacion/index.html", ("home.png",)),
    ("manual/resumen/index.html", ("home.png",)),
    ("manual/mesas/index.html", ("mesas_board.png",)),
    ("manual/comandas/index.html", ("comanda.png",)),
    ("manual/carta/index.html", ("gestion.png", "menu.png")),
    ("manual/locales/index.html", ("locales.png",)),
    ("manual/invitaciones/index.html", ("invitaciones.png",)),
    ("manual/ajustes/index.html", ("ajustes.png",)),
    ("manual/cuenta/index.html", ("auth.png",)),
)


def img_names(html: str) -> set[str]:
    names: set[str] = set()
    for src in IMG_SRC.findall(html):
        names.add(Path(src.split("?", 1)[0]).name)
    return names


def comprobar(site: Path) -> list[str]:
    fallos: list[str] = []
    if not site.is_dir():
        return [f"no existe el directorio del sitio: {site}"]

    for rel, expected in PAGE_SHOTS:
        path = site / rel
        if not path.is_file():
            fallos.append(f"falta HTML {rel}")
            continue
        html = path.read_text(encoding="utf-8")
        if RAW_MD_SHOT.search(html):
            fallos.append(f"{rel} dejó markdown de captura sin convertir a <img>")
        found = img_names(html)
        for name in expected:
            if name not in found:
                fallos.append(f"{rel} no pinta <img> de {name}")

    for html_path in site.rglob("*.html"):
        html = html_path.read_text(encoding="utf-8")
        if RAW_MD_SHOT.search(html):
            rel = html_path.relative_to(site).as_posix()
            msg = f"{rel} dejó markdown de captura sin convertir a <img>"
            if msg not in fallos:
                fallos.append(msg)

    return fallos


def _write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def _fixture_ok(site: Path) -> None:
    shots = {
        "index.html": ["home.png"],
        "manual/index.html": [
            "home.png",
            "mesas_board.png",
            "comanda.png",
            "menu.png",
            "locales.png",
            "invitaciones.png",
            "ajustes.png",
            "auth.png",
        ],
        "manual/instalacion/index.html": ["home.png"],
        "manual/resumen/index.html": ["home.png"],
        "manual/mesas/index.html": ["mesas_board.png"],
        "manual/comandas/index.html": ["comanda.png"],
        "manual/carta/index.html": ["gestion.png", "menu.png"],
        "manual/locales/index.html": ["locales.png"],
        "manual/invitaciones/index.html": ["invitaciones.png"],
        "manual/ajustes/index.html": ["ajustes.png"],
        "manual/cuenta/index.html": ["auth.png"],
    }
    for rel, names in shots.items():
        imgs = "\n".join(f'<img src="../../screenshots/{n}" alt="{n}">' for n in names)
        _write(site / rel, f"<html><body>{imgs}</body></html>\n")


def selftest() -> int:
    with tempfile.TemporaryDirectory() as tmp:
        site = Path(tmp) / "site"
        _fixture_ok(site)
        fallos = comprobar(site)
        if fallos:
            print("SELFTEST FAIL: fixture verde no debía fallar", fallos, file=sys.stderr)
            return 1

        _fixture_ok(site)
        resumen = site / "manual" / "resumen" / "index.html"
        resumen.write_text(
            '<div>![Resumen](../screenshots/home.png)</div>\n',
            encoding="utf-8",
        )
        fallos = comprobar(site)
        if not any("sin convertir" in f for f in fallos):
            print("SELFTEST FAIL: debía detectar markdown crudo", file=sys.stderr)
            return 1

        _fixture_ok(site)
        (site / "manual" / "mesas" / "index.html").write_text(
            "<html><body><p>sin foto</p></body></html>\n",
            encoding="utf-8",
        )
        fallos = comprobar(site)
        if not any("mesas_board.png" in f for f in fallos):
            print("SELFTEST FAIL: debía exigir img en mesas", file=sys.stderr)
            return 1

        _fixture_ok(site)
        (site / "manual" / "carta" / "index.html").unlink()
        fallos = comprobar(site)
        if not any("carta" in f and "falta HTML" in f for f in fallos):
            print("SELFTEST FAIL: debía detectar HTML ausente", file=sys.stderr)
            return 1

    print("Docs shots selftest OK")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--selftest", action="store_true")
    parser.add_argument("--site-dir", type=Path, default=ROOT / "site")
    args = parser.parse_args()
    if args.selftest:
        return selftest()
    fallos = comprobar(args.site_dir)
    if fallos:
        for f in fallos:
            print(f"SHOTS {f}")
        return 1
    print("Documentation screenshots OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
