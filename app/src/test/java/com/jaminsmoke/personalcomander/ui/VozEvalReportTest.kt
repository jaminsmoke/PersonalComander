package com.jaminsmoke.personalcomander.ui

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jaminsmoke.personalcomander.data.Seed
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Si existe `devartifacts/voz-eval/hypotheses.jsonl` (corrida on-device),
 * puntúa contra gold y escribe `reports/latest.json`. No falla el build:
 * la tasa STT no es una regresión de CI.
 */
class VozEvalReportTest {

    data class HypothesisLine(
        val id: String,
        val hypothesis: String,
        val voice: String? = null,
        val snr: String? = null,
        val distance: String? = null,
        val rmsMax: Double? = null,
        val vozCercana: Boolean? = null,
        @SerializedName("source") val source: String? = null
    )

    @Test
    fun escribeInformeSiHayHipotesis() {
        val root = File("..")
        val hypFile = File(root, "devartifacts/voz-eval/hypotheses.jsonl")
        assumeTrue("sin corrida on-device", hypFile.isFile)

        val gold = loadVozEvalGold(
            javaClass.getResource("/voz-eval/gold.json")!!.readText()
        ).associateBy { it.id }
        val catalogo = Seed.productos()
        val gson = Gson()
        val lines = hypFile.readLines().filter { it.isNotBlank() }.map {
            gson.fromJson(it, HypothesisLine::class.java)
        }

        val rows = lines.mapNotNull { hyp ->
            val case = gold[hyp.id] ?: return@mapNotNull null
            val score = scoreVozHypothesis(case, hyp.hypothesis, catalogo)
            val gated = score.exact && (hyp.vozCercana == true)
            mapOf(
                "id" to hyp.id,
                "voice" to hyp.voice,
                "snr" to hyp.snr,
                "distance" to hyp.distance,
                "rmsMax" to hyp.rmsMax,
                "vozCercana" to hyp.vozCercana,
                "gatedExact" to gated,
                "source" to hyp.source,
                "hypothesis" to hyp.hypothesis,
                "verdict" to score.verdict.name,
                "exact" to score.exact,
                "tags" to case.tags,
                "missing" to score.missing.map { "${it.cantidad}×${it.nombre}" },
                "extra" to score.extra.map { "${it.cantidad}×${it.nombre}" },
                "qtyMismatch" to score.qtyMismatch.map {
                    "${it.nombre}:${it.expectedQty}->${it.actualQty}"
                }
            )
        }

        val tagged = rows.map { row ->
            @Suppress("UNCHECKED_CAST")
            val tags = (row["tags"] as List<String>).toMutableList()
            (row["voice"] as? String)?.let { tags += "voice_$it" }
            (row["snr"] as? String)?.let { tags += "snr_$it" }
            (row["distance"] as? String)?.let { tags += "dist_$it" }
            Triple(tags, row["exact"] as Boolean, row)
        }
        val slice = mutableMapOf<String, Pair<Int, Int>>()
        for ((tags, exact, _) in tagged) {
            for (tag in tags) {
                val (ok, total) = slice[tag] ?: (0 to 0)
                slice[tag] = (ok + if (exact) 1 else 0) to (total + 1)
            }
        }
        val rates = slice.mapValues { (_, v) ->
            if (v.second == 0) 0.0 else v.first.toDouble() / v.second
        }

        val byDistance = rows.groupBy { it["distance"] as? String ?: "unknown" }
        val distanceGate = byDistance.mapValues { (_, group) ->
            val rms = group.mapNotNull { it["rmsMax"] as? Number }.map { it.toDouble() }
            val cercanas = group.count { it["vozCercana"] == true }
            mapOf(
                "n" to group.size,
                "meanRms" to if (rms.isEmpty()) null else rms.average(),
                "vozCercanaRate" to cercanas.toDouble() / group.size.coerceAtLeast(1),
                "exactRate" to group.count { it["exact"] == true }.toDouble() / group.size.coerceAtLeast(1),
                "gatedExactRate" to group.count { it["gatedExact"] == true }.toDouble() / group.size.coerceAtLeast(1),
            )
        }

        val n = rows.size.coerceAtLeast(1)
        val report = mapOf(
            "n" to rows.size,
            "exactRate" to rows.count { it["exact"] == true }.toDouble() / n,
            "gatedExactRate" to rows.count { it["gatedExact"] == true }.toDouble() / n,
            "rmsUmbral" to RMS_UMBRAL_CERCANIA,
            "slices" to rates,
            "distanceGate" to distanceGate,
            "failures" to rows.filter { it["exact"] != true }
        )
        val outDir = File(root, "devartifacts/voz-eval/reports")
        outDir.mkdirs()
        File(outDir, "latest.json").writeText(gson.toJson(report))
    }
}
