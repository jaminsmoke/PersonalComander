# Guía de contribución — Personal Comander

¡Gracias por tu interés en contribuir! 🛎️ Este proyecto es mantenido principalmente por **jaminsmoke** con ayuda de agentes de IA. Tu contribución — humana o asistida por IA — es bienvenida.

Antes de empezar, lee el [README](README.md) para conocer el proyecto.

## Cómo empezar

1. **Crea un fork** del repositorio.
2. **Crea una rama** desde `main` con un nombre descriptivo:
   - `feature/<descripción>` para nuevas funcionalidades
   - `fix/<descripción>` para correcciones
   - `chore/<descripción>` para tareas técnicas
   - `infra/<descripción>` para infraestructura (CI, docs del repo, GitHub)
3. **Abre un Pull Request** contra `main` cuando tu cambio esté listo.

## Issues

- Antes de abrir un issue, busca si ya existe uno similar.
- Usa títulos descriptivos y explica: qué ocurre, qué esperabas, pasos para reproducir y versión/dispositivo.
- **No reportes vulnerabilidades de seguridad aquí**: consulta [SECURITY.md](SECURITY.md) para el proceso de reporte privado.
- Los cambios que siguen el flujo kanban interno (proyecto GitHub) se gestionan aparte; los issues públicos son bienvenidos igualmente.

## Convenciones de commits

Usamos **Conventional Commits**:

```
feat(ui): añade tema dark premium
fix(voz): corrige timeout del reconocimiento
docs(repo): README bilingüe
chore(build): actualiza dependencias
```

Tipos habituales: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `perf`, `build`, `ci`.

## Requisitos de calidad

Antes de abrir un PR, verifica localmente:

```bash
# Compila
./gradlew assembleDebug

# Tests unitarios
./gradlew test

# Tests de instrumentación (requiere emulador)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

- Todos los tests deben pasar y el lint no debe introducir errores nuevos.
- Para cambios de UI, verifica visualmente en emulador o dispositivo real.
- Para cambios de datos (Room), incluye la migración correspondiente y su test (`app/schemas/`).

## Estilo de código

Sigue las convenciones del proyecto (resumen):

- **UI/Compose**: funciones `@Composable` en `PascalCase`; modificadores como primer parámetro cuando aplique.
- **Estado**: `StateFlow` en ViewModels, `collectAsState()` en la UI.
- **Colores**: preferir `MaterialTheme.colorScheme.*` sobre colores hardcodeados.
- **Icons**: `Icons.Default.*` o `Icons.AutoMirrored.Filled.*`; siempre con `contentDescription` (nunca `null` para iconos interactivos).
- **Strings**: todo texto visible en `res/values/strings.xml` (y `values-en` para inglés).
- **Idioma**: código en inglés (símbolos), UI y comentarios en español.
- **Room**: operaciones con 2+ tablas usan `@Transaction` o `db.withTransaction {}`.

## Contribuciones asistidas por IA

Si el código fue generado o refactorizado con IA, indícalo brevemente en la descripción del PR (qué herramienta/prompts se usaron) y asegúrate de que pase la revisión y los checks estándar igual que cualquier otra contribución.

## Review

- Los PRs son revisados por el owner (`jaminsmoke`) según [CODEOWNERS](.github/CODEOWNERS).
- Sé receptivo al feedback: los comentarios son para mejorar el código, no una crítica personal.
