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

/**
 * A CVSS rating of a vulnerability.
 * @param primary Rated by the NVD (type *Primary*), otherwise by the CNA that assigned the CVE (*Secondary*).
 */
data class NvdCvss(val score: BigDecimal, val severity: String?, val vector: String?, val primary: Boolean)

/** A vulnerability as delivered by the NVD, parsed by [NvdCveParser]. */
data class NvdCve(
    val cveId: String,
    val status: String?,
    val published: Instant?,
    val lastModified: Instant?,
    /** The English description. */
    val description: String?,
    /** CVSS v3.1, or v3.0 for older ones (the vector names the version). */
    val cvss3: NvdCvss?,
    val cvss4: NvdCvss?,
    val cwes: List<String>,
    /** When the CISA added it to its catalogue of actively exploited vulnerabilities (KEV), null if not listed. */
    val kevAdded: Instant? = null,
    /** The `cve` object of the NVD as JSON. */
    val rawJson: String,
    /** The vulnerable CPE entries of its configurations (MAT-01). */
    val cpes: List<NvdCpeEntry> = emptyList(),
) {
    /**
     * The rating counting for the threshold and the lists (IMP-10): the higher one of the NVD, without one the higher
     * one of the CNA; null if not rated (yet).
     */
    val rating: NvdCvss?
        get() = listOfNotNull(cvss3, cvss4).let { ratings ->
            ratings.filter { it.primary }.maxByOrNull { it.score } ?: ratings.maxByOrNull { it.score }
        }

    val rejected: Boolean
        get() = status.equals("Rejected", ignoreCase = true)
}

/** One page of the answer of the NVD API. */
data class NvdPage(val totalResults: Int, val startIndex: Int, val cves: List<NvdCve>)

/** Fetches the vulnerabilities modified in a time range (at most 120 days), page by page. */
fun interface NvdCveSource {
    fun fetch(lastModStart: Instant, lastModEnd: Instant, startIndex: Int): NvdPage
}
