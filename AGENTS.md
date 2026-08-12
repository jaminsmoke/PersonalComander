# AGENTS.md — Personal Comander

## Project

**Personal Comander** is an Android app for restaurant table & order management. Camareros manage tables on a visual board, take orders via touch or voice, sync products from POS systems, and track daily revenue.

- Package: `com.jaminsmoke.personalcomander`
- Min SDK: 24 · Target SDK: 36 · Compile SDK: 37
- Version: 1.4 (versionCode 5)
- Repo: `jaminsmoke/PersonalComander`

## Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | `androidx.navigation.compose` |
| State | `ViewModel` + `StateFlow` |
| DB | Room (SQLite) with KSP |
| Serialization | Gson (TPV sync, JSON import/export) |
| Voice | Android `SpeechRecognizer` (on-device) |
| DI | Manual (Application cast via `viewModel` factory) |
| Build | Gradle KTS + Version Catalog (`libs.versions.toml`) |
| Desugaring | `desugar_jdk_libs` (Java 11 → API 24) |

## Project structure

```
app/src/main/java/com/jaminsmoke/personalcomander/
├── MainActivity.kt              # Entry point, edge-to-edge, theme
├── PersonalComanderApp.kt       # DB init, seed, Room builder
├── data/
│   ├── Entities.kt              # Mesa, Producto, Pedido, LineaPedido + enums
│   ├── Daos.kt                  # Room DAOs + @Transaction ops
│   ├── AppDatabase.kt           # Room DB, migrations, repos
│   ├── Seed.kt                  # First-launch seed
│   ├── BackupJson.kt            # JSON import/export models
│   ├── Tpv.kt / TpvCliente.kt   # POS sync adapters
│   ├── CategoriaIcono.kt        # Category → emoji mapping
│   └── ...
└── ui/
    ├── PersonalComanderApp.kt   # NavHost (home → mesas | menu | ajustes | comanda/{id})
    ├── HomeScreen.kt / HomeViewModel.kt
    ├── MesasScreen.kt / MesasViewModel.kt / MesasBoard.kt  # Board + list views
    ├── ComandaScreen.kt / ComandaViewModel.kt               # Order taking
    ├── MenuScreen.kt / MenuViewModel.kt                     # Product CRUD
    ├── AjustesScreen.kt / AjustesViewModel.kt               # TPV sync, backup
    ├── Voz.kt / VozParser.kt     # Voice recognition + NL parser
    ├── Formato.kt                # Double.formatoEuro() extension
    ├── ShimmerEffect.kt          # Loading skeleton
    └── theme/                    # Color.kt, Theme.kt, Type.kt
```

## Build & run

```bash
# Typecheck
./gradlew assembleDebug

# Install on emulator (Pixel_9a AVD)
./gradlew installDebug

# Launch emulator
emulador.bat   # Windows; or: emulator -avd Pixel_9a

# Tests
./gradlew test                         # unit
./gradlew connectedAndroidTest         # instrumentation
```

## 🎯 Kanban workflow — GitHub Project

