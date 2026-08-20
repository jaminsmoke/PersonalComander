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
    ├── PersonalComanderApp.kt   # NavHost (home, mesas, gestión, ajustes, auth, perfil, comanda/{id})
    ├── HomeScreen.kt / HomeViewModel.kt
    ├── MesasScreen.kt / MesasViewModel.kt / MesasBoard.kt  # Board + list views
    ├── ComandaScreen.kt / ComandaViewModel.kt              # Order taking
    ├── AjustesScreen.kt / AjustesAcceso.kt / AjustesViewModel.kt  # Hub Ajustes (TPV, Copias)
    ├── gestion/                                            # Hub Gestión (Carta, Locales, Invitaciones)
    ├── sesion/                                             # Auth, perfil, QR y visibilidad
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
                  Identity / Personal Bar (v1.7)
```

- Los ViewModels exponen `StateFlow` que la UI consume con `collectAsState()`.
- Las operaciones que tocan 2+ tablas usan `@Transaction` o `db.withTransaction {}`.
- El catálogo TPV se importa como JSON sobre TCP en la red local y se inserta con `replace`.
- La identidad del camarero se registra en Identity (servicio camareros en el VPS, HTTPS). El modo Local conserva el funcionamiento offline; la sesión se cachea si Identity no responde.
- Las membresías de establecimiento las sirve Identity (`GET /v1/camareros/me/establecimientos`); Bar consulta Identity. Ligar el **turno** sigue siendo Bar LAN. El radar de Resumen oye el beacon UDP 8788 de Bar y confirma con `GET /health` (el host no se pinta).
- En modo Establecimiento, una comanda enviada conserva primero el estado local y después intenta publicar una ronda a Personal Bar.

## Voz

El reconocimiento y el parser NLP viven en `Voz.kt` / `VozParser.kt`. Timeouts, Bluetooth y matching están documentados en [Voz](voz.md).

## Migraciones y schema

- El schema de Room se exporta a `app/schemas/` (KSP `room.schemaLocation`).
- Cada cambio de entidad requiere una migración numerada + test de migración en `androidTest` (ver [Modelo de datos](data-model.md)).
