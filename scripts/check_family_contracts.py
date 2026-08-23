#!/usr/bin/env python3
"""Comprueba que Commander no pide rutas que Identity o Bar ya no exponen
y deja un informe de aprovechamiento (summary de Actions / stdout).

Además de método+path, verifica la autenticación (Authorization) de cada
ruta que Commander consume contra el contrato del proveedor:
- rutas que Commander llama SIN token deben ser públicas en el OpenAPI;
- rutas con Bearer/sesión deben declarar security en el proveedor
  (si el proveedor las declara públicas pero Commander manda token → warning).

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
BAR_SNAPSHOT = ROOT / "docs" / "bar-contract-paths.txt"

# Fuente de verdad de las rutas Identity que Commander consume: el propio
# cliente Kotlin (IdentityCliente.Rutas.*). No hay snapshot separado.
IDENTITY_KOTLIN = (
    ROOT
    / "app"
    / "src"
    / "main"
    / "java"
    / "com"
    / "jaminsmoke"
    / "personalcomander"
    / "data"
    / "sesion"
    / "IdentityCliente.kt"
)
RUTAS_IDENTITY_RE = re.compile(r'"((?:/v1)/[^"]+)"')

KTOR_RUTA = re.compile(
    r"""(?:get|post|put|delete|patch|sse)\(\s*["']([^"']+)["']""",
    re.IGNORECASE,
)

HTTP_VERBOS = frozenset({"get", "post", "put", "delete", "patch", "options", "head", "trace"})

# Método HTTP con el que Commander llama a cada ruta LAN (deriva de BarLanCliente).
# `/v1/eventos` es SSE: en el contrato de Bar se documenta como GET text/event-stream.
METODOS_BAR = {
    "/health": "get",
    "/v1/rondas": "post",
    "/v1/sesion": "post",
    "/v1/sesion/iniciar": "post",
    "/v1/sesion/cortar": "post",
    "/v1/heartbeat": "post",
    "/v1/estado": "get",
    "/v1/eventos": "get",
    "/v1/carta": "get",
}

# Rutas que Commander llama SIN token de sesión (Bearer LAN/Identity).
SIN_TOKEN_IDENTITY = frozenset({
    "/v1/camareros/registro",
    "/v1/auth/login",
    "/v1/auth/refresh",
})
SIN_TOKEN_BAR = frozenset({
    "/health",
    "/v1/sesion",
})


def load_snapshot(path: Path) -> set[str]:
    rutas: set[str] = set()
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if line and not line.startswith("#"):
            rutas.add(line)
    return rutas


def rutas_identity_fuente() -> set[str]:
    """Rutas del contrato Identity que Commander consume, derivadas del cliente."""
    if not IDENTITY_KOTLIN.is_file():
        raise FileNotFoundError(f"No se encuentra IdentityCliente.kt: {IDENTITY_KOTLIN}")
    return set(RUTAS_IDENTITY_RE.findall(IDENTITY_KOTLIN.read_text(encoding="utf-8")))


def openapi_paths(path: Path) -> set[str]:
    spec = json.loads(path.read_text(encoding="utf-8"))
    paths = spec.get("paths")
    if not isinstance(paths, dict):
        raise ValueError(f"{path} no tiene objeto paths")
    return set(paths)


def openapi_ops(path: Path) -> dict[str, set[str]]:
    """path -> verbos declarados (minúsculas) del spec OpenAPI."""
    spec = json.loads(path.read_text(encoding="utf-8"))
    paths = spec.get("paths")
    if not isinstance(paths, dict):
        raise ValueError(f"{path} no tiene objeto paths")
    out: dict[str, set[str]] = {}
    for raw, item in paths.items():
        if not isinstance(item, dict):
            continue
        out[raw] = {k.lower() for k in item if k.lower() in HTTP_VERBOS}
    return out


def security_de_operacion(spec: dict, ruta: str, metodo: str | None = None) -> list | None:
    """security efectiva de una operación (la operación manda sobre la global)."""
    item = spec.get("paths", {}).get(ruta)
    if not isinstance(item, dict):
        return None
    verbos = [k for k in item if k.lower() in HTTP_VERBOS]
    op = None
    if verbos:
        if metodo and metodo in [v.lower() for v in verbos]:
            op = item[next(v for v in verbos if v.lower() == metodo)]
        else:
            op = item[verbos[0]]
    if op is None:
        return None
    if "security" in op:
        return op["security"] or []
    return spec.get("security") or []


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


def bullets_auth(items: list[str]) -> str:
    if not items:
        return "_Ninguno._"
    return "\n".join(f"- {it}" for it in items)


@dataclass
class Informe:
    markdown: str
    fallos: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)


def comprobar(
    identity_openapi: Path,
    negocio_openapi: Path,
    bar_module: Path,
    bar_openapi: Path | None = None,
) -> Informe:
    identity_want = rutas_identity_fuente()
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

    if bar_openapi is not None:
        # Contrato estructural de Bar (openapi-lan.json): ruta + método.
        bar_ops = openapi_ops(bar_openapi)
        for ruta, metodo in sorted(METODOS_BAR.items()):
            verbos = bar_ops.get(ruta)
            if verbos is None:
                fallos.append(
                    f"openapi-lan.json de Bar no documenta {ruta} que Commander llama"
                )
            elif metodo not in verbos:
                fallos.append(
                    f"openapi-lan.json de Bar no declara {metodo.upper()} {ruta} "
                    f"(solo {', '.join(sorted(v.upper() for v in verbos))})"
                )

    auth_id: list[str] = []
    try:
        identity_spec = json.loads(identity_openapi.read_text(encoding="utf-8"))
    except Exception:
        identity_spec = {}
    for ruta in sorted(identity_want & identity_have):
        security = security_de_operacion(identity_spec, ruta)
        if ruta in SIN_TOKEN_IDENTITY:
            if security:
                fallos.append(
                    f"Commander llama a {ruta} sin token pero Identity la declara "
                    f"protegida (security: {security})"
                )
        elif security == []:
            auth_id.append(
                f"Identity declara {ruta} pública pero Commander le envía Bearer"
            )

    auth_bar: list[str] = []
    if bar_openapi is not None:
        try:
            bar_spec = json.loads(bar_openapi.read_text(encoding="utf-8"))
        except Exception:
            bar_spec = {}
        for ruta, metodo in METODOS_BAR.items():
            if ruta not in (bar_spec.get("paths") or {}):
                continue
            security = security_de_operacion(bar_spec, ruta, metodo)
            if ruta in SIN_TOKEN_BAR:
                if security:
                    fallos.append(
                        f"Commander llama a {ruta} sin token LAN pero Bar la declara "
                        f"protegida (security: {security})"
                    )
            elif not security:
                auth_bar.append(
                    f"Bar declara {ruta} sin token pero Commander le envía Bearer LAN"
                )

    warnings_list = [
        f"Identity camareros pública no usada por Commander: {ruta}"
        for ruta in unused_public
    ]
    warnings_list += auth_id + auth_bar

    error_id = bullets(missing_id, "_Ninguno._")
    error_bar = bullets(missing_bar, "_Ninguno._")
    auth_id_md = bullets_auth(auth_id)
    auth_bar_md = bullets_auth(auth_bar)
    otras_md = ""
    if otras_bar:
        otras_md = (
            "\n### Otras rutas LAN no usadas por Commander\n\n"
            + bullets(otras_bar)
            + "\n"
        )

    markdown = f"""# Family contracts — informe

Rojo solo si Commander pide una ruta que Identity o Bar ya no exponen,
o si una ruta que Commander usa sin token pasa a estar protegida.

## Identity camareros (`:8080`)

### Usadas por Commander

{bullets(used_id)}

### Públicas no usadas

{bullets(unused_public)}

### Internas (no son deuda de Commander)

{bullets(internas)}

### Autenticación (Bearer)

{auth_id_md}

### Error

{error_id}

## Identity negocio (`:8082`)

Commander no llama a este servicio (oficio). No es deuda.

{bullets(negocio_have)}

## Bar LAN (`:8787`)

### Usadas por Commander

{bullets(used_bar)}

### Autorización (Bearer LAN)

{auth_bar_md}

### Solo expo Bar

{bullets(expo_bar)}
{otras_md}
### Error

{error_bar}
"""
    return Informe(markdown=markdown.strip() + "\n", fallos=fallos, warnings=warnings_list)


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
            "/v1/auth/login": {"post": {}},
            "/v1/auth/refresh": {"post": {}},
            "/v1/camareros/me": {"get": {}},
            "/v1/camareros/me/establecimientos": {"get": {}},
            "/v1/camareros/me/foto": {"get": {}},
            "/v1/camareros/me/qr": {"get": {}},
            "/v1/camareros/me/renovar": {"post": {}},
            "/v1/camareros/me/revocar": {"post": {}},
            "/v1/camareros/me/visibilidad": {"get": {}},
            "/v1/camareros/me/visibilidad-establecimientos": {"put": {}},
            "/v1/camareros/me/password": {"post": {}},
            "/v1/camareros/me/jornadas": {"get": {}},
            "/v1/camareros/me/jornadas/iniciar": {"post": {}},
            "/v1/camareros/me/jornadas/cortar": {"post": {}},
            "/v1/camareros/me/resumen": {"get": {}},
            "/v1/camareros/me/invitaciones": {"get": {}},
            "/v1/camareros/me/invitaciones/{invitacion_id}/aceptar": {"post": {}},
            "/v1/camareros/me/invitaciones/{invitacion_id}/rechazar": {"post": {}},
            "/v1/camareros/registro": {"post": {}},
            "/health": {"get": {}},
            "/v1/keys/qr": {"get": {}},
            "/v1/meta": {"get": {}},
            "/internal/camareros/buscar": {"post": {}},
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
        post("/v1/sesion") {}
        post("/v1/sesion/iniciar") {}
        post("/v1/sesion/cortar") {}
        post("/v1/heartbeat") {}
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

        # Identity con security global: login/refresh/registro públicas, resto Bearer.
        con_sec = json.loads(json.dumps(identity_ok))
        con_sec["security"] = [{"HTTPBearer": []}]
        for p in ("/v1/auth/login", "/v1/auth/refresh", "/v1/camareros/registro"):
            op = list(con_sec["paths"][p].keys())[0]
            con_sec["paths"][p] = {op: {"security": []}}
        openapi.write_text(json.dumps(con_sec), encoding="utf-8")
        informe = comprobar(openapi, negocio, modulo)
        if informe.fallos:
            print("SELFTEST FAIL: auth de Identity correcta debería pasar", file=sys.stderr)
            return 1

        # Login como protegida (Commander no le envía token) → rojo.
        broken_auth = json.loads(json.dumps(con_sec))
        broken_auth["paths"]["/v1/auth/login"] = {"post": {"security": [{"HTTPBearer": []}]}}
        openapi.write_text(json.dumps(broken_auth), encoding="utf-8")
        fallos = comprobar(openapi, negocio, modulo).fallos
        if not any("login" in f and "sin token" in f for f in fallos):
            print("SELFTEST FAIL: debía detectar login de Identity protegido sin token", file=sys.stderr)
            return 1

        # /v1/camareros/me pública (sin security) → warning, no rojo.
        pub_auth = json.loads(json.dumps(con_sec))
        pub_auth["paths"]["/v1/camareros/me"] = {"get": {"security": []}}
        openapi.write_text(json.dumps(pub_auth), encoding="utf-8")
        r = comprobar(openapi, negocio, modulo)
        if r.fallos or not any("camareros/me" in w and "Bearer" in w for w in r.warnings):
            print("SELFTEST FAIL: debía avisar de /v1/camareros/me pública con Bearer", file=sys.stderr)
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

        # Método+path contra openapi-lan.json.
        modulo.write_text(bar_ok, encoding="utf-8")
        bar_spec = {
            "paths": {
                "/health": {"get": {}},
                "/v1/rondas": {"post": {}},
                "/v1/sesion": {"post": {}},
                "/v1/sesion/iniciar": {"post": {}},
                "/v1/sesion/cortar": {"post": {}},
                "/v1/heartbeat": {"post": {}},
                "/v1/estado": {"get": {}},
                "/v1/eventos": {"get": {}},
                "/v1/carta": {"get": {}},
            }
        }
        bar_spec_path = tmp_path / "openapi-lan.json"
        bar_spec_path.write_text(json.dumps(bar_spec), encoding="utf-8")
        fallos = comprobar(openapi, negocio, modulo, bar_openapi=bar_spec_path).fallos
        if fallos:
            print("SELFTEST FAIL: openapi-lan correcto debería pasar", file=sys.stderr)
            print("\n".join(fallos), file=sys.stderr)
            return 1

        # Bar con security global: /health y /v1/sesion públicas → sin fallos.
        bar_sec = json.loads(json.dumps(bar_spec))
        bar_sec["security"] = [{"sesionLan": []}]
        bar_sec["paths"]["/v1/sesion"] = {"post": {"security": []}}
        bar_sec["paths"]["/health"] = {"get": {"security": []}}
        bar_spec_path.write_text(json.dumps(bar_sec), encoding="utf-8")
        fallos = comprobar(openapi, negocio, modulo, bar_openapi=bar_spec_path).fallos
        if fallos:
            print("SELFTEST FAIL: auth de Bar correcta debería pasar", file=sys.stderr)
            return 1

        # /health como protegida (Commander no le manda token) → rojo.
        bar_priv = json.loads(json.dumps(bar_sec))
        bar_priv["paths"]["/health"] = {"get": {"security": [{"sesionLan": []}]}}
        bar_spec_path.write_text(json.dumps(bar_priv), encoding="utf-8")
        fallos = comprobar(openapi, negocio, modulo, bar_openapi=bar_spec_path).fallos
        if not any("health" in f and "sin token" in f for f in fallos):
            print("SELFTEST FAIL: debía detectar /health de Bar protegido sin token", file=sys.stderr)
            return 1

        # Ruta que Commander llama y openapi-lan no documenta → ROJO.
        bar_missing = json.loads(json.dumps(bar_sec))
        bar_missing["paths"].pop("/v1/carta")
        bar_spec_path.write_text(json.dumps(bar_missing), encoding="utf-8")
        fallos = comprobar(openapi, negocio, modulo, bar_openapi=bar_spec_path).fallos
        if not any("no documenta /v1/carta" in f for f in fallos):
            print("SELFTEST FAIL: debía detectar /v1/carta ausente en openapi-lan", file=sys.stderr)
            return 1

        # Método incorrecto (GET /v1/rondas en vez de POST) → ROJO.
        bar_bad = json.loads(json.dumps(bar_sec))
        bar_bad["paths"]["/v1/rondas"] = {"get": {}}
        bar_spec_path.write_text(json.dumps(bar_bad), encoding="utf-8")
        fallos = comprobar(openapi, negocio, modulo, bar_openapi=bar_spec_path).fallos
        if not any("no declara POST /v1/rondas" in f for f in fallos):
            print("SELFTEST FAIL: debía detectar método incorrecto", file=sys.stderr)
            print("\n".join(fallos), file=sys.stderr)
            return 1
    print("Family contracts selftest OK")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--identity-openapi", type=Path)
    parser.add_argument("--negocio-openapi", type=Path)
    parser.add_argument("--bar-module", type=Path)
    parser.add_argument("--bar-openapi", type=Path)
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
    if not BAR_SNAPSHOT.is_file():
        print("Falta docs/bar-contract-paths.txt", file=sys.stderr)
        return 1
    informe = comprobar(
        args.identity_openapi,
        args.negocio_openapi,
        args.bar_module,
        bar_openapi=args.bar_openapi,
    )
    escribir_informe(informe)
    if informe.fallos:
        for f in informe.fallos:
            print(f, file=sys.stderr)
        return 1
    print(
        f"Family contracts OK: {len(rutas_identity_fuente())} Identity "
        f"(del código), {len(load_snapshot(BAR_SNAPSHOT))} Bar",
        file=sys.stderr,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())