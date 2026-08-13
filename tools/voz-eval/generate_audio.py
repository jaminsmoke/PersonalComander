#!/usr/bin/env python3
"""Genera WAV sintéticos (TTS + ruido) para el eval de voz.

Lee app/src/test/resources/voz-eval/gold.json y escribe en
devartifacts/voz-eval/audio/ y un manifiesto clips.json.

Requisitos: pip install edge-tts numpy imageio-ffmpeg
"""
from __future__ import annotations

import argparse
import asyncio
import json
import subprocess
import sys
import wave
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[2]
GOLD = ROOT / "app/src/test/resources/voz-eval/gold.json"
OUT = ROOT / "devartifacts/voz-eval"

VOICES = {
    "es-ES": "es-ES-ElviraNeural",
    "es-MX": "es-MX-DaliaNeural",
    "es-AR": "es-AR-ElenaNeural",
}

SNR_DB = {"clean": None, "snr20": 20.0, "snr8": 8.0}
# Voces concurrentes (no el gold). babble8 ≈ sala; babble0 = mismo nivel que la comanda.
BABBLE_SNR = {"babble8": 8.0, "babble0": 0.0}
NOISE_KEYS = set(SNR_DB) | set(BABBLE_SNR)

BABBLE_PHRASES = [
    "la cuenta por favor",
    "hace mucho calor aqui",
    "otra ronda de canas",
    "nos vamos ya para casa",
    "teneis mesa para cuatro",
]

# Presión ~ 1/r. Referencia near = 0,5 m al micro (camarero).
# mid ≈ 1,6 m (−10 dB); far ≈ 6 m (−22 dB). Se aplica DESPUÉS del SNR.
DISTANCE_GAIN = {
    "near": 1.0,
    "mid": 10 ** (-10.0 / 20.0),
    "far": 10 ** (-22.0 / 20.0),
    "veryfar": 10 ** (-40.0 / 20.0),  # para vencer AGC del STT (~0,01)
}


def ffmpeg_exe() -> str:
    import imageio_ffmpeg
    return imageio_ffmpeg.get_ffmpeg_exe()


def mp3_to_wav(mp3: Path, wav: Path, rate: int = 16000) -> None:
    wav.parent.mkdir(parents=True, exist_ok=True)
    subprocess.check_call(
        [ffmpeg_exe(), "-y", "-i", str(mp3), "-ac", "1", "-ar", str(rate), str(wav)],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )


def read_wav(path: Path) -> tuple[np.ndarray, int]:
    with wave.open(str(path), "rb") as w:
        rate = w.getframerate()
        n = w.getnframes()
        raw = w.readframes(n)
        audio = np.frombuffer(raw, dtype=np.int16).astype(np.float32)
    return audio, rate


def write_wav(path: Path, audio: np.ndarray, rate: int) -> None:
    clipped = np.clip(audio, -32767, 32767).astype(np.int16)
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(clipped.tobytes())


def _scale_mix(audio: np.ndarray, noise: np.ndarray, snr_db: float) -> np.ndarray:
    sig_pow = np.mean(audio ** 2) + 1e-9
    noise_pow = np.mean(noise ** 2) + 1e-9
    target = sig_pow / (10 ** (snr_db / 10.0))
    noise = noise * np.sqrt(target / noise_pow)
    mixed = audio + noise
    peak = np.max(np.abs(mixed)) + 1e-9
    if peak > 30000:
        mixed = mixed * (30000 / peak)
    return mixed


def mix_noise(audio: np.ndarray, snr_db: float, rng: np.random.Generator) -> np.ndarray:
    noise = rng.normal(0, 1, size=audio.shape).astype(np.float32)
    noise = np.convolve(noise, np.ones(32) / 32, mode="same")
    return _scale_mix(audio, noise, snr_db)


def _tile(src: np.ndarray, n: int) -> np.ndarray:
    if len(src) >= n:
        return src[:n]
    reps = int(np.ceil(n / max(len(src), 1)))
    return np.tile(src, reps)[:n]


