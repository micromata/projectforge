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

package org.projectforge.rest

import org.projectforge.business.admin.SystemStatistics
import org.projectforge.business.admin.SystemStatisticsData
import org.projectforge.common.extensions.capitalize
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The system statistics page (`/next/systemStatistics`), successor of the dynamic React page.
 *
 * The statistics are delivered section by section (one per registered [SystemStatistics] builder), so the
 * frontend shows each section as soon as it is built instead of waiting for the slow ones (gc, disk usage).
 *
 * **Access:** visible for all users (see `MenuCreator`), but non-admins only get the database section, and of it
 * only the key figures listed in [ENTRIES_FOR_NON_ADMINS] (no pool or server details).
 */
@RestController
@RequestMapping("${Rest.URL}/systemStatistics")
class SystemStatisticsRest {
    class Section(val id: String)

    class Entry(
        val id: String,
        val group: String,
        val title: String,
        val value: String,
        val gauge: SystemStatisticsData.Gauge?,
    )

    class SectionData(val id: String, val entries: List<Entry>)

    @Autowired
    private lateinit var systemStatistics: SystemStatistics

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @GetMapping("sections")
    @AccessChecked("Logged-in user; non-admins only get the sections not marked as adminOnly")
    fun getSections(): List<Section> {
        val isAdmin = accessChecker.isLoggedInUserMemberOfAdminGroup
        return systemStatistics.builders.filter { isAdmin || !it.adminOnly }.map { Section(it.id) }
    }

    @GetMapping("section/{id}")
    @AccessChecked("Logged-in user; admin only sections and entries are filtered for non-admins")
    fun getSection(@PathVariable id: String): ResponseEntity<SectionData> {
        // ResponseEntity instead of ResponseStatusException: the global exception handler would answer it
        // with HTTP 400 and report it as an unexpected error.
        val isAdmin = accessChecker.isLoggedInUserMemberOfAdminGroup
        val builder = systemStatistics.builders.find { it.id == id }
            ?: return ResponseEntity.notFound().build()
        if (!isAdmin && builder.adminOnly) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val stats = systemStatistics.getSystemStatistics(id) ?: return ResponseEntity.notFound().build()
        val entries = stats.entries
            .filter { isAdmin || it.id in ENTRIES_FOR_NON_ADMINS }
            .map { Entry(it.id, it.group.capitalize(), translate(it.title), it.valueAsString(), it.gauge) }
        return ResponseEntity.ok(SectionData(id, entries))
    }

    companion object {
        /**
         * Whitelist: only these entries are visible for non-admins.
         */
        private val ENTRIES_FOR_NON_ADMINS = setOf(
            "totalNumberOfTimesheets",
            "totalTimesheetDurations",
            "totalNumberOfUsers",
            "totalNumberOfTasks",
            "totalNumberOfHistoryEntries",
            "totalNumberOfHistoryAttrs",
        )
    }
}
