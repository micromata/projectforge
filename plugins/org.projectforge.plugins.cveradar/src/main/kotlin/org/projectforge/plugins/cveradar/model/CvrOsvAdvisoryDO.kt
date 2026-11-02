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

package org.projectforge.plugins.cveradar.model

import jakarta.persistence.Basic
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.Date

/**
 * An advisory of OSV.dev (GHSA, PYSEC, GO, RUSTSEC, MAL, ...) for libraries of the ecosystems (IMP-13), written by the
 * import only ([org.projectforge.plugins.cveradar.sync.OsvImportService]), neither historised nor edited. Its affected
 * versions per package are the index [CvrPurlMatchDO].
 *
 * The advisories of one CVE (often a GHSA and a PYSEC) share the [vulnerabilityKey]: the CVE id, otherwise the GHSA id,
 * otherwise the OSV id. An advisory becomes a [CvrVulnerabilityDO] with that key once it matches the inventory (or the
 * NVD import brings the CVE): [vulnerability] is null before, so the list of vulnerabilities isn't flooded with the
 * advisories of all libraries.
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_OSV_ADVISORY",
    uniqueConstraints = [UniqueConstraint(name = "unique_t_plugin_cveradar_osv_advisory_osv_id", columnNames = ["osv_id"])],
    indexes = [
        Index(name = "idx_fk_t_plugin_cveradar_osv_advisory_vulnerability_fk", columnList = "vulnerability_fk"),
        Index(name = "idx_t_plugin_cveradar_osv_advisory_vulnerability_key", columnList = "vulnerability_key"),
    ],
)
open class CvrOsvAdvisoryDO {
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Id
    @get:Column(name = "pk")
    open var id: Long? = null

    /** E.g. GHSA-jq4v-f5q6-mjqq, as OSV spells it. */
    @get:Column(name = "osv_id", length = 100, nullable = false)
    open var osvId: String? = null

    /** The [CvrVulnerabilityDO.cveId] of its vulnerability (upper case), see the class comment. */
    @get:Column(name = "vulnerability_key", length = 30, nullable = false)
    open var vulnerabilityKey: String? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "vulnerability_fk")
    open var vulnerability: CvrVulnerabilityDO? = null

    /** As OSV gives it; an unchanged advisory isn't read again. */
    @get:Column(name = "modified", nullable = false)
    open var modified: Date? = null

    @get:Column(name = "published")
    open var published: Date? = null

    /** Withdrawn by its source: it matches nothing any more. */
    @get:Column(name = "withdrawn")
    open var withdrawn: Date? = null

    /** The other ids of the advisory, comma separated, e.g. "CVE-2021-28957, PYSEC-2021-19". */
    @get:Column(name = "aliases", length = 1000)
    open var aliases: String? = null

    @get:Column(name = "summary", length = 1000)
    open var summary: String? = null

    /** LOW, MEDIUM, HIGH or CRITICAL, as the source rates it (GitHub: `database_specific.severity`). */
    @get:Column(name = "severity", length = 20)
    open var severity: String? = null

    @get:Column(name = "cvss31_vector", length = 255)
    open var cvss31Vector: String? = null

    @get:Column(name = "cvss40_vector", length = 255)
    open var cvss40Vector: String? = null

    /** The weaknesses, comma separated, e.g. "CWE-79, CWE-89". */
    @get:Column(name = "cwes", length = 1000)
    open var cwes: String? = null

    /** A malicious package (OSV id `MAL-…`): every version is affected, the document isn't kept. */
    @get:Column(name = "malicious", nullable = false)
    open var malicious: Boolean = false

    /** The OSV document (JSON, gzipped) in its latest version; null for a malicious package. */
    @get:Basic(fetch = FetchType.LAZY)
    @get:Column(name = "raw_document", columnDefinition = "BLOB")
    open var rawDocument: ByteArray? = null
}
