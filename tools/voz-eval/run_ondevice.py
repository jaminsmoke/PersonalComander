#!/usr/bin/env python3
"""Orquesta el eval on-device: adb push WAV → VozEvalActivity → pull hipótesis.

Requiere: adb, app debug instalada, permiso de micro concedido.
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CLIPS = ROOT / "devartifacts/voz-eval/clips.json"
PKG = "com.jaminsmoke.personalcomander"
REMOTE = f"/storage/emulated/0/Android/data/{PKG}/files/voz-eval"
ACTIVITY = f"{PKG}/.debug.VozEvalActivity"


def adb(*args: str) -> subprocess.CompletedProcess:
    return subprocess.run(["adb", *args], check=True, capture_output=True, text=True)


def read_remote(path: str) -> str:
    attempts = [
        ["adb", "exec-out", "cat", path],
        ["adb", "shell", "su", "0", "cat", path],
    ]
    for cmd in attempts:
        proc = subprocess.run(cmd, capture_output=True)
        if proc.returncode == 0 and proc.stdout:
            return proc.stdout.decode("utf-8", errors="replace")
    return ""


def wait_last(timeout: float) -> dict:
    remote_last = f"{REMOTE}/last.json"
    t0 = time.time()
    while time.time() - t0 < timeout:
        raw = read_remote(remote_last).strip()
        if raw.startswith("{"):
            try:
                return json.loads(raw.splitlines()[-1])
            except json.JSONDecodeError:
                pass
        time.sleep(1.0)
    raise TimeoutError("timeout esperando last.json del dispositivo")


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--timeout", type=float, default=45.0)
    p.add_argument("--limit", type=int, default=None)
    p.add_argument("--voice", default=None)
    p.add_argument("--snr", default=None)
    p.add_argument("--distance", default=None)
    args = p.parse_args()
    if not CLIPS.is_file():
        sys.exit(f"Falta {CLIPS}. Ejecuta generate_audio.py primero.")
    clips = json.loads(CLIPS.read_text(encoding="utf-8"))
    if args.voice:
        want_v = {v.strip() for v in args.voice.split(",") if v.strip()}
        clips = [c for c in clips if c["voice"] in want_v]
    if args.snr:
        want_s = {s.strip() for s in args.snr.split(",") if s.strip()}
        clips = [c for c in clips if c["snr"] in want_s]
    if args.distance:
        want = {d.strip() for d in args.distance.split(",") if d.strip()}
        clips = [c for c in clips if c.get("distance", "near") in want]
    if args.limit:
        clips = clips[: args.limit]

    adb("shell", "mkdir", "-p", f"{REMOTE}/audio")
    adb("shell", "chmod", "777", REMOTE)
    adb("shell", "chmod", "777", f"{REMOTE}/audio")
    # reset last.json
    subprocess.run(["adb", "shell", "rm", "-f", f"{REMOTE}/last.json"], check=False)

    out_local = ROOT / "devartifacts/voz-eval/hypotheses.jsonl"
    out_local.parent.mkdir(parents=True, exist_ok=True)
    if out_local.exists():
        out_local.unlink()

    for i, clip in enumerate(clips, 1):
        wav_host = ROOT / clip["wav"]
        distance = clip.get("distance", "near")
        remote_wav = f"{REMOTE}/audio/{clip['voice']}_{clip['snr']}_{distance}_{clip['id']}.wav"
        print(f"[{i}/{len(clips)}] {clip['id']} {clip['voice']} {clip['snr']} {distance}", flush=True)
        adb("push", str(wav_host), remote_wav)
        job = {
            "id": clip["id"],
            "wav": remote_wav,
            "voice": clip["voice"],
            "snr": clip["snr"],
            "distance": distance,
            "utterance": clip["utterance"],
        }
        job_host = ROOT / "devartifacts/voz-eval/job.json"
        job_host.write_text(json.dumps(job, ensure_ascii=False), encoding="utf-8")
        adb("push", str(job_host), f"{REMOTE}/job.json")
        subprocess.run(["adb", "shell", "rm", "-f", f"{REMOTE}/last.json"], check=False)
        adb("shell", "am", "force-stop", PKG)
        time.sleep(0.4)
        adb(
            "shell", "am", "start", "-n", ACTIVITY,
            "--ez", "auto", "true",
        )
        last = wait_last(args.timeout)
        line = {
            "id": clip["id"],
            "hypothesis": last.get("hypothesis", ""),
            "voice": clip["voice"],
            "snr": clip["snr"],
            "distance": last.get("distance", distance),
            "rmsMax": last.get("rmsMax"),
            "vozCercana": last.get("vozCercana"),
            "source": last.get("source", "speech_recognizer"),
            "sttError": last.get("sttError"),
        }
        with out_local.open("a", encoding="utf-8") as f:
            f.write(json.dumps(line, ensure_ascii=False) + "\n")
        time.sleep(0.6)

    print(f"OK hipotesis -> {out_local}")
    print("Puntuar: ./gradlew :app:testDebugUnitTest --tests com.jaminsmoke.personalcomander.ui.VozEvalReportTest")


if __name__ == "__main__":
    main()
