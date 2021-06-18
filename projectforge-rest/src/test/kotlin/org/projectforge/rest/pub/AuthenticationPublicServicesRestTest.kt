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

package org.projectforge.rest.pub

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.projectforge.business.user.UserAuthenticationsDao
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.utils.Crypt
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class AuthenticationPublicServicesRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var authenticationPublicServicesRest: AuthenticationPublicServicesRest

    @Autowired
    private lateinit var userAuthenticationsDao: UserAuthenticationsDao

    @Test
    fun getAuthenticationCredentialsTest() {
        val user = logon(TEST_USER)
        val q = authenticationPublicServicesRest.createTemporaryToken()
        val credentials = authenticationPublicServicesRest.getAuthenticationCredentials(q)
        assertEquals(user.username, credentials.username)
        assertEquals(user.id, credentials.uid)
        val token = userAuthenticationsDao.getToken(user.id, UserTokenType.REST_CLIENT)
        assertEquals(token, credentials.authenticationToken)
        assertEquals("http://localhost:8080", credentials.url)

        try {
            authenticationPublicServicesRest.getAuthenticationCredentials("OXn1Nq1T7qZUkOCHEdp3LB")
            fail("Failure expected, invalid q")
        } catch (ex: Exception) {
            // expected
        }
    }

    @Test
    fun temporaryTokenTest() {
        val uid = getUserId(TEST_ADMIN_USER)
        val qNow = authenticationPublicServicesRest.createTemporaryToken(uid, System.currentTimeMillis())
        assertEquals(qNow, authenticationPublicServicesRest.checkQuery(qNow).token)
        val qExpired = authenticationPublicServicesRest.createTemporaryToken(uid, System.currentTimeMillis() - AuthenticationPublicServicesRest.EXPIRE_TIME_IN_MILLIS - 1)
        try {
            authenticationPublicServicesRest.checkQuery(qExpired)
            fail("Token should be expired and an exception was expected.")
        } catch (ex: Exception) {
            // expected
        }
    }

    @Test
    fun paramCheckTest() {
        logon(TEST_USER)
        val uid = getUserId(TEST_USER)
        paramCheck(uid, -1000, true, "1 second ago should be valid.")
        paramCheck(uid, -60 * 1000, true, "1 minute ago should be valid.")
        paramCheck(uid, -1 - AuthenticationPublicServicesRest.EXPIRE_TIME_IN_MILLIS, false, "more than 2 minutea ago should be invalid.")
        paramCheck(uid, +1000, false, "Any time in the future should be invalid.")
        try {
            authenticationPublicServicesRest.checkQuery("OXn1Nq1T7qZUkOCHEdp3LB")
            fail("Invalid param q. Failure was expected.")
        } catch (ex: Exception) {
            // excepted
        }
        val token = userAuthenticationsDao.getToken(uid, UserTokenType.REST_CLIENT)!!
        try {
            authenticationPublicServicesRest.checkQuery(Crypt.encrypt(token, "nonumber")!!)
            fail("Invalid system time im millis. Failure was expected.")
        } catch (ex: Exception) {
            // excepted
        }
    }

    private fun paramCheck(uid: Int, timeOffset: Long, expectedResult: Boolean, msg: String) {
        val q = authenticationPublicServicesRest.createTemporaryToken(uid, System.currentTimeMillis() + timeOffset)
        try {
            authenticationPublicServicesRest.checkQuery(q)
            assertTrue(expectedResult, "Oups, check failure expected: $msg")
        } catch (ex: Exception) {
            assertFalse(expectedResult, "Oups, check failure not expected: $msg")
        }
    }
}
