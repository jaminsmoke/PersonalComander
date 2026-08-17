# Instalación

## Requisitos

- **Android 7.0+** (API 24)
- Un **móvil en vertical** (es el formato de la app). En desarrollo, el AVD de referencia es `Movil-Pixel10a`
- Para sincronizar con un TPV: red local con el servidor del TPV
- Para el turno en un local: Personal Bar en la misma LAN (`:8787`)
- Para cuenta y ficha: red hacia Identity (`https://camareros.siberia.solutions`)

La app funciona en tablet, pero el oficio de Commander es el teléfono del camarero. El puesto del negocio en tablet es Personal Bar.

## Instalar el APK

1. Descarga la última versión desde [Releases](https://github.com/jaminsmoke/PersonalComander/releases/latest) (fichero `.apk`). La release pública actual es **v1.6**.
2. Copia el APK al dispositivo o descárgalo directamente.
3. Toca el fichero y acepta la instalación de orígenes desconocidos si el sistema lo pide.
4. Abre Personal Comander.

## Primer arranque

En el primer arranque la app crea un menú de demostración y un plano de mesas, para explorar todas las pantallas sin configurar nada.

El modo por defecto es **Local** (todo en el teléfono, sin cuenta). El login diario, cuando lo necesites, es el botón **Entrar** del header — no está en Ajustes.

## Compilar desde código (desarrolladores)

```bash
./gradlew installDebug
```

Requiere JDK 17+ y Android SDK con `platforms;android-37`. Guía de contribución en el [README](https://github.com/jaminsmoke/PersonalComander/blob/main/README.md).
