/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.privacyprotection

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.time.PFDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * You may define calendars, where entries older than n days will be deleted. This is useful e. g. for
 * calendars for illness days of employees.
 *
 * @author Kai Reinhard
 */
@Component
class PurgeCalendarEntries : IPrivacyProtectionJob {
    class CalendarEntry(var calendarId: Int? = null, var expiryDays: Int? = null)

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var purgeCronPrivacyProtectionJob: CronPrivacyProtectionJob

    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var teamEventDao: TeamEventDao

    @Value("\${projectforge.privacyProtection.purgeCalendars}")
    private var calendarEntriesConfig: String? = null

    private var calendarEntries: List<CalendarEntry>? = null

    @PostConstruct
    private fun postConstruct() {
        if (!calendarEntriesConfig.isNullOrBlank()) {
            try {
                val entries = JsonUtils.fromJson(calendarEntriesConfig, Array<CalendarEntry>::class.java)
                calendarEntries = entries?.toList()
                purgeCronPrivacyProtectionJob.register(this)
            } catch (ex: Exception) {
                log.error(
                    "Error in configuration of property 'projectforge.privacyProtection.purgeCalendars': ${ex.message}",
                    ex
                )
            }
        }
    }

    override fun execute() {
        if (calendarEntries.isNullOrEmpty()) {
            return // Nothing to do, no calendar defined.
        }
        log.info("Purge calendars...")
        calendarEntries!!.forEach {
            val calendar = teamCalDao.internalGetById(it.calendarId)
            val expiryDays = it.expiryDays ?: 0
            if (calendar == null) {
                log.error { "No calendar found with id #${it.calendarId}. Can't purge this calendar." }
            } else if (expiryDays <= 0) {
                log.error { "Expiry days property (${it.expiryDays}) of calendar #${it.calendarId} must be greater than 0. Can't purge this calendar." }
            } else {
                val expiryDate = PFDateTime.now().minusDays(expiryDays.toLong())
                log.info { "Purging calendar #${it.calendarId} '${calendar.title}' by deleting entries of ${it.expiryDays} or more days in the past (before ${expiryDate.isoString}Z)..." }
                persistenceService.runInTransaction { context ->
                    var counter = 0
                    val eventsToPurge = context.em
                        .createNamedQuery(TeamEventDO.SELECT_ENTRIES_IN_THE_PAST_TO_PURGE, TeamEventDO::class.java)
                        .setParameter("calendarId", calendar.id)
                        .setParameter("endDate", expiryDate.utilDate)
                        .resultList
                    eventsToPurge.forEach { event ->
                        ++counter
                        teamEventDao.internalForceDelete(event)
                    }
                    if (counter > 0) {
                        log.info("Removed $counter calendar entries in calendar #${it.calendarId} '${calendar.title}' in the past (before ${expiryDate.isoString}Z).")
                    } else {
                        log.info("No calendar event older than ${expiryDate.isoString}Z. Nothing to delete.")
                    }
                    true
                }
            }
        }
        log.info("Purging of calendars done.")
    }
}
