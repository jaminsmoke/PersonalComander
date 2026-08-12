package com.jaminsmoke.personalcomander.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.up
import com.jaminsmoke.personalcomander.data.Mesa
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MesasBoardUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun segundo_drag_usa_la_posicion_actualizada() {
        val mesa = Mesa(numero = 1, zona = "Interior", indiceZona = 1)
        var posicion by mutableFloatStateOf(40f)
        var baseCapturada = -1f

        composeRule.setContent {
            val posicionDeEstaComposicion = posicion
            MaterialTheme {
                MesaCard(
                    mesa = mesa,
                    isDragging = false,
                    onClick = {},
                    onEditClick = {},
                    onDeleteClick = {},
                    onDragStarted = { baseCapturada = posicionDeEstaComposicion },
                    onDrag = {},
                    onDragEnd = {},
                    onRotateClick = {},
                    onPointerActive = {}
                )
            }
        }

        fun dragLargo() {
            composeRule.onNodeWithText(mesa.nombreVisible).performTouchInput {
                down(center)
                advanceEventTime(700)
                moveBy(Offset(24f, 0f))
                up()
            }
            composeRule.waitForIdle()
        }

        dragLargo()
        assertEquals(40f, baseCapturada, 0.001f)

        composeRule.runOnUiThread { posicion = 240f }
        composeRule.waitForIdle()

        dragLargo()
        assertEquals(240f, baseCapturada, 0.001f)
    }
}
