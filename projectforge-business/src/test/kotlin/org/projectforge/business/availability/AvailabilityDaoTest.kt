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


package org.projectforge.business.availability

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class AvailabilityDaoTest : AbstractTestBase() {
    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Test
    fun `period is validated on insert and update`() {
        logon(TEST_HR_USER)
        val user = createUser("validate")
        val other = createUser("validate-other")
        val start = LocalDate.of(2030, 3, 2)
        // End before start:
        assertError(AvailabilityDao.ERROR_END_DATE_BEFORE_START_DATE) {
            availabilityDao.insert(create(user, start, start.minusDays(1)))
        }
        val first = create(user, start, start.plusDays(9))
        availabilityDao.insert(first)
        // Overlapping the first one (also by a single day at its end):
        assertError(AvailabilityDao.ERROR_COLLISION) {
            availabilityDao.insert(create(user, start.plusDays(9), start.plusDays(12)))
        }
        // Adjacent periods and other users don't collide:
        val second = create(user, start.plusDays(10), start.plusDays(12))
        availabilityDao.insert(second)
        availabilityDao.insert(create(other, start, start.plusDays(9)))
        // An entry doesn't collide with itself on update, but with another one:
        first.endDate = start.plusDays(8)
        availabilityDao.update(first)
        first.endDate = start.plusDays(10)
        assertError(AvailabilityDao.ERROR_COLLISION) { availabilityDao.update(first) }
        // A deleted entry frees its period, and blocks its undelete if it's taken meanwhile:
        availabilityDao.markAsDeleted(second)
        first.endDate = start.plusDays(12)
        availabilityDao.update(first)
        assertError(AvailabilityDao.ERROR_COLLISION) { availabilityDao.undelete(second) }
    }

    @Test
    fun `a user without an employee record (freelancer) enters the own availability, not others'`() {
        val freelancer = createUser("freelancer")
        val other = createUser("freelancer-other")
        logon(freelancer)
        val own = create(freelancer, LocalDate.of(2030, 6, 1), LocalDate.of(2030, 6, 5))
        availabilityDao.insert(own)
        Assertions.assertEquals(freelancer.id, availabilityDao.find(own.id)!!.user?.id)
        assertThrows<AccessException> {
            availabilityDao.insert(create(other, LocalDate.of(2030, 6, 1), LocalDate.of(2030, 6, 5)))
        }
    }

    @Test
    fun `internal comment is only visible to the user and HR staff`() {
        logon(TEST_HR_USER)
        val user = createUser("comment")
        val colleague = createUser("comment-colleague")
        val entry = create(user, LocalDate.of(2031, 5, 4), LocalDate.of(2031, 5, 8))
        entry.comment = "public"
        entry.internalComment = "internal"
        availabilityDao.insert(entry)
        Assertions.assertTrue(availabilityDao.isInternalCommentVisibleFor(getUser(TEST_HR_USER), entry))
        Assertions.assertTrue(availabilityDao.isInternalCommentVisibleFor(user, entry))
        Assertions.assertFalse(availabilityDao.isInternalCommentVisibleFor(colleague, entry))

        Assertions.assertEquals("internal", availabilityDao.find(entry.id)!!.internalComment)
        logon(user)
        Assertions.assertEquals("internal", availabilityDao.find(entry.id)!!.internalComment)
        // A colleague (e.g. of the same group) reading it: the public comment only.
        logon(colleague)
        val copy = create(user, entry.startDate!!, entry.endDate!!).also {
            it.comment = entry.comment
            it.internalComment = entry.internalComment
        }
        availabilityDao.afterLoad(copy)
        Assertions.assertEquals("public", copy.comment)
        Assertions.assertNull(copy.internalComment)
    }

    @Test
    fun `the type is named only for the user and HR staff, the status for everybody else`() {
        val user = createUser("label-user")
        val colleague = createUser("label-colleague")
        val entry = create(user, LocalDate.of(2031, 3, 3), LocalDate.of(2031, 3, 7))
        val typed = "Workation (${translate("availability.status.reachableIfNeeded")})"
        Assertions.assertEquals(typed, availabilityDao.labelFor(user, entry))
        Assertions.assertEquals(typed, availabilityDao.labelFor(getUser(TEST_HR_USER), entry))
        Assertions.assertEquals(translate("availability.status.reachableIfNeeded"), availabilityDao.labelFor(colleague, entry))
        Assertions.assertEquals(
            translate("availability.status.reachableIfNeeded"), availabilityDao.labelFor(null, entry),
            "Without a viewer (a calendar subscription)."
        )
        entry.status = AvailabilityStatus.NOT_AVAILABLE
        Assertions.assertEquals("Workation", availabilityDao.labelFor(user, entry))
        Assertions.assertEquals(translate("availability.status.notAvailable"), availabilityDao.labelFor(colleague, entry))
        Assertions.assertTrue(AvailabilityStatus.NOT_AVAILABLE.absent)
        Assertions.assertEquals(listOf(AvailabilityStatus.NOT_AVAILABLE), AvailabilityStatus.entries.filter { it.absent })
    }

    @Test
    fun `members of the user's groups see the entries as vacations, not those ended more than 31 days ago`() {
        val user = createUser("past-user")
        val colleague = createUser("past-colleague")
        val stranger = createUser("past-stranger")
        logon(TEST_ADMIN_USER)
        initTestDB.addGroup("${this.javaClass.simpleName}.past", user.username!!, colleague.username!!)
        val today = LocalDate.now()
        val recent = create(user, today.minusDays(40), today.minusDays(30))
        val old = create(user, today.minusDays(40), today.minusDays(31))
        val future = create(user, today.plusDays(10), today.plusDays(20))
        listOf(recent, future).forEach { Assertions.assertTrue(availabilityDao.hasUserSelectAccess(colleague, it, false)) }
        Assertions.assertFalse(availabilityDao.hasUserSelectAccess(colleague, old, false), "Ended 31 days ago.")
        listOf(recent, old, future).forEach {
            Assertions.assertFalse(availabilityDao.hasUserSelectAccess(stranger, it, false), "Not of the same group.")
            Assertions.assertTrue(availabilityDao.hasUserSelectAccess(user, it, false), "The own entries.")
            Assertions.assertTrue(availabilityDao.hasUserSelectAccess(getUser(TEST_HR_USER), it, false), "HR staff.")
        }
    }

    private fun assertError(expectedKey: String, block: () -> Unit) {
        val ex = assertThrows<UserException> { block() }
        Assertions.assertEquals(expectedKey, ex.i18nKey)
    }

    private fun create(user: PFUserDO, startDate: LocalDate, endDate: LocalDate): AvailabilityDO {
        val obj = AvailabilityDO()
        obj.user = user
        obj.startDate = startDate
        obj.endDate = endDate
        obj.type = "Workation"
        obj.status = AvailabilityStatus.REACHABLE_IF_NEEDED
        return obj
    }

    private fun createUser(name: String): PFUserDO {
        val user = PFUserDO()
        user.firstname = "$name.firstname"
        user.lastname = "$name.lastname"
        user.username = "${this.javaClass.simpleName}.$name.username"
        return initTestDB.addUser(user)
    }
}
