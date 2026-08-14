# AGENTS.md — Personal Comander

## Project

**Personal Comander** is the **waiter-facing** Android app of the PersonalHostel family: table board, touch/voice orders, POS catalog sync, and daily revenue. It is the specialized tool for **camareros** — one waiter, one phone, one floor shift.

It is **not** the venue back office. That job belongs to **Personal Bar** (sibling repo): LAN room node, expo queues, whitelist of the establishment, canonical map. Bar assigns existing waiter accounts to the business (consulting Identity); it does not create or edit waiter identity. **Identity** (VPS / Docker) is the source of truth for waiter accounts, businesses and memberships. Local Room / session prefs are a cache and fallback. The nick shown in venues is owned here (`PVTI_lAHOBM87Yc4BgJWOzg2gWTY`).

- Package: `com.jaminsmoke.personalcomander`
- Min SDK: 24 · Target SDK: 36 · Compile SDK: 37
- Version: 1.5 (versionCode 6)
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
│   └── sesion/                  # Modos Local/Identidad/Establecimiento, Identity + Bar LAN
└── ui/
    ├── PersonalComanderApp.kt   # NavHost (home, mesas, menu, ajustes, auth, perfil, comanda/{id})
    ├── HomeScreen.kt / HomeViewModel.kt
    ├── MesasScreen.kt / MesasViewModel.kt / MesasBoard.kt  # Board + list views
    ├── ComandaScreen.kt / ComandaViewModel.kt               # Order taking
    ├── MenuScreen.kt / MenuViewModel.kt                     # Product CRUD
    ├── AjustesScreen.kt / AjustesViewModel.kt               # TPV sync, backup, sesión
    ├── sesion/               # Auth, perfil QR (ZXing)
    ├── Voz.kt / VozParser.kt     # Voice recognition + NL parser
    ├── Formato.kt                # Double.formatoEuro() extension
    ├── ShimmerEffect.kt          # Loading skeleton
    └── theme/                    # Color.kt, Theme.kt, Type.kt
```

## Build & run

La UI de **Comander** está pensada para **móvil en vertical** (AVD `Pixel_9a`, portrait). Sigue siendo responsive, pero las pruebas visuales van en ese emulador.

Personal Bar (repo hermano) se prueba en **tablet apaisada**. Dos AVDs a la vez: el teléfono no ve el tablet por `10.0.2.2` a menos que el host reenvíe el puerto (`adb -s emulator-5556 forward tcp:8787 tcp:8787`) y Comander apunte a `10.0.2.2:8787`. Identity servicio **camareros** en Docker del host: `http://10.0.2.2:8080`. El servicio **negocio** es `:8082` (Bar / Identity Web), no Commander.

```bash
# Typecheck
./gradlew assembleDebug

# Install on emulator (Pixel_9a AVD, portrait)
./gradlew installDebug

# Launch emulator
emulador.bat   # Windows; or: emulator -avd Pixel_9a

# Tests
./gradlew test                         # unit
./gradlew connectedAndroidTest         # instrumentation
python scripts/check_family_contracts.py --selftest
# El job CI «Family contracts» (no required aún) pinea Identity/Bar main:
# python scripts/check_family_contracts.py --identity-openapi … --bar-module …
```

## Oficios (familia PersonalHostel)

Acordado con Bar (14-08-2026) y matizado el mismo día: Identity es la **fuente de verdad** (VPS, dos servicios/BD). La UX visual sigue partida. No convertir Commander en expo de barra ni Bar en un segundo Commander.

| Capa / app | Oficio | Quién | Qué no hace |
|---|---|---|---|
| **Identity** | Registro canónico: cuentas camarero (`:8080`), cuentas negocio y establecimientos (`:8082`). | Docker / VPS. | No es el nodo de sala. |
| **Commander** (este repo) | Herramienta **del camarero**: mesas, comanda (toque y voz), alta/edición visual de la cuenta profesional (registro, perfil, QR, nick). SQLite/prefs = fallback. | Un camarero en sala, móvil vertical. | No gestiona el negocio, no llama a `:8082`, no es el nodo LAN ni la cola de expo. |
| **Personal Bar** | Puesto del **negocio**: nodo LAN, colas, lista blanca, mapa de servicio. Alta visual de establecimientos; **consulta** Identity (no es el registro). | Tablet apaisada; **varios** camareros de servicio a la vez. | No crea ni edita la cuenta del camarero; solo **asigna** cuentas ya existentes. |

