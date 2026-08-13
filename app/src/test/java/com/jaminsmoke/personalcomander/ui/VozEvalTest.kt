package com.jaminsmoke.personalcomander.ui

import com.jaminsmoke.personalcomander.data.Seed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VozEvalTest {

    private val catalogo = Seed.productos()

    @Test
    fun exact_anadir_cafe() {
        val case = VozEvalCase(
            id = "t1",
            utterance = "dos cafés con leche",
            expected = listOf(VozEvalExpected("Café con leche", 2)),
            tags = listOf("clean")
        )
        val score = scoreVozHypothesis(case, "dos cafés con leche", catalogo)
        assertTrue(score.exact)
        assertEquals(VozEvalVerdict.EXACT, score.verdict)
    }

    @Test
    fun fail_si_stt_inventa_otro_producto() {
        val case = VozEvalCase(
            id = "t2",
            utterance = "un café solo",
            expected = listOf(VozEvalExpected("Café solo", 1))
        )
        val score = scoreVozHypothesis(case, "una pizza margarita", catalogo)
        assertEquals(VozEvalVerdict.FAIL, score.verdict)
        assertEquals(1, score.missing.size)
        assertEquals(1, score.extra.size)
    }

    @Test
    fun partial_si_cantidad_mal() {
        val case = VozEvalCase(
            id = "t3",
            utterance = "tres croquetas caseras",
            expected = listOf(VozEvalExpected("Croquetas caseras", 3))
        )
        val score = scoreVozHypothesis(case, "dos croquetas caseras", catalogo)
        assertEquals(VozEvalVerdict.PARTIAL, score.verdict)
        assertEquals(1, score.qtyMismatch.size)
        assertEquals(3, score.qtyMismatch[0].expectedQty)
        assertEquals(2, score.qtyMismatch[0].actualQty)
    }

    @Test
    fun quitar_exact() {
        val case = VozEvalCase(
            id = "t4",
            utterance = "quita un café con leche",
            expected = listOf(VozEvalExpected("Café con leche", 1)),
            action = VozEvalAction.QUITAR,
            comandaActual = listOf(
                VozEvalExpected("Café con leche", 2),
                VozEvalExpected("Agua mineral", 1)
            )
        )
        val score = scoreVozHypothesis(case, "quita un café con leche", catalogo)
        assertTrue(score.exact)
    }

    @Test
    fun sliceRates_por_tag() {
        val exact = VozEvalScore(VozEvalVerdict.EXACT, emptyList(), emptyList(), emptyList())
        val fail = VozEvalScore(
            VozEvalVerdict.FAIL,
            listOf(VozEvalExpected("Agua mineral", 1)),
            emptyList(),
            emptyList()
        )
        val rates = sliceRates(
            listOf(
                listOf("clean", "qty") to exact,
                listOf("clean") to fail,
                listOf("noisy") to fail
            )
        )
        assertEquals(0.5, rates.getValue("clean"), 0.001)
        assertEquals(1.0, rates.getValue("qty"), 0.001)
        assertEquals(0.0, rates.getValue("noisy"), 0.001)
    }

    @Test
    fun gold_json_oracle_parser_sobre_utterance() {
        val json = javaClass.getResource("/voz-eval/gold.json")!!.readText()
        val cases = loadVozEvalGold(json)
        assertTrue(cases.size >= 20)
        val catalogo = Seed.productos()
        val scores = cases.map { it to scoreVozHypothesis(it, it.utterance, catalogo) }
        val exact = scores.count { it.second.exact }
        val clean = scores.filter { "clean" in it.first.tags }
        val cleanExact = clean.count { it.second.exact }
        assertTrue("oracle clean $cleanExact/${clean.size}", cleanExact >= (clean.size * 0.6).toInt())
        assertTrue("oracle global $exact/${scores.size}", exact >= (scores.size * 0.5).toInt())
    }

    @Test
    fun fugas_sala_para_cuatro_no_cuela_pizza() {
        val case = VozEvalCase(
            id = "leak_para_cuatro",
            utterance = "tres croquetas caseras",
            expected = listOf(VozEvalExpected("Croquetas caseras", 3)),
            tags = listOf("babble")
        )
        val score = scoreVozHypothesis(
            case,
            "tres croquetas caseras para cuatro",
            catalogo
        )
        assertTrue(score.exact)
        assertTrue(score.extra.isEmpty())
    }
}
