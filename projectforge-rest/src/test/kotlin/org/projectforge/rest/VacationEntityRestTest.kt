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
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.business.vacation.service.ConflictingVacationsCache
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.api.SortOrder
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.core.AbstractEntityRest
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Vacation
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import java.math.BigDecimal
import java.time.LocalDate

class VacationEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var vacationEntityRest: VacationEntityRest

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var vacationDao: VacationDao

    @Autowired
    private lateinit var conflictingVacationsCache: ConflictingVacationsCache

    @Test
    fun `a new entry is of the own employee unless HR staff presets another one`() {
        val own = createEmployee("new-own")
        val other = createEmployee("new-other")
        val hr = createEmployee("new-hr", hrAccess = true)
        val request = MockHttpServletRequest().also { it.addParameter("employee", "${other.id}") }

        logon(own.user!!)
        vacationEntityRest.newBaseDTO(request).let {
            assertEquals(own.id, it.employee?.id, "The preset of somebody else is ignored for non HR staff.")
            assertEquals(false, it.employeeEditable)
            assertEquals(VacationStatus.IN_PROGRESS, it.status)
            assertFalse(it.allowedStatus!!.contains(VacationStatus.APPROVED), "The own entry can't be approved.")
        }

        logon(hr.user!!)
        vacationEntityRest.newBaseDTO(request).let {
            assertEquals(other.id, it.employee?.id)
            assertEquals(true, it.employeeEditable)
            assertEquals(VacationStatus.entries.toSet(), it.allowedStatus!!.toSet())
        }
    }

    @Test
    fun `manager and replacement are required`() {
        logon(TEST_ADMIN_USER)
        val errors = mutableListOf<ValidationError>()
        vacationEntityRest.validate(errors, Vacation())
        assertEquals(setOf("manager", "replacement"), errors.mapNotNull { it.fieldId }.toSet())
    }

    @Test
    fun `the stats follow the form and tell the days left of the own employee only`() {
        val own = createEmployee("stats-own")
        val other = createEmployee("stats-other")
        val manager = createEmployee("stats-manager")
        val hr = createEmployee("stats-hr", hrAccess = true)
        // A week of February without holidays, far enough in the future.
        val monday = LocalDate.of(LocalDate.now().year + 3, 2, 4).let { it.plusDays((8L - it.dayOfWeek.value) % 7) }
        val friday = monday.plusDays(4)
        logon(hr.user!!)
        vacationDao.insert(VacationDO().also {
            it.employee = other
            it.manager = manager
            it.replacement = own
            it.startDate = monday
            it.endDate = friday
            it.status = VacationStatus.APPROVED
        })
        val form = Vacation(
            employee = Employee(other.id),
            startDate = monday,
            endDate = friday,
            halfDayEnd = true,
            manager = Employee(manager.id),
            replacement = Employee(own.id),
        )
        val leftOfOther = vacationEntityRest.stats(PostData(form, watchFieldsTriggered = null, serverData = null)).vacationDaysLeftInYear
        // The own week with the only substitute away all of it: charted with three days around, the five uncovered.
        val chart = vacationEntityRest.stats(
            PostData(
                Vacation(
                    employee = Employee(own.id), startDate = monday, endDate = friday,
                    manager = Employee(manager.id), replacement = Employee(other.id),
                ), watchFieldsTriggered = null, serverData = null
            )
        )
        val days = chart.chartDays!!
        assertEquals(monday.minusDays(3), days.first().date)
        assertEquals(friday.plusDays(3), days.last().date)
        assertEquals(11, days.size)
        assertEquals((0L..4L).map { monday.plusDays(it) }, days.filter { it.uncovered }.map { it.date })
        assertFalse(days.first { it.date == monday.minusDays(1) }.workingDay, "Sunday")
        assertTrue(chart.conflict == true)
        assertEquals(listOf(monday), chart.conflictingVacations!!.map { it.startDate }, "Not trimmed")

        logon(own.user!!)
        val stats = vacationEntityRest.stats(PostData(form, watchFieldsTriggered = null, serverData = null))
        assertEquals(0, BigDecimal("4.5").compareTo(stats.workingDays), "Half a day less: ${stats.workingDays}")
        assertNotEquals(leftOfOther, stats.vacationDaysLeftInYear, "Days left of somebody else are not told.")
        assertTrue(stats.allowedStatus!!.isNotEmpty())
    }

    @Test
    fun `the year filter applies together with the employee filter`() {
        val own = createEmployee("year-own")
        val manager = createEmployee("year-manager")
        val hr = createEmployee("year-hr", hrAccess = true)
        logon(hr.user!!)
        val year = LocalDate.now().year + 4
        listOf(year - 1, year).forEach { y ->
            vacationDao.insert(VacationDO().also {
                it.employee = own
                it.manager = manager
                it.replacement = manager
                // A Tuesday in March: a working day, no holiday.
                val day = LocalDate.of(y, 3, 4).let { d -> d.plusDays((9L - d.dayOfWeek.value) % 7) }
                it.startDate = day
                it.endDate = day
                it.status = VacationStatus.IN_PROGRESS
                it.special = true
            })
        }
        val filter = MagicFilter()
        filter.entries.add(MagicFilterEntry("employee").also { it.value.id = own.id })
        // As the client sends a text filter: wrapped in the wildcards of a LIKE search.
        filter.entries.add(MagicFilterEntry("year", "*$year*"))
        val result = vacationEntityRest.listPage(
            MockHttpServletRequest(), AbstractEntityRest.ListPageRequest(filter = filter, refresh = true)
        )
        assertEquals(listOf(year), result.resultSet.map { (it as Vacation).startDate?.year })
    }

    @Test
    fun `the period filter finds the vacations overlapping it by their dates`() {
        val own = createEmployee("period-own")
        val manager = createEmployee("period-manager")
        val hr = createEmployee("period-hr", hrAccess = true)
        logon(hr.user!!)
        val year = LocalDate.now().year + 6
        fun insert(start: LocalDate, end: LocalDate) = vacationDao.insert(VacationDO().also {
            it.employee = own
            it.manager = manager
            it.replacement = manager
            it.startDate = start
            it.endDate = end
            it.status = VacationStatus.IN_PROGRESS
            it.special = true
        })
        val overYears = insert(LocalDate.of(year - 1, 12, 20), LocalDate.of(year, 1, 10))
        val inYear = insert(LocalDate.of(year, 6, 2), LocalDate.of(year, 6, 13))
        val nextYear = insert(LocalDate.of(year + 1, 6, 2), LocalDate.of(year + 1, 6, 13))
        fun ids(from: String?, to: String?): Set<Long?> {
            val filter = MagicFilter()
            filter.entries.add(MagicFilterEntry("employee").also { it.value.id = own.id })
            filter.entries.add(MagicFilterEntry("period").also { it.value.fromValue = from; it.value.toValue = to })
            return vacationEntityRest.listPage(
                MockHttpServletRequest(), AbstractEntityRest.ListPageRequest(filter = filter, refresh = true)
            ).resultSet.map { (it as Vacation).id }.toSet()
        }
        assertEquals(setOf(overYears, inYear), ids("$year-01-01", "$year-12-31"), "Begun the year before, too.")
        assertEquals(setOf(inYear, nextYear), ids("$year-06-13", null), "Open end.")
        assertEquals(setOf(overYears), ids(null, "$year-01-01"), "Open begin.")
    }

    @Test
    fun `the list shows a normal user the own vacations and the overlapping ones of their substitutes only`() {
        val own = createEmployee("visible-own")
        val substitute = createEmployee("visible-substitute")
        val colleague = createEmployee("visible-colleague")
        val manager = createEmployee("visible-manager")
        val finance = createEmployee("visible-finance", finance = true)
        val hr = createEmployee("visible-hr", hrAccess = true)
        logon(TEST_ADMIN_USER)
        // One group: the read right alone would let everybody see the others' current vacations.
        initTestDB.addGroup(
            "${javaClass.simpleName}.visible",
            *listOf(own, substitute, colleague, manager, finance).map { it.user!!.username!! }.toTypedArray(),
        )
        logon(hr.user!!)
        val year = LocalDate.now().year + 2
        fun insert(
            employee: EmployeeDO,
            replacement: EmployeeDO,
            start: LocalDate,
            end: LocalDate,
            manager: EmployeeDO = replacement,
            status: VacationStatus = VacationStatus.APPROVED,
        ) = vacationDao.insert(vacation(employee, replacement, start, end).also {
            it.manager = manager
            it.status = status
        })
        val mine = insert(own, substitute, LocalDate.of(year, 12, 1), LocalDate.of(year, 12, 5), manager)
        val substituteOverlapping = insert(substitute, colleague, LocalDate.of(year, 12, 4), LocalDate.of(year, 12, 8))
        val substituteOther = insert(substitute, colleague, LocalDate.of(year, 12, 15), LocalDate.of(year, 12, 16))
        val colleagueReplacedByMe = insert(colleague, own, LocalDate.of(year, 12, 10), LocalDate.of(year, 12, 11))
        val colleagueManagedByMe =
            insert(colleague, substitute, LocalDate.of(year, 12, 20), LocalDate.of(year, 12, 21), own)
        val colleagueOther = insert(colleague, manager, LocalDate.of(year, 12, 8), LocalDate.of(year, 12, 9))
        // Rejected: doesn't reveal the leave of its substitute (the colleague) overlapping it.
        val rejected =
            insert(own, colleague, LocalDate.of(year, 12, 8), LocalDate.of(year, 12, 9), status = VacationStatus.REJECTED)
        fun ids(user: EmployeeDO): Set<Long?> {
            logon(user.user!!)
            val filter = MagicFilter()
            filter.entries.add(MagicFilterEntry("period").also {
                it.value.fromValue = "$year-12-01"
                it.value.toValue = "$year-12-31"
            })
            return vacationEntityRest.listPage(
                MockHttpServletRequest(), AbstractEntityRest.ListPageRequest(filter = filter, refresh = true)
            ).resultSet.map { (it as Vacation).id }.toSet()
        }
        // Not the substitute's vacation after the own one, nor the colleague's of the same group only.
        assertEquals(
            setOf(mine, rejected, substituteOverlapping, colleagueReplacedByMe, colleagueManagedByMe),
            ids(own),
        )
        assertTrue(ids(finance).containsAll(listOf(substituteOther, colleagueOther)), "Finance staff see the group.")
    }

    @Test
    fun `the days left after this entry count it as edited instead of as stored`() {
        val own = createEmployee("after-own")
        val manager = createEmployee("after-manager")
        val hr = createEmployee("after-hr", hrAccess = true)
        logon(hr.user!!)
        // Weeks of March without holidays, in a future year (no carry from the year before).
        val monday = LocalDate.of(LocalDate.now().year + 3, 3, 4).let { it.plusDays((8L - it.dayOfWeek.value) % 7) }
        val stored = vacationDao.insert(VacationDO().also {
            it.employee = own
            it.manager = manager
            it.replacement = manager
            it.startDate = monday
            it.endDate = monday.plusDays(4)
            it.status = VacationStatus.APPROVED
        })
        fun stats(id: Long?, start: LocalDate, end: LocalDate, special: Boolean? = null) = vacationEntityRest.stats(
            PostData(
                Vacation(
                    employee = Employee(own.id), startDate = start, endDate = end, special = special,
                    manager = Employee(manager.id), replacement = Employee(manager.id),
                ).also { it.id = id; it.status = VacationStatus.IN_PROGRESS }, watchFieldsTriggered = null, serverData = null
            )
        )
        val nextWeek = monday.plusDays(7)
        stats(null, nextWeek, nextWeek.plusDays(4)).let {
            assertEquals(0, BigDecimal(25).compareTo(it.vacationDaysLeftInYear), "${it.vacationDaysLeftInYear}")
            assertEquals(0, BigDecimal(20).compareTo(it.vacationDaysLeftAfterThis), "A new entry counts on top.")
        }
        stats(stored, monday, monday.plusDays(3)).let {
            assertEquals(0, BigDecimal(26).compareTo(it.vacationDaysLeftAfterThis), "The stored state replaced.")
        }
        stats(stored, monday, monday.plusDays(4)).let {
            assertEquals(0, it.vacationDaysLeftInYear!!.compareTo(it.vacationDaysLeftAfterThis), "Unchanged.")
        }
        stats(null, nextWeek, nextWeek.plusDays(4), special = true).let {
            assertEquals(0, BigDecimal(25).compareTo(it.vacationDaysLeftAfterThis), "Special leave isn't counted.")
        }
    }

    @Test
    fun `the list sorts by the working days and by the status as displayed`() {
        val own = createEmployee("sort-own")
        val manager = createEmployee("sort-manager")
        val hr = createEmployee("sort-hr", hrAccess = true)
        logon(hr.user!!)
        val year = LocalDate.now().year + 7
        fun insert(month: Int, days: Long, status: VacationStatus) = vacationDao.insert(VacationDO().also {
            it.employee = own
            it.manager = manager
            it.replacement = manager
            it.startDate = LocalDate.of(year, month, 3).let { d -> d.plusDays((8L - d.dayOfWeek.value) % 7) }
            it.endDate = it.startDate!!.plusDays(days - 1)
            it.status = status
            it.special = true
        })
        val long = insert(3, 5, VacationStatus.REJECTED)
        val short = insert(5, 2, VacationStatus.APPROVED)
        val middle = insert(7, 3, VacationStatus.IN_PROGRESS)
        fun sorted(property: String): List<Long?> {
            val filter = MagicFilter()
            filter.entries.add(MagicFilterEntry("employee").also { it.value.id = own.id })
            filter.entries.add(MagicFilterEntry("period").also { it.value.fromValue = "$year-01-01"; it.value.toValue = "$year-12-31" })
            filter.sortProperties.add(SortProperty(property, SortOrder.ASCENDING))
            return vacationEntityRest.listPage(
                MockHttpServletRequest(), AbstractEntityRest.ListPageRequest(filter = filter, refresh = true)
            ).resultSet.map { (it as Vacation).id }
        }
        assertEquals(listOf(short, middle, long), sorted("workingDaysFormatted"))
        val byStatus = listOf(long to VacationStatus.REJECTED, short to VacationStatus.APPROVED, middle to VacationStatus.IN_PROGRESS)
            .sortedBy { translate(it.second.i18nKey) }.map { it.first }
        assertEquals(byStatus, sorted("status"))
    }

    @Test
    fun `the list starts with the current calendar year`() {
        val period = vacationEntityRest.newMagicFilter().entries.single { it.field == "period" }.value
        val year = LocalDate.now().year
        assertEquals("$year-01-01", period.fromValue)
        assertEquals("$year-12-31", period.toValue)
        assertEquals("year", period.periodKind)
    }

    /** An employee with a user of its own and 30 days of annual leave; HR staff with the HR vacation right. */
    @Test
    fun `the list tells the weekends and the days without any substitute on duty`() {
        val own = createEmployee("bar-own")
        val other = createEmployee("bar-other")
        val hr = createEmployee("bar-hr", hrAccess = true)
        // Monday to Sunday of a week of February, years ahead; the substitute is away Wednesday to Friday.
        val monday = LocalDate.of(LocalDate.now().year + 4, 2, 4).let { it.plusDays((8L - it.dayOfWeek.value) % 7) }
        logon(hr.user!!)
        val ownId = vacationDao.insert(vacation(own, replacement = other, monday, monday.plusDays(6)))
        val otherId = vacationDao.insert(vacation(other, replacement = hr, monday.plusDays(2), monday.plusDays(4)))
        conflictingVacationsCache.forceReload()

        // Through the list: its rows get their uncovered days all at once (postProcessResultSet).
        val filter = MagicFilter()
        filter.entries.add(MagicFilterEntry("period").also {
            it.value.fromValue = monday.toString(); it.value.toValue = monday.plusDays(6).toString()
        })
        val rows = vacationEntityRest.listPage(
            MockHttpServletRequest(), AbstractEntityRest.ListPageRequest(filter = filter, refresh = true)
        ).resultSet.map { it as Vacation }.associateBy { it.id }
        val listed = rows[ownId]!!
        assertEquals(true, listed.conflict)
        assertEquals(listOf(monday.plusDays(5), monday.plusDays(6)), listed.nonWorkingDays)
        assertEquals((2L..4L).map { monday.plusDays(it) }, listed.uncoveredDays)
        val free = rows[otherId]!!
        assertEquals(null, free.uncoveredDays, "No conflict: the substitute of the other is on duty.")
        assertEquals(emptyList<LocalDate>(), free.nonWorkingDays)
    }

    @Test
    fun `a substitute sees neither the days left of the entry's employee nor the comments of other entries`() {
        val own = createEmployee("privacy-own")
        val other = createEmployee("privacy-other")
        val hr = createEmployee("privacy-hr", hrAccess = true)
        val monday = LocalDate.of(LocalDate.now().year + 5, 2, 4).let { it.plusDays((8L - it.dayOfWeek.value) % 7) }
        logon(hr.user!!)
        // The entry of the other, the own employee substituting; the hr employee, the own one's substitute, away too.
        // The helper's manager is the replacement: the manager may see the days left, the substitute alone not.
        val otherId = vacationDao.insert(vacation(other, replacement = own, monday, monday.plusDays(4)).also {
            it.manager = hr
        })
        vacationDao.insert(vacation(hr, replacement = other, monday, monday.plusDays(1)).also {
            it.comment = "private"
        })
        val asHr = vacationEntityRest.stats(PostData(Vacation(vacationDao.find(otherId)!!).also {
            it.replacement = Employee(hr.id)
        }, watchFieldsTriggered = null, serverData = null))
        assertNotNull(asHr.vacationDaysLeftInYear, "HR staff see the days left.")
        assertEquals(listOf<String?>(null), asHr.conflictingVacations!!.map { it.comment }, "No comment of the others.")

        logon(own.user!!)
        val stored = vacationDao.find(otherId)!!
        val stats = vacationEntityRest.stats(PostData(Vacation(stored), watchFieldsTriggered = null, serverData = null))
        assertNull(stats.vacationDaysLeftInYear, "The substitute doesn't see the remaining leave of the other.")
        assertNull(stats.vacationDaysLeftAfterThis)
        val opened = vacationEntityRest.transformFromDB(stored, editMode = true)
        assertNull(opened.vacationDaysLeftInYear)
    }

    @Test
    fun `the stats show a normal user the vacations of any substitute as conflicts, of strangers reduced`() {
        val own = createEmployee("leak-own")
        val colleague = createEmployee("leak-colleague")
        val stranger = createEmployee("leak-stranger")
        val hr = createEmployee("leak-hr", hrAccess = true)
        logon(TEST_ADMIN_USER)
        initTestDB.addGroup("VacationEntityRestTest.leak", own.user!!.username!!, colleague.user!!.username!!)
        val monday = LocalDate.of(LocalDate.now().year + 8, 2, 4).let { it.plusDays((8L - it.dayOfWeek.value) % 7) }
        val friday = monday.plusDays(4)
        // Long past: before the 31 days a colleague's vacation is visible.
        val pastMonday = LocalDate.now().minusDays(120).let { it.plusDays((8L - it.dayOfWeek.value) % 7) }
        val pastFriday = pastMonday.plusDays(4)
        logon(hr.user!!)
        vacationDao.insert(vacation(colleague, replacement = hr, monday, friday))
        vacationDao.insert(
            vacation(stranger, replacement = hr, monday, friday).also {
                it.comment = "private"
                it.halfDayEnd = true
            })
        // Outside the period asked for.
        vacationDao.insert(vacation(stranger, replacement = hr, monday.plusDays(14), friday.plusDays(14)))
        vacationDao.insert(vacation(stranger, replacement = hr, pastMonday, pastFriday))
        fun stats(replacement: EmployeeDO, start: LocalDate = monday, end: LocalDate = friday) = vacationEntityRest.stats(
            PostData(
                Vacation(
                    employee = Employee(own.id), startDate = start, endDate = end,
                    manager = Employee(hr.id), replacement = Employee(replacement.id),
                ), watchFieldsTriggered = null, serverData = null
            )
        )
        // HR staff see all of them.
        assertEquals(1, stats(stranger).conflictingVacations!!.size)

        logon(own.user!!)
        // The colleague of an own group, away in the coming week: shown.
        stats(colleague).let {
            assertEquals(1, it.conflictingVacations!!.size)
            assertEquals(true, it.conflict)
            assertEquals(5, it.chartDays!!.count { day -> day.uncovered })
        }
        // Anybody else named as substitute: still a conflict, but only their dates and status are told.
        stats(stranger).let {
            assertEquals(true, it.conflict)
            assertEquals(5, it.chartDays!!.count { day -> day.uncovered })
            val reduced = it.conflictingVacations!!.single()
            assertEquals(stranger.id, reduced.employee?.id)
            assertEquals(monday, reduced.startDate)
            assertEquals(friday, reduced.endDate)
            assertEquals(true, reduced.halfDayEnd)
            assertEquals(VacationStatus.APPROVED, reduced.status)
            assertNull(reduced.id)
            assertNull(reduced.comment)
            assertNull(reduced.manager)
            assertNull(reduced.replacement)
            assertNull(reduced.special)
        }
        // Their vacation long past isn't told, though it still is a conflict of the entry.
        stats(stranger, pastMonday, pastFriday).let {
            assertTrue(it.conflictingVacations!!.isEmpty())
            assertEquals(true, it.conflict)
            assertEquals(0, it.chartDays!!.count { day -> day.uncovered })
        }
        // Nor does a period longer than the chart draws.
        stats(colleague, monday.minusDays(500), friday).let {
            assertNull(it.conflictingVacations)
            assertNull(it.chartDays)
        }
    }

    @Test
    fun `opening a rejected entry doesn't mark it as a conflict`() {
        val own = createEmployee("rejected-own")
        val other = createEmployee("rejected-other")
        val hr = createEmployee("rejected-hr", hrAccess = true)
        val monday = LocalDate.of(LocalDate.now().year + 6, 2, 4).let { it.plusDays((8L - it.dayOfWeek.value) % 7) }
        logon(hr.user!!)
        val ownId = vacationDao.insert(vacation(own, replacement = other, monday, monday.plusDays(4)).also {
            it.status = VacationStatus.REJECTED
        })
        vacationDao.insert(vacation(other, replacement = hr, monday, monday.plusDays(4)))
        conflictingVacationsCache.forceReload()

        val opened = vacationEntityRest.transformFromDB(vacationDao.find(ownId)!!, editMode = true)
        assertEquals(true, opened.conflict, "The form still tells the days without any substitute.")
        assertFalse(conflictingVacationsCache.hasConflict(ownId), "But the cache of the current conflicts isn't touched.")
    }

    @Test
    fun `saving and rejecting an entry update the conflicts at once`() {
        val own = createEmployee("save-own")
        val other = createEmployee("save-other")
        val hr = createEmployee("save-hr", hrAccess = true)
        val monday = LocalDate.of(LocalDate.now().year + 7, 2, 4).let { it.plusDays((8L - it.dayOfWeek.value) % 7) }
        logon(hr.user!!)
        val ownId = vacationDao.insert(vacation(own, replacement = other, monday, monday.plusDays(4)))
        conflictingVacationsCache.forceReload()
        assertFalse(conflictingVacationsCache.hasConflict(ownId))

        // The substitute's vacation makes a conflict of the own one, without anybody opening it.
        val otherId = vacationDao.insert(vacation(other, replacement = hr, monday, monday.plusDays(4)))
        assertTrue(conflictingVacationsCache.hasConflict(ownId))

        val rejected = vacationDao.find(otherId)!!.also { it.status = VacationStatus.REJECTED }
        vacationDao.update(rejected)
        assertFalse(conflictingVacationsCache.hasConflict(ownId), "The substitute is on duty again.")
    }

    @Test
    fun `a running vacation counts its days already past as its opening does`() {
        val own = createEmployee("running-own")
        val other = createEmployee("running-other")
        val hr = createEmployee("running-hr", hrAccess = true)
        val today = LocalDate.now()
        logon(hr.user!!)
        val ownId = vacationDao.insert(vacation(own, replacement = other, today.minusDays(10), today.plusDays(10)))
        // The substitute was away in the first days only, and is back since.
        vacationDao.insert(vacation(other, replacement = hr, today.minusDays(10), today.minusDays(5)))
        conflictingVacationsCache.forceReload()
        assertTrue(conflictingVacationsCache.hasConflict(ownId))
        val opened = vacationEntityRest.transformFromDB(vacationDao.find(ownId)!!, editMode = true)
        assertEquals(true, opened.conflict)
    }

    private fun vacation(
        employee: EmployeeDO,
        replacement: EmployeeDO,
        startDate: LocalDate,
        endDate: LocalDate,
    ) = VacationDO().also {
        it.employee = employee
        it.manager = replacement
        it.replacement = replacement
        it.startDate = startDate
        it.endDate = endDate
        it.status = VacationStatus.APPROVED
        it.special = true
    }

    private fun createEmployee(name: String, hrAccess: Boolean = false, finance: Boolean = false): EmployeeDO {
        val loggedInUser = ThreadLocalUserContext.loggedInUser
        logon(TEST_ADMIN_USER)
        val user = PFUserDO()
        user.firstname = "$name.firstname"
        user.lastname = "$name.lastname"
        user.username = "${javaClass.simpleName}.$name.username"
        if (hrAccess) {
            user.addRight(UserRightDO(UserRightId.HR_VACATION, UserRightValue.READWRITE))
        }
        initTestDB.addUser(user)
        listOfNotNull(
            ProjectForgeGroup.HR_GROUP.takeIf { hrAccess },
            ProjectForgeGroup.FINANCE_GROUP.takeIf { finance },
        ).forEach {
            val group = getGroup(it.toString())
            group.assignedUsers!!.add(user)
            groupDao.update(group)
        }
        val employee = EmployeeDO()
        employee.user = user
        employeeDao.insert(employee, checkAccess = false)
        employeeService.insertAnnualLeaveDays(employee, LocalDate.now().minusYears(1), BigDecimal(30), false)
        loggedInUser?.let { logon(it) }
        return employee
    }
}
