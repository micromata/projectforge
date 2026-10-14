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
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.business.teamcal.CalendarAccessStatus
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.rest.dto.TeamCal
import org.springframework.beans.factory.annotation.Autowired

class TeamCalEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var teamCalEntityRest: TeamCalEntityRest

    /**
     * The url of an external subscription may hold credentials: only the form of a user allowed to save gets it,
     * the list and everyone else the anonymized one.
     */
    @Test
    fun `the subscription url reaches the form of users allowed to save only`() {
        val owner = logon(TEST_USER)
        val reader = getUser(TEST_USER2)
        val url = "https://user:secret@calendar.example.org/basic.ics"
        val calId = teamCalDao.insert(TeamCalDO().also {
            it.title = "Subscription url test"
            it.owner = owner
            it.externalSubscription = true
            it.externalSubscriptionUrl = url
            it.readonlyAccessUserIds = "${reader.id}"
        }, checkAccess = false)

        teamCalEntityRest.transformFromDB(teamCalDao.find(calId)!!, editMode = true).let {
            assertEquals(url, it.externalSubscriptionUrl)
            assertEquals(CalendarAccessStatus.OWNER, it.accessStatus)
        }
        teamCalEntityRest.transformFromDB(teamCalDao.find(calId)!!, editMode = false).let {
            assertNull(it.externalSubscriptionUrl)
            assertNotNull(it.externalSubscriptionUrlAnonymized)
        }

        logon(reader)
        teamCalEntityRest.transformFromDB(teamCalDao.find(calId)!!, editMode = true).let {
            assertNull(it.externalSubscriptionUrl)
            assertNotNull(it.externalSubscriptionUrlAnonymized)
            assertEquals(CalendarAccessStatus.READONLY_ACCESS, it.accessStatus)
        }
    }

    /** Users with access to a calendar may still not save it: only its owner (and admins) may. */
    @Test
    fun `the form offers save and delete to the owner only`() {
        val owner = logon(TEST_USER)
        val fullAccessUser = getUser(TEST_USER2)
        val calId = teamCalDao.insert(TeamCalDO().also {
            it.title = "Write access test"
            it.owner = owner
            it.fullAccessUserIds = "${fullAccessUser.id}"
        })
        (teamCalEntityRest.getItem(calId).body as TeamCal).let {
            assertEquals(true, it.writeAccess)
            assertEquals(true, it.deleteAccess)
        }
        logon(fullAccessUser)
        (teamCalEntityRest.getItem(calId).body as TeamCal).let {
            assertEquals(CalendarAccessStatus.FULL_ACCESS, it.accessStatus)
            assertEquals(false, it.writeAccess)
            assertEquals(false, it.deleteAccess)
        }
    }

    /** The feed of a calendar, formerly part of the React layout, comes with the form of a stored calendar. */
    @Test
    fun `the subscription info comes with the form of a stored calendar only`() {
        val owner = logon(TEST_USER)
        val calId = teamCalDao.insert(TeamCalDO().also {
            it.title = "Subscription info test"
            it.owner = owner
        })
        val cal = teamCalDao.find(calId)!!
        assertNull(teamCalEntityRest.transformFromDB(cal, editMode = false).subscriptionInfo)
        val info = teamCalEntityRest.transformFromDB(cal, editMode = true).subscriptionInfo
        assertNotNull(info)
        assertNotNull(info!!.url)
        assertNotNull(info.urlWithoutExportedReminders)
        assertNotEquals(info.url, info.urlWithoutExportedReminders)
        assertEquals(true, info.remindersExportDefaultValue)
        assertNull(teamCalEntityRest.transformFromDB(TeamCalDO().also { it.owner = owner }, editMode = true).subscriptionInfo)
    }

    /** The access lists travel as users and groups and come back as the csv lists of ids of the DO. */
    @Test
    fun `access lists survive the round trip through the form`() {
        val owner = logon(TEST_USER)
        val other = getUser(TEST_USER2)
        val group = getGroup(TEST_GROUP)
        val calId = teamCalDao.insert(TeamCalDO().also {
            it.title = "Round trip test"
            it.owner = owner
            it.fullAccessUserIds = "${other.id}"
            it.minimalAccessGroupIds = "${group.id}"
            it.includeLeaveDaysForUsers = "${other.id}"
        })
        val dto = teamCalEntityRest.transformFromDB(teamCalDao.find(calId)!!, editMode = true)
        assertEquals(listOf(other.id), dto.fullAccessUsers?.map { it.id })
        assertNotNull(dto.fullAccessUsers?.single()?.displayName)
        val saved = teamCalEntityRest.transformForDB(dto)
        assertEquals("${other.id}", saved.fullAccessUserIds)
        assertEquals("${group.id}", saved.minimalAccessGroupIds)
        assertEquals("${other.id}", saved.includeLeaveDaysForUsers)
        assertEquals(owner.id, saved.owner?.id)
    }
}
