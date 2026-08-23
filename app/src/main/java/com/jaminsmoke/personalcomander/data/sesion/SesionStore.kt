package com.jaminsmoke.personalcomander.data.sesion

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class SesionStore(context: Context) {
    private val ctx = context.applicationContext
    private val gson = Gson()

    /**
     * EncryptedSharedPreferences con clave AES-256 en Android Keystore.
     * Si la clave del Keystore se pierde (cambio de bloqueo de pantalla, restauración
     * en otro dispositivo), se descarta la sesión anterior y se fuerza re-autenticación.
     */
    private val prefs: SharedPreferences by lazy { crearOReiniciar() }

    private fun crearOReiniciar(): SharedPreferences {
        // Primero migrar datos legacy si existen en texto plano
        val legacy = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val tieneDatosLegacy = legacy.contains(KEY_TOKEN)
        val datosLegacy = if (tieneDatosLegacy) legacy.all.toMap() else emptyMap()

        return try {
            val masterKey = MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            @Suppress("DEPRECATION")
            val encrypted = EncryptedSharedPreferences.create(
                ctx,
                PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
            // Migrar datos legacy al almacén cifrado
            if (tieneDatosLegacy && !encrypted.contains(KEY_TOKEN)) {
                val editor = encrypted.edit()
                datosLegacy.forEach { (k, v) ->
                    when (v) {
                        is String -> editor.putString(k, v)
                        is Int -> editor.putInt(k, v as Int)
                        is Boolean -> editor.putBoolean(k, v as Boolean)
                        is Long -> editor.putLong(k, v as Long)
                        is Float -> editor.putFloat(k, v as Float)
                    }
                }
                editor.apply()
                // Limpiar el almacén legacy
                legacy.edit().clear().apply()
            }
            encrypted
        } catch (e: Exception) {
            // Keystore corrupto o inaccesible: descartar sesión, forzar re-auth
            try { legacy.edit().clear().apply() } catch (_: Exception) {}
            val masterKey = MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                ctx,
                PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }

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

    /** Último esquema de carta visto (`GET /v1/carta`). 0 = aún no visto. */
    var cartaSchema: Int
        get() = prefs.getInt(KEY_CARTA_SCHEMA, 0)
        set(value) {
            prefs.edit().putInt(KEY_CARTA_SCHEMA, value).apply()
        }

    /** Token de sesión LAN emitido por Bar v0.2. Null en Bar 0.1 o sin jornada. */
    var tokenLan: String?
        get() = prefs.getString(KEY_BAR_TOKEN_LAN, null)?.trim()?.takeIf { it.isNotEmpty() }
        set(value) {
            prefs.edit().putString(KEY_BAR_TOKEN_LAN, value).apply()
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
                establecimientoId = prefs.getString(KEY_BAR_ESTABLECIMIENTO_ID, null)?.takeIf { it.isNotBlank() },
                sesionTrabajo = prefs.getBoolean(KEY_BAR_SESION_TRABAJO, false),
                fichaUrl = fichaUrl,
                tokenLan = tokenLan,
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
            .remove(KEY_BAR_ESTABLECIMIENTO_ID)
            .remove(KEY_BAR_SESION_TRABAJO)
            .remove(KEY_BAR_TOKEN_LAN)
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
            .putString(KEY_BAR_ESTABLECIMIENTO_ID, modo.establecimientoId.orEmpty())
            .putBoolean(KEY_BAR_SESION_TRABAJO, modo.sesionTrabajo)
            .putString(KEY_BAR_TOKEN_LAN, modo.tokenLan.orEmpty())
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
            .remove(KEY_BAR_ESTABLECIMIENTO_ID)
            .remove(KEY_BAR_SESION_TRABAJO)
            .remove(KEY_BAR_TOKEN_LAN)
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
        private const val KEY_BAR_ESTABLECIMIENTO_ID = "bar_establecimiento_id"
        private const val KEY_BAR_SESION_TRABAJO = "bar_sesion_trabajo"
        private const val KEY_BAR_TOKEN_LAN = "bar_token_lan"
        private const val KEY_MEMBRESIAS = "membresias_identity"
        private const val KEY_VISIBILIDAD = "visibilidad"
        private const val KEY_CARTA_SCHEMA = "carta_schema"
    }
}
