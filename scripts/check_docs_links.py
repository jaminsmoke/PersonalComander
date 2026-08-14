#!/usr/bin/env python3
"""Comprueba referencias locales de Markdown y HTML en la documentación."""

from __future__ import annotations

import re
import sys
from pathlib import Path
from urllib.parse import unquote


ROOT = Path(__file__).resolve().parent.parent
MARKDOWN_LINK = re.compile(r"!?\[[^\]]*\]\(([^)]+)\)")
HTML_REFERENCE = re.compile(r"(?:src|href)=\"([^\"]+)\"")


def is_external(target: str) -> bool:
    return target.startswith(("http://", "https://", "mailto:", "//", "file:"))


def local_target(source: Path, raw_target: str) -> Path | None:
    target = raw_target.strip().strip("<>").split("#", 1)[0].split("?", 1)[0]
    if not target or is_external(target):
        return None
    return (source.parent / unquote(target)).resolve()


def references(source: Path) -> list[str]:
    text = source.read_text(encoding="utf-8")
    return MARKDOWN_LINK.findall(text) + HTML_REFERENCE.findall(text)


def main() -> int:
    files = [ROOT / "README.md", ROOT / "README.es.md"]
    files.extend(sorted((ROOT / "docs").rglob("*.md")))

    missing: list[tuple[Path, str, Path]] = []
    checked = 0
    for source in files:
        for raw_target in references(source):
            target = local_target(source, raw_target)
            if target is None:
                continue
            checked += 1
            if not target.exists():
                missing.append((source.relative_to(ROOT), raw_target, target))

    if missing:
        for source, raw_target, target in missing:
            print(f"MISSING {source}: {raw_target} -> {target}")
        return 1

    print(f"Documentation links/assets OK: {checked} local references checked")
    return 0


if __name__ == "__main__":
    sys.exit(main())
