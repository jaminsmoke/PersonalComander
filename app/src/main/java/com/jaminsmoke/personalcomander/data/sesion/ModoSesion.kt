package com.jaminsmoke.personalcomander.data.sesion

/**
 * Sesión de primer nivel del camarero. Local no exige cuenta.
 * [Establecimiento] con [admitido] false = Bar visto por health, pendiente de lista blanca.
 */
sealed class ModoSesion {
    data object Local : ModoSesion()

    data class Identidad(
        val perfil: PerfilCamarero,
        val qr: String?,
        val token: String,
    ) : ModoSesion()

    data class Establecimiento(
        val perfil: PerfilCamarero,
        val qr: String?,
        val token: String,
        val barHost: String,
        val barPuerto: Int = BarLanCliente.PUERTO,
        val admitido: Boolean = false,
    ) : ModoSesion()
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
) {
    val nombreCompleto: String
        get() = "$nombre $apellidos".trim()

    /** Referencia coloquial: nick si hay; si no, el primer nombre. */
    val mote: String
        get() = nick?.trim()?.takeIf { it.isNotEmpty() } ?: nombre

    val iniciales: String
        get() = buildString {
            nombre.firstOrNull()?.let { append(it.uppercaseChar()) }
            apellidos.firstOrNull()?.let { append(it.uppercaseChar()) }
        }.ifBlank { "?" }
}

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

fun ModoSesion.etiquetaHeader(): String? = when (this) {
    ModoSesion.Local -> null
    is ModoSesion.Identidad -> perfil.mote
    is ModoSesion.Establecimiento -> perfil.mote
}
