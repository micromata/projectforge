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

import mu.KotlinLogging
import org.projectforge.business.address.AddressDao
import org.projectforge.business.timesheet.OrderDirection
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetFilter
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
 * The settings are a JSON object in the comment of the user's own address (first and last name must match the
 * user exactly), see [IHKCommentObject] and the plugin's README.
 */
@Service
class IHKService {
    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    enum class SettingsErrorReason { NOT_FOUND, EMPTY, PARSING }

    class Settings(val ausbildungsbeginn: LocalDate, val ausbildungsjahr: Int, val teamname: String?)

    class SettingsError(
        val reason: SettingsErrorReason,
        /** The parser message, only for [SettingsErrorReason.PARSING]. */
        val detail: String? = null,
    )

    /** Either [settings] or [error] is given. [addressId] is the user's address, if found. */
    class SettingsResult(
        val settings: Settings? = null,
        val error: SettingsError? = null,
        val addressId: Long? = null,
    )

    class Report(val filename: String, val content: ByteArray)

    fun loadSettings(): SettingsResult {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val firstname = user.firstname
        val lastname = user.lastname
        // Addresses may have no name and/or no first name at all, so compare starting from the user's values.
        val address = if (firstname != null && lastname != null) {
            addressDao.select(BaseSearchFilter()).find { lastname == it.name && firstname == it.firstName }
        } else null
        if (address == null) {
            log.info { "IHK-Plugin: no address found for the user." }
            return SettingsResult(error = SettingsError(SettingsErrorReason.NOT_FOUND))
        }
        val comment = address.comment
        if (comment.isNullOrBlank()) {
            log.info { "IHK-Plugin: comment of the user's address is empty." }
            return SettingsResult(
                error = SettingsError(SettingsErrorReason.EMPTY),
                addressId = address.id,
            )
        }
        return try {
            val obj = JsonUtils.fromJson(comment, IHKCommentObject::class.java)!!
            SettingsResult(
                settings = Settings(
                    ausbildungsbeginn = LocalDate.parse(obj.ausbildungStartDatum),
                    ausbildungsjahr = obj.ausbildungsjahr,
                    teamname = obj.teamname,
                ),
                addressId = address.id,
            )
        } catch (ex: Exception) {
            log.warn { "IHK-Plugin: wasn't able to parse json from the comment of the user's address: ${ex.message}" }
            SettingsResult(
                error = SettingsError(SettingsErrorReason.PARSING, ex.message),
                addressId = address.id,
            )
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
    fun export(settings: Settings, day: LocalDate, timesheets: List<TimesheetDO>): Report? {
        val exporter = IHKExporter()
        val xlsx = exporter.getExcel(
            timesheets, settings.ausbildungsbeginn, settings.teamname, settings.ausbildungsjahr,
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
        /** The Monday of the week [day] lies in: the IHK reports always run Monday to Sunday. */
        @JvmStatic
        fun mondayOf(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
}
