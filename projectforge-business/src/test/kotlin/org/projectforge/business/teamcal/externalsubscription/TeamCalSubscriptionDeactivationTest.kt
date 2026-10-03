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


package org.projectforge.business.teamcal.externalsubscription

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.springframework.beans.factory.annotation.Autowired
import java.util.Date

class TeamCalSubscriptionDeactivationTest : AbstractTestBase() {
    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var subscriptionCache: TeamEventExternalSubscriptionCache

    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @Test
    fun `permanently failing subscription is deactivated`() {
        val admin = logon(ADMIN)
        val url = "http://localhost:9/calendar.ics" // Nobody listens on port 9: connection refused.
        val cal = TeamCalDO().also {
            it.title = "Failing subscription"
            it.owner = admin
            it.description = "My holidays"
            it.externalSubscription = true
            it.externalSubscriptionUrl = url
        }
        val id = teamCalDao.insert(cal)
        // The first failure (update on insert) is persisted:
        val firstFailure = reload(id).externalSubscriptionFailingSince
        assertNotNull(firstFailure)

        // A user's save (without the technical field) keeps it:
        reload(id).let {
            it.title = "Renamed"
            it.externalSubscriptionFailingSince = null
            teamCalDao.update(it)
        }
        assertEquals(firstFailure!!.time / 1000, reload(id).externalSubscriptionFailingSince!!.time / 1000)

        // Not due yet (back-off), so no further attempt:
        assertEquals(TeamEventExternalSubscriptionCache.UpdateResult.SKIPPED, subscriptionCache.updateCache(reload(id)))

        // Failing for 91 days:
        teamCalDao.updateExternalSubscriptionFailingSince(id, Date(System.currentTimeMillis() - 91L * 24 * 60 * 60 * 1000))
        val failing = reload(id)
        assertEquals(
            TeamEventExternalSubscriptionCache.UpdateResult.DEACTIVATION_DUE,
            subscriptionCache.updateCache(failing, true),
        )
        assertEquals(1, subscriptionCache.handleFailingSubscriptions(listOf(failing), listOf(failing), System.currentTimeMillis()))
        reload(id).let {
            assertFalse(it.externalSubscription)
            assertEquals(url, it.externalSubscriptionUrl, "The url is kept for reactivation.")
            assertNull(it.externalSubscriptionFailingSince)
            assertTrue(it.description!!.startsWith("My holidays\n\n"), it.description)
            assertTrue(it.description!!.contains("http://localhost"), it.description)
            assertTrue(it.description!!.contains("Connection refused"), it.description) // Last error.
            assertFalse(it.description!!.contains("???"), it.description) // i18n key found.
        }
        assertFalse(subscriptionCache.isExternalSubscribedCalendar(id))
    }

    @Test
    fun `nothing is deactivated if most subscriptions fail`() {
        val admin = logon(ADMIN)
        val ids = (1..3).map { i ->
            teamCalDao.insert(TeamCalDO().also {
                it.title = "Failing subscription $i"
                it.owner = admin
                it.externalSubscription = true
                it.externalSubscriptionUrl = "http://localhost:9/calendar$i.ics"
            })
        }
        val ago91Days = Date(System.currentTimeMillis() - 91L * 24 * 60 * 60 * 1000)
        ids.forEach { teamCalDao.updateExternalSubscriptionFailingSince(it, ago91Days) }
        val calendars = ids.map { reload(it) }
        val sysopEntry = configurationDao.getEntry(ConfigurationParam.SYSTEM_ADMIN_E_MAIL)!!
        try {
            sysopEntry.stringValue = "sysop@example.org; sysop2@example.org"
            configurationDao.update(sysopEntry, false)
            // 3 of 3 failing: probably our bug, so nothing is deactivated, but the admins are informed.
            assertEquals(0, subscriptionCache.handleFailingSubscriptions(calendars, calendars, System.currentTimeMillis()))
            val recipients = TeamEventExternalSubscriptionCache.getAdministratorRecipients()
            assertTrue(recipients.containsAll(listOf("sysop@example.org", "sysop2@example.org")), recipients.toString())
            val mail = subscriptionCache.createSystemicFailureMail(recipients, calendars, 3)
            assertEquals(recipients.size, mail.to.size)
            assertTrue(mail.subject.contains("3 of 3 failing"), mail.subject)
            ids.forEach { assertTrue(mail.content.contains("#$it ("), mail.content) }
            assertTrue(mail.content.contains("Connection refused"), mail.content) // Last errors.
        } finally {
            sysopEntry.stringValue = null
            configurationDao.update(sysopEntry, false)
        }
        ids.forEach { assertTrue(reload(it).externalSubscription) }
    }

    private fun reload(id: Long): TeamCalDO {
        return teamCalDao.find(id, checkAccess = false)!!
    }
}
