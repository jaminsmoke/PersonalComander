package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.annotations.SerializedName

/**
 * Sesión de primer nivel del camarero. Local no exige cuenta.
 * [Establecimiento] se persiste solo si Bar te tiene en lista blanca ([admitido]).
 * Un Bar visto en la LAN y no admitido no liga el teléfono: el radar de Resumen lo pinta apagado.
 */
sealed class ModoSesion {
    data object Local : ModoSesion()

    data class Identidad(
        val perfil: PerfilCamarero,
        val qr: String?,
        val token: String,
        /** URL pública de ficha que manda Identity. No se inventa en Commander. */
        val fichaUrl: String? = null,
    ) : ModoSesion()

    data class Establecimiento(
        val perfil: PerfilCamarero,
        val qr: String?,
        val token: String,
        val barHost: String,
        val barPuerto: Int = BarLanCliente.PUERTO,
        val admitido: Boolean = false,
        /** Nombre de `GET /health`, no la IP. */
        val nombreEstablecimiento: String? = null,
        /** UUID Identity de `GET /health.establecimiento_id`. Null en prefs viejos o nodo sin cuenta. */
        val establecimientoId: String? = null,
        /** Jornada concedida por Bar (`POST /v1/sesion/iniciar`). Distinto de [admitido]. */
        val sesionTrabajo: Boolean = false,
        /** URL pública de ficha que manda Identity. No se inventa en Commander. */
        val fichaUrl: String? = null,
    ) : ModoSesion()
}

/** Linaje de la cuenta en Identity. No es un rol; inmutable tras el alta. */
enum class DataOrigin {
    @SerializedName("real") Real,
    @SerializedName("test") Test,
    @SerializedName("demo") Demo,
    ;

    val wire: String
        get() = when (this) {
            Real -> "real"
            Test -> "test"
            Demo -> "demo"
        }

    companion object {
        fun fromWire(value: String?): DataOrigin = when (value?.trim()?.lowercase()) {
            "test" -> Test
            "demo" -> Demo
            else -> Real
        }
    }
}

/**
 * Opt-in al directorio de establecimientos (Bar). Contrato Identity
 * `visible_otros_establecimientos`. Default seguro: [Nunca].
 */
enum class VisibleOtrosEstablecimientos {
    @SerializedName("nunca") Nunca,
    @SerializedName("solo_libre") SoloLibre,
    @SerializedName("siempre") Siempre,
    ;

    val wire: String
        get() = when (this) {
            Nunca -> "nunca"
            SoloLibre -> "solo_libre"
            Siempre -> "siempre"
        }

    companion object {
        fun fromWire(value: String?): VisibleOtrosEstablecimientos = when (value?.trim()?.lowercase()) {
            "siempre" -> Siempre
            "solo_libre" -> SoloLibre
            else -> Nunca
        }
    }
}

data class PerfilCamarero(
    val id: String,
    val nombre: String,
    val apellidos: String,
    val email: String,
    val telefono: String? = null,
    val fotoUrl: String? = null,
    /** Mote visible en establecimientos (colas, voz). Distinto del nombre legal. */
    val nick: String? = null,
    /** Dirección de la ficha. Identity; null si no informada o prefs viejos. */
    val direccion: String? = null,
    /** Ciudad de la ficha. Identity; null si no informada o prefs viejos. */
    val ciudad: String? = null,
    /** Linaje Identity. Null en prefs viejos: [origen] cae a Real. */
    val dataOrigin: DataOrigin? = DataOrigin.Real,
    /**
     * Directorio de locales. Null en prefs viejos: [visibleDirectorio] cae a Nunca.
     * Mirror de Identity; no se inventa el valor.
     */
    val visibleOtrosEstablecimientos: VisibleOtrosEstablecimientos? = VisibleOtrosEstablecimientos.Nunca,
) {
    val nombreCompleto: String
        get() = "$nombre $apellidos".trim()

    /** Referencia coloquial: nick si hay; si no, el primer nombre. */
    val mote: String
        get() = nick?.trim()?.takeIf { it.isNotEmpty() } ?: nombre

    val origen: DataOrigin
        get() = dataOrigin ?: DataOrigin.Real

    val visibleDirectorio: VisibleOtrosEstablecimientos
        get() = visibleOtrosEstablecimientos ?: VisibleOtrosEstablecimientos.Nunca

    val iniciales: String
        get() = buildString {
            nombre.firstOrNull()?.let { append(it.uppercaseChar()) }
            apellidos.firstOrNull()?.let { append(it.uppercaseChar()) }
        }.ifBlank { "?" }
}

