# Code review checklist — PersonalComander

Documento de trabajo temporal. Marcar cada ítem al completarlo. Cuando todo esté hecho → borrar este archivo.

Última revisión: 2026-08-11

---

## Cómo usar

- `[ ]` pendiente · `[x]` hecho
- Orden sugerido: críticos → medios → seguridad → optimizaciones → mejoras
- Al terminar todo: eliminar `CODE_REVIEW_CHECKLIST.md`

---

## Bugs críticos

- [x] **C1 — `updateEstado` borra el alias de la mesa**  
  `MesaDao.updateEstado` escribe `alias = :alias` y `ComandaViewModel` siempre pasa `null` (abrir comanda, enviar a cocina, cerrar).  
  **Fix:** quitar `alias` del UPDATE de estado, o no tocarlo si no cambia.  
  **Archivos:** `data/Daos.kt`, `ui/ComandaViewModel.kt`

- [ ] **C2 — Tras “Enviar a cocina” no se puede reenviar**  
  El botón solo se habilita si `pedidoEstado == ABIERTA`. Si se añaden más productos con mesa `EN_COCINA`/`ENVIADA`, no hay 2ª ronda.  
  **Fix:** al añadir líneas con pedido `ENVIADA`, volver a `ABIERTA` (o permitir reenvío con líneas nuevas).  
  **Archivos:** `ui/ComandaViewModel.kt`, `ui/ComandaScreen.kt`

- [x] **C3 — `cerrarMesa` marca `cerrada = true` antes del mutex**  
  Si el update falla, `cerrada` se queda en `true` y no se puede añadir nada hasta salir.  
  **Fix:** poner `cerrada = true` solo tras éxito (o resetear en el `catch`).  
  **Archivos:** `ui/ComandaViewModel.kt`

- [ ] **C4 — Voz: feedback antes de persistir líneas**  
  `procesarVoz` lanza `addProducto` (cada uno su coroutine) y enseña snackbar al momento; las líneas pueden no estar aún en BD.  
  **Fix:** insert batch dentro del mismo `mutex`/suspend; feedback al terminar.  
  **Archivos:** `ui/ComandaViewModel.kt`

- [ ] **C5 — Borrar mesa con comanda activa**  
  `deleteMesa` no comprueba `comandaActivaId` ni pedidos abiertos → huérfanos + renumeración a mitad de servicio.  
  **Fix:** bloquear borrado si hay comanda, o cascada + confirmación fuerte.  
  **Archivos:** `ui/MesasViewModel.kt`, `ui/MesasScreen.kt`, DAOs si hace falta

- [ ] **C6 — Operaciones multi-tabla sin `@Transaction`**  
  Crear pedido + ocupar mesa, cerrar pedido + liberar mesa, etc. Si falla a mitad → estado inconsistente.  
  **Fix:** `@Transaction` en DAOs o `db.withTransaction { }`.  
  **Archivos:** `data/Daos.kt` / nuevo repo, `ui/ComandaViewModel.kt`

---

## Bugs medios

- [ ] **M1 — FKs / índices faltantes**  
  Sin FK/índice en `Pedido.mesaId`, `LineaPedido.pedidoId` / `productoId`; `comandaActivaId` sin `@Index`.  
  **Archivos:** `data/Entities.kt` (+ migración Room)

- [ ] **M2 — Borrar producto usado en comandas**  
  No avisa si está en líneas; `productoId` queda roto (el nombre snapshot salva el ticket).  
  **Fix:** avisar / soft-delete / bloquear si hay líneas abiertas.  
  **Archivos:** `ui/MenuViewModel.kt`, `ui/MenuScreen.kt`

- [ ] **M3 — “Total hoy” no se reinicia tras medianoche**  
  `HomeViewModel` calcula `inicioDelDia` una sola vez al crear el VM.  
  **Fix:** recalcular por día (timer, `distinctUntilChanged` por fecha, o query con fecha actual).  
  **Archivos:** `ui/HomeViewModel.kt`