The project is tracked at [github.com/users/jaminsmoke/projects/9](https://github.com/users/jaminsmoke/projects/9).

### Lifecycle

```
Detectado → Debate → Roadmap → Ejecutando → Verificando → Changelog
  Draft      Draft     Draft     Issue OPEN    Issue OPEN    Issue CLOSED
```

- **Drafts** until `Ejecutando` — NEVER convert to issue before that
- **Al pasar de Roadmap → Ejecutando**: convertir draft → issue, añadir labels (1 Tipo + 1 Área). Aquí empieza la implementación real (código).
- **Ejecutando**: fase de implementación. Escribir código, hacer commits locales. El body debe reflejar avances.
- **Verificando**: ejecutar validaciones — typecheck (`./gradlew assembleDebug`), tests (`./gradlew test`), lint, y cualquier otra verificación aplicable. Documentar resultados en el body (sección Verificación).
- **Changelog**: ANTES de mover, hacer commit con la referencia de los cambios. Al mover a Changelog, anotar el SHA del commit en el body (sección Implementación). Luego cerrar el issue, añadir ✅ al título, setear fechas de completado. Finalmente, hacer push a la rama de trabajo o a `main`.
- **No skipping**: every item advances in order. Exception: `Cancelado` → Changelog
- **Version always > latest release**: consult `gh release list`, pick the next one (currently v1.5)

### CLI (all commands from project root)

```bash
KANBAN="bun run devartifacts/jarvis-skills/packages/kanban-cli/cli.ts"

# Create item
$KANBAN create --title "..." --tipo Bug --area UI/UX --priority Alta --version "v1.5"

# List
$KANBAN list

# Show item
$KANBAN show <itemId>

# Read/set body
$KANBAN body <itemId>              # read
$KANBAN body <itemId> --set "..."  # replace

# Move status
$KANBAN move <itemId> --status Debate

# Convert draft → issue (only at Ejecutando)
$KANBAN convert-draft <itemId>
gh issue edit <N> --add-label "bug,UI/UX"

# Ejecutando: convertir draft → issue antes de implementar
$KANBAN convert-draft <itemId>
$KANBAN move <itemId> --status Ejecutando
gh issue edit <N> --add-label "bug,UI/UX"

# Verificando: ejecutar validaciones
git add -A && git commit -m "..."   # commit local
./gradlew assembleDebug              # typecheck
./gradlew test                       # unit tests

# Changelog: commit final, push, cerrar
git add -A && git commit -m "..."   # commit con SHA referenciable
# Anotar SHA en body → sección Implementación
$KANBAN move <itemId> --status Changelog
gh issue close <N> -r completed
git push                             # a la rama de trabajo o main

# Delete (IRREVERSIBLE, requires --yes)
$KANBAN delete <itemId> --yes
```

### Body sections by phase

Each item's body evolves through the lifecycle. The CLI generates a template at creation — **always fill it with specific content**, never leave the placeholders.

| Phase | Sections to fill |
|---|---|
| **Detectado** | Contexto, Hallazgo y evidencia, Impacto, Alcance a debatir, Preguntas para Debate, Criterio para avanzar, Clasificación preliminar |
| **Debate** | + Alternativas, trade-offs, Decision |
| **Roadmap** | + Decisión acordada, Plan aprobado, Criterios de aceptación, Riesgos |
| **Ejecutando** | Convertir draft → issue. Implementar el código. Hacer commits locales. |
| **Verificando** | + Implementación (commits, archivos). Ejecutar validaciones: typecheck, tests, lint, compilación. Documentar resultados en Verificación (checklist). |
| **Changelog** | ANTES de mover: commit con referencia. Anotar SHA en Implementación. Cerrar issue, ✅ en título, setear `Completado` / `Completado exacto`. Push a la rama. |

### Fields reference

| Field | Type | Purpose |
|---|---|---|
| Status | SingleSelect | Detectado → ... → Changelog |
| Prioridad | SingleSelect | Alta, Media, Baja |
| Tipo | SingleSelect | Bug, Feature, Mejora, Tarea |
| Área principal | SingleSelect | UI/UX, Datos, Voz, Sync, Build/CI, Docs |
| Versión | SingleSelect | v1.4, v1.5, v1.6... |
| Decision | SingleSelect | Pendiente, Aprobado, Diferido, Cancelado |
| HighLighted | SingleSelect | Yes, No (for changelog highlights) |
| Inicio exacto | Text | ISO-8601 UTC timestamp |
| Inicio | Date | YYYY-MM-DD |
| Completado exacto | Text | ISO-8601 UTC (set on Changelog) |
| Completado | Date | YYYY-MM-DD (set on Changelog) |

Config lives in `.kanbanrc.json` (gitignored, template at root for reference).

## Code conventions

- **Language**: Spanish for UI strings & comments, English for code symbols
- **Compose**: `@Composable` functions use `PascalCase`; modifiers as first parameter where possible
- **State**: `StateFlow` in ViewModels, `collectAsState()` in UI
- **Colors**: prefer `MaterialTheme.colorScheme.*` over hardcoded `Color(0xFF...)` — dynamic theming (Android 12+)
- **Icons**: `Icons.Default.*` or `Icons.AutoMirrored.Filled.*`; always set `contentDescription` (never `null` for interactive icons)
- **Strings**: all user-facing text in `res/values/strings.xml`
- **Formatting**: `Double.formatoEuro()` → `"12,50 €"` using `Locale.getDefault()`
- **Room**: operations touching 2+ tables MUST use `@Transaction` or `db.withTransaction {}`
- **Migrations**: schema exported to `app/schemas/`; migration tests in `androidTest`

## Data model

```
Mesa (table)  1──* Pedido (order)  1──* LineaPedido (line items)
                    │
                    └── linked via comandaActivaId on Mesa
                    
Producto (product) ─── referenced by LineaPedido.productoId
```

Key enums: `MesaEstado` (LIBRE, OCUPADA, EN_COCINA), `MesaForma` (REDONDA, CUADRADA, RECTANGULAR, RECTANGULAR_XL), `PedidoEstado` (ABIERTA, ENVIADA, CERRADA).

## Voice recognition

- `VozRecognizer` wraps Android `SpeechRecognizer` with adaptive timeouts (15/30/45s based on RMS)
- `VozParser` does NLP: tokenizes Spanish comanda text → finds products via exact/fuzzy (Levenshtein) match
- Supports: "dos cafés con leche y una tarta" → `[(Café con leche, 2), (Tarta de queso, 1)]`
- `RMS_UMBRAL_CERCANIA = 6.0f` distinguishes close/far speech
- Bluetooth headset detection via `BluetoothAdapter.getProfileConnectionState(HEADSET)`
- Feedback: currently Snackbar only (item #10 tracks this improvement)

## Keys & security

- `keystore.properties` at root (gitignored) for release signing
- `local.properties` for SDK path (gitignored)
- `cleartextTraffic=true` for LAN POS sync — revisit if app goes beyond local network
- GraphQL token for kanban CLI uses `GH_TOKEN` / `GITHUB_TOKEN` from `gh auth`

## Dev tools

```
devartifacts/                 # gitignored
├── README.md                 # explains tools
├── agora.db / productos.json # test data
└── jarvis-skills/            # cloned skills repo
    └── packages/kanban-cli/  # bun install'd

emulador.bat                  # launches Pixel_9a AVD
.kanbanrc.json.template       # reference for kanban config
```

`.agents/skills/` contains installed skills (via `npx skills`). `.claude/`, `skills-lock.json` are agent-specific — all gitignored.
