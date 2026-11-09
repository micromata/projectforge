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
 * The affected versions of a package in an OSV advisory, the index the matching looks up by package (MAT-01): one
 * range ([introduced] to [fixed] or [lastAffected]) or one [exactVersion] per row. Derived from the advisory and
 * replaced with every import of it, so it is neither historised nor edited.
 *
 * [packageName] is the name in the spelling of [org.projectforge.plugins.cveradar.sync.OsvPackages.key] (Maven
 * `group:artifact`, npm `@scope/name`, PyPI normalised, all lower case); [shortName] is the artifact of Maven, for
 * libraries whose groupId is unknown (jar listings): such a match is only possible.
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_PURL_MATCH",
    indexes = [
        Index(name = "idx_fk_t_plugin_cveradar_purl_match_advisory_fk", columnList = "advisory_fk"),
        Index(name = "idx_t_plugin_cveradar_purl_match_package_name", columnList = "package_name"),
        Index(name = "idx_t_plugin_cveradar_purl_match_short_name", columnList = "short_name"),
    ],
)
open class CvrPurlMatchDO {
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Id
    @get:Column(name = "pk")
    open var id: Long? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "advisory_fk", nullable = false)
    open var advisory: CvrOsvAdvisoryDO? = null

    /** The ecosystem of OSV, e.g. `Maven`. */
    @get:Column(length = 50, nullable = false)
    open var ecosystem: String? = null

    @get:Column(name = "package_name", length = 500, nullable = false)
    open var packageName: String? = null

    @get:Column(name = "short_name", length = 255)
    open var shortName: String? = null

    /** The first affected version, null (or `0`) for all versions before [fixed]. */
    @get:Column(length = 100)
    open var introduced: String? = null

    /** The first version that isn't affected any more, null: no fix (yet). */
    @get:Column(length = 100)
    open var fixed: String? = null

    /** The last affected version, if the source names no fix. */
    @get:Column(name = "last_affected", length = 100)
    open var lastAffected: String? = null

    /** A single affected version (advisories without ranges); the range fields are null then. */
    @get:Column(name = "exact_version", length = 100)
    open var exactVersion: String? = null

    /**
     * The operating systems the vulnerable code is built for, comma separated (Go advisories, e.g. `windows`); null for
     * all. A component known to run on another one isn't affected (MAT-14).
     */
    @get:Column(length = 255)
    open var goos: String? = null
}
