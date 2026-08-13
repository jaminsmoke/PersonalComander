# Arquitectura

Personal Comander es una app Android **offline-first**: todo el estado vive en una base de datos Room local y las únicas conexiones de red son el cliente TPV (LAN) y (opcionalmente) el reconocimiento de voz del sistema.

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
    ├── AjustesScreen.kt / AjustesViewModel.kt              # TPV sync, backup
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
```

- Los ViewModels exponen `StateFlow` que la UI consume con `collectAsState()`.
- Las operaciones que tocan 2+ tablas usan `@Transaction` o `db.withTransaction {}`.
- El catálogo TPV se importa como JSON sobre TCP en la red local y se inserta con `replace`.

## Voz

- `VozRecognizer` envuelve Android `SpeechRecognizer` con timeouts adaptativos (15/30/45s según el RMS).
- `VozParser` hace NLP: tokeniza la comanda en español y encuentra productos por match exacto/fuzzy (Levenshtein).
- Ejemplo: `"dos cafés con leche y una tarta"` → `[(Café con leche, 2), (Tarta de queso, 1)]`.
- `RMS_UMBRAL_CERCANIA = 6.0f` distingue habla cercana/lejana; hay detección de auriculares Bluetooth.

## Migraciones y schema

- El schema de Room se exporta a `app/schemas/` (KSP `room.schemaLocation`).
- Cada cambio de entidad requiere una migración numerada + test de migración en `androidTest` (ver [Modelo de datos](data-model.md)).
