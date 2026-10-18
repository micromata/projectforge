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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.availability.AbsenceKind
import org.projectforge.business.availability.AvailabilityDO
import org.projectforge.business.availability.AvailabilityDao
import org.projectforge.business.availability.AvailabilityStatus
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.teamcal.ical.ICalGenerator
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.projectforge.rest.calendar.AbsenceProvider
import org.projectforge.rest.calendar.CalendarSettings
import org.projectforge.rest.calendar.FullCalendarEvent
import org.projectforge.rest.dto.Availability
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.rest.dto.VacationExportRequest
import org.projectforge.rest.pub.CalendarSubscriptionServiceRest
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The rule set of AbsenceVisibility where the absences of others are shown: the calendar and its tooltips, the
 * subscription of a team calendar, the list and the form, the substitutes' absences in the form and the vacation
 * export. The past of somebody else's availability entry is told nowhere, its real begin neither.
 */
class AbsenceVisibilityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var absenceProvider: AbsenceProvider

    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Autowired
    private lateinit var availabilityEntityRest: AvailabilityEntityRest

    @Autowired
    private lateinit var calendarSubscriptionServiceRest: CalendarSubscriptionServiceRest

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var vacationDao: VacationDao

    @Autowired
    private lateinit var vacationEntityRest: VacationEntityRest

    @Autowired
    private lateinit var vacationExportRest: VacationExportRest

    private val today = LocalDate.now()

    @Test
    fun `the calendar shows a group mate's running entry from today on, until its end in the tooltip`() {
        val owner = createUser("calendar-owner")
        val colleague = createUser("calendar-colleague")
        groupOf("calendar", owner, colleague)
        logon(TEST_HR_USER)
        val pastId = insertAvailability(owner, today.minusDays(20), today.minusDays(6))
        val runningId = insertAvailability(owner, today.minusDays(5), today.plusDays(5))
        fun events(from: LocalDate = today.minusDays(30), until: LocalDate = today.plusDays(30)) =
            mutableListOf<FullCalendarEvent>().also { events ->
                absenceProvider.addEvents(
                    PFDateTime.from(from), PFDateTime.from(until), events, groupIds = null, userIds = setOf(owner.id),
                    kinds = AbsenceKind.entries.toSet(), settings = CalendarSettings(),
                )
            }

        logon(colleague)
        val event = events().single()
        assertEquals(runningId, event.extendedProps?.dbId, "Not the past one.")
        assertEquals(today, event.start?.day, "From today on.")
        val tooltip = event.extendedProps?.tooltip?.text!!
        assertTrue(tooltip.contains(translateMsg("absence.until", PFDay.from(today.plusDays(5)).format())), tooltip)
        assertFalse(tooltip.contains(PFDay.from(today.minusDays(5)).format()), "The real begin isn't told.")
        assertTrue(events(today.minusDays(30), today.minusDays(1)).isEmpty(), "Nothing of the past.")

        logon(owner)
        events().let { events ->
            assertEquals(listOf(pastId, runningId), events.map { it.extendedProps?.dbId }, "The own entries in full.")
            assertEquals(today.minusDays(5), events.last().start?.day)
            assertFalse(events.last().extendedProps?.tooltip?.text!!.contains(translateMsg("absence.until", "")))
        }
    }

    @Test
    fun `the subscription of a team calendar begins a group mate's running entry today`() {
        val owner = createUser("ics-owner")
        val colleague = createUser("ics-colleague")
        groupOf("ics", owner, colleague)
        logon(TEST_HR_USER)
        insertAvailability(owner, today.minusDays(20), today.minusDays(6))
        insertAvailability(owner, today.minusDays(5), today.plusDays(5))
        fun startDays(): List<String> {
            val generator = ICalGenerator()
            calendarSubscriptionServiceRest.addAbsences(
                generator, today.minusDays(30), today.plusDays(30), null, setOf(owner.id), AbsenceKind.entries.toSet(),
                mutableSetOf(),
            )
            val ics = generator.asByteArray?.let { String(it) }.orEmpty()
            return Regex("DTSTART;VALUE=DATE:(\\d{8})").findAll(ics).map { it.groupValues[1] }.toList()
        }

        logon(colleague)
        assertEquals(listOf(basic(today)), startDays())
        logon(owner)
        assertEquals(listOf(basic(today.minusDays(20)), basic(today.minusDays(5))), startDays())
    }

    @Test
    fun `list and form show a group mate's entry from today on, without its timestamps`() {
        val owner = createUser("list-owner")
        val colleague = createUser("list-colleague")
        groupOf("list", owner, colleague)
        logon(TEST_HR_USER)
        val id = insertAvailability(owner, today.minusDays(5), today.plusDays(5))

        logon(colleague)
        availabilityEntityRest.transformFromDB(availabilityDao.find(id)!!, editMode = false).let {
            assertEquals(today, it.startDate)
            assertEquals(today.plusDays(5), it.endDate)
            assertEquals(true, it.startHidden)
            assertNull(it.created, "The creation would tell the begin.")
            assertNull(it.lastUpdate)
            assertNull(it.type, "Personal.")
        }
        (availabilityEntityRest.getItem(id).body as Availability).let {
            assertEquals(today, it.startDate, "The form, too.")
            assertEquals(true, it.startHidden)
        }
        logon(owner)
        availabilityEntityRest.transformFromDB(availabilityDao.find(id)!!, editMode = false).let {
            assertEquals(today.minusDays(5), it.startDate)
            assertNull(it.startHidden)
            assertNotNull(it.created)
        }
    }

    @Test
    fun `the list filters and sorts a group mate's entry by the begin shown, not the real one`() {
        val owner = createUser("filter-owner")
        val other = createUser("filter-other")
        val colleague = createUser("filter-colleague")
        groupOf("filter", owner, other, colleague)
        logon(TEST_HR_USER)
        // Both shown from today on: the earlier real begin first in the database's order.
        val earlierId = insertAvailability(other, today.minusDays(10), today.plusDays(5))
        val laterId = insertAvailability(owner, today.minusDays(5), today.plusDays(5))
        fun ids(until: LocalDate?, sorted: Boolean = false): List<Long?> {
            val filter = MagicFilter()
            filter.entries.add(MagicFilterEntry("period").also {
                it.value.fromValue = today.minusDays(30).toString()
                it.value.toValue = until?.toString()
            })
            if (sorted) {
                filter.sortProperties.add(SortProperty("startDate"))
            }
            return availabilityEntityRest.getFilteredList(filter).map { it.id }
                .filter { it == earlierId || it == laterId }
        }

        logon(colleague)
        assertEquals(emptyList<Long>(), ids(today.minusDays(1)), "A period of the past tells no begin.")
        assertEquals(setOf(earlierId, laterId), ids(today).toSet())
        assertEquals(listOf(laterId, earlierId), ids(null, sorted = true), "Equal begins shown: by the id.")
        logon(TEST_HR_USER)
        assertEquals(setOf(earlierId, laterId), ids(today.minusDays(1)).toSet(), "HR staff: by the real begin.")
        assertEquals(listOf(earlierId, laterId), ids(null, sorted = true))
    }

    @Test
    fun `the vacation list filters a group mate's vacation by the begin shown`() {
        val owner = createEmployee("vacation-filter-owner")
        val manager = createEmployee("vacation-filter-manager")
        val colleague = createEmployee("vacation-filter-colleague")
        groupOf("vacation-filter", owner.user!!, colleague.user!!)
        val newYear = LocalDate.of(today.year, 1, 1)
        logon(TEST_HR_USER)
        val id = vacationDao.insert(VacationDO().also {
            it.employee = owner
            it.manager = manager
            it.replacement = manager
            it.startDate = newYear.minusDays(5)
            it.endDate = newYear.plusDays(1)
            it.status = VacationStatus.APPROVED
            it.special = true // No leave days needed.
        })
        // The list shows the colleague the vacations of the substitutes of an own one overlapping it
        // (VacationVisibilityFilter).
        vacationDao.insert(VacationDO().also {
            it.employee = colleague
            it.manager = manager
            it.replacement = owner
            it.startDate = newYear.minusDays(5)
            it.endDate = newYear.plusDays(1)
            it.status = VacationStatus.APPROVED
            it.special = true // No leave days needed.
        })
        fun ids(configure: MagicFilter.() -> Unit): List<Long?> {
            val filter = MagicFilter()
            filter.configure()
            return vacationEntityRest.getFilteredList(filter).map { it.id }.filter { it == id }
        }
        val lastYear: MagicFilter.() -> Unit = {
            entries.add(MagicFilterEntry("year", (today.year - 1).toString()))
        }
        val untilLastYear: MagicFilter.() -> Unit = {
            entries.add(MagicFilterEntry("period").also { it.value.toValue = newYear.minusDays(1).toString() })
        }

        logon(colleague.user!!)
        assertEquals(emptyList<Long>(), ids(lastYear), "Not of last year: shown from 1 January on.")
        assertEquals(emptyList<Long>(), ids(untilLastYear))
        assertEquals(listOf(id), ids { entries.add(MagicFilterEntry("year", today.year.toString())) })
        logon(manager.user!!)
        assertEquals(listOf(id), ids(lastYear), "The manager: by the real begin.")
        assertEquals(listOf(id), ids(untilLastYear))
    }

    @Test
    fun `list and form show a group mate's vacation from 1 January on`() {
        val owner = createEmployee("vacation-owner")
        val manager = createEmployee("vacation-manager")
        val colleague = createUser("vacation-colleague")
        groupOf("vacation", owner.user!!, colleague)
        val newYear = LocalDate.of(today.year, 1, 1)
        logon(TEST_HR_USER)
        val id = vacationDao.insert(VacationDO().also {
            it.employee = owner
            it.manager = manager
            it.replacement = manager
            it.startDate = newYear.minusDays(5)
            it.endDate = newYear.plusDays(1)
            it.halfDayBegin = true
            it.status = VacationStatus.APPROVED
            it.special = true
        })

        logon(colleague)
        vacationEntityRest.transformFromDB(vacationDao.find(id)!!, editMode = false).let {
            assertEquals(newYear, it.startDate)
            assertEquals(true, it.startHidden)
            assertEquals(false, it.halfDayBegin, "The half day was the real begin's.")
        }
        logon(manager.user!!)
        vacationEntityRest.transformFromDB(vacationDao.find(id)!!, editMode = false).let {
            assertEquals(newYear.minusDays(5), it.startDate, "The manager: in full.")
            assertNull(it.startHidden)
        }
    }

    @Test
    fun `the substitutes' entries in the form from today on, no uncovered day of the past drawn by them`() {
        val own = createUser("substitutes-own") // A freelancer: no employee, no group shared with the substitute.
        val substitute = createUser("substitutes-substitute")
        logon(TEST_HR_USER)
        insertAvailability(substitute, today.minusDays(10), today.plusDays(4))
        val dto = Availability().also {
            it.user = User().also { user -> user.id = own.id }
            it.startDate = today.minusDays(10)
            it.endDate = today.plusDays(4)
            it.status = AvailabilityStatus.NOT_AVAILABLE
            it.substitutes = listOf(User().also { user -> user.id = substitute.id })
        }
        val asHr = availabilityEntityRest.stats(PostData(dto, null, null))
        assertEquals(today.minusDays(10), asHr.substituteAvailabilities!!.single().startDate, "HR staff: in full.")
        val uncoveredAsHr = asHr.chartDays!!.filter { it.uncovered }.map { it.date }

        logon(own)
        dto.user = User().also { it.id = own.id }
        val stats = availabilityEntityRest.stats(PostData(dto, null, null))
        assertEquals(true, stats.conflict, "Calculated with all of it.")
        val shown = stats.substituteAvailabilities!!.single()
        assertEquals(today, shown.startDate, "From today on.")
        assertEquals(today.plusDays(4), shown.endDate)
        val uncovered = stats.chartDays!!.filter { it.uncovered }.map { it.date }
        assertEquals(uncoveredAsHr.filter { !it.isBefore(today) }, uncovered)
        assertTrue(uncoveredAsHr.size > uncovered.size, "The days of the past are drawn for HR staff only.")
    }

    @Test
    fun `the conflict of a past period isn't told by the substitutes' hidden entries`() {
        val own = createUser("conflict-own")
        val substitute = createUser("conflict-substitute")
        logon(TEST_HR_USER)
        insertAvailability(substitute, today.minusDays(10), today.minusDays(5))
        val dto = Availability().also {
            it.user = User().also { user -> user.id = own.id }
            it.startDate = today.minusDays(8)
            it.endDate = today.minusDays(7)
            it.status = AvailabilityStatus.NOT_AVAILABLE
            it.substitutes = listOf(User().also { user -> user.id = substitute.id })
        }
        assertEquals(true, availabilityEntityRest.stats(PostData(dto, null, null)).conflict, "HR staff: all of it.")

        logon(own)
        val stats = availabilityEntityRest.stats(PostData(dto, null, null))
        assertEquals(false, stats.conflict, "Would tell the substitute was away (sick) then.")
        assertTrue(stats.substituteAvailabilities.isNullOrEmpty())
        assertTrue(stats.chartDays!!.none { it.uncovered })
    }

    @Test
    fun `the vacation export shows a group mate's availability entry from today on`() {
        val own = createEmployee("export-own")
        val colleague = createEmployee("export-colleague")
        groupOf("export", own.user!!, colleague.user!!)
        logon(TEST_HR_USER)
        insertAvailability(colleague.user!!, today.minusDays(5), today.plusDays(5))
        val request = VacationExportRequest(
            employeeIds = listOf(colleague.id!!), from = today.minusDays(20), until = today.plusDays(20),
            absenceKinds = AbsenceKind.entries,
        )
        // The period of the export begins with 1 January for anybody but HR and finance staff.
        val from = maxOf(today.minusDays(5), LocalDate.of(today.year, 1, 1))

        logon(own.user!!)
        assertEquals(today, vacationExportRest.view(request).rows.single().availabilities.single().startDate)
        logon(TEST_HR_USER)
        assertEquals(from, vacationExportRest.view(request).rows.single().availabilities.single().startDate)
    }

    private fun basic(date: LocalDate): String = date.format(DateTimeFormatter.BASIC_ISO_DATE)

    private fun insertAvailability(user: PFUserDO, startDate: LocalDate, endDate: LocalDate): Long =
        availabilityDao.insert(AvailabilityDO().also {
            it.user = user
            it.startDate = startDate
            it.endDate = endDate
            it.status = AvailabilityStatus.NOT_AVAILABLE
            it.type = "Sickness"
        })

    private fun groupOf(name: String, vararg users: PFUserDO) {
        logon(TEST_ADMIN_USER)
        initTestDB.addGroup("$PREFIX.$name", *users.map { it.username!! }.toTypedArray())
    }

    private fun createEmployee(name: String): EmployeeDO {
        logon(TEST_ADMIN_USER)
        val employee = EmployeeDO()
        employee.user = createUser(name)
        employeeDao.insert(employee, checkAccess = false)
        return employee
    }

    private fun createUser(name: String): PFUserDO {
        val user = PFUserDO()
        user.firstname = "$PREFIX.$name"
        user.lastname = "$name.lastname"
        user.username = "$PREFIX.$name.username"
        return initTestDB.addUser(user)
    }

    companion object {
        private const val PREFIX = "AbsenceVisibilityRestTest"
    }
}
