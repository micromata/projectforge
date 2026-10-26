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
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.Date

/**
 * An employee device (EXT-03, EXT-07), known only from the scanner and the log server, never maintained by hand.
 * Personal data: visible to the security team and the user only (VIS-15).
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_DEVICE",
    indexes = [
        Index(name = "idx_t_plugin_cveradar_device_serial", columnList = "serial_number"),
        Index(name = "idx_t_plugin_cveradar_device_user_fk", columnList = "user_fk"),
    ],
)
open class CvrDeviceDO {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    open var id: Long? = null

    @get:Column(nullable = false)
    open var created: Date? = null

    /** Serial number, the key of a device if known (unique then). */
    @get:Column(name = "serial_number", length = 100)
    open var serialNumber: String? = null

    /** The user name as reported by the sources. */
    @get:Column(length = 255)
    open var username: String? = null

    /** The PF user of [username], null if none matches. */
    @get:Column(name = "user_fk")
    open var userId: Long? = null

    @get:Column(length = 255)
    open var hostname: String? = null

    /** Id of the asset (list) in the scanner. */
    @get:Column(name = "scan_asset_id", length = 50)
    open var scanAssetId: String? = null

    @get:Column(name = "scan_asset_name", length = 255)
    open var scanAssetName: String? = null

    /** Last time any source reported the device. */
    @get:Column(name = "last_seen")
    open var lastSeen: Date? = null

    /** Last sync of the scanner reporting the device. */
    @get:Column(name = "last_scan_sync")
    open var lastScanSync: Date? = null

    /** Version of the client agent (EXT-04). */
    @get:Column(name = "agent_version", length = 50)
    open var agentVersion: String? = null

    @get:Column(name = "agent_version_at")
    open var agentVersionAt: Date? = null

    /**
     * The latest results of the checks of the client agent (EXT-05) as JSON: check id → result and time, see
     * [org.projectforge.plugins.cveradar.CvrDeviceService.AgentCheck].
     */
    @get:Column(name = "agent_checks", length = 4000)
    open var agentChecks: String? = null
}
