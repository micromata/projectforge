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

package org.projectforge.rest.calendar.importer

import mu.KotlinLogging
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.importer.ImportPairEntry
import org.projectforge.rest.importer.ImportSettings
import org.projectforge.rest.importer.ImportStorage

private val log = KotlinLogging.logger {}

/**
 * The session-held storage of an ics import into one team calendar, the successor of the Wicket
 * `TeamCalImportDao`. It keeps the parsed events and reconciles them against the target calendar
 * ([teamCalId]), matching by the iCalendar uid: no stored event with the uid is a NEW event, one is
 * MODIFIED/UNMODIFIED by the displayed values.
 *
 * Unlike the Wicket import, an ambiguous uid is not imported (FAULTY): one that occurs twice in the file, or
 * that more than one event of the calendar carries — the Wicket import silently inserted a duplicate then.
 * Without a target calendar every row carries an error, so nothing can be committed.
 *
 * @author Kai Reinhard
 */
class TeamEventImportStorage : ImportStorage<TeamEventImportDTO>(ImportSettings()) {

    override val targetEntityTitle: String
        get() = translate("plugins.teamcal.events")

    /** The calendar the events are imported into; set by the upload or by choosing another target. */
    var teamCalId: Long? = null

    var teamCalTitle: String? = null

    /** The events parsed from the file, kept so a re-reconcile (e.g. for another target) can rebuild the pairs. */
    val readEvents = mutableListOf<TeamEventImportDTO>()

    override fun prepareEntity(): TeamEventImportDTO {
        return TeamEventImportDTO()
    }

    override fun commitEntity(obj: TeamEventImportDTO) {
        readEvents.add(obj)
        addEntry(ImportPairEntry(read = obj))
    }

    override fun doReconcileImportStorage(rereadDatabaseEntries: Boolean) {
        val teamEventDao = ApplicationContextProvider.getApplicationContext().getBean(TeamEventDao::class.java)
        val calId = teamCalId
        val uidCounts = readEvents.mapNotNull { it.uid }.groupingBy { it }.eachCount()
        clearEntries()
        readEvents.forEach { read ->
            val uid = read.uid
            var stored: TeamEventImportDTO? = null
            var error: String? = null
            if (calId == null) {
                error = translate("plugins.teamcal.import.ics.error.noCalendar")
            } else if (uid != null) {
                if ((uidCounts[uid] ?: 0) > 1) {
                    error = translate("plugins.teamcal.import.ics.error.duplicateUid")
                } else {
                    val matches = teamEventDao.selectByUid(calId, uid)
                    if (matches.size > 1) {
                        error = translate("plugins.teamcal.import.ics.error.ambiguousUid")
                    }
                    stored = matches.firstOrNull()?.let { TeamEventImportDTO.stored(it) }
                }
            }
            // An event without uid is always new; the DAO creates one on insert.
            val pairEntry = ImportPairEntry(read = read, stored = stored)
            error?.let { pairEntry.addError(it) }
            addEntry(pairEntry)
        }
        log.debug { "Reconciled ${pairEntries.size} ics import events against calendar #$calId." }
    }
}
