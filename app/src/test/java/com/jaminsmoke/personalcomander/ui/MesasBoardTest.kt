package com.jaminsmoke.personalcomander.ui

import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaForma
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MesasBoardTest {

    private fun ocupada(x: Float, y: Float, w: Float = CARD_W, h: Float = CARD_W) =
        listOf(x, y, w, h)

    // ── colisionan ──

    @Test
    fun colisionan_detecta_solape() {
        assertTrue(colisionan(0f, 0f, 100f, 100f, 50f, 50f, 100f, 100f))
    }

    @Test
    fun colisionan_no_detecta_adyacentes() {
        // 100dp a la derecha, sin solape
        assertFalse(colisionan(0f, 0f, 100f, 100f, 200f, 0f, 100f, 100f))
    }

    @Test
    fun colisionan_bordes_justos_no_colisionan() {
        // x1+w1 == x2 exactamente → no hay solape
        assertFalse(colisionan(0f, 0f, 100f, 100f, 100f, 0f, 100f, 100f))
    }

    // ── findNearestFreeCell ──

    @Test
    fun findNearestFreeCell_devuelve_target_si_libre() {
        val (x, y) = findNearestFreeCell(200f, 200f, CARD_W, CARD_W, emptyList())
        assertEquals(200f, x, 0.001f)
        assertEquals(200f, y, 0.001f)
    }

    @Test
    fun findNearestFreeCell_no_empuja_fuera_limite_derecho() {
        // Target pegado al borde derecho (ZONA_ANCHO=2000): mesa de 120dp
        // con margen → el centro válido máximo es 2000-120-40 = 1840
        val (x, y) = findNearestFreeCell(1960f, 200f, CARD_W, CARD_W, emptyList())
        assertTrue("x=$x debe quedar dentro del grid", x + CARD_W <= ZONA_ANCHO - CELL_F + 0.001f)
        assertFalse("no debe salirse de límites", estaFueraDeLimites(x, y, CARD_W, CARD_W))
    }

    @Test
    fun findNearestFreeCell_no_empuja_fuera_limite_inferior() {
        val (x, y) = findNearestFreeCell(200f, 2520f, CARD_W, CARD_W, emptyList())
        assertTrue("y=$y debe quedar dentro del grid", y + CARD_W <= ZONA_ALTO - CELL_F + 0.001f)
        assertFalse(estaFueraDeLimites(x, y, CARD_W, CARD_W))
    }

    @Test
    fun findNearestFreeCell_esquiva_obstaculo_sin_salirse() {
        // Mesa bloqueando justo a la derecha del target → la espiral debe
        // encontrar una celda libre SIN salirse del grid
        val obstaculo = ocupada(200f, 200f)
        val (x, y) = findNearestFreeCell(40f, 200f, CARD_W, CARD_W, listOf(obstaculo))
        assertFalse("$x,$y no debe colisionar", colisionan(x, y, CARD_W, CARD_W, 200f, 200f, CARD_W, CARD_W))
        assertFalse(estaFueraDeLimites(x, y, CARD_W, CARD_W))
    }

    @Test
    fun findNearestFreeCell_clampa_target_negativo() {
        val (x, y) = findNearestFreeCell(-300f, -50f, CARD_W, CARD_W, emptyList())
        assertEquals(CELL_F, x, 0.001f)
        assertEquals(CELL_F, y, 0.001f)
    }

    @Test
    fun findNearestFreeCell_respeta_limites_personalizados() {
        // Grid pequeño de prueba (400x400): con obstáculos en todas las celdas
        // válidas, no debe devolver nada fuera
        val lx = 400f
        val ly = 400f
        val ocupadas = listOf(
            ocupada(40f, 40f), ocupada(200f, 40f),
            ocupada(40f, 200f), ocupada(200f, 200f)
        )
        val (x, y) = findNearestFreeCell(40f, 40f, CARD_W, CARD_W, ocupadas, lx, ly)
        assertFalse(estaFueraDeLimites(x, y, CARD_W, CARD_W, lx, ly))
    }

    // ── estaFueraDeLimites ──

    @Test
    fun estaFueraDeLimites_dentro_devuelve_false() {
        assertFalse(estaFueraDeLimites(200f, 200f, CARD_W, CARD_W))
    }

    @Test
    fun estaFueraDeLimites_izquierda_devuelve_true() {
        assertTrue(estaFueraDeLimites(-40f, 200f, CARD_W, CARD_W))
        assertTrue(estaFueraDeLimites(0f, 200f, CARD_W, CARD_W))
    }

    @Test
    fun estaFueraDeLimites_derecha_devuelve_true() {
        // x + w > ZONA_ANCHO - CELL_F → fuera
        assertTrue(estaFueraDeLimites(1900f, 200f, CARD_W, CARD_W))
    }

    @Test
    fun estaFueraDeLimites_inferior_devuelve_true() {
        assertTrue(estaFueraDeLimites(200f, 2520f, CARD_W, CARD_W))
    }

    // ── clampAlBorde ──

    @Test
    fun clampAlBorde_mete_dentro_la_mesa() {
        val (x, y) = clampAlBorde(1960f, 2520f, CARD_W, CARD_W)
        assertFalse(estaFueraDeLimites(x, y, CARD_W, CARD_W))
    }

    @Test
    fun clampAlBorde_alinea_al_grid() {
        val (x, y) = clampAlBorde(1957f, 2513f, CARD_W, CARD_W)
        assertEquals(0f, x % CELL_F, 0.001f)
        assertEquals(0f, y % CELL_F, 0.001f)
    }

    @Test
    fun clampAlBorde_respeta_posicion_ya_valida() {
        val (x, y) = clampAlBorde(200f, 200f, CARD_W, CARD_W)
        assertEquals(200f, x, 0.001f)
        assertEquals(200f, y, 0.001f)
    }

    @Test
    fun clampAlBorde_no_sale_por_negativo() {
        val (x, y) = clampAlBorde(-500f, -500f, CARD_W, CARD_W)
        assertTrue(x >= CELL_F)
        assertTrue(y >= CELL_F)
    }

    @Test
    fun estaFueraDeLimites_borde_exacto_devuelve_false() {
        // x + w == limiteX - CELL_F exactamente → dentro (margen incluido)
        val x = ZONA_ANCHO - CELL_F - CARD_W
        assertFalse(estaFueraDeLimites(x, 200f, CARD_W, CARD_W))
    }

    @Test
    fun estaFueraDeLimites_un_pixel_mas_devuelve_true() {
        val x = ZONA_ANCHO - CELL_F - CARD_W + 0.1f
        assertTrue(estaFueraDeLimites(x, 200f, CARD_W, CARD_W))
    }

    @Test
    fun findNearestFreeCell_mesa_gigante_no_crashea() {
        // Mesa más ancha que el grid: maxX = limite - w - CELL_F < CELL_F
        val (x, y) = findNearestFreeCell(100f, 100f, ZONA_ANCHO, ZONA_ALTO, emptyList())
        assertTrue(x >= CELL_F)
        assertTrue(y >= CELL_F)
    }

    @Test
    fun findNearestFreeCell_devuelve_celda_alineada_al_grid() {
        val (x, y) = findNearestFreeCell(1960f, 200f, CARD_W, CARD_W, emptyList())
        assertEquals(0f, x % CELL_F, 0.001f)
        assertEquals(0f, y % CELL_F, 0.001f)
    }

    @Test
    fun findNearestFreeCell_alinea_tambien_un_target_interior() {
        val (x, y) = findNearestFreeCell(213f, 267f, CARD_W, CARD_W, emptyList())
        assertEquals(0f, x % CELL_F, 0.001f)
        assertEquals(0f, y % CELL_F, 0.001f)
    }

    @Test
    fun clampAlBorde_no_redondea_fuera_con_dimensiones_no_multiplos() {
        val (w, h) = mesaDims(MesaForma.RECTANGULAR_XL, girada = false)
        val (x, y) = clampAlBorde(ZONA_ANCHO, ZONA_ALTO, w, h)
        assertFalse(estaFueraDeLimites(x, y, w, h))
        assertEquals(0f, x % CELL_F, 0.001f)
        assertEquals(0f, y % CELL_F, 0.001f)
    }

    // ── cámara ──

    @Test
    fun autoFit_puede_bajar_de_20_por_ciento_para_mostrar_todo() {
        val scale = calcularEscalaAjuste(
            viewportW = 360f,
            viewportH = 600f,
            contentW = ZONA_ANCHO,
            contentH = ZONA_ALTO,
            padding = 12f
        )
        assertTrue("scale=$scale debe caber en una pantalla estrecha", scale < 0.2f)
        assertTrue(ZONA_ANCHO * scale <= 360f - 24f + 0.001f)
        assertTrue(ZONA_ALTO * scale <= 600f - 24f + 0.001f)
    }

    @Test
    fun limitarPan_centra_el_grid_si_cabe() {
        assertEquals(300f, limitarPan(pan = 0f, viewport = 1000f, content = 400f), 0.001f)
    }

    @Test
    fun limitarPan_no_deja_perder_bordes_si_el_grid_es_grande() {
        assertEquals(0f, limitarPan(pan = 80f, viewport = 400f, content = 1000f), 0.001f)
        assertEquals(-600f, limitarPan(pan = -900f, viewport = 400f, content = 1000f), 0.001f)
    }

    @Test
    fun panTrasZoom_conserva_el_punto_bajo_el_foco() {
        val nuevoPan = panTrasZoom(pan = -100f, focoAnterior = 200f, focoActual = 200f, ratio = 2f)
        assertEquals(-400f, nuevoPan, 0.001f)
    }

    // ── normalización legacy ──

    @Test
    fun normalizarMesasEnGrid_repara_fuera_de_limites_y_solapes() {
        val mesas = listOf(
            mesaEn(id = 1, x = -300f, y = 40f),
            mesaEn(id = 2, x = -300f, y = 40f),
            mesaEn(id = 3, x = 5000f, y = 5000f)
        )
        val posiciones = normalizarMesasEnGrid(mesas)

        posiciones.forEach { (_, pos) ->
            assertFalse(estaFueraDeLimites(pos.x, pos.y, CARD_W, CARD_W))
            assertEquals(0f, pos.x % CELL_F, 0.001f)
            assertEquals(0f, pos.y % CELL_F, 0.001f)
        }
        val p1 = posiciones.getValue(1)
        val p2 = posiciones.getValue(2)
        assertFalse(colisionan(p1.x, p1.y, CARD_W, CARD_W, p2.x, p2.y, CARD_W, CARD_W))
    }

    // ── traerCerca ──

    private fun mesaEn(x: Float, y: Float, id: Long = 0) = Mesa(
        id = id, numero = id.toInt().coerceAtLeast(1), alias = null, zona = "Z", indiceZona = id.toInt().coerceAtLeast(1),
        forma = MesaForma.CUADRADA, capacidad = 4, posX = x, posY = y
    )

    @Test
    fun traerCerca_con_mesas_devuelve_dentro_de_limites() {
        val (x, y) = traerCerca(listOf(mesaEn(40f, 40f)))
        assertFalse(estaFueraDeLimites(x, y, CARD_W, CARD_W))
    }

    @Test
    fun traerCerca_sin_mesas_devuelve_esquina() {
        val (x, y) = traerCerca(emptyList())
        assertEquals(CELL_F, x, 0.001f)
        assertEquals(CELL_F, y, 0.001f)
    }

    // ── grid fijo ──

    @Test
    fun grid_estandar_tiene_tamano_generoso() {
        assertTrue(ZONA_ANCHO >= 1600f)
        assertTrue(ZONA_ALTO >= 2000f)
        assertEquals(0f, ZONA_ANCHO % CELL_F, 0.001f)
        assertEquals(0f, ZONA_ALTO % CELL_F, 0.001f)
    }
}
