package com.jaminsmoke.personalcomander.data.sesion

/**
 * Sesión de primer nivel del camarero. Local no exige cuenta.
 * Sala con [admitido] false = Bar visto por health, pendiente de lista blanca.
 */
sealed class ModoSesion {
    data object Local : ModoSesion()

    data class Identidad(
        val perfil: PerfilCamarero,
        val qr: String?,
        val token: String,
    ) : ModoSesion()

    data class Sala(
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
) {
    val nombreCompleto: String
        get() = "$nombre $apellidos".trim()

    val iniciales: String
        get() = buildString {
            nombre.firstOrNull()?.let { append(it.uppercaseChar()) }
            apellidos.firstOrNull()?.let { append(it.uppercaseChar()) }
        }.ifBlank { "?" }
}

val ModoSesion.cartaEditable: Boolean
    get() = when (this) {
        is ModoSesion.Sala -> !admitido
        else -> true
    }

val ModoSesion.puedeAdherirseABar: Boolean
    get() = when (this) {
        is ModoSesion.Identidad -> qr != null
        is ModoSesion.Sala -> qr != null
        ModoSesion.Local -> false
    }

val ModoSesion.credencialRevocada: Boolean
    get() = when (this) {
        is ModoSesion.Identidad -> qr == null
        is ModoSesion.Sala -> qr == null
        ModoSesion.Local -> false
    }

val ModoSesion.token: String?
    get() = when (val m = this) {
        is ModoSesion.Identidad -> m.token
        is ModoSesion.Sala -> m.token
        ModoSesion.Local -> null
    }

val ModoSesion.perfil: PerfilCamarero?
    get() = when (val m = this) {
        is ModoSesion.Identidad -> m.perfil
        is ModoSesion.Sala -> m.perfil
        ModoSesion.Local -> null
    }

val ModoSesion.qr: String?
    get() = when (val m = this) {
        is ModoSesion.Identidad -> m.qr
        is ModoSesion.Sala -> m.qr
        ModoSesion.Local -> null
    }

fun ModoSesion.etiquetaHeader(): String? = when (this) {
    ModoSesion.Local -> null
    is ModoSesion.Identidad -> perfil.nombre
    is ModoSesion.Sala -> perfil.nombre
}