## 🎯 Kanban workflow — GitHub Project

The project is tracked at [github.com/users/jaminsmoke/projects/9](https://github.com/users/jaminsmoke/projects/9).

### Lifecycle

```
Detectado → Debate → Roadmap → Ejecutando → Verificando → Changelog
  Draft      Draft     Draft     Issue OPEN    Issue OPEN    Issue CLOSED
```

**Drafts** until `Ejecutando` — NEVER convert to issue before that.

**No skipping**: every item advances in order. Exception: `Cancelado` → Changelog.

**Version always > latest release**: consult `gh release list`, pick the next one (currently v1.6).

#### 1. Detectado — Describir el problema a fondo

El body debe contener una descripción **muy completa** del item y del problema detectado. No perder contexto: cuanta más información se documente aquí, más fácil será retomarlo en el futuro.

- Rellenar TODAS las secciones de la plantilla con contenido específico, no placeholders.
- Incluir: archivos exactos, líneas de código, trazas, versiones, métricas, capturas si aplica.
- Describir el impacto real en el usuario/producto, no solo el síntoma técnico.

#### 2. Debate — Preguntar al usuario, NO decidir solo

**Regla de oro**: NUNCA pasar de Debate a Roadmap sin preguntar al usuario y recibir su aprobación explícita.

**Investigación previa (obligatoria al entrar en Debate, antes de listar opciones)**:

Al pasar a Debate — y **antes** de redactar `Alternativas` — investigar a fondo la causa y el espacio de soluciones. Documentar en el body la sección `Investigación previa`:

- Archivos, flujos y dependencias leídos (con rutas concretas).
- Hipótesis de causa(s): los ítems suelen ser **multicausales**; no quedarse en el síntoma superficial.
- Patrones del proyecto / ecosistema relevantes (Compose, Room, Material 3, etc.).
- Restricciones reales (API, datos, UX en sala, alcance de versión).
- Qué se descartó y por qué (aunque sea breve).
- **Estrategia de rama / integración**: ¿el cambio justifica rama dedicada (`feature/...`) vs trabajo en `main`? Anotar propuesta (nombre de rama, merge a `main` al Changelog, PRs si aplica). En cambios grandes (rediseño UI, migraciones, features transversales) la rama dedicada es la opción por defecto a contemplar.

Sin esta sección no se presentan las opciones. La investigación vive en Debate (no hincha Detectado); Detectado aporta el problema, Debate aporta el mapa de soluciones.

**Formato fijo de alternativas** — siempre presentar exactamente estas **4** opciones (en este orden):

1. **Solución raíz** 🌳 — va al origen del problema (modelo, arquitectura, navegación, contrato de datos, identidad de producto…). No se limita a “hacerlo bien dentro de lo que hay”; puede proponer rediseño o cambio de enfoque. Exige basarse en la `Investigación previa`. Si el problema es genuinamente superficial y no hay causa estructural, indicar **"no aplica"** con una frase de justificación (casi siempre sí conviene explorarla: un bug “simple” puede esconder una solución más robusta).
2. **Opción sólida** 🏗️ — la más correcta y robusta **dentro del diseño actual** (o con cambios acotados). Mejor arquitectura/mantenibilidad/escalabilidad sin replantear el sistema entero.
3. **Opción rápida** ⚡ — la más rápida de implementar. Puede coincidir o no con la sólida/raíz. Prioriza velocidad sobre perfección.
4. **Opción intermedia** ⚖️ — equilibrio entre profundidad y velocidad. Solo cuando exista un punto medio real; si no hay, indicar "no aplica".

Cada opción debe llevar:
- Descripción clara de la solución
- Número estimado de líneas/cambios
- Pros (✅) y contras (⚠️)

**Recomendación situacional (revisar por ítem)**: al final, recomendar una opción **según el contexto concreto de ese ítem**, no por regla mecánica. Orientaciones de partida (siempre contrastarlas con lo hallado en la investigación):

- Bug crítico en producción → suele favorecer la **rápida** (mitigar ya), sin ocultar si la raíz merece un follow-up.
- Mejora sin urgencia → suele favorecer la **sólida** o la **raíz**, según si el diseño actual basta o hay que replantear.
- Deuda técnica acumulada → suele favorecer la **intermedia** o la **raíz** si la deuda es estructural.
- Rediseño / identidad de producto / problema multicausal profundo → valorar explícitamente la **raíz**.

La recomendación debe citar **por qué** encaja este ítem (1–3 frases), no solo etiquetar el tipo.

**Proceso**:
- Añadir secciones `Investigación previa`, `Análisis`, `Alternativas` (con las 4 opciones) y `Recomendación` al body.
- **Parar y preguntar** al usuario. Solo cuando él decida, marcar `Decision: Aprobado` y mover a Roadmap.
- Si `Decision: Cancelado` → documentar motivo, convertir a issue, cerrar, mover a Changelog.
- Si `Decision: Diferido` → documentar motivo y condición, devolver a Detectado.

#### 3. Roadmap — Planificar en profundidad antes de tocar código

Con la decisión ya tomada y acordada en la fase anterior, detallar **mucho más** el plan de implementación.

- Investigar a fondo: leer archivos relacionados, imports necesarios, dependencias, posibles efectos colaterales.
- Revisar si el plan acordado en Debate se queda corto — añadir lo que falte.
- Documentar: `Decisión acordada`, `Plan aprobado` (paso a paso), `Criterios de aceptación`, `Plan de verificación`, `Riesgos y recuperación`.
- Solo cuando el plan sea sólido y completo, mover a Ejecutando.

#### 4. Ejecutando — Implementar el plan

- Al entrar: convertir draft → issue, añadir labels (1 Tipo + 1 Área). **Aquí empieza el código.**
- Implementar siguiendo el plan detallado de Roadmap.
- Si algo difiere del plan original, **documentarlo** en el body (sección `Implementación`) explicando el porqué del cambio.
- Hacer commits locales con mensajes descriptivos en una **rama de trabajo** (`feature/...`, `infra/...`). Ver [Branch protection](#branch-protection-rulesets).

#### 5. Verificando — Tests, lint y comprobaciones exhaustivas

**No es solo compilar.** Es verificar que el cambio funciona, no rompe nada y cumple estándares de calidad.

**Checklist obligatorio** (siempre ejecutar TODO):
1. **Typecheck**: `./gradlew assembleDebug` — debe ser BUILD SUCCESSFUL
2. **Tests unitarios**: `./gradlew test` — todos deben pasar
3. **Lint**: `./gradlew lint` — debe pasar sin errores
   - Si hay **errores que introdujimos**, corregirlos obligatoriamente
   - Si hay **warnings preexistentes** relacionados con nuestro cambio, corregirlos si es posible
   - Si hay **warnings no relacionados**, documentarlos pero no es bloqueante
4. **Tests nuevos**: crear tests unitarios para la lógica nueva si no existen
   - ViewModel: test de funciones principales (cierre, reapertura, undo, etc.)
   - Parser: test de parseo si se modificó
   - Funciones puras: test de utilidades nuevas
   - NO crear tests de UI (Compose) salvo que sea crítico
5. **Revisión visual**: si hay cambios UI, verificar en emulador que se ve correcto

**Ejecutar validaciones adicionales según el tipo de item**:
- UI/UX → `assembleDebug`, tests de UI, revisar visualmente si aplica.
- Datos → tests de Room, migraciones, integridad de datos.
- Voz → probar reconocimiento, timeouts, Bluetooth.
- Sync → probar import/export, conectividad.

**Reglas de lint**:
- `LocalContextGetResourceValueCall`: usar `stringResource()` en composables, no `context.getString()`
- `EmptySuperCall`: eliminar `super.onCleared()` si el método está vacío
- `UnusedResources`: eliminar strings no usados o usar `@SuppressLint("UnusedResources")` si es temporal
- `OldTargetApi` / `NotShrinkingResources`: no corregir sin autorización (son decisiones de build)

**Antes de pasar a Changelog**:
- Documentar TODO en el body: sección `Verificación` con checklist de lo ejecutado y resultados
- Si se encontraron y corrigieron errores preexistentes, documentarlos
- Hacer commit con los fixes de verificación en la rama de trabajo
- **Abrir PR a `main`** y esperar los checks verdes (el CI corre en cada PR; `main` está protegido — ver [Branch protection](#branch-protection-rulesets))
- Solo cuando el PR tenga los checks verdes y esté listo, mergear y pasar a Changelog

#### 6. Changelog — Cerrar, fechar y publicar

1. **Commit final** con mensaje descriptivo (si no se hizo ya en Verificando) en la rama de trabajo.
2. **Merge del PR a `main`** (con los checks verdes; nunca push directo a `main`).
3. Anotar el **SHA del commit mergeado** en el body (sección `Commit`).
4. Mover status a `Changelog`.
5. Setear `Completado` (fecha) y `Completado exacto` (ISO-8601).
6. Añadir ✅ al título del issue.
7. Cerrar el issue (`gh issue close -r completed`).

### CLI (all commands from project root)

```bash
KANBAN="bun run devartifacts/jarvis-skills/packages/kanban-cli/cli.ts"

# Create item
$KANBAN create --title "..." --tipo Bug --area UI/UX --priority Alta --version "v1.6"

# List
$KANBAN list

# Show item
$KANBAN show <itemId>

# Read/set body
$KANBAN body <itemId>              # read
$KANBAN body <itemId> --set "..."  # replace

# Change status (use set-field, NOT move)
$KANBAN set-field <itemId> --field "Status" --option "Debate"

# Convert draft → issue (only at Ejecutando)
$KANBAN convert-draft <itemId>
gh issue edit <N> --add-label "tipo:bug,area:ui-ux"

# Verificando: checklist obligatorio
./gradlew assembleDebug              # 1. typecheck
./gradlew test                       # 2. unit tests
./gradlew lint                       # 3. lint (corregir errores introducidos)
# 4. Crear tests nuevos si no existen para la lógica modificada
# 5. Revisión visual en emulador si hay cambios UI
# Añadir comprobaciones específicas según área (UI, Datos, Voz, Sync...)

# Changelog: merge PR → SHA → cerrar → push de la rama
# (main está protegido: los cambios entran SOLO por PR con checks verdes)
git checkout -b feature/<nombre> && git add <files> && git commit -m "..."
git push -u origin feature/<nombre>
gh pr create --base main --head feature/<nombre> --title "..." --body "Cierra el item kanban #N..."
# Esperar checks verdes (Lint, Unit tests, Assemble debug) y mergear
gh pr merge <PR> --squash --delete-branch
$KANBAN body <itemId> --append "Commit" --content "SHA: \`$(git rev-parse --short HEAD)\`"
$KANBAN set-field <itemId> --field "Status" --option "Changelog"
$KANBAN set-field <itemId> --field "Completado" --date "YYYY-MM-DD"
$KANBAN set-field <itemId> --field "Completado exacto" --text "YYYY-MM-DDTHH:MM:SSZ"
gh issue edit <N> --title "✅ ..."
gh issue close <N> -r completed

# Delete (IRREVERSIBLE, requires --yes)
$KANBAN delete <itemId> --yes
```

### Body sections by phase

Each item's body evolves through the lifecycle. The CLI generates a template at creation — **always fill it with specific content**, never leave the placeholders.

| Phase | Body sections | Reglas |
|---|---|---|
| **Detectado** | Contexto, Hallazgo y evidencia, Impacto, Alcance a debatir, Preguntas para Debate, Criterio para avanzar, Clasificación preliminar | Descripción MUY completa. No perder contexto. |
| **Debate** | + Investigación previa, Análisis, Alternativas (4: raíz / sólida / rápida / intermedia), Recomendación | Investigar antes de opciones. **PARAR y preguntar.** No avanzar sin aprobación explícita. |
| **Roadmap** | + Decisión acordada, Plan aprobado, Criterios de aceptación, Plan de verificación, Riesgos y recuperación | Investigar a fondo. Añadir lo que falte al plan. |
| **Ejecutando** | + Implementación (qué se hizo realmente, diferencias con el plan si las hay) | Convertir draft→issue al ENTRAR. Documentar cambios sobre el plan. |
| **Verificando** | + Verificación (checklist de tests, typecheck, lint, comprobaciones específicas) | Ejecutar TODO lo aplicable. Arreglar errores preexistentes si se encuentran. |
| **Changelog** | + Commit (SHA). Setear `Completado`, `Completado exacto`. ✅ en título. | Merge PR (checks verdes) → SHA al body → cerrar issue → push rama. |

### Fields reference

| Field | Type | Purpose |
|---|---|---|
| Status | SingleSelect | Detectado → ... → Changelog |
| Prioridad | SingleSelect | Alta, Media, Baja |
| Tipo | SingleSelect | Bug, Feature, Mejora, Tarea |
| Área principal | SingleSelect | UI/UX, Datos, Voz, Sync, Android, Build/CI, Docs |
| Versión | SingleSelect | v1.4, v1.5, v1.6... |
| Decision | SingleSelect | Pendiente, Aprobado, Diferido, Cancelado |
| HighLighted | SingleSelect | Yes, No (for changelog highlights) |
| Inicio exacto | Text | ISO-8601 UTC timestamp |
| Inicio | Date | YYYY-MM-DD |
| Completado exacto | Text | ISO-8601 UTC (set on Changelog) |
| Completado | Date | YYYY-MM-DD (set on Changelog) |

### Labels canónicas

Cada Issue debe tener exactamente **1 label de Tipo + 1 label de Área**. Status,
Prioridad y Versión viven exclusivamente en campos del Project y no se duplican
como labels.

| Campo Tipo | Label | Uso |
|---|---|---|
| Bug | `tipo:bug` | Comportamiento incorrecto o regresión verificable |
| Feature | `tipo:feature` | Capacidad nueva observable para usuario o producto |
| Mejora | `tipo:mejora` | Calidad, UX, rendimiento o mantenibilidad |
| Tarea | `tipo:tarea` | Trabajo operativo o técnico acotado |

| Área principal | Label | Incluye |
|---|---|---|
| UI/UX | `area:ui-ux` | Compose, interacción, accesibilidad, diseño y navegación |
| Datos | `area:datos` | Room, DAOs, migraciones, backup e integridad |
| Voz | `area:voz` | SpeechRecognizer, parser, audio, RMS y Bluetooth de voz |
| Sync | `area:sync` | TPV, red local, importación y exportación |
| Android | `area:android` | Ciclo de vida, permisos, SDK, dispositivos y APIs Android |
| Build/CI | `area:build-ci` | Gradle, KSP, tests, firma, CI y releases |
| Docs | `area:docs` | Documentación, guías y contratos para agentes |

Labels auxiliares permitidas cuando correspondan: `security`, `dependencies`,
`duplicate`, `invalid`, `wontfix`, `question`, `good first issue` y `help wanted`.
No usar los aliases antiguos `bug`, `enhancement` o `documentation`.

### Configuración local del Kanban

`.kanbanrc.json` contiene IDs específicos del Project y permanece gitignored.
`.kanbanrc.json.template` se versiona como referencia reproducible.

Tras crear, borrar o modificar opciones de un campo SingleSelect, todos sus IDs
pueden cambiar. Regenerar y validar inmediatamente:

```bash
$KANBAN config generate --project PVT_kwHOBM87Yc4BgJWO
# El generador deja estos valores como REPLACE_ME; restaurarlos antes de continuar:
# repoId: R_kgDOT09T4w
# repo: jaminsmoke/PersonalComander
$KANBAN config validate
```

Después, comprobar que ningún ítem perdió el valor del campo modificado, reponerlo
por nombre si fuera necesario y actualizar `.kanbanrc.json.template` con los IDs
nuevos. Nunca ejecutar `convert-draft` mientras `repoId` sea `REPLACE_ME`.

## Branch protection (rulesets)

`main` está protegido con **rulesets** (GitHub, no classic protection):

- **`main-protegida`** (branch `main`): requiere **PR a `main`** (pull_request, 0 approvals — evita deadlock con 1 solo colaborador), **required status checks** (`Lint`, `Unit tests`, `Assemble debug`), bloquea **force-push** y **borrado**.
- **`tags-protegidos`** (tags `v*`): bloquea **force-push** y **borrado** (protege las releases publicadas).
- **`deleteBranchOnMerge=true`**: las ramas se borran solas al mergear el PR.
- **Bypass**: solo `jaminsmoke` (admin) puede saltarse las reglas (emergencias). **Los agentes NUNCA deben pushear directo a `main`** aunque su token lo permita — todo entra por PR con checks verdes.

Flujo estándar de cualquier cambio: rama de trabajo → commits → push → **PR a `main`** → esperar checks → merge (`gh pr merge --squash --delete-branch`).

Para ver la config actual:

```bash
gh api repos/jaminsmoke/PersonalComander/rulesets --jq '.[] | {name, target, enforcement}'
```

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
Sala (room of the venue)  1──* Mesa (table)  1──* Pedido (order)  1──* LineaPedido (line items)
                                    │
                                    └── linked via comandaActivaId on Mesa

Producto (product) ─── referenced by LineaPedido.productoId
```

El camarero se liga en turno a un **establecimiento** vía Bar LAN, no a una sala. Las **salas** (barra, interior, terraza…) son del mapa del local. Las membresías canónicas (`GET /v1/camareros/me/establecimientos`) viven en Identity; Room y `SesionStore` cachean. `ModoSesion.Establecimiento` + `admitido` candan carta y mapa.

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

## Community & contribution

- Public-facing guides live in the repo: [`CONTRIBUTING.md`](CONTRIBUTING.md) (human/AI contribution guide), [`SECURITY.md`](SECURITY.md) (private vulnerability reporting) and `.github/CODEOWNERS` (single owner `@jaminsmoke` + sensitive paths). PR template at `.github/pull_request_template.md`.
- All agents must keep these files in sync with AGENTS.md when conventions change.

## License & business model

- **License**: [MIT](LICENSE) (repo público). Permisiva: cualquiera puede usar el código comercialmente sin obligación de compartir.
- **Open core en evaluación** (modelo de negocio): si se introducen features premium de pago, el código premium debe vivir en un **módulo `:pro` privado** (repo/submodule aparte, fuera de este repo público) con validación por licencia (key o servidor). **NO escribir código premium en el repo público.**
- Relicenciar a futuro es posible pero solo aplica a versiones nuevas; el código ya publicado bajo MIT queda disponible bajo MIT.

## Dev tools

```
devartifacts/                 # gitignored
├── README.md                 # explains tools
├── agora.db / productos.json # test data
└── jarvis-skills/            # cloned skills repo
    └── packages/kanban-cli/  # bun install'd

emulador.bat                  # launches Pixel_9a AVD
.kanbanrc.json                # local Project IDs (gitignored)
.kanbanrc.json.template       # versioned reproducible reference
```

### Assets de marca y capturas (pipeline reproducible)

Los assets públicos (`docs/assets/logo.png`, `docs/assets/favicon.png`, `docs/screenshots/*.png`) se regeneran con scripts versionados, NO a mano:

```bash
python scripts/generate_assets.py   # logo + favicon desde ic_brand_shield.webp (Pillow)
bash scripts/capture_screens.sh     # 5 capturas desde emulador (adb + uiautomator)
# Si hay teléfono y tablet activos, fijar el teléfono explícitamente:
ADB_DEVICE=emulator-5556 bash scripts/capture_screens.sh
```

Ejecutarlos tras cada rebranding/rediseño de UI para no desincronizar README/sitio/wiki.

`.agents/skills/` contains installed skills (via `npx skills`). `.claude/`, `skills-lock.json` are agent-specific — all gitignored.
