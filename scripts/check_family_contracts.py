#!/usr/bin/env python3
"""Comprueba que Commander no pide rutas que Identity o Bar ya no exponen.

Uso:
    python scripts/check_family_contracts.py \\
        --identity-openapi path/openapi-camareros.json \\
        --bar-module path/BarLanModule.kt

    python scripts/check_family_contracts.py --selftest
"""

from __future__ import annotations

import argparse
import json
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
IDENTITY_SNAPSHOT = ROOT / "docs" / "identity-contract-paths.txt"
BAR_SNAPSHOT = ROOT / "docs" / "bar-contract-paths.txt"


def load_snapshot(path: Path) -> set[str]:
    rutas: set[str] = set()
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if line and not line.startswith("#"):
            rutas.add(line)
    return rutas


def openapi_paths(path: Path) -> set[str]:
    spec = json.loads(path.read_text(encoding="utf-8"))
    paths = spec.get("paths")
    if not isinstance(paths, dict):
        raise ValueError(f"{path} no tiene objeto paths")
    return set(paths)


def ruta_en_ktor(ruta: str, fuente: str) -> bool:
    return f'"{ruta}"' in fuente or f"'{ruta}'" in fuente


def comprobar(identity_openapi: Path, bar_module: Path) -> list[str]:
    fallos: list[str] = []
    identity_want = load_snapshot(IDENTITY_SNAPSHOT)
    identity_have = openapi_paths(identity_openapi)
    missing_id = sorted(identity_want - identity_have)
    if missing_id:
        fallos.append(
            "Identity OpenAPI camareros no tiene rutas que Commander declara: "
            + ", ".join(missing_id),
        )

    bar_want = load_snapshot(BAR_SNAPSHOT)
    bar_src = bar_module.read_text(encoding="utf-8")
    missing_bar = sorted(r for r in bar_want if not ruta_en_ktor(r, bar_src))
    if missing_bar:
        fallos.append(
            "BarLanModule.kt no declara rutas que Commander llama: "
            + ", ".join(missing_bar),
        )
    return fallos


def selftest() -> int:
    identity_ok = {
        "paths": {
            "/v1/auth/login": {},
            "/v1/camareros/me": {},
            "/v1/camareros/me/establecimientos": {},
            "/v1/camareros/me/foto": {},
            "/v1/camareros/me/qr": {},
            "/v1/camareros/me/renovar": {},
            "/v1/camareros/me/revocar": {},
            "/v1/camareros/registro": {},
        }
    }
    bar_ok = """
        get("/health") {}
        post("/v1/rondas") {}
        get("/v1/estado") {}
        sse("/v1/eventos") {}
    """
    with tempfile.TemporaryDirectory() as tmp:
        tmp_path = Path(tmp)
        openapi = tmp_path / "openapi.json"
        modulo = tmp_path / "BarLanModule.kt"
        openapi.write_text(json.dumps(identity_ok), encoding="utf-8")
        modulo.write_text(bar_ok, encoding="utf-8")
        if comprobar(openapi, modulo):
            print("SELFTEST FAIL: fixtures completas deberían pasar", file=sys.stderr)
            return 1

        broken = dict(identity_ok)
        broken["paths"] = dict(identity_ok["paths"])
        del broken["paths"]["/v1/auth/login"]
        openapi.write_text(json.dumps(broken), encoding="utf-8")
        fallos = comprobar(openapi, modulo)
        if not any("login" in f for f in fallos):
            print("SELFTEST FAIL: debía detectar /v1/auth/login ausente", file=sys.stderr)
            return 1

        openapi.write_text(json.dumps(identity_ok), encoding="utf-8")
        modulo.write_text('get("/health") {}', encoding="utf-8")
        fallos = comprobar(openapi, modulo)
        if not any("rondas" in f for f in fallos):
            print("SELFTEST FAIL: debía detectar /v1/rondas ausente", file=sys.stderr)
            return 1
    print("Family contracts selftest OK")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--identity-openapi", type=Path)
    parser.add_argument("--bar-module", type=Path)
    parser.add_argument("--selftest", action="store_true")
    args = parser.parse_args()
    if args.selftest:
        return selftest()
    if args.identity_openapi is None or args.bar_module is None:
        parser.error("hace falta --identity-openapi y --bar-module (o --selftest)")
    if not IDENTITY_SNAPSHOT.is_file() or not BAR_SNAPSHOT.is_file():
        print("Faltan docs/identity-contract-paths.txt o docs/bar-contract-paths.txt", file=sys.stderr)
        return 1
    fallos = comprobar(args.identity_openapi, args.bar_module)
    if fallos:
        for f in fallos:
            print(f, file=sys.stderr)
        return 1
    print(
        f"Family contracts OK: {len(load_snapshot(IDENTITY_SNAPSHOT))} Identity, "
        f"{len(load_snapshot(BAR_SNAPSHOT))} Bar",
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
