# Voz

Personal Comander permite tomar comandas **por voz** con reconocimiento on-device y un parser NLP en español. No requiere conexión a internet.

## Componentes

| Componente | Fichero | Función |
|---|---|---|
| `VozRecognizer` | `Voz.kt` | Envuelve Android `SpeechRecognizer` con timeouts adaptativos |
| `VozParser` | `VozParser.kt` | NLP: tokeniza la comanda → productos con cantidades |

## Reconocimiento

- **Timeouts adaptativos**: 15 / 30 / 45 segundos según el RMS de la señal (distancia de la voz).
- **`RMS_UMBRAL_CERCANIA = 6.0f`**: distingue habla cercana de lejana para ajustar el timeout.
- **Bluetooth**: detección de auricular con `BluetoothAdapter.getProfileConnectionState(HEADSET)`.
- Al destruirse, el recognizer se limpia con `activo = false` para evitar crashes por timeout residual.

## Parser NLP

El parser tokeniza la frase, identifica cantidades (palabras y números) y busca productos:

- Match **exacto** por nombre del producto.
- Match **fuzzy** con distancia de Levenshtein (tolerante a errores de transcripción).
- Ejemplo: `"dos cafés con leche y una tarta"` → `[(Café con leche, 2), (Tarta de queso, 1)]`.

## Fiabilidad

- Hay una **eval de voz** (`VozEval.kt`) con dataset sintético y tasa de acierto por escenario (item #14 del kanban).
- El parser es **resistente al parloteo de sala**: ignora habla ajena (item #15), evitando que productos de otras mesas se cuelen en la comanda.

## Buenas prácticas al desarrollar

- Probar en emulador con micrófono virtual (`VozIntegrationTest`).
- Mantener los umbrales en constantes nombradas (`RMS_UMBRAL_CERCANIA`).
- Los cambios de parser deben acompañarse de tests unitarios (`VozParserTest`).
