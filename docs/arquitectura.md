# Arquitectura

Personal Comander es una app Android **offline-first**: el estado de sala (mesas, carta, comandas) vive en Room. Identity es la **fuente de verdad** de la cuenta del camarero y de las membresías; `SesionStore` y Room actúan como cache/fallback. Las otras conexiones de red son el cliente TPV (LAN), Bar LAN (turno) y (opcionalmente) el reconocimiento de voz del sistema.

## Stack

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navegación | `androidx.navigation.compose` |
| Estado | `ViewModel` + `StateFlow` |
| Base de datos | Room (SQLite) con KSP |
| Serialización | Gson (sync TPV, backup JSON) |
| Voz | Android `SpeechRecognizer` (on-device) |
| DI | Manual (Application cast vía `viewModel` factory) |
| Build | Gradle KTS + Version Catalog |
| Desugaring | `desugar_jdk_libs` (Java 11 → API 24) |

## Estructura del proyecto

```
app/src/main/java/com/jaminsmoke/personalcomander/
├── MainActivity.kt              # Entry point, edge-to-edge, theme
├── PersonalComanderApp.kt       # DB init, seed, Room builder
├── data/
│   ├── Entities.kt              # Mesa, Producto, Pedido, LineaPedido + enums
│   ├── Daos.kt                  # Room DAOs + @Transaction ops
│   ├── AppDatabase.kt           # Room DB, migraciones, repos
│   ├── Seed.kt                  # First-launch seed
│   ├── BackupJson.kt            # JSON import/export models
│   ├── Tpv.kt / TpvCliente.kt   # POS sync adapters
│   ├── CategoriaIcono.kt        # Category → emoji mapping
│   └── ...
└── ui/
    ├── PersonalComanderApp.kt   # NavHost (home → mesas | menu | ajustes | comanda/{id})
    ├── HomeScreen.kt / HomeViewModel.kt
    ├── MesasScreen.kt / MesasViewModel.kt / MesasBoard.kt  # Board + list views
    ├── ComandaScreen.kt / ComandaViewModel.kt              # Order taking
    ├── MenuScreen.kt / MenuViewModel.kt                    # Product CRUD
    ├── AjustesScreen.kt / AjustesViewModel.kt              # TPV sync, backup, cuenta y sala
    ├── sesion/                                             # Auth, perfil, QR y conexión al establecimiento
    ├── Voz.kt / VozParser.kt     # Voice recognition + NL parser
    ├── Formato.kt                # Double.formatoEuro() extension
    └── theme/                    # Color.kt, Theme.kt, Type.kt
```

## Flujo de datos

```
UI (Compose) → ViewModel (StateFlow) → Repository → Room (SQLite)
                    ↑                        ↓
                 VozParser (NLP)        Sync TPV (Gson/LAN)
                  Backup JSON (import/export)
                  Identity / Personal Bar (v1.6)
```

- Los ViewModels exponen `StateFlow` que la UI consume con `collectAsState()`.
- Las operaciones que tocan 2+ tablas usan `@Transaction` o `db.withTransaction {}`.
- El catálogo TPV se importa como JSON sobre TCP en la red local y se inserta con `replace`.
- La identidad del camarero se registra en Identity (servicio camareros `:8080`). El modo Local conserva el funcionamiento offline; la sesión se cachea si Identity no responde.
- Las membresías de establecimiento las sirve Identity (`GET /v1/camareros/me/establecimientos`); Bar consulta Identity. Ligar el **turno** sigue siendo Bar LAN.
- En modo Establecimiento, una comanda enviada conserva primero el estado local y después intenta publicar una ronda a Personal Bar.

## Voz

- `VozRecognizer` envuelve Android `SpeechRecognizer` con timeouts adaptativos (15/30/45s según el RMS).
- `VozParser` hace NLP: tokeniza la comanda en español y encuentra productos por match exacto/fuzzy (Levenshtein).
- Ejemplo: `"dos cafés con leche y una tarta"` → `[(Café con leche, 2), (Tarta de queso, 1)]`.
- `RMS_UMBRAL_CERCANIA = 6.0f` distingue habla cercana/lejana; hay detección de auriculares Bluetooth.

## Migraciones y schema

- El schema de Room se exporta a `app/schemas/` (KSP `room.schemaLocation`).
- Cada cambio de entidad requiere una migración numerada + test de migración en `androidTest` (ver [Modelo de datos](data-model.md)).