/** Sin turno LAN: funciones locales completas (con o sin cuenta). */
val ModoSesion.esStandalone: Boolean
    get() = this !is ModoSesion.Establecimiento

/** Turno abierto contra un nodo Bar (admitido o no). No implica jornada. */
val ModoSesion.esActivo: Boolean
    get() = this is ModoSesion.Establecimiento

/** Bar concedió sesión de trabajo: se pueden mandar rondas. */
val ModoSesion.esJornada: Boolean
    get() = this is ModoSesion.Establecimiento && sesionTrabajo

/** Nombre de sala para UI. Vacío si el health no trajo establecimiento; nunca una IP. */
fun ModoSesion.Establecimiento.etiquetaLocal(): String =
    nombreEstablecimiento?.trim().orEmpty()

val ModoSesion.cartaEditable: Boolean
    get() = when (this) {
        is ModoSesion.Establecimiento -> !admitido
        else -> true
    }

/** Mismo candado que la carta: el mapa lo marca el Bar solo si hay alta. */
val ModoSesion.mapaEditable: Boolean
    get() = cartaEditable

val ModoSesion.puedeAdherirseABar: Boolean
    get() = when (this) {
        is ModoSesion.Identidad -> qr != null
        is ModoSesion.Establecimiento -> qr != null
        ModoSesion.Local -> false
    }

val ModoSesion.credencialRevocada: Boolean
    get() = when (this) {
        is ModoSesion.Identidad -> qr == null
        is ModoSesion.Establecimiento -> qr == null
        ModoSesion.Local -> false
    }

val ModoSesion.token: String?
    get() = when (val m = this) {
        is ModoSesion.Identidad -> m.token
        is ModoSesion.Establecimiento -> m.token
        ModoSesion.Local -> null
    }

val ModoSesion.perfil: PerfilCamarero?
    get() = when (val m = this) {
        is ModoSesion.Identidad -> m.perfil
        is ModoSesion.Establecimiento -> m.perfil
        ModoSesion.Local -> null
    }

val ModoSesion.qr: String?
    get() = when (val m = this) {
        is ModoSesion.Identidad -> m.qr
        is ModoSesion.Establecimiento -> m.qr
        ModoSesion.Local -> null
    }

val ModoSesion.fichaUrl: String?
    get() = when (val m = this) {
        is ModoSesion.Identidad -> normalizarFichaUrl(m.fichaUrl)
        is ModoSesion.Establecimiento -> normalizarFichaUrl(m.fichaUrl)
        ModoSesion.Local -> null
    }

/** Payload del QR visible: `ficha_url` http(s) de Identity, o el phid1 si no hay URL. */
val ModoSesion.qrVisible: String?
    get() = qrVisibleDe(qr, fichaUrl)

/**
 * Reescribe la ficha legacy de Identity a la web canónica del profesional.
 * Identity manda `ficha_url`; Commander no la inventa. Solo mapea
 * `ficha.siberia.solutions/{ficha|camareros}` → `web.camareros…/camareros`.
 */
fun normalizarFichaUrl(raw: String?): String? {
    val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val uri = try {
        java.net.URI(trimmed)
    } catch (_: Exception) {
        return trimmed
    }
    val host = uri.host?.lowercase().orEmpty()
    if (host != FICHA_LEGACY_HOST) return trimmed
    val path = uri.rawPath.trimEnd('/').ifEmpty { "/" }
    if (path != "/ficha" && path != "/camareros") return trimmed
    val query = uri.rawQuery?.let { "?$it" }.orEmpty()
    return "$FICHA_CANONICA_BASE$query"
}

fun qrVisibleDe(qr: String?, fichaUrl: String?): String? {
    val url = normalizarFichaUrl(fichaUrl).orEmpty()
    if (url.startsWith("https://") || url.startsWith("http://")) return url
    return qr
}

private const val FICHA_LEGACY_HOST = "ficha.siberia.solutions"
private const val FICHA_CANONICA_BASE = "https://web.camareros.siberia.solutions/camareros"

fun ModoSesion.etiquetaHeader(): String? = when (this) {
    ModoSesion.Local -> null
    is ModoSesion.Identidad -> perfil.mote
    is ModoSesion.Establecimiento -> perfil.mote
}
