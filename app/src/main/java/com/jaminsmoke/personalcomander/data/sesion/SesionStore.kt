package com.jaminsmoke.personalcomander.data.sesion

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class SesionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()

    var identityBaseUrl: String
        get() {
            val stored = prefs.getString(KEY_URL, null)
            val efectiva = urlIdentityEfectiva(stored)
            if (!stored.isNullOrBlank() && esIdentityDockerLocal(stored)) {
                prefs.edit().putString(KEY_URL, efectiva).apply()
            }
            return efectiva
        }
        set(value) {
            prefs.edit().putString(KEY_URL, urlIdentityEfectiva(value)).apply()
        }

    fun cargar(): ModoSesion {
        val token = prefs.getString(KEY_TOKEN, null) ?: return ModoSesion.Local
        val perfilJson = prefs.getString(KEY_PERFIL, null) ?: return ModoSesion.Local
        val qrRaw = prefs.getString(KEY_QR, null) ?: return ModoSesion.Local
        val qr = qrRaw.ifBlank { null }
        val fichaUrlRaw = prefs.getString(KEY_FICHA_URL, null)?.trim()?.takeIf { it.isNotEmpty() }
        val fichaUrl = normalizarFichaUrl(fichaUrlRaw)
        if (fichaUrl != fichaUrlRaw) {
            prefs.edit().putString(KEY_FICHA_URL, fichaUrl.orEmpty()).apply()
        }
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
                nombreEstablecimiento = prefs.getString(KEY_BAR_NOMBRE, null)?.takeIf { it.isNotBlank() },
                sesionTrabajo = prefs.getBoolean(KEY_BAR_SESION_TRABAJO, false),
                fichaUrl = fichaUrl,
            )
        }
        return ModoSesion.Identidad(perfil, qr, token, fichaUrl)
    }

    fun guardarIdentidad(perfil: PerfilCamarero, qr: String?, token: String, fichaUrl: String? = null) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_PERFIL, gson.toJson(perfil))
            .putString(KEY_QR, qr.orEmpty())
            .putString(KEY_FICHA_URL, normalizarFichaUrl(fichaUrl).orEmpty())
            .remove(KEY_BAR_HOST)
            .remove(KEY_BAR_ADMITIDO)
            .remove(KEY_BAR_NOMBRE)
            .remove(KEY_BAR_SESION_TRABAJO)
            .apply()
    }

    fun guardarEstablecimiento(modo: ModoSesion.Establecimiento) {
        prefs.edit()
            .putString(KEY_TOKEN, modo.token)
            .putString(KEY_PERFIL, gson.toJson(modo.perfil))
            .putString(KEY_QR, modo.qr.orEmpty())
            .putString(KEY_FICHA_URL, normalizarFichaUrl(modo.fichaUrl).orEmpty())
            .putString(KEY_BAR_HOST, modo.barHost)
            .putInt(KEY_BAR_PUERTO, modo.barPuerto)
            .putBoolean(KEY_BAR_ADMITIDO, modo.admitido)
            .putString(KEY_BAR_NOMBRE, modo.nombreEstablecimiento.orEmpty())
            .putBoolean(KEY_BAR_SESION_TRABAJO, modo.sesionTrabajo)
            .apply()
    }

    fun cargarMembresias(): List<MembresiaEstablecimiento> {
        val json = prefs.getString(KEY_MEMBRESIAS, null) ?: return emptyList()
        return IdentityJson.parseEstablecimientos(json) ?: emptyList()
    }

    fun cargarVisibilidad(): VisibilidadCamarero {
        val json = prefs.getString(KEY_VISIBILIDAD, null) ?: return VisibilidadCamarero.DEFAULT
        return try {
            IdentityJson.parseVisibilidad(json)
        } catch (_: Exception) {
            VisibilidadCamarero.DEFAULT
        }
    }

    fun guardarVisibilidad(visibilidad: VisibilidadCamarero) {
        prefs.edit()
            .putString(KEY_VISIBILIDAD, IdentityJson.cuerpoVisibilidadCompleto(visibilidad))
            .apply()
    }

    fun guardarMembresias(lista: List<MembresiaEstablecimiento>) {
        val arr = JsonArray()
        lista.forEach { m ->
            arr.add(
                JsonObject().apply {
                    addProperty("id", m.id)
                    addProperty("nombre", m.nombre)
                    addProperty("cuenta_negocio_id", m.cuentaNegocioId)
                    addProperty("rol", m.rol)
                },
            )
        }
        prefs.edit().putString(KEY_MEMBRESIAS, arr.toString()).apply()
    }

    fun limpiarBar() {
        prefs.edit()
            .remove(KEY_BAR_HOST)
            .remove(KEY_BAR_PUERTO)
            .remove(KEY_BAR_ADMITIDO)
            .remove(KEY_BAR_NOMBRE)
            .remove(KEY_BAR_SESION_TRABAJO)
            .apply()
    }

    fun limpiarTodo() {
        val url = identityBaseUrl
        prefs.edit().clear().apply()
        identityBaseUrl = url
    }

    companion object {
        /**
         * Servicio camareros de Identity en el VPS (Docker en el servidor, igual que prod).
         * No es Docker del host ni el servicio negocio `:8082`.
         */
        const val DEFAULT_IDENTITY_URL = "https://camareros.siberia.solutions"

        fun urlIdentityEfectiva(guardada: String?): String {
            val t = guardada?.trim().orEmpty()
            if (t.isEmpty() || esIdentityDockerLocal(t)) return DEFAULT_IDENTITY_URL
            return t.trimEnd('/')
        }

        fun esIdentityDockerLocal(url: String): Boolean {
            val t = url.trim().trimEnd('/').lowercase()
            return t == "http://10.0.2.2:8080" ||
                t == "http://127.0.0.1:8080" ||
                t == "http://localhost:8080"
        }

        private const val PREFS = "pc_sesion"
        private const val KEY_URL = "identity_base_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_PERFIL = "perfil"
        private const val KEY_QR = "qr"
        private const val KEY_FICHA_URL = "ficha_url"
        private const val KEY_BAR_HOST = "bar_host"
        private const val KEY_BAR_PUERTO = "bar_puerto"
        private const val KEY_BAR_ADMITIDO = "bar_admitido"
        private const val KEY_BAR_NOMBRE = "bar_nombre"
        private const val KEY_BAR_SESION_TRABAJO = "bar_sesion_trabajo"
        private const val KEY_MEMBRESIAS = "membresias_identity"
        private const val KEY_VISIBILIDAD = "visibilidad"
    }
}
