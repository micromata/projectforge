/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.plugins.cveradar

import java.math.BigDecimal
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * The base score of a CVSS 3.0/3.1 vector (`CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H` → 9.8), by the formula of the
 * specification (https://www.first.org/cvss/v3.1/specification-document, section 7). For the advisories of OSV, which
 * give the vector only. CVSS 4.0 isn't computed (its score comes from a table of the macro vectors).
 */
object Cvss3 {
    /** The base score, null if the vector isn't a complete CVSS 3 vector. */
    fun baseScore(vector: String?): BigDecimal? {
        val parts = vector?.trim()?.split('/') ?: return null
        if (parts.firstOrNull()?.startsWith("CVSS:3.") != true) return null
        val metrics = parts.drop(1).mapNotNull { part -> part.split(':').takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
        val changed = when (metrics["S"]) {
            "U" -> false
            "C" -> true
            else -> return null
        }
        val av = mapOf("N" to 0.85, "A" to 0.62, "L" to 0.55, "P" to 0.2)[metrics["AV"]] ?: return null
        val ac = mapOf("L" to 0.77, "H" to 0.44)[metrics["AC"]] ?: return null
        val pr = (if (changed) mapOf("N" to 0.85, "L" to 0.68, "H" to 0.5) else mapOf("N" to 0.85, "L" to 0.62, "H" to 0.27))[metrics["PR"]]
            ?: return null
        val ui = mapOf("N" to 0.85, "R" to 0.62)[metrics["UI"]] ?: return null
        val cia = mapOf("H" to 0.56, "L" to 0.22, "N" to 0.0)
        val c = cia[metrics["C"]] ?: return null
        val i = cia[metrics["I"]] ?: return null
        val a = cia[metrics["A"]] ?: return null
        val iss = 1 - (1 - c) * (1 - i) * (1 - a)
        val impact = if (changed) 7.52 * (iss - 0.029) - 3.25 * (iss - 0.02).pow(15) else 6.42 * iss
        val exploitability = 8.22 * av * ac * pr * ui
        val score = when {
            impact <= 0 -> 0.0
            changed -> roundUp(minOf(1.08 * (impact + exploitability), 10.0))
            else -> roundUp(minOf(impact + exploitability, 10.0))
        }
        return BigDecimal.valueOf(score).setScale(1)
    }

    /** NONE, LOW, MEDIUM, HIGH or CRITICAL of a score (section 5). */
    fun severity(score: BigDecimal): String = when {
        score.signum() == 0 -> "NONE"
        score < BigDecimal(4) -> "LOW"
        score < BigDecimal(7) -> "MEDIUM"
        score < BigDecimal(9) -> "HIGH"
        else -> "CRITICAL"
    }

    /** The smallest number with one decimal not lower than [value], free of floating point errors (Appendix A). */
    private fun roundUp(value: Double): Double {
        val int = (value * 100_000).roundToLong()
        return if (int % 10_000 == 0L) int / 100_000.0 else (floor(int / 10_000.0) + 1) / 10.0
    }
}
