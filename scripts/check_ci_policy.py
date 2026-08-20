#!/usr/bin/env python3
"""Política versionada de la línea base de CI de PersonalComander.

Verifica sobre `.github/workflows/*.yml` (sin dependencias, por texto):

1. **Permisos explícitos**: cada workflow declara `permissions:` a nivel de
   workflow (antes de `jobs:`), sin heredar el token por defecto.
2. **Actions pinnadas**: ninguna `uses:` referencia un ref movible (`@vN`,
   `@main`, `@master`, `@latest`); solo SHA de 40 hex (con comentario de versión).
3. **Workflows requeridos**: existen `ci.yml` y `codeql.yml`.

Se ejecuta en el job `workflow-security` de `ci.yml` junto a actionlint y zizmor.

Uso:
    python scripts/check_ci_policy.py [--workflows DIR]
    python scripts/check_ci_policy.py --selftest
"""

from __future__ import annotations

import argparse
import re
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_WORKFLOWS = ROOT / ".github/workflows"

REQUIRED_WORKFLOWS = ("ci.yml", "codeql.yml")

USES_RE = re.compile(r"^\s*-\s+uses:\s*(\S+)", re.MULTILINE)
PERMISSIONS_RE = re.compile(r"^permissions:", re.MULTILINE)
JOBS_RE = re.compile(r"^jobs:", re.MULTILINE)
SHA_REF_RE = re.compile(r"^[0-9a-fA-F]{40}$")
MOVABLE_REF_RE = re.compile(r"@(v\d+(?:\.\d+)*|main|master|latest|develop|next)$")


def fallos_de_workflow(texto: str, nombre: str) -> list[str]:
    fallos: list[str] = []
    # 1. permissions a nivel de workflow (antes de jobs:).
    prefijo = texto.split("jobs:", 1)[0] if JOBS_RE.search(texto) else texto
    if not PERMISSIONS_RE.search(prefijo):
        fallos.append(f"{nombre}: falta `permissions:` a nivel de workflow")
    # 2. uses pinnadas por SHA (sin refs movibles).
    for m in USES_RE.finditer(texto):
        ref = m.group(1)
        if "@" not in ref:
            fallos.append(f"{nombre}: `uses: {ref}` sin ref (pinnar por SHA)")
            continue
        _, sha = ref.rsplit("@", 1)
        if MOVABLE_REF_RE.search(ref):
            fallos.append(f"{nombre}: `uses: {ref}` usa ref movible (pinnar por SHA)")
        elif not SHA_REF_RE.match(sha):
            fallos.append(f"{nombre}: `uses: {ref}` con ref no SHA de 40 hex")
    return fallos


def comprobar(workflows: Path = DEFAULT_WORKFLOWS) -> tuple[list[str], list[str]]:
    fallos: list[str] = []
    nombres: list[str] = []
    if not workflows.is_dir():
        return [f"Directorio de workflows no encontrado: {workflows}"], []
    for fichero in sorted(workflows.glob("*.yml")):
        nombres.append(fichero.name)
        fallos.extend(fallos_de_workflow(fichero.read_text(encoding="utf-8"), fichero.name))
    for requerido in REQUIRED_WORKFLOWS:
        if requerido not in nombres:
            fallos.append(f"Falta el workflow requerido: {requerido}")
    return fallos, nombres


def main() -> None:
    parser = argparse.ArgumentParser(description="Política de línea base de CI")
    parser.add_argument("--workflows", type=Path, default=DEFAULT_WORKFLOWS)
    parser.add_argument("--selftest", action="store_true")
    args = parser.parse_args()

    if args.selftest:
        selftest()
        return
    fallos, nombres = comprobar(args.workflows)
    print(f"Workflows revisados: {', '.join(nombres) or '-'}")
    if fallos:
        for f in fallos:
            print(f"::error::{f}")
        sys.exit(1)
    print("Política de CI OK")


def selftest() -> None:
    bueno = """\
name: ok
on: push
permissions:
  contents: read
jobs:
  a:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@11d5960a326750d5838078e36cf38b85af677262 # v4
"""
    malo = """\
name: mal
on: push
jobs:
  a:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@a26af69be951a213d495a4c3e4e4022e16d87065 # v5
      - uses: actions/cache@main
"""
    with tempfile.TemporaryDirectory() as td:
        td = Path(td)
        (td / "ok.yml").write_text(bueno, encoding="utf-8")
        (td / "mal.yml").write_text(malo, encoding="utf-8")
        fallos, _ = comprobar(td)
        # ok.yml pasa; mal.yml falla por permissions ausentes, @v4 y @main.
        assert not any("ok.yml" in f for f in fallos), fallos
        assert any("mal.yml: falta `permissions:`" in f for f in fallos), fallos
        assert any("mal.yml: `uses: actions/checkout@v4` usa ref movible" in f for f in fallos), fallos
        assert any("mal.yml: `uses: actions/cache@main` usa ref movible" in f for f in fallos), fallos
        # Workflow requerido ausente.
        (td / "ok.yml").unlink()
        fallos, _ = comprobar(td)
        assert any("Falta el workflow requerido: ci.yml" in f for f in fallos), fallos
    print("selftest OK")


if __name__ == "__main__":
    main()
