#!/usr/bin/env python3
"""Cruza superficies de la app (NavHost, tabs, hub Gestión) con el manual.

Uso:
    python scripts/check_docs_cobertura.py
    python scripts/check_docs_cobertura.py --selftest
"""

from __future__ import annotations

import argparse
import re
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

SECTIONS = ("routes", "tabs", "gestion", "extras")
DEST_ENTRY = re.compile(
    r"^\s*([A-Z][A-Z0-9_]*)\s*\(\s*\"([^\"]+)\"",
    re.MULTILINE,
)
ENUM_ROUTE = re.compile(r"route\s*=\s*TopLevelDestination\.(\w+)\.route")
INTERPOLATED_ROUTE = re.compile(
    r"route\s*=\s*\"\$\{TopLevelDestination\.(\w+)\.route\}[^\"]*\"",
)
LITERAL_ROUTE = re.compile(r"route\s*=\s*\"([^\"$]+)\"")
GESTION_ENTRY = re.compile(r"^\s*([A-Z][A-Z0-9_]*)\s*\(", re.MULTILINE)


def parse_cobertura(text: str) -> dict[str, dict[str, dict]]:
    """YAML restringido: secciones → id → page/needles u omit."""
    out: dict[str, dict[str, dict]] = {s: {} for s in SECTIONS}
    section: str | None = None
    current: dict | None = None
    for raw in text.splitlines():
        stripped = raw.split("#", 1)[0].rstrip()
        if not stripped.strip():
            continue
        indent = len(stripped) - len(stripped.lstrip(" "))
        token = stripped.strip()
        if indent == 0 and token.endswith(":"):
            name = token[:-1]
            if name not in out:
                raise ValueError(f"sección desconocida en cobertura: {name}")
            section = name
            current = None
            continue
        if section is None:
            raise ValueError(f"clave fuera de sección: {token}")
        if indent == 2 and token.endswith(":"):
            ident = token[:-1]
            current = {}
            out[section][ident] = current
            continue
        if current is None:
            raise ValueError(f"propiedad sin id en {section}: {token}")
        if indent == 4:
            if token == "needles:":
                current["needles"] = []
                continue
            key, _, value = token.partition(":")
            current[key.strip()] = value.strip().strip("\"'")
            continue
        if indent == 6 and token.startswith("- "):
            current.setdefault("needles", []).append(token[2:].strip().strip("\"'"))
            continue
        raise ValueError(f"línea no reconocida: {raw}")
    return out


def top_level_destinations(text: str) -> dict[str, str]:
    start = text.find("enum class TopLevelDestination")
    if start < 0:
        return {}
    block = text[start:]
    end = block.find("\n}")
    block = block if end < 0 else block[:end]
    return {m.group(1): m.group(2) for m in DEST_ENTRY.finditer(block)}


def nav_routes(app_text: str, dest: dict[str, str]) -> set[str]:
    routes: set[str] = set()
    for name in ENUM_ROUTE.findall(app_text):
        if name not in dest:
            raise ValueError(f"TopLevelDestination.{name} no está en el enum")
        routes.add(dest[name])
    for name in INTERPOLATED_ROUTE.findall(app_text):
        if name not in dest:
            raise ValueError(f"TopLevelDestination.{name} no está en el enum")
        routes.add(dest[name])
    for raw in LITERAL_ROUTE.findall(app_text):
        routes.add(raw.split("?", 1)[0])
    return routes


def gestion_ids(text: str) -> set[str]:
    start = text.find("enum class GestionAcceso")
    if start < 0:
        return set()
    block = text[start:]
    end = block.find(";")
    block = block if end < 0 else block[:end]
    return {m.group(1) for m in GESTION_ENTRY.finditer(block)}


def _page_text(root: Path, page: str) -> str | None:
    path = root / "docs" / page
    if not path.is_file():
        return None
    return path.read_text(encoding="utf-8")