- [ ] **M4 — Shimmer eterno con 0 mesas**  
  `mesas.isEmpty()` se trata como loading → shimmer infinito si no hay mesas.  
  **Fix:** estado de carga explícito vs lista vacía.  
  **Archivos:** `ui/MesasScreen.kt`, `ui/MesasViewModel.kt`

- [ ] **M5 — Seed incompleto**  
  Solo si `count mesas == 0`; menú vacío si hay mesas sin productos.  
  **Fix:** seed de productos si `productoDao.count() == 0` independientemente.  
  **Archivos:** `PersonalComanderApp.kt`

- [ ] **M6 — `fallbackToDestructiveMigration` + migraciones parciales**  
  Solo 4→5 y 5→6; upgrades viejos pueden destruir datos.  
  **Fix:** migraciones desde versión mínima soportada o documentar wipe; valorar quitar destructive en release.  
  **Archivos:** `data/AppDatabase.kt`, `PersonalComanderApp.kt`

- [ ] **M7 — Color de mensaje TPV hardcodeado a ES**  
  Comprueba prefijos `"Sincronizados"` / `"Se encontraron"` → roto en EN.  
  **Fix:** enum/estado de éxito-error, no parsear el string.  
  **Archivos:** `ui/AjustesScreen.kt`, `ui/AjustesViewModel.kt`

- [ ] **M8 — `observeAbiertos` solo cuenta `ABIERTA`**  
  No incluye `ENVIADA`. Confirmar si es intencional; si no, contar ambos o renombrar métrica.  
  **Archivos:** `data/Daos.kt`, `ui/HomeViewModel.kt`, strings Home

- [ ] **M9 — `LineaEstado.SERVIDA` no se usa en UI**  
  Dead code de dominio o feature incompleta.  
  **Fix:** usarlo en panel de comanda (pendiente vs servido) o eliminarlo hasta que se necesite.  
  **Archivos:** `data/Entities.kt`, `ui/ComandaScreen.kt`, `ui/ComandaViewModel.kt`

- [ ] **M10 — Tras cerrar mesa no hay auto-back**  
  Usuario se queda en comanda “muerta” (`cerrada = true`).  
  **Fix:** `popBackStack` o pantalla de resumen al cerrar.  
  **Archivos:** `ui/ComandaScreen.kt`, `ui/ComandaViewModel.kt`

---

## Seguridad / red

- [ ] **S1 — `usesCleartextTraffic="true"` global**  
  OK en LAN de bar; arriesgado si se amplía.  
  **Fix:** network security config (cleartext solo a IPs privadas / debug).  
  **Archivos:** `AndroidManifest.xml`, `res/xml/`

- [ ] **S2 — Filtro SQL TPV concatenado**  
  `WHERE $filtro` en `SqliteFilasProvider`; hoy viene del enum; si se hace editable → inyección.  
  **Fix:** whitelist de filtros o parámetros bound; no aceptar SQL libre del usuario.  
  **Archivos:** `data/TpvCliente.kt`, `data/Tpv.kt`, UI ajustes si aplica

- [ ] **S3 — Escaneo LAN sin cancelación**  
  254 hosts × N puertos, pool 40, hasta 30s; no se cancela al salir de Ajustes.  
  **Fix:** `coroutineScope` + cancel en `onCleared` / al abandonar pantalla.  
  **Archivos:** `data/TpvCliente.kt`, `ui/AjustesViewModel.kt`

- [ ] **S4 — Release sin minify**  
  `isMinifyEnabled = false`.  
  **Fix:** activar R8/Proguard en release y probar.  
  **Archivos:** `app/build.gradle.kts`, `proguard-rules.pro`

---

## Optimizaciones

- [ ] **O1 — Batch insert en voz / addProducto**  
  Un solo batch + una lectura de líneas en lugar de N coroutines.  
  **Archivos:** `ui/ComandaViewModel.kt`

