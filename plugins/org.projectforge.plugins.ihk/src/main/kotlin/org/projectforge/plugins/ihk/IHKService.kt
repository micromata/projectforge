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

package org.projectforge.plugins.ihk

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.address.AddressDao
import org.projectforge.business.timesheet.OrderDirection
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetFilter
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private val log = KotlinLogging.logger {}

/**
 * Reads the apprentice's training settings and builds the weekly IHK training report (xlsx) from the
 * logged-in user's time sheets.
 *
 * The settings are a user pref ([IHKSettings]), edited on the IHK page. Earlier they were a JSON object in the
 * comment of the user's own address ([IHKCommentObject]); as long as the user has no pref yet, that JSON is taken
 * over once, so existing apprentices need not set anything up again.
 */
@Service
class IHKService {
    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var userPrefService: UserPrefService

    /** [settings] is null while the user has not set them up. */
    class SettingsResult(
        val settings: IHKSettings? = null,
        /** True if [settings] were just taken over from the comment of the user's address. */
        val migratedFromAddress: Boolean = false,
    )

    class Report(val filename: String, val content: ByteArray)

    fun loadSettings(): SettingsResult {
        userPrefService.getEntry(PREF_AREA, PREF_NAME, IHKSettings::class.java)?.let { settings ->
            return SettingsResult(settings.takeIf { it.ausbildungsbeginn != null })
        }
        val settings = readFromAddress() ?: return SettingsResult()
        log.info { "IHK-Plugin: settings taken over from the comment of the user's address." }
        saveSettings(settings)
        return SettingsResult(settings, migratedFromAddress = true)
    }

    fun saveSettings(settings: IHKSettings) {
        userPrefService.putEntry(PREF_AREA, PREF_NAME, settings)
    }

    /**
     * The settings of the former setup: a JSON object in the comment of the address that has exactly the user's
     * first and last name. Null if there is no such address or its comment holds no valid settings.
     */
    private fun readFromAddress(): IHKSettings? {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val firstname = user.firstname ?: return null
        val lastname = user.lastname ?: return null
        val comment = addressDao.select(BaseSearchFilter())
            .find { lastname == it.name && firstname == it.firstName }
            ?.comment
        if (comment.isNullOrBlank()) {
            return null
        }
        return try {
            val obj = JsonUtils.fromJson(comment, IHKCommentObject::class.java) ?: return null
            IHKSettings(
                ausbildungsbeginn = LocalDate.parse(obj.ausbildungStartDatum),
                // A missing value is parsed as 0, which means "calculate it" as -1 does.
                ausbildungsjahr = if (obj.ausbildungsjahr > 0) obj.ausbildungsjahr else IHKSettings.AUSBILDUNGSJAHR_AUTO,
                teamname = obj.teamname?.trim()?.ifEmpty { null },
            )
        } catch (ex: Exception) {
            log.info { "IHK-Plugin: the comment of the user's address holds no valid settings: ${ex.message}" }
            null
        }
    }

    /**
     * The logged-in user's time sheets of the week [day] lies in, ascending. The IHK reports are German, so the week
     * is always Monday to Sunday (in the user's time zone), whatever first day of week the user has set.
     */
    fun findTimesheets(day: LocalDate): List<TimesheetDO> {
        val startDate = PFDateTime.from(mondayOf(day)).beginOfDay
        val stopDate = startDate.plusDays(6).endOfDay
        val filter = TimesheetFilter()
        filter.orderType = OrderDirection.ASC
        filter.startTime = startDate.utilDate
        filter.stopTime = stopDate.utilDate
        filter.userId = ThreadLocalUserContext.requiredLoggedInUserId
        filter.isRecursive = true
        return timesheetDao.select(filter)
    }

    /**
     * Builds the report of the given time sheets, null if the exporter fails.
     */
    fun export(settings: IHKSettings, day: LocalDate, timesheets: List<TimesheetDO>): Report? {
        val ausbildungsbeginn = settings.ausbildungsbeginn ?: return null
        val exporter = IHKExporter()
        val xlsx = exporter.getExcel(
            timesheets, ausbildungsbeginn, settings.teamname, settings.ausbildungsjahr,
            ThreadLocalUserContext.timeZone,
        )
        val filename = "WB-Nr_${exporter.docNr}_${DateHelper.getDateAsFilenameSuffix(PFDateTime.from(mondayOf(day)).utilDate)}.xlsx"
        if (xlsx == null || xlsx.isEmpty()) {
            log.error { "Oups, xlsx has zero size. Filename: $filename" }
            return null
        }
        return Report(filename, xlsx)
    }

    companion object {
        private const val PREF_AREA = "ihk"
        private const val PREF_NAME = "settings"

        /** The Monday of the week [day] lies in: the IHK reports always run Monday to Sunday. */
        @JvmStatic
        fun mondayOf(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
}