def _check_entry(
    kind: str,
    ident: str,
    spec: dict,
    root: Path,
) -> list[str]:
    fallos: list[str] = []
    if spec.get("omit"):
        return fallos
    page = spec.get("page")
    if not page:
        fallos.append(f"{kind} {ident}: falta page")
        return fallos
    text = _page_text(root, page)
    if text is None:
        fallos.append(f"{kind} {ident}: no existe docs/{page}")
        return fallos
    lower = text.lower()
    needles = spec.get("needles") or []
    if not needles:
        fallos.append(f"{kind} {ident}: hace falta al menos una aguja (o omit)")
        return fallos
    for needle in needles:
        if needle.lower() not in lower:
            fallos.append(
                f"{kind} {ident}: docs/{page} no menciona {needle!r}",
            )
    return fallos


def comprobar(
    root: Path,
    cobertura_text: str,
    app_text: str,
    bar_text: str,
    gestion_text: str,
) -> list[str]:
    fallos: list[str] = []
    try:
        mapa = parse_cobertura(cobertura_text)
    except ValueError as exc:
        return [str(exc)]

    dest = top_level_destinations(bar_text)
    if not dest:
        fallos.append("no se leyó TopLevelDestination en PcBottomBar.kt")
    try:
        routes = nav_routes(app_text, dest) if dest else set()
    except ValueError as exc:
        fallos.append(str(exc))
        routes = set()
    tabs = set(dest)
    gestion = gestion_ids(gestion_text)
    if not gestion:
        fallos.append("no se leyó GestionAcceso")

    extracted = {"routes": routes, "tabs": tabs, "gestion": gestion}
    for kind, have in extracted.items():
        declared = set(mapa.get(kind, {}))
        for ident in sorted(have - declared):
            fallos.append(f"{kind} {ident}: en el código y no en cobertura.yml")
        for ident in sorted(declared - have):
            fallos.append(f"{kind} {ident}: en cobertura.yml y no en el código")
        for ident in sorted(have & declared):
            fallos.extend(_check_entry(kind, ident, mapa[kind][ident], root))

    for ident, spec in mapa.get("extras", {}).items():
        fallos.extend(_check_entry("extras", ident, spec, root))
    return fallos


def comprobar_repo_files(root: Path) -> list[str]:
    paths = {
        "cobertura": root / "docs" / "manual" / "cobertura.yml",
        "app": root
        / "app"
        / "src"
        / "main"
        / "java"
        / "com"
        / "jaminsmoke"
        / "personalcomander"
        / "ui"
        / "PersonalComanderApp.kt",
        "bar": root
        / "app"
        / "src"
        / "main"
        / "java"
        / "com"
        / "jaminsmoke"
        / "personalcomander"
        / "ui"
        / "components"
        / "PcBottomBar.kt",
        "gestion": root
        / "app"
        / "src"
        / "main"
        / "java"
        / "com"
        / "jaminsmoke"
        / "personalcomander"
        / "ui"
        / "gestion"
        / "GestionAcceso.kt",
    }
    for path in paths.values():
        if not path.is_file():
            return [f"falta {path.as_posix()}"]
    return comprobar(
        root,
        paths["cobertura"].read_text(encoding="utf-8"),
        paths["app"].read_text(encoding="utf-8"),
        paths["bar"].read_text(encoding="utf-8"),
        paths["gestion"].read_text(encoding="utf-8"),
    )


