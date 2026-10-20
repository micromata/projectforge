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

package org.projectforge.rest.orga

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.orga.PostType
import org.projectforge.business.orga.PostausgangDO
import org.projectforge.business.orga.PostausgangDao
import org.projectforge.business.orga.PosteingangDO
import org.projectforge.business.orga.PosteingangDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.framework.time.PFDay
import org.projectforge.rest.core.ResultSet
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import java.time.LocalDate

/**
 * The inbox and the outbox of projectforge-next ([PosteingangEntityRest], [PostausgangEntityRest]): the preset of a
 * new entry, the check of the date, an update by the form, which posts the entity without its creation date, and
 * the access to the endpoints.
 */
class PostEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var posteingangEntityRest: PosteingangEntityRest

    @Autowired
    private lateinit var postausgangEntityRest: PostausgangEntityRest

    @Autowired
    private lateinit var posteingangDao: PosteingangDao

    @Autowired
    private lateinit var postausgangDao: PostausgangDao

    @Autowired
    private lateinit var groupDao: GroupDao

    @Test
    fun `a new entry is dated today, received as e-mail and sent as letter`() {
        logon(TEST_ADMIN_USER)
        val today = PFDay.now().localDate
        posteingangEntityRest.newBaseDO(MockHttpServletRequest()).let {
            assertEquals(today, it.datum)
            assertEquals(PostType.E_MAIL, it.type)
        }
        postausgangEntityRest.newBaseDO(MockHttpServletRequest()).let {
            assertEquals(today, it.datum)
            assertEquals(PostType.BRIEF, it.type)
        }
    }

    @Test
    fun `a date in the future is refused, today is accepted`() {
        logon(TEST_ADMIN_USER)
        val today = PFDay.now().localDate
        assertTrue(validateInbox(today).isEmpty())
        assertTrue(validateOutbox(today).isEmpty())
        assertEquals(listOf("datum"), validateInbox(today.plusDays(1)).map { it.fieldId })
        assertEquals(listOf("datum"), validateOutbox(today.plusDays(1)).map { it.fieldId })
    }

    /** The form posts no `created`: an update keeps the stored one. */
    @Test
    fun `an update keeps the creation date`() {
        logon(TEST_ADMIN_USER)
        val inboxId = posteingangDao.insert(inbox(), checkAccess = false)
        val inboxCreated = posteingangDao.find(inboxId, checkAccess = false)!!.created
        assertNotNull(inboxCreated)
        posteingangDao.update(
            posteingangEntityRest.transformForDB(inbox().also {
                it.id = inboxId
                it.inhalt = "Changed content"
            }),
            checkAccess = false,
        )
        posteingangDao.find(inboxId, checkAccess = false)!!.let {
            assertEquals("Changed content", it.inhalt)
            assertEquals(inboxCreated, it.created)
        }

        val outboxId = postausgangDao.insert(outbox(), checkAccess = false)
        val outboxCreated = postausgangDao.find(outboxId, checkAccess = false)!!.created
        assertNotNull(outboxCreated)
        postausgangDao.update(
            postausgangEntityRest.transformForDB(outbox().also {
                it.id = outboxId
                it.empfaenger = "Changed receiver"
            }),
            checkAccess = false,
        )
        postausgangDao.find(outboxId, checkAccess = false)!!.let {
            assertEquals("Changed receiver", it.empfaenger)
            assertEquals(outboxCreated, it.created)
        }
    }

    @Test
    fun `only a user with the mail book rights may list and add entries`() {
        logon(TEST_ADMIN_USER)
        val inboxId = posteingangDao.insert(inbox(), checkAccess = false)
        val outboxId = postausgangDao.insert(outbox(), checkAccess = false)

        logon(TEST_USER)
        assertThrows<AccessException> { posteingangEntityRest.getList(MockHttpServletRequest(), MagicFilter()) }
        assertThrows<AccessException> { postausgangEntityRest.getList(MockHttpServletRequest(), MagicFilter()) }
        assertThrows<AccessException> { posteingangEntityRest.getNewEntry(MockHttpServletRequest()) }
        assertThrows<AccessException> { postausgangEntityRest.getNewEntry(MockHttpServletRequest()) }

        logon(createOrgaUser())
        assertTrue(listedIds(posteingangEntityRest.getList(MockHttpServletRequest(), MagicFilter())).contains(inboxId))
        assertTrue(listedIds(postausgangEntityRest.getList(MockHttpServletRequest(), MagicFilter())).contains(outboxId))
        assertEquals(PostType.E_MAIL, (posteingangEntityRest.getNewEntry(MockHttpServletRequest()).body as PosteingangDO).type)
        assertEquals(PostType.BRIEF, (postausgangEntityRest.getNewEntry(MockHttpServletRequest()).body as PostausgangDO).type)
    }

    /** A member of the organization team with both mail book rights, as the rights are granted in production. */
    private fun createOrgaUser(): PFUserDO {
        logon(TEST_ADMIN_USER)
        val user = PFUserDO().also {
            it.firstname = "Orga"
            it.lastname = "User"
            it.username = "${javaClass.simpleName}.orgaUser"
            it.addRight(UserRightDO(UserRightId.ORGA_INCOMING_MAIL, UserRightValue.READWRITE))
            it.addRight(UserRightDO(UserRightId.ORGA_OUTGOING_MAIL, UserRightValue.READWRITE))
        }
        initTestDB.addUser(user)
        val group = getGroup(ProjectForgeGroup.ORGA_TEAM.toString())
        group.assignedUsers!!.add(user)
        groupDao.update(group)
        return user
    }

    private fun listedIds(resultSet: ResultSet<*>): List<Long?> =
        resultSet.resultSet.map { (it as BaseDO<*>).id as Long? }

    private fun inbox() = PosteingangDO().also {
        it.datum = LocalDate.of(2026, 10, 1)
        it.type = PostType.BRIEF
        it.absender = "Sender"
        it.inhalt = "Content"
    }

    private fun outbox() = PostausgangDO().also {
        it.datum = LocalDate.of(2026, 10, 1)
        it.type = PostType.BRIEF
        it.empfaenger = "Receiver"
        it.inhalt = "Content"
    }

    private fun validateInbox(date: LocalDate): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        posteingangEntityRest.validate(errors, PosteingangDO().also { it.datum = date })
        return errors
    }

    private fun validateOutbox(date: LocalDate): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        postausgangEntityRest.validate(errors, PostausgangDO().also { it.datum = date })
        return errors
    }
}
