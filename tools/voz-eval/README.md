# Eval de voz (laboratorio)

Mide **utterance gold → audio (TTS + ruido/acento) → STT → `VozParser`**.

`SpeechRecognizer` no oye el altavoz del emulador (loopback). En AVD el camino
honesto es **inyectar PCM** con `RecognizerIntent.EXTRA_AUDIO_SOURCE` (API 33+):
el mismo motor on-device, sin micro físico. Artefactos pesados en
`devartifacts/voz-eval/` (gitignored). Gold y scorer van en el repo.

## Qué se puede testear sin móvil

| Capa | Dónde | Qué mide | CI |
|---|---|---|---|
| Parser (STT perfecto) | `./gradlew :app:testDebugUnitTest --tests …VozEvalTest` | Gold → `VozParser` | sí |
| Parser + Room | `connectedAndroidTest` `VozIntegrationTest` | texto simulado → BD | emulador |
| STT real + parser | `run_ondevice.py` en AVD (inject PCM) | acento/ruido del **motor Android** | no (laboratorio) |
| Informe de tasas | `VozEvalReportTest` si hay `hypotheses.jsonl` | exact/partial/fail | no falla el build |

No fingir `SpeechRecognizer` en tests JVM: el parser sí; el ASR no.

## 1. Corpus

`app/src/test/resources/voz-eval/gold.json` — catálogo `Seed.productos()`.

Tags: `clean`, `multi`, `qty`, `ambiguous`, `quitar`, `relleno`, `long`.

## 2. Baseline parser (sin STT)

```bash
./gradlew :app:testDebugUnitTest --tests com.jaminsmoke.personalcomander.ui.VozEvalTest
```

Oracle: puntúa cada utterance gold como si el STT fuera perfecto.

## 3. Generar audio

```bash
pip install edge-tts numpy imageio-ffmpeg
python tools/voz-eval/generate_audio.py --voices es-ES,es-MX --snr clean,snr8
```

`--limit N` para un subset. Voces: `es-ES`, `es-MX`, `es-AR`.
SNR: `clean`, `snr20`, `snr8` (ruido rosa), `babble8` / `babble0` (otras frases TTS encima).
Distancia: `near` (0 dB, ~0,5 m), `mid` (−10 dB), `far` (−22 dB), `veryfar` (−40 dB, vence el tope RMS=10 del GMS).

```bash
python tools/voz-eval/generate_audio.py --voices es-ES,es-MX,es-AR --snr clean,babble8,babble0 --distance near --limit 8
```

## 4. Captura en emulador (inject, no loopback)

Pixel_9a (API 36). Instalar debug, conceder micro (el servicio STT aún lo pide):

```bash
./gradlew installDebug
adb shell pm grant com.jaminsmoke.personalcomander android.permission.RECORD_AUDIO
python tools/voz-eval/run_ondevice.py
```

Activity debug: `com.jaminsmoke.personalcomander/.debug.VozEvalActivity`.

Verificado en Pixel_9a (API 36): GMS **sí** transcribe el PCM inyectado. El resultado
llega en `onPartialResults` y el motor cierra con `ERROR_NO_MATCH` (7); el harness
guarda el último parcial. El reconocedor online (`NetworkSpeechRecognizer`) es el
que responde; SODA offline no tiene pack `es-ES` en este AVD. El loopback
altavoz→micro **no** funciona.

## 5. Informe

```bash
./gradlew :app:testDebugUnitTest --tests com.jaminsmoke.personalcomander.ui.VozEvalReportTest
```

Escribe `devartifacts/voz-eval/reports/latest.json` (tasa global, slices, fallos).

## Mitigación

Usar `failures` del informe: si el producto gold no está en la hipótesis → ASR
(vocabulario, ruido, acento). Si la hipótesis es correcta y el parser falla →
`VozParser`. Si hay extra de un primo (Café solo vs con leche) → ambigüedad del
catálogo / fuzzy.
