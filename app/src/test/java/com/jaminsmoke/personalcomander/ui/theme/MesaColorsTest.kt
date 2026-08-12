package com.jaminsmoke.personalcomander.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.jaminsmoke.personalcomander.data.MesaEstado
import org.junit.Assert.assertEquals
import org.junit.Test

class MesaColorsTest {

    private val scheme = darkColorScheme(
        tertiary = Color(0xFF80D6C3),
        error = Color(0xFFFFB4AB),
        secondary = Color(0xFFE9C349),
    )

    @Test
    fun mesaAccent_libre_usaTertiary() {
        assertEquals(scheme.tertiary, scheme.mesaAccent(MesaEstado.LIBRE))
    }

    @Test
    fun mesaAccent_ocupada_usaError() {
        assertEquals(scheme.error, scheme.mesaAccent(MesaEstado.OCUPADA))
    }

    @Test
    fun mesaAccent_enCocina_usaSecondary() {
        assertEquals(scheme.secondary, scheme.mesaAccent(MesaEstado.EN_COCINA))
    }

    @Test
    fun mesaBoardFill_esTokenMarca() {
        assertEquals(PcMesaFill, mesaBoardFill())
    }
}
