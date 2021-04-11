/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.user

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.UserAuthenticationsDO
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class UserAuthenticationsDaoTest : AbstractTestBase() {
    @Autowired
    private lateinit var userAuthenticationsDao: UserAuthenticationsDao

    @Test
    fun accessTest() {
        logon(TEST_FINANCE_USER)
        val otherUser = getUser(TEST_USER)
        val loggedInUser = ThreadLocalUserContext.getUser()
        try {
            userAuthenticationsDao.getByUserId(otherUser.id)
            fail("Access exception exptected.")
        } catch (ex: AccessException) {
            // OK
        }
        try {
            userAuthenticationsDao.renewToken(otherUser.id, UserTokenType.STAY_LOGGED_IN_KEY)
            fail("Access exception exptected.")
        } catch (ex: AccessException) {
            // OK
        }
        var authentications = userAuthenticationsDao.getByUserId(ThreadLocalUserContext.getUserId())
        var stayLoggedInKey = authentications!!.getToken(UserTokenType.STAY_LOGGED_IN_KEY)
        val calendarToken = authentications.getToken(UserTokenType.CALENDAR_REST)
        val davToken = authentications.getToken(UserTokenType.DAV_TOKEN)
        val restClientToken = authentications.getToken(UserTokenType.REST_CLIENT)
        authentications = userAuthenticationsDao.getByUserId(ThreadLocalUserContext.getUserId())
        assertTokens(authentications!!, stayLoggedInKey, calendarToken, davToken, restClientToken)
        userAuthenticationsDao.renewToken(loggedInUser.id, UserTokenType.STAY_LOGGED_IN_KEY)
        authentications = userAuthenticationsDao.getByUserId(ThreadLocalUserContext.getUserId())
        Assertions.assertTrue(authentications!!.stayLoggedInKey != stayLoggedInKey)
        stayLoggedInKey = authentications.stayLoggedInKey
        assertTokens(authentications, stayLoggedInKey, calendarToken, davToken, restClientToken)

        logon(TEST_ADMIN_USER)
        userAuthenticationsDao.getByUserId(otherUser.id)
        userAuthenticationsDao.renewToken(otherUser.id, UserTokenType.STAY_LOGGED_IN_KEY)
    }

    @Test
    fun createTokenTest() {
        val token = userAuthenticationsDao.createAuthenticationToken()
        Assertions.assertEquals(19, token.length)
        Assertions.assertTrue(token.matches("[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}".toRegex()), "Token '$token' isn't of format xxxx-xxxx-xxxx-xxxx")
    }

    @Test
    fun getUserByTokenTest() {
        logon(TEST_USER)
        val loggedInUser = ThreadLocalUserContext.getUser()
        userAuthenticationsDao.getByUserId(loggedInUser.id)
        userAuthenticationsDao.renewToken(loggedInUser.id, UserTokenType.STAY_LOGGED_IN_KEY)
        val stayLoggedInKey = userAuthenticationsDao.getToken(loggedInUser.id, UserTokenType.STAY_LOGGED_IN_KEY)!!
        logoff()
        var user = userAuthenticationsDao.getUserByToken(loggedInUser.id, UserTokenType.STAY_LOGGED_IN_KEY, stayLoggedInKey)!!
        Assertions.assertEquals(loggedInUser.id, user.id)

        user = userAuthenticationsDao.getUserByToken(loggedInUser.username!!, UserTokenType.STAY_LOGGED_IN_KEY, stayLoggedInKey)!!
        Assertions.assertEquals(loggedInUser.id, user.id)
    }

    @Test
    fun decryptTest() {
        logon(TEST_USER)
        val loggedInUser = ThreadLocalUserContext.getUser()
        val authentications = userAuthenticationsDao.getByUserId(loggedInUser.id)!!
        val stayLoggedInKey = userAuthenticationsDao.getToken(loggedInUser.id, UserTokenType.STAY_LOGGED_IN_KEY)!!
        Assertions.assertNotEquals(stayLoggedInKey, authentications.stayLoggedInKey, "Should be stored as encrypted value.")
        userAuthenticationsDao.decryptAllTokens(authentications)
        Assertions.assertEquals(stayLoggedInKey, authentications.stayLoggedInKey, "Is now decrypted.")
    }

    private fun assertTokens(authentications: UserAuthenticationsDO, expectedStayLoggedInKey: String?, expectedCalendarToken: String?, expectedDAVToken: String?, expectedRestClientToken: String?) {
        Assertions.assertTrue(!expectedStayLoggedInKey.isNullOrBlank() && expectedStayLoggedInKey.trim().length > 10)
        Assertions.assertTrue(!expectedCalendarToken.isNullOrBlank() && expectedCalendarToken.trim().length > 10)
        Assertions.assertTrue(!expectedDAVToken.isNullOrBlank() && expectedDAVToken.trim().length > 10)
        Assertions.assertEquals(expectedStayLoggedInKey, authentications.stayLoggedInKey)
        Assertions.assertEquals(expectedCalendarToken, authentications.calendarExportToken)
        Assertions.assertEquals(expectedDAVToken, authentications.davToken)
        Assertions.assertEquals(expectedRestClientToken, authentications.restClientToken)

        assertEncryptedToken(authentications, UserTokenType.CALENDAR_REST, expectedCalendarToken)
        assertEncryptedToken(authentications, UserTokenType.DAV_TOKEN, expectedDAVToken)
        assertEncryptedToken(authentications, UserTokenType.REST_CLIENT, expectedRestClientToken)
        assertEncryptedToken(authentications, UserTokenType.STAY_LOGGED_IN_KEY, expectedStayLoggedInKey)
    }

    private fun assertEncryptedToken(authentications: UserAuthenticationsDO, type: UserTokenType, expectedEncryptedKey: String?) {
        expectedEncryptedKey ?: return
        Assertions.assertEquals(userAuthenticationsDao.decryptToken(expectedEncryptedKey), userAuthenticationsDao.getToken(authentications.userId!!, type))
    }
}
