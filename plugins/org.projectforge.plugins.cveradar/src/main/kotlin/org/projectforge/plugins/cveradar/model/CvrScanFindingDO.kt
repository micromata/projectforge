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
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.Date

/**
 * A finding of the vulnerability scanner for a device (EXT-01): one per device and scanner plugin; resolved when the
 * latest sync doesn't report it any more, reopened when reported again (EXT-02).
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_SCAN_FINDING",
    uniqueConstraints = [UniqueConstraint(
        name = "unique_t_plugin_cveradar_scan_finding",
        columnNames = ["device_fk", "plugin_id"],
    )],
)
open class CvrScanFindingDO {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    open var id: Long? = null

    @get:Column(name = "device_fk", nullable = false)
    open var deviceId: Long? = null

    @get:Column(name = "plugin_id", length = 50, nullable = false)
    open var pluginId: String? = null

    @get:Column(name = "plugin_name", length = 1000)
    open var pluginName: String? = null

    /** 0 info, 1 low, 2 medium, 3 high, 4 critical. */
    @get:Column
    open var severity: Int? = null

    /** Comma separated. */
    @get:Column(name = "cve_ids", length = 4000)
    open var cveIds: String? = null

    @get:Column(name = "first_seen")
    open var firstSeen: Date? = null

    @get:Column(name = "last_seen")
    open var lastSeen: Date? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var status: CvrScanFindingStatus? = null

    /** When the sync didn't report it any more. */
    @get:Column(name = "resolved_at")
    open var resolvedAt: Date? = null
}
