#!/usr/bin/env python3
"""Deriva los assets de marca del sitio MkDocs desde el escudo camarero.

Genera:
  - docs/assets/logo.png    (128x128, PNG lossless)
  - docs/assets/favicon.png (32x32,  PNG lossless)

a partir de app/src/main/res/drawable-nodpi/ic_brand_shield.webp (la marca
actual). El recorte usa el bbox del canal alpha con un umbral para ignorar el
halo de anti-aliasing, y centra el contenido en un lienzo cuadrado.

Uso:
    python scripts/generate_assets.py
"""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SHIELD = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi" / "ic_brand_shield.webp"
OUT_LOGO = ROOT / "docs" / "assets" / "logo.png"
OUT_FAVICON = ROOT / "docs" / "assets" / "favicon.png"

ALPHA_THRESHOLD = 16  # píxeles con alpha < umbral se tratan como transparentes


def trim_alpha(img: Image.Image) -> Image.Image:
    """Recorta al bbox del contenido visible (canal alpha con umbral)."""
    alpha = img.getchannel("A")
    # Mapa de píxeles "opacos" según el umbral; un píxel es contenido si alpha >= umbral.
    mask = alpha.point(lambda a: 255 if a >= ALPHA_THRESHOLD else 0)
    bbox = mask.getbbox()
    if bbox is None:
        return img  # sin contenido visible: devolver tal cual
    return img.crop(bbox)


def square_canvas(img: Image.Image) -> Image.Image:
    """Centra la imagen recortada en un lienzo cuadrado (lado = max dimension)."""
    w, h = img.size
    side = max(w, h)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(img, ((side - w) // 2, (side - h) // 2), img)
    return canvas


def main() -> None:
    if not SHIELD.exists():
        raise SystemExit(f"No se encuentra la marca: {SHIELD}")

    img = Image.open(SHIELD).convert("RGBA")
    trimmed = trim_alpha(img)
    squared = square_canvas(trimmed)

    OUT_LOGO.parent.mkdir(parents=True, exist_ok=True)

    squared.resize((128, 128), Image.LANCZOS).save(OUT_LOGO, "PNG")
    squared.resize((32, 32), Image.LANCZOS).save(OUT_FAVICON, "PNG")

    print(f"logo.png    -> {OUT_LOGO.relative_to(ROOT)} (128x128)")
    print(f"favicon.png -> {OUT_FAVICON.relative_to(ROOT)} (32x32)")


if __name__ == "__main__":
    main()