def mix_babble(
    audio: np.ndarray,
    beds: list[np.ndarray],
    snr_db: float,
    rng: np.random.Generator,
    talkers: int = 3,
) -> np.ndarray:
    """Superpone otras frases TTS (no el gold) como voces de sala."""
    n = len(audio)
    if not beds:
        return mix_noise(audio, snr_db, rng)
    noise = np.zeros(n, dtype=np.float32)
    pick = [beds[i] for i in rng.choice(len(beds), size=min(talkers, len(beds)), replace=False)]
    for bed in pick:
        shifted = np.roll(_tile(bed, n), int(rng.integers(0, max(n, 1))))
        noise += shifted
    return _scale_mix(audio, noise, snr_db)


async def tts_to_mp3(text: str, voice: str, dest: Path) -> None:
    import edge_tts
    dest.parent.mkdir(parents=True, exist_ok=True)
    comm = edge_tts.Communicate(text, voice)
    await comm.save(str(dest))


def apply_distance(audio: np.ndarray, distance: str) -> np.ndarray:
    return audio * DISTANCE_GAIN[distance]


async def ensure_wav(text: str, voice_key: str, dest: Path) -> None:
    if dest.exists():
        return
    print(f"TTS {voice_key} {dest.stem}", flush=True)
    mp3 = dest.with_suffix(".mp3")
    await tts_to_mp3(text, VOICES[voice_key], mp3)
    mp3_to_wav(mp3, dest)


async def generate(
    voices: list[str],
    snrs: list[str],
    distances: list[str],
    limit: int | None,
) -> None:
    gold = json.loads(GOLD.read_text(encoding="utf-8"))
    cases = gold["cases"]
    if limit:
        cases = cases[:limit]
    rng = np.random.default_rng(42)
    clips = []
    tmp = OUT / "tmp"
    tmp.mkdir(parents=True, exist_ok=True)

    need_babble = any(s in BABBLE_SNR for s in snrs)
    babble_beds: list[np.ndarray] = []
    if need_babble:
        for voice_key in voices:
            for i, phrase in enumerate(BABBLE_PHRASES):
                dest = tmp / f"babble_{voice_key}_{i}.wav"
                await ensure_wav(phrase, voice_key, dest)
                audio, _ = read_wav(dest)
                babble_beds.append(audio)

    for case in cases:
        cid = case["id"]
        text = case["utterance"]
        for voice_key in voices:
            wav_clean = tmp / f"{cid}__{voice_key}.wav"
            await ensure_wav(text, voice_key, wav_clean)
            audio, rate = read_wav(wav_clean)
            for snr_key in snrs:
                if snr_key in BABBLE_SNR:
                    mixed = mix_babble(audio, babble_beds, BABBLE_SNR[snr_key], rng)
                else:
                    snr = SNR_DB[snr_key]
                    mixed = audio if snr is None else mix_noise(audio, snr, rng)
                for distance in distances:
                    out = OUT / "audio" / voice_key / snr_key / distance / f"{cid}.wav"
                    write_wav(out, apply_distance(mixed, distance), rate)
                    clips.append({
                        "id": cid,
                        "utterance": text,
                        "action": case.get("action", "anadir"),
                        "voice": voice_key,
                        "snr": snr_key,
                        "distance": distance,
                        "wav": str(out.relative_to(ROOT)).replace("\\", "/"),
                    })
    (OUT / "clips.json").write_text(json.dumps(clips, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"OK {len(clips)} clips -> {OUT / 'clips.json'}")


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--voices", default="es-ES,es-MX", help="es-ES,es-MX,es-AR")
    p.add_argument("--snr", default="clean,snr8", help="clean,snr20,snr8,babble8,babble0")
    p.add_argument("--distance", default="near", help="near,mid,far (cerca / 1.6 m / 6 m)")
    p.add_argument("--limit", type=int, default=None, help="solo los N primeros casos gold")
    args = p.parse_args()
    voices = [v.strip() for v in args.voices.split(",") if v.strip()]
    snrs = [s.strip() for s in args.snr.split(",") if s.strip()]
    distances = [d.strip() for d in args.distance.split(",") if d.strip()]
    for v in voices:
        if v not in VOICES:
            sys.exit(f"voz desconocida: {v}")
    for s in snrs:
        if s not in NOISE_KEYS:
            sys.exit(f"snr desconocido: {s}")
    for d in distances:
        if d not in DISTANCE_GAIN:
            sys.exit(f"distancia desconocida: {d}")
    asyncio.run(generate(voices, snrs, distances, args.limit))


if __name__ == "__main__":
    main()
