#!/usr/bin/env bash
# Captura las pantallas principales de la app desde un emulador y las guarda
# en docs/screenshots/{home,mesas_board,menu,comanda,ajustes}.png normalizadas a 540px.
#
# Requisitos:
#   - Un emulador con la app instalada (o se instala aquí con installDebug).
#   - adb del Android SDK accesible (busca en $ANDROID_HOME / $ANDROID_SDK_ROOT).
#
# Uso:
#   bash scripts/capture_screens.sh
#   ADB_DEVICE=emulator-5556 bash scripts/capture_screens.sh
#   SKIP_INSTALL=1 ADB_DEVICE=emulator-5556 bash scripts/capture_screens.sh
#
# Variables opcionales:
#   ADB_DEVICE  serial concreto; recomendable si hay teléfono y tablet activos.
#   SKIP_INSTALL=1  reutiliza una instalación existente.
#   PYTHON_BIN  intérprete con Pillow (python o python3 por defecto).
set -euo pipefail

# En git bash de Windows, las rutas de dispositivo (/sdcard/...) no deben
# convertirse a rutas Windows, pero las rutas locales que reciben adb.exe o
# python.exe (binarios nativos) sí. Se convierte explícitamente con cygpath.
export MSYS_NO_PATHCONV=1

PKG="com.jaminsmoke.personalcomander"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/docs/screenshots"
TMP="$ROOT/devartifacts"
WIDTH=540
PYTHON_BIN="${PYTHON_BIN:-python}"
if ! command -v "$PYTHON_BIN" >/dev/null 2>&1; then
  PYTHON_BIN=python3
fi

win() {
  if command -v cygpath >/dev/null 2>&1; then
    cygpath -m "$1"
  elif command -v wslpath >/dev/null 2>&1; then
    wslpath -w "$1" | tr -d '\r'
  else
    echo "$1"
  fi
}

# --- localizar adb -----------------------------------------------------------
find_adb() {
  local c
  for c in \
    "${ANDROID_HOME:-}/platform-tools/adb" \
    "${ANDROID_HOME:-}/platform-tools/adb.exe" \
    "${ANDROID_SDK_ROOT:-}/platform-tools/adb" \
    "${ANDROID_SDK_ROOT:-}/platform-tools/adb.exe" \
    "${LOCALAPPDATA:-}/Android/Sdk/platform-tools/adb.exe" \
    "$HOME/AppData/Local/Android/Sdk/platform-tools/adb.exe" \
    "$HOME/Android/Sdk/platform-tools/adb"; do
    [[ -n "$c" && -f "$c" ]] && { echo "$c"; return 0; }
  done
  return 1
}

ADB="$(find_adb || true)"
if [[ -z "$ADB" ]]; then
  echo "ERROR: no se encontró adb. Define ANDROID_HOME." >&2
  exit 1
fi
echo "adb: $ADB"

if [[ -n "${ADB_DEVICE:-}" ]]; then
  DEVICE="$ADB_DEVICE"
else
  DEVICE="$($ADB devices | tr -d '\r' | awk 'NR>1 && $2=="device"{print $1; exit}')"
fi
if [[ -z "$DEVICE" ]]; then
  echo "ERROR: no hay emulador/dispositivo activo." >&2
  exit 1
fi
echo "dispositivo: $DEVICE"

adb() { "$ADB" -s "$DEVICE" "$@"; }

# Altura de pantalla (para descartar la bottom bar al localizar mesas).
SCREEN_H="$(adb shell wm size | sed -n 's/.*: \([0-9]*\)x\([0-9]*\)/\2/p' | head -1)"
SCREEN_H="${SCREEN_H:-2424}"

mkdir -p "$OUT" "$TMP"

# --- instalar y arrancar con seed limpio --------------------------------------
echo "==> Instalando app (installDebug)..."
if [[ "${SKIP_INSTALL:-0}" == "1" ]]; then
  echo "  instalación omitida (SKIP_INSTALL=1)"
else
  (cd "$ROOT" && ./gradlew installDebug >/dev/null)
fi

echo "==> Limpiando datos para seed y arrancando..."
adb shell pm clear "$PKG" >/dev/null || true
adb shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 6

