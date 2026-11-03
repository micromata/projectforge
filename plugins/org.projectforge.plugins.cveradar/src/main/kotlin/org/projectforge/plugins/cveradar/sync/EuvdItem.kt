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


package org.projectforge.plugins.cveradar.sync

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** A product of an EUVD entry: names only, no CPE and no version ranges. [version] is free text ("n/a", "0 <1.2.3"). */
data class EuvdProduct(val vendor: String, val product: String, val version: String?)

/** A vulnerability as delivered by the EUVD of the ENISA, parsed by [EuvdParser]. */
data class EuvdItem(
    /** E.g. EUVD-2026-12345. */
    val id: String,
    /** The first CVE id of the aliases, null if the EUVD knows it under other ids only (e.g. GHSA). */
    val cveId: String?,
    /** All aliases other than [cveId]. */
    val aliases: List<String>,
    val description: String?,
    val published: Instant?,
    val updated: Instant?,
    /** Null if not rated (the EUVD gives 0.0 then). */
    val score: BigDecimal?,
    val vector: String?,
    /** The vector is CVSS 4.0 (otherwise 3.x), only relevant if [score] is given. */
    val cvss4: Boolean,
    val epss: BigDecimal?,
    val exploitedSince: Instant?,
    val products: List<EuvdProduct>,
) {
    /** The key of the vulnerability in the radar: the CVE id, otherwise the EUVD id. */
    val key: String
        get() = cveId ?: id
}

/** One page of the answer of the EUVD search. */
data class EuvdPage(val total: Int, val items: List<EuvdItem>)

/**
 * Searches the EUVD. The date range filters the publication day (UTC), both days included, not the day of the last
 * change; without dates all entries are searched. [exploited]: only those known to be exploited.
 */
fun interface EuvdSource {
    fun search(from: LocalDate?, to: LocalDate?, minScore: Double?, exploited: Boolean, page: Int): EuvdPage
}
