package com.jaminsmoke.personalcomander.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.jaminsmoke.personalcomander.R

enum class AjustesAcceso(
    val labelRes: Int,
    val icon: ImageVector,
    val navKey: String,
) {
    TURNO(R.string.ajustes_acceso_turno, Icons.Default.Work, "turno"),
    TPV(R.string.ajustes_acceso_tpv, Icons.Default.Sync, "tpv"),
    COPIAS(R.string.ajustes_acceso_copias, Icons.Default.Backup, "copias"),
    AVANZADO(R.string.ajustes_acceso_avanzado, Icons.Default.Tune, "avanzado"),
    ;

    companion object {
        const val NAV_HUB = "hub"

        fun fromNav(abrir: String?): AjustesAcceso? = when (abrir?.trim()?.lowercase()) {
            TURNO.navKey -> TURNO
            TPV.navKey -> TPV
            COPIAS.navKey -> COPIAS
            AVANZADO.navKey -> AVANZADO
            else -> null
        }
    }
}
