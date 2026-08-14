#!/usr/bin/env python3
"""Comprueba que Commander no pide rutas que Identity o Bar ya no exponen
y deja un informe de aprovechamiento (summary de Actions / stdout).

Uso:
    python scripts/check_family_contracts.py \\
        --identity-openapi path/openapi-camareros.json \\
        --negocio-openapi path/openapi-negocio.json \\
        --bar-module path/BarLanModule.kt

    python scripts/check_family_contracts.py --selftest
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import tempfile
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
IDENTITY_SNAPSHOT = ROOT / "docs" / "identity-contract-paths.txt"
BAR_SNAPSHOT = ROOT / "docs" / "bar-contract-paths.txt"

KTOR_RUTA = re.compile(
    r"""(?:get|post|put|delete|patch|sse)\(\s*["']([^"']+)["']""",
    re.IGNORECASE,
)


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


def ktor_paths(fuente: str) -> set[str]:
    return set(KTOR_RUTA.findall(fuente))


def ruta_en_ktor(ruta: str, fuente: str) -> bool:
    return f'"{ruta}"' in fuente or f"'{ruta}'" in fuente


def es_interna(ruta: str) -> bool:
    return ruta.startswith("/internal")


def es_expo_bar(ruta: str) -> bool:
    base = ruta.rstrip("/")
    return base.endswith("/preparado") or base.endswith("/recogido")


def bullets(rutas: list[str], vacio: str = "_Ninguna._") -> str:
    if not rutas:
        return vacio
    return "\n".join(f"- `{r}`" for r in rutas)


@dataclass
class Informe:
    markdown: str
    fallos: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)


def comprobar(
    identity_openapi: Path,
    negocio_openapi: Path,
    bar_module: Path,
) -> Informe:
    identity_want = load_snapshot(IDENTITY_SNAPSHOT)
    identity_have = openapi_paths(identity_openapi)
    missing_id = sorted(identity_want - identity_have)
    used_id = sorted(identity_want & identity_have)
    unused_public = sorted(
        p for p in identity_have - identity_want if not es_interna(p)
    )
    internas = sorted(p for p in identity_have if es_interna(p))

    negocio_have = sorted(openapi_paths(negocio_openapi))

    bar_want = load_snapshot(BAR_SNAPSHOT)
    bar_src = bar_module.read_text(encoding="utf-8")
    bar_have = ktor_paths(bar_src)
    missing_bar = sorted(r for r in bar_want if not ruta_en_ktor(r, bar_src))
    used_bar = sorted(r for r in bar_want if ruta_en_ktor(r, bar_src))
    expo_bar = sorted(p for p in bar_have if es_expo_bar(p))
    otras_bar = sorted(
        p for p in bar_have if p not in bar_want and not es_expo_bar(p)
    )

    fallos: list[str] = []
    if missing_id:
        fallos.append(
            "Identity OpenAPI camareros no tiene rutas que Commander declara: "
            + ", ".join(missing_id),
        )
    if missing_bar:
        fallos.append(
            "BarLanModule.kt no declara rutas que Commander llama: "
            + ", ".join(missing_bar),
        )

    warnings = [
        f"Identity camareros pública no usada por Commander: {ruta}"
        for ruta in unused_public
    ]

    error_id = bullets(missing_id, "_Ninguno._")
    error_bar = bullets(missing_bar, "_Ninguno._")
    otras_md = ""
    if otras_bar:
        otras_md = (
            "\n### Otras rutas LAN no usadas por Commander\n\n"
            + bullets(otras_bar)
            + "\n"
        )

    markdown = f"""# Family contracts — informe

Rojo solo si Commander pide una ruta que Identity o Bar ya no exponen.
Lo no usado no falla el job: es señal para decidir ítem o deuda.

## Identity camareros (`:8080`)

### Usadas por Commander

{bullets(used_id)}

### Públicas no usadas

{bullets(unused_public)}

### Internas (no son deuda de Commander)

{bullets(internas)}

### Error

{error_id}

## Identity negocio (`:8082`)

Commander no llama a este servicio (oficio). No es deuda.

{bullets(negocio_have)}

## Bar LAN (`:8787`)

### Usadas por Commander

{bullets(used_bar)}

### Solo expo Bar

{bullets(expo_bar)}
{otras_md}
### Error

{error_bar}
"""
    return Informe(markdown=markdown.strip() + "\n", fallos=fallos, warnings=warnings)


def escribir_informe(informe: Informe) -> None:
    sys.stdout.write(informe.markdown)
    if not informe.markdown.endswith("\n"):
        sys.stdout.write("\n")
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as fh:
            fh.write(informe.markdown)
            if not informe.markdown.endswith("\n"):
                fh.write("\n")
    for msg in informe.warnings:
        print(f"::warning::{msg}")


def _fixtures_ok() -> tuple[dict, dict, str]:
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
            "/health": {},
            "/v1/keys/qr": {},
            "/v1/meta": {},
            "/internal/camareros/buscar": {},
        }
    }
    negocio_ok = {
        "paths": {
            "/v1/auth/negocio/login": {},
            "/v1/establecimientos": {},
        }
    }
    bar_ok = """
        get("/health") {}
        post("/v1/rondas") {}
        get("/v1/estado") {}
        sse("/v1/eventos") {}
        get("/v1/carta") {}
        post("/v1/tickets/{id}/preparado") {}
        post("/v1/tickets/{id}/recogido") {}
    """
    return identity_ok, negocio_ok, bar_ok


def selftest() -> int:
    identity_ok, negocio_ok, bar_ok = _fixtures_ok()
    with tempfile.TemporaryDirectory() as tmp:
        tmp_path = Path(tmp)
        openapi = tmp_path / "openapi.json"
        negocio = tmp_path / "negocio.json"
        modulo = tmp_path / "BarLanModule.kt"
        openapi.write_text(json.dumps(identity_ok), encoding="utf-8")
        negocio.write_text(json.dumps(negocio_ok), encoding="utf-8")
        modulo.write_text(bar_ok, encoding="utf-8")

        informe = comprobar(openapi, negocio, modulo)
        if informe.fallos:
            print("SELFTEST FAIL: fixtures completas deberían pasar", file=sys.stderr)
            return 1
        md = informe.markdown
        if "/v1/meta" not in md or "/v1/keys/qr" not in md:
            print("SELFTEST FAIL: el informe debía listar públicas no usadas", file=sys.stderr)
            return 1
        if "/internal/camareros/buscar" not in md:
            print("SELFTEST FAIL: el informe debía listar internas", file=sys.stderr)
            return 1
        if "Commander no llama" not in md or "/v1/auth/negocio/login" not in md:
            print("SELFTEST FAIL: el informe debía cubrir negocio (oficio)", file=sys.stderr)
            return 1
        if "/v1/tickets/{id}/preparado" not in md or "/v1/tickets/{id}/recogido" not in md:
            print("SELFTEST FAIL: el informe debía listar expo Bar", file=sys.stderr)
            return 1
        if not any("/v1/meta" in w for w in informe.warnings):
            print("SELFTEST FAIL: debía avisar de /v1/meta no usada", file=sys.stderr)
            return 1
        if any(es_interna(w.split(": ", 1)[-1]) for w in informe.warnings):
            print("SELFTEST FAIL: no debe avisar de rutas internas", file=sys.stderr)
            return 1
        if any("negocio" in w.lower() for w in informe.warnings):
            print("SELFTEST FAIL: no debe avisar de negocio", file=sys.stderr)
            return 1

        broken = dict(identity_ok)
        broken["paths"] = dict(identity_ok["paths"])
        del broken["paths"]["/v1/auth/login"]
        openapi.write_text(json.dumps(broken), encoding="utf-8")
        fallos = comprobar(openapi, negocio, modulo).fallos
        if not any("login" in f for f in fallos):
            print("SELFTEST FAIL: debía detectar /v1/auth/login ausente", file=sys.stderr)
            return 1

        openapi.write_text(json.dumps(identity_ok), encoding="utf-8")
        modulo.write_text('get("/health") {}', encoding="utf-8")
        fallos = comprobar(openapi, negocio, modulo).fallos
        if not any("rondas" in f for f in fallos):
            print("SELFTEST FAIL: debía detectar /v1/rondas ausente", file=sys.stderr)
            return 1
    print("Family contracts selftest OK")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--identity-openapi", type=Path)
    parser.add_argument("--negocio-openapi", type=Path)
    parser.add_argument("--bar-module", type=Path)
    parser.add_argument("--selftest", action="store_true")
    args = parser.parse_args()
    if args.selftest:
        return selftest()
    if (
        args.identity_openapi is None
        or args.negocio_openapi is None
        or args.bar_module is None
    ):
        parser.error(
            "hace falta --identity-openapi, --negocio-openapi y --bar-module (o --selftest)",
        )
    if not IDENTITY_SNAPSHOT.is_file() or not BAR_SNAPSHOT.is_file():
        print(
            "Faltan docs/identity-contract-paths.txt o docs/bar-contract-paths.txt",
            file=sys.stderr,
        )
        return 1
    informe = comprobar(args.identity_openapi, args.negocio_openapi, args.bar_module)
    escribir_informe(informe)
    if informe.fallos:
        for f in informe.fallos:
            print(f, file=sys.stderr)
        return 1
    print(
        f"Family contracts OK: {len(load_snapshot(IDENTITY_SNAPSHOT))} Identity, "
        f"{len(load_snapshot(BAR_SNAPSHOT))} Bar",
        file=sys.stderr,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
