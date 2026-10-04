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
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.importer.AbstractImportJob
import org.projectforge.rest.importer.ImportPairEntry

private val log = KotlinLogging.logger {}

/**
 * Persists the selected events of an ics import into the storage's target calendar: an event with a stored
 * counterpart is updated by its id, any other is inserted. The DAO checks the write access per event, so an
 * event of a calendar the user may no longer write is refused (the Wicket import skipped that check).
 *
 * @author Kai Reinhard
 */
class TeamEventImportJob(
    private val teamCalDao: TeamCalDao,
    private val teamEventDao: TeamEventDao,
    private val selectedEntries: List<ImportPairEntry<TeamEventImportDTO>>,
    private val importStorage: TeamEventImportStorage,
) : AbstractImportJob(
    translateMsg("plugins.teamcal.import.ics.job.title", selectedEntries.size.toString()),
    area = "TeamEventImport",
    queueName = "teamEventImport",
    timeoutSeconds = 600,
    importStorage = importStorage,
    selectedEntries = selectedEntries,
) {

    init {
        totalNumber = selectedEntries.size
        processedNumber = 0
    }

    override fun onBeforeStart() {
        importStorage.reconcileImportStorage(rereadDatabaseEntries = true)
        // Mark all entries as not reconciled so they show status UNKNOWN until processed.
        importStorage.pairEntries.forEach { it.reconciled = false }
    }

    override fun onAfterTermination() {
        importStorage.reconcileImportStorage(rereadDatabaseEntries = true)
    }

    override suspend fun run() {
        importSelected { isActive }
    }

    /**
     * The import itself, apart from the coroutine: [active] is asked before each event, so a cancelled job
     * stops (and a test can run it directly).
     */
    internal fun importSelected(active: () -> Boolean = { true }) {
        val teamCalId = importStorage.teamCalId
        if (teamCalId == null || teamCalDao.writableCalendars.none { it.id == teamCalId }) {
            result.errorMessages = listOf(translate("plugins.teamcal.import.ics.error.noCalendar"))
            return
        }
        log.info { "Starting import of ${selectedEntries.size} events into calendar #$teamCalId." }
        val calendar = teamCalDao.find(teamCalId, checkAccess = false)
        val toPersist = mutableListOf<TeamEventDO>()
        for (entry in selectedEntries) {
            if (!active()) {
                return
            }
            // A copy, so the parsed event in the session stays untouched by the persisting (and a second
            // commit of the same upload starts clean). clone() leaves out the uid and the original ics entry.
            val parsed = entry.read?.event ?: continue
            val event = parsed.clone()
            event.uid = parsed.uid
            event.originalIcsEntry = parsed.originalIcsEntry
            event.calendar = calendar
            if (HibernateUtils.shortenProperties(TeamEventDO::class.java, event, "note", "location", "subject", "organizer")) {
                log.info { "Properties of the event were shortened: uid=${event.uid}" }
            }
            val storedId = entry.stored?.id
            if (storedId != null) {
                event.id = storedId
                result.updated += 1
            } else {
                result.inserted += 1
            }
            toPersist.add(event)
            processedNumber += 1
        }
        if (toPersist.isNotEmpty()) {
            teamEventDao.insertOrUpdate(toPersist, INSERT_BLOCK_SIZE, checkAccess = true)
        }
        log.info { "Import completed: inserted=${result.inserted}, updated=${result.updated}." }
    }

    override fun writeAccess(user: PFUserDO?): Boolean {
        return isOwner
    }

    companion object {
        private const val INSERT_BLOCK_SIZE = 50
    }
}
