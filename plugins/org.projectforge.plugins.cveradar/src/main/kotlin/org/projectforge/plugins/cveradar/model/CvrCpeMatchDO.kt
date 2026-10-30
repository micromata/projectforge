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

/**
 * A vulnerable entry (`cpeMatch`) of the configurations of an NVD document, the index the matching looks up by product
 * (MAT-01). Derived from [CvrVulnerabilityDO.rawDocument] and replaced with every import of the vulnerability, so it
 * is neither historised nor edited. `*` and `-` as the version mean all versions: [cpeVersion] is null then.
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_CPE_MATCH",
    indexes = [
        Index(name = "idx_fk_t_plugin_cveradar_cpe_match_vulnerability_fk", columnList = "vulnerability_fk"),
        Index(name = "idx_t_plugin_cveradar_cpe_match_product", columnList = "product"),
    ],
)
open class CvrCpeMatchDO {
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Id
    @get:Column(name = "pk")
    open var id: Long? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "vulnerability_fk", nullable = false)
    open var vulnerability: CvrVulnerabilityDO? = null

    /** `a` (application), `o` (operating system) or `h` (hardware). */
    @get:Column(name = "cpe_part", length = 1, nullable = false)
    open var part: String? = null

    /** Lower case, as in the CPE dictionary. */
    @get:Column(length = 255, nullable = false)
    open var vendor: String? = null

    @get:Column(length = 255, nullable = false)
    open var product: String? = null

    @get:Column(name = "cpe_version", length = 100)
    open var cpeVersion: String? = null

    @get:Column(name = "version_start_incl", length = 100)
    open var versionStartIncluding: String? = null

    @get:Column(name = "version_start_excl", length = 100)
    open var versionStartExcluding: String? = null

    @get:Column(name = "version_end_incl", length = 100)
    open var versionEndIncluding: String? = null

    @get:Column(name = "version_end_excl", length = 100)
    open var versionEndExcluding: String? = null

    /** The CPE as the NVD names it, for the display. */
    @get:Column(length = 500, nullable = false)
    open var criteria: String? = null
}