# --- helpers ------------------------------------------------------------------
# Vuelca la jerarquía UI y devuelve el centro (x y) del primer nodo cuyo text
# contiene needle. Devuelve éxito si lo encuentra.
find_tap() {
  local needle="$1"
  local xml="$TMP/ui.xml"
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$(win "$xml")" >/dev/null 2>&1
  "$PYTHON_BIN" - "$(win "$xml")" "$needle" <<'PY'
import re, sys
xml_path, needle = sys.argv[1], sys.argv[2]
xml = open(xml_path, encoding='utf-8').read()
nodes = []
for m in re.finditer(r'<node[^>]*?text="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    nodes.append((m.group(1), m.group(2), m.group(3), m.group(4), m.group(5)))
# 1) coincidencia exacta (evita ambigüedad "Gestión" vs "Gestión del menú")
for text, x1, y1, x2, y2 in nodes:
    if text.strip() == needle:
        print((int(x1) + int(x2)) // 2, (int(y1) + int(y2)) // 2)
        sys.exit(0)
# 2) substring como fallback
for text, x1, y1, x2, y2 in nodes:
    if needle in text:
        print((int(x1) + int(x2)) // 2, (int(y1) + int(y2)) // 2)
        sys.exit(0)
sys.exit(1)
PY
}

tap_text() {
  local needle="$1"
  local bounds
  if bounds="$(find_tap "$needle")"; then
    adb shell input tap $bounds >/dev/null
    sleep 2
    return 0
  fi
  echo "  ! no se encontró el nodo '$needle'; se omite" >&2
  return 1
}

tap_text_any() {
  local needle
  for needle in "$@"; do
    if tap_text "$needle"; then
      return 0
    fi
  done
  return 1
}

capture() {
  local name="$1"
  local dev="/sdcard/raw_$name.png"
  adb shell screencap -p "$dev" >/dev/null || { echo "  ! error capturando $name" >&2; return 1; }
  adb pull "$dev" "$(win "$TMP/raw_$name.png")" >/dev/null 2>&1 || { echo "  ! error descargando $name" >&2; return 1; }
  "$PYTHON_BIN" - "$(win "$TMP/raw_$name.png")" "$(win "$OUT/$name.png")" "$WIDTH" <<'PY'
import sys
from PIL import Image
src, dst, width = sys.argv[1], sys.argv[2], int(sys.argv[3])
im = Image.open(src).convert("RGBA")
if im.width != width:
    h = round(im.height * width / im.width)
    im = im.resize((width, h), Image.LANCZOS)
im.save(dst, "PNG")
PY
  rm -f "$TMP/raw_$name.png"
  echo "  ✓ $name.png"
}

echo "==> Capturando pantallas..."
capture home

echo "  navegando a Mesas..."
tap_text_any "Mesas" "Tables" && capture mesas_board

echo "  navegando a Gestión (hub → carta)..."
tap_text_any "Gestión" "Management" "Menu" && {
  tap_text_any "Carta" "Menu" || true
  capture menu
}

echo "  navegando a Mesas y abriendo una comanda..."
tap_text_any "Mesas" "Tables" && {
  xml="$TMP/ui.xml"
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$(win "$xml")" >/dev/null 2>&1
  MESA_BOUNDS="$("$PYTHON_BIN" - "$(win "$xml")" "$SCREEN_H" <<'PY'
import re, sys
xml_path, screen_h = sys.argv[1], int(sys.argv[2])
xml = open(xml_path, encoding='utf-8').read()
for m in re.finditer(r'<node[^>]*?text="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    text = m.group(1).strip()
    x1, y1, x2, y2 = map(int, m.group(2, 3, 4, 5))
    # mesas con texto tipo "B1" / "M1" / "12"; descartar la bottom bar (parte inferior)
    if re.match(r'^[A-Za-z]?\d{1,3}$', text) and y2 < screen_h * 0.8:
        print((x1 + x2) // 2, (y1 + y2) // 2)
        sys.exit(0)
sys.exit(1)
PY
)" || MESA_BOUNDS=""
  if [[ -n "$MESA_BOUNDS" ]]; then
    adb shell input tap $MESA_BOUNDS >/dev/null
    sleep 3
    capture comanda
  else
    echo "  ! no se localizó una mesa; captura de comanda omitida" >&2
  fi
}

echo "  navegando a Ajustes..."
adb shell input keyevent 4 >/dev/null 2>&1 || true
sleep 2
tap_text_any "Ajustes" "Settings" && capture ajustes

echo "==> Listo. Capturas en $OUT/"