def _write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def _fixture(root: Path) -> None:
    _write(
        root / "app" / "src" / "main" / "java" / "com" / "jaminsmoke" / "personalcomander" / "ui" / "components" / "PcBottomBar.kt",
        """
enum class TopLevelDestination(
    val route: String,
) {
    HOME("home", 1),
    MESAS("mesas", 2),
}
""",
    )
    _write(
        root / "app" / "src" / "main" / "java" / "com" / "jaminsmoke" / "personalcomander" / "ui" / "PersonalComanderApp.kt",
        """
            composable(route = TopLevelDestination.HOME.route) {}
            composable(route = TopLevelDestination.MESAS.route) {}
            composable(route = "auth") {}
""",
    )
    _write(
        root / "app" / "src" / "main" / "java" / "com" / "jaminsmoke" / "personalcomander" / "ui" / "gestion" / "GestionAcceso.kt",
        """
enum class GestionAcceso(
    val navKey: String,
) {
    CARTA(R.string.x, Icons.Default.A, "carta"),
    LOCALES(R.string.y, Icons.Default.B, "locales"),
    ;
}
""",
    )
    _write(
        root / "docs" / "manual" / "cobertura.yml",
        """
routes:
  home:
    page: manual/home.md
    needles:
      - Resumen
  mesas:
    page: manual/mesas.md
    needles:
      - Mesas
  auth:
    page: manual/cuenta.md
    needles:
      - Entrar
tabs:
  HOME:
    page: manual/home.md
    needles:
      - Resumen
  MESAS:
    page: manual/mesas.md
    needles:
      - Mesas
gestion:
  CARTA:
    page: manual/carta.md
    needles:
      - Carta
  LOCALES:
    omit: aún no hay guía
extras:
  recoger:
    page: manual/mesas.md
    needles:
      - Mesas
""",
    )
    _write(root / "docs" / "manual" / "home.md", "Resumen del día\n")
    _write(root / "docs" / "manual" / "mesas.md", "Mesas del board\n")
    _write(root / "docs" / "manual" / "cuenta.md", "Entrar con correo\n")
    _write(root / "docs" / "manual" / "carta.md", "Carta desde Gestión\n")


def selftest() -> int:
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        _fixture(root)
        fallos = comprobar_repo_files(root)
        if fallos:
            print("SELFTEST FAIL: fixture verde", fallos, file=sys.stderr)
            return 1

        app = root / "app" / "src" / "main" / "java" / "com" / "jaminsmoke" / "personalcomander" / "ui" / "PersonalComanderApp.kt"
        app.write_text(
            app.read_text(encoding="utf-8") + '\n            composable(route = "perfil") {}\n',
            encoding="utf-8",
        )
        fallos = comprobar_repo_files(root)
        if not any("perfil" in f for f in fallos):
            print("SELFTEST FAIL: debía exigir cobertura de perfil", file=sys.stderr)
            return 1

        _fixture(root)
        cob = root / "docs" / "manual" / "cobertura.yml"
        cob.write_text(
            cob.read_text(encoding="utf-8").replace(
                "routes:\n  home:",
                "routes:\n  fantasma:\n    page: manual/home.md\n    needles:\n      - Resumen\n  home:",
            ),
            encoding="utf-8",
        )
        fallos = comprobar_repo_files(root)
        if not any("fantasma" in f for f in fallos):
            print("SELFTEST FAIL: debía detectar ruta fantasma en YAML", file=sys.stderr)
            return 1

        _fixture(root)
        (root / "docs" / "manual" / "home.md").write_text("sin la palabra clave\n", encoding="utf-8")
        fallos = comprobar_repo_files(root)
        if not any("Resumen" in f for f in fallos):
            print("SELFTEST FAIL: debía exigir aguja Resumen", file=sys.stderr)
            return 1

        _fixture(root)
        app = root / "app" / "src" / "main" / "java" / "com" / "jaminsmoke" / "personalcomander" / "ui" / "PersonalComanderApp.kt"
        app.write_text(
            app.read_text(encoding="utf-8").replace(
                "TopLevelDestination.HOME.route",
                '"${TopLevelDestination.HOME.route}?abrir={abrir}"',
            ),
            encoding="utf-8",
        )
        fallos = comprobar_repo_files(root)
        if fallos:
            print("SELFTEST FAIL: ruta interpolada debía resolverse", fallos, file=sys.stderr)
            return 1

    print("Docs cobertura selftest OK")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--selftest", action="store_true")
    args = parser.parse_args()
    if args.selftest:
        return selftest()
    fallos = comprobar_repo_files(ROOT)
    if fallos:
        for f in fallos:
            print(f"COBERTURA {f}")
        return 1
    print("Documentation coverage OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
