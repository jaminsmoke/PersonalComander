# Política de seguridad — Personal Comander

## Reportar una vulnerabilidad

**No abras un issue público para vulnerabilidades de seguridad.**

Usa el **reporte privado de vulnerabilidades** de GitHub (Private Vulnerability Reporting) desde la pestaña *Security* del repositorio, o contacta con el mantenedor por correo a través del perfil de GitHub.

Proceso:

1. Describe la vulnerabilidad con detalle: pasos para reproducir, impacto y versión afectada.
2. El mantenedor acusará recibo en **48-72 horas** y evaluará la severidad.
3. Trabajaremos en una corrección y coordinaremos la divulgación contigo antes de hacerla pública.

## Versiones soportadas

| Versión | Soportada |
|---|---|
| Última release (`latest`) | ✅ Se corrigen activamente |
| Rama `main` | ✅ Se corrigen activamente |
| Versiones anteriores | ❌ No reciben parches |

Se recomienda usar siempre la última versión publicada en [Releases](https://github.com/jaminsmoke/PersonalComander/releases).

## Áreas sensibles

Personal Comander es una app Android offline-first con sync por LAN. Las áreas con implicaciones de seguridad incluyen:

- **Firma y keystore**: la firma de release usa `keystore.properties` (local, nunca versionado). No compartir keystores ni contraseñas.
- **Sync TPV por LAN**: la app usa `cleartextTraffic=true` y escaneo TCP en la red local para importar catálogos. El tráfico LAN no está cifrado: usa la app solo en redes de confianza.
- **Datos locales**: las comandas y el menú viven en una base de datos Room local. La copia de seguridad (exportación JSON) debe protegerse.
- **Voz**: el reconocimiento de voz es on-device (no envía audio a la nube).

## Buena práctica para investigadores

Si haces pruebas de seguridad, ten en cuenta:

- Realiza las pruebas en tu propia instalación o con el permiso del propietario.
- No ejecutes escaneos de red agresivos contra servidores TPV ajenos.
- Reporta hallazgos a través del proceso privado; espera el visto bueno del mantenedor antes de divulgarlos públicamente.
