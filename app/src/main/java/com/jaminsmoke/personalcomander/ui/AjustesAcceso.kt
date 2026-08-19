package com.jaminsmoke.personalcomander.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Sync
import androidx.compose.ui.graphics.vector.ImageVector
import com.jaminsmoke.personalcomander.R

enum class AjustesAcceso(
    val labelRes: Int,
    val icon: ImageVector,
    val navKey: String,
) {
    TPV(R.string.ajustes_acceso_tpv, Icons.Default.Sync, "tpv"),
    COPIAS(R.string.ajustes_acceso_copias, Icons.Default.Backup, "copias"),
    ;

    companion object {
        const val NAV_HUB = "hub"

        fun fromNav(abrir: String?): AjustesAcceso? = when (abrir?.trim()?.lowercase()) {
            TPV.navKey -> TPV
            COPIAS.navKey -> COPIAS
            else -> null
        }
    }
}
