package com.jaminsmoke.personalcomander.data.sesion

import android.content.Context
import com.google.gson.Gson

class SesionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()

    var identityBaseUrl: String
        get() = prefs.getString(KEY_URL, DEFAULT_IDENTITY_URL) ?: DEFAULT_IDENTITY_URL
        set(value) {
            prefs.edit().putString(KEY_URL, value.trim()).apply()
        }

    fun cargar(): ModoSesion {
        val token = prefs.getString(KEY_TOKEN, null) ?: return ModoSesion.Local
        val perfilJson = prefs.getString(KEY_PERFIL, null) ?: return ModoSesion.Local
        val qrRaw = prefs.getString(KEY_QR, null) ?: return ModoSesion.Local
        val qr = qrRaw.ifBlank { null }
        val perfil = try {
            gson.fromJson(perfilJson, PerfilCamarero::class.java)
        } catch (_: Exception) {
            return ModoSesion.Local
        }
        val barHost = prefs.getString(KEY_BAR_HOST, null)
        if (!barHost.isNullOrBlank()) {
            return ModoSesion.Establecimiento(
                perfil = perfil,
                qr = qr,
                token = token,
                barHost = barHost,
                barPuerto = prefs.getInt(KEY_BAR_PUERTO, BarLanCliente.PUERTO),
                admitido = prefs.getBoolean(KEY_BAR_ADMITIDO, false),
            )
        }
        return ModoSesion.Identidad(perfil, qr, token)
    }

    fun guardarIdentidad(perfil: PerfilCamarero, qr: String?, token: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_PERFIL, gson.toJson(perfil))
            .putString(KEY_QR, qr.orEmpty())
            .remove(KEY_BAR_HOST)
            .remove(KEY_BAR_ADMITIDO)
            .apply()
    }

    fun guardarEstablecimiento(modo: ModoSesion.Establecimiento) {
        prefs.edit()
            .putString(KEY_TOKEN, modo.token)
            .putString(KEY_PERFIL, gson.toJson(modo.perfil))
            .putString(KEY_QR, modo.qr.orEmpty())
            .putString(KEY_BAR_HOST, modo.barHost)
            .putInt(KEY_BAR_PUERTO, modo.barPuerto)
            .putBoolean(KEY_BAR_ADMITIDO, modo.admitido)
            .apply()
    }

    fun limpiarBar() {
        prefs.edit()
            .remove(KEY_BAR_HOST)
            .remove(KEY_BAR_PUERTO)
            .remove(KEY_BAR_ADMITIDO)
            .apply()
    }

    fun limpiarTodo() {
        val url = identityBaseUrl
        prefs.edit().clear().apply()
        identityBaseUrl = url
    }

    companion object {
        const val DEFAULT_IDENTITY_URL = "http://10.0.2.2:8080"
        private const val PREFS = "pc_sesion"
        private const val KEY_URL = "identity_base_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_PERFIL = "perfil"
        private const val KEY_QR = "qr"
        private const val KEY_BAR_HOST = "bar_host"
        private const val KEY_BAR_PUERTO = "bar_puerto"
        private const val KEY_BAR_ADMITIDO = "bar_admitido"
    }
}