- [ ] **O2 — Filtro productos comanda**  
  El `combine` recalcula categorías/filtro en cada emisión.  
  **Fix:** `distinctUntilChanged` / cachear categorías.  
  **Archivos:** `ui/ComandaViewModel.kt`

- [ ] **O3 — Board mesas: anim + grid de puntos**  
  `animateFloatAsState` por mesa + redraw del grid en todo el canvas.  
  **Fix:** grid estático / `drawWithCache`; animar solo al soltar.  
  **Archivos:** `ui/MesasScreen.kt`, `ui/MesasBoard.kt`

- [ ] **O4 — Import TPV: updates en batch**  
  `fusion.actualizar.forEach { update }` → `@Update` lista o upsert.  
  **Archivos:** `data/Daos.kt`, `ui/AjustesViewModel.kt`

- [ ] **O5 — `EscaneadorRed` con coroutines**  
  Sustituir `Executor` + `awaitTermination` por coroutines cancelables.  
  **Archivos:** `data/TpvCliente.kt` (ligado a S3)

- [ ] **O6 — Unificar `normalizar`**  
  Duplicado en `Voz.kt` y `Tpv.kt`.  
  **Fix:** util compartido en `data` o `util`.  
  **Archivos:** `ui/Voz.kt`, `data/Tpv.kt`, nuevo util

- [ ] **O7 — Query “total hoy”**  
  Se recalcula en cada cambio de línea; valorar agregación o índice en `creadoEn`.  
  **Archivos:** `data/Daos.kt`, `data/Entities.kt` (+ migración)

---

## Mejoras de arquitectura / producto

- [ ] **A1 — Capa Repository**  
  VMs llaman DAOs directo; repo fino facilita tests sin Room.  
  **Archivos:** nuevo `data/*Repository.kt`, VMs

- [ ] **A2 — DI (Hilt/Koin)**  
  En lugar de cast a `PersonalComanderApp` para el DB.  
  **Archivos:** app module, VMs, `PersonalComanderApp.kt`

- [ ] **A3 — `exportSchema = true` + tests de migración**  
  **Archivos:** `data/AppDatabase.kt`, tests, gradle schema dir

- [ ] **A4 — Tests adicionales**  
  `fusionarProductos`, `ComandaViewModel` (estado mesa / alias), `updateEstado` no pisa alias.  
  **Archivos:** `app/src/test/...`

- [ ] **A5 — `swapMesas` muerto en UI**  
  Existe en VM/DAO pero no se usa en pantalla. Usar o eliminar.  
  **Archivos:** `ui/MesasViewModel.kt`, `data/Daos.kt`, UI si se expone

- [ ] **A6 — Usar `LineaEstado` en comanda (si se mantiene M9 como feature)**  
  Panel pendiente vs servido.  
  **Archivos:** comanda UI/VM

---

## Orden sugerido de acometida

| Orden | IDs | Motivo |
|------:|-----|--------|
| 1 | C1 | Pérdida de datos de alias en cada comanda |
| 2 | C3 | Deja la comanda bloqueada si falla el cierre |
| 3 | C6 + C2 | Consistencia + flujo real de sala |
| 4 | C4 + O1 | Voz fiable y más simple |
| 5 | C5 | No borrar mesas a mitad de servicio |
| 6 | M1, M5, M6 | Modelo de datos y seed |
| 7 | M3, M4, M7, M8, M10 | Home / UX / i18n |
| 8 | M2, M9, A6 | Productos y líneas |
| 9 | S1–S4 | Seguridad y release |
| 10 | O2–O7 | Rendimiento |
| 11 | A1–A5 | Arquitectura y tests |

---

## Progreso

| Bloque | Total | Hechos |
|--------|------:|-------:|
| Críticos | 6 | 2 |
| Medios | 10 | 0 |
| Seguridad | 4 | 0 |
| Optimizaciones | 7 | 0 |
| Arquitectura | 6 | 0 |
| **Total** | **33** | **2** |

Cuando Total hechos = 33 → desechar este documento.
