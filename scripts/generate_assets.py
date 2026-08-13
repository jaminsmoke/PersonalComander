#!/usr/bin/env python3
"""Deriva los assets de marca del sitio MkDocs desde el escudo camarero.

Genera:
  - docs/assets/logo.png     (128x128, PNG lossless)
  - docs/assets/favicon.png  (32x32,  PNG lossless)
  - docs/assets/og-image.png (1200x630, social card con fondo sólido)

a partir de app/src/main/res/drawable-nodpi/ic_brand_shield.webp (la marca
actual). El recorte usa el bbox del canal alpha con un umbral para ignorar el
halo de anti-aliasing, y centra el contenido en un lienzo cuadrado.

Uso:
    python scripts/generate_assets.py
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
SHIELD = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi" / "ic_brand_shield.webp"
OUT_LOGO = ROOT / "docs" / "assets" / "logo.png"
OUT_FAVICON = ROOT / "docs" / "assets" / "favicon.png"
OUT_OG = ROOT / "docs" / "assets" / "og-image.png"

ALPHA_THRESHOLD = 16  # píxeles con alpha < umbral se tratan como transparentes

# Colores de marca (theme/Color.kt)
NAVY = (0x0B, 0x0F, 0x10)     # PcSurfaceContainerLowest
GOLD = (0xE9, 0xC3, 0x49)     # PcSecondary
LIGHT = (0xE0, 0xE3, 0xE5)    # PcOnSurface

FONT_CANDIDATES = [
    "C:/Windows/Fonts/seguisb.ttf",   # Segoe UI Semibold
    "C:/Windows/Fonts/segoeui.ttf",   # Segoe UI
    "C:/Windows/Fonts/arial.ttf",     # Arial
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
]


def trim_alpha(img: Image.Image) -> Image.Image:
    """Recorta al bbox del contenido visible (canal alpha con umbral)."""
    alpha = img.getchannel("A")
    mask = alpha.point(lambda a: 255 if a >= ALPHA_THRESHOLD else 0)
    bbox = mask.getbbox()
    if bbox is None:
        return img
    return img.crop(bbox)


def square_canvas(img: Image.Image) -> Image.Image:
    """Centra la imagen recortada en un lienzo cuadrado (lado = max dimension)."""
    w, h = img.size
    side = max(w, h)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(img, ((side - w) // 2, (side - h) // 2), img)
    return canvas


def find_font(size: int):
    """Devuelve una ImageFont o None si no hay fuente disponible."""
    for path in FONT_CANDIDATES:
        if Path(path).exists():
            try:
                return ImageFont.truetype(path, size)
            except OSError:
                continue
    return None


def generate_og_image(shield_square: Image.Image) -> None:
    """Genera la social card 1200x630 con fondo sólido navy + escudo + wordmark."""
    W, H = 1200, 630
    canvas = Image.new("RGBA", (W, H), NAVY + (255,))
    draw = ImageDraw.Draw(canvas)

    shield_size = 340
    shield = shield_square.resize((shield_size, shield_size), Image.LANCZOS)
    shield_x = 90
    shield_y = (H - shield_size) // 2
    canvas.paste(shield, (shield_x, shield_y), shield)

    wordmark = "Personal Comander"
    tagline = "Gestión de mesas y comandas para restaurantes"

    text_x = shield_x + shield_size + 70

    title_font = find_font(72)
    tag_font = find_font(32)

    if title_font is not None:
        # bloque de texto centrado verticalmente junto al escudo
        title_bbox = draw.textbbox((0, 0), wordmark, font=title_font)
        title_h = title_bbox[3] - title_bbox[1]
        tag_bbox = draw.textbbox((0, 0), tagline, font=tag_font) if tag_font else (0, 0, 0, 0)
        tag_h = tag_bbox[3] - tag_bbox[1]
        gap = 24
        block_h = title_h + gap + tag_h
        y = (H - block_h) // 2

        draw.text((text_x, y), wordmark, font=title_font, fill=GOLD + (255,))
        if tag_font is not None:
            draw.text((text_x, y + title_h + gap), tagline, font=tag_font, fill=LIGHT + (255,))

    # banda inferior gold sutil
    draw.rectangle([0, H - 8, W, H], fill=GOLD + (255,))

    OUT_OG.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert("RGB").save(OUT_OG, "PNG", optimize=True)
    print(f"og-image.png -> {OUT_OG.relative_to(ROOT)} (1200x630)")


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

    generate_og_image(squared)


if __name__ == "__main__":
    main()
