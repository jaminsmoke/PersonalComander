package com.jaminsmoke.personalcomander.ui

import com.jaminsmoke.personalcomander.data.LineaPedido
import com.jaminsmoke.personalcomander.data.Producto
import com.jaminsmoke.personalcomander.data.normalizarNombre
import com.google.gson.Gson

private data class VozEvalGoldFile(
    val catalog: String? = null,
    val cases: List<VozEvalGoldCaseJson> = emptyList()
)

private data class VozEvalGoldCaseJson(
    val id: String,
    val utterance: String,
    val expected: List<VozEvalExpected>? = null,
    val tags: List<String>? = null,
    val action: String? = null,
    val comandaActual: List<VozEvalExpected>? = null
)

fun loadVozEvalGold(json: String, gson: Gson = Gson()): List<VozEvalCase> {
    val file = gson.fromJson(json, VozEvalGoldFile::class.java)
    return file.cases.map { dto ->
        VozEvalCase(
            id = dto.id,
            utterance = dto.utterance,
            expected = dto.expected.orEmpty(),
            tags = dto.tags.orEmpty(),
            action = if (dto.action.equals("quitar", ignoreCase = true)) VozEvalAction.QUITAR
            else VozEvalAction.ANADIR,
            comandaActual = dto.comandaActual.orEmpty()
        )
    }
}

/** Línea esperada en el corpus de eval de voz (nombre de catálogo Seed). */
data class VozEvalExpected(val nombre: String, val cantidad: Int)

enum class VozEvalAction { ANADIR, QUITAR }

data class VozEvalCase(
    val id: String,
    val utterance: String,
    val expected: List<VozEvalExpected>,
    val tags: List<String> = emptyList(),
    val action: VozEvalAction = VozEvalAction.ANADIR,
    val comandaActual: List<VozEvalExpected> = emptyList()
)

enum class VozEvalVerdict { EXACT, PARTIAL, FAIL }

data class VozEvalQtyMismatch(
    val nombre: String,
    val expectedQty: Int,
    val actualQty: Int
)

/**
 * Resultado de comparar hipótesis (transcripción STT) contra gold.
 * [exact] implica [verdict] = EXACT.
 */
data class VozEvalScore(
    val verdict: VozEvalVerdict,
    val missing: List<VozEvalExpected>,
    val extra: List<VozEvalExpected>,
    val qtyMismatch: List<VozEvalQtyMismatch>
) {
    val exact: Boolean get() = verdict == VozEvalVerdict.EXACT
}

fun scoreVozHypothesis(
    case: VozEvalCase,
    hypothesis: String,
    catalogo: List<Producto>
): VozEvalScore {
    return when (case.action) {
        VozEvalAction.ANADIR -> {
            val parsed = parsearComanda(hypothesis, catalogo)
            scoreLines(
                expected = case.expected,
                actual = parsed.lineas.map { VozEvalExpected(it.producto.nombre, it.cantidad) }
            )
        }
        VozEvalAction.QUITAR -> {
            val lineas = case.comandaActual.mapIndexed { idx, e ->
                LineaPedido(
                    id = idx + 1L,
                    pedidoId = 1,
                    productoId = idx + 1L,
                    nombreProducto = e.nombre,
                    precioUnitario = 0.0,
                    cantidad = e.cantidad
                )
            }
            val texto = when (val accion = extraerAccion(hypothesis)) {
                is AccionVoz.Quitar -> accion.texto
                is AccionVoz.Anadir -> accion.texto
            }
            val parsed = parsearQuitar(texto, lineas)
            scoreLines(
                expected = case.expected,
                actual = parsed.lineas.map { VozEvalExpected(it.nombreProducto, it.cantidad) }
            )
        }
    }
}

internal fun scoreLines(
    expected: List<VozEvalExpected>,
    actual: List<VozEvalExpected>
): VozEvalScore {
    val expMap = mergeByName(expected)
    val actMap = mergeByName(actual)
    val missing = mutableListOf<VozEvalExpected>()
    val extra = mutableListOf<VozEvalExpected>()
    val qtyMismatch = mutableListOf<VozEvalQtyMismatch>()

    for ((key, exp) in expMap) {
        val act = actMap[key]
        if (act == null) missing.add(exp)
        else if (act.cantidad != exp.cantidad) {
            qtyMismatch.add(VozEvalQtyMismatch(exp.nombre, exp.cantidad, act.cantidad))
        }
    }
    for ((key, act) in actMap) {
        if (key !in expMap) extra.add(act)
    }

    val verdict = when {
        missing.isEmpty() && extra.isEmpty() && qtyMismatch.isEmpty() -> VozEvalVerdict.EXACT
        missing.size < expected.size || qtyMismatch.isNotEmpty() -> VozEvalVerdict.PARTIAL
        else -> VozEvalVerdict.FAIL
    }
    return VozEvalScore(verdict, missing, extra, qtyMismatch)
}

private fun mergeByName(lines: List<VozEvalExpected>): Map<String, VozEvalExpected> {
    val acc = linkedMapOf<String, VozEvalExpected>()
    for (line in lines) {
        val key = normalizarNombre(line.nombre)
        val prev = acc[key]
        acc[key] = if (prev == null) line
        else prev.copy(cantidad = prev.cantidad + line.cantidad)
    }
    return acc
}

fun sliceRates(scores: List<Pair<List<String>, VozEvalScore>>): Map<String, Double> {
    val byTag = mutableMapOf<String, Pair<Int, Int>>()
    for ((tags, score) in scores) {
        val tagsOrAll = tags.ifEmpty { listOf("untagged") }
        for (tag in tagsOrAll) {
            val (ok, total) = byTag[tag] ?: (0 to 0)
            byTag[tag] = (ok + if (score.exact) 1 else 0) to (total + 1)
        }
    }
    return byTag.mapValues { (_, v) -> if (v.second == 0) 0.0 else v.first.toDouble() / v.second }
}
