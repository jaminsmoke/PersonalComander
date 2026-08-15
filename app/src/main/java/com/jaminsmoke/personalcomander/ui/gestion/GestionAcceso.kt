package com.jaminsmoke.personalcomander.ui.gestion

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import com.jaminsmoke.personalcomander.R

enum class GestionAcceso(
    val labelRes: Int,
    val icon: ImageVector,
    val navKey: String,
) {
    CARTA(R.string.gestion_carta, Icons.Default.RestaurantMenu, "carta"),
    LOCALES(R.string.gestion_locales, Icons.Default.Storefront, "locales"),
    INVITACIONES(R.string.gestion_invitaciones, Icons.Default.Email, "invitaciones"),
    ;

    companion object {
        const val NAV_HUB = "hub"

        fun fromNav(abrir: String?): GestionAcceso? = when (abrir?.trim()?.lowercase()) {
            CARTA.navKey -> CARTA
            LOCALES.navKey -> LOCALES
            INVITACIONES.navKey -> INVITACIONES
            else -> null
        }
    }
}
