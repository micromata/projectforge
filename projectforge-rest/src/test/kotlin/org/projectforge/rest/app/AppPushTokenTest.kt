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

package org.projectforge.rest.app

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.AppDeviceDao
import org.projectforge.business.user.PushEnvironment
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import java.util.Date

/** The push token of an app (`POST /rs/appAuth/pushToken`): only the own app, only valid tokens. */
class AppPushTokenTest : AbstractTestBase() {
    @Autowired
    private lateinit var appAuthRest: AppAuthRest

    @Autowired
    private lateinit var appDeviceDao: AppDeviceDao

    @AfterEach
    fun cleanUp() {
        appDeviceDao.deleteAll(getUser(TEST_USER).id)
        appDeviceDao.deleteAll(getUser(TEST_ADMIN_USER).id)
    }

    @Test
    fun `app stores and removes its push token`() {
        val (device, token) = appDeviceDao.createDevice(getUser(TEST_USER), "iPhone", "ios", Date(), null)
        logonByAppToken(TEST_USER)

        val response = appAuthRest.pushToken(appRequest(token), body(HEX))
        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertEquals(true, (response.body as AppAuthRest.PushTokenResponse).deviceTagged)
        val stored = appDeviceDao.getPushDevices(getUser(TEST_USER).id).single()
        Assertions.assertEquals(device.id, stored.id)
        Assertions.assertEquals(HEX, stored.pushToken)
        Assertions.assertEquals(PushEnvironment.SANDBOX, stored.pushEnvironment)

        Assertions.assertEquals(HttpStatus.OK, appAuthRest.removePushToken(appRequest(token)).statusCode)
        Assertions.assertTrue(appDeviceDao.getPushDevices(getUser(TEST_USER).id).isEmpty())
    }

    @Test
    fun `push token is refused without the app token of the user`() {
        val (_, token) = appDeviceDao.createDevice(getUser(TEST_USER), "iPhone", "ios", Date(), null)

        // A browser session, even with the app token in the header:
        logon(TEST_USER)
        Assertions.assertEquals(HttpStatus.FORBIDDEN, appAuthRest.pushToken(appRequest(token), body(HEX)).statusCode)
        // A REST client token (no app token):
        logonByAppToken(TEST_USER)
        Assertions.assertEquals(HttpStatus.FORBIDDEN, appAuthRest.pushToken(MockHttpServletRequest(), body(HEX)).statusCode)
        // The app token of another user:
        val (_, adminToken) = appDeviceDao.createDevice(getUser(TEST_ADMIN_USER), "iPhone", "ios", Date(), null)
        Assertions.assertEquals(HttpStatus.FORBIDDEN, appAuthRest.pushToken(appRequest(adminToken), body(HEX)).statusCode)
        Assertions.assertTrue(appDeviceDao.getPushDevices(getUser(TEST_USER).id).isEmpty())
    }

    @Test
    fun `invalid push token is rejected`() {
        val (_, token) = appDeviceDao.createDevice(getUser(TEST_USER), "iPhone", "ios", Date(), null)
        logonByAppToken(TEST_USER)
        listOf("", "xyz", "ab12", HEX + "'; --").forEach {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, appAuthRest.pushToken(appRequest(token), body(it)).statusCode, it)
        }
        Assertions.assertEquals(
            HttpStatus.BAD_REQUEST,
            appAuthRest.pushToken(appRequest(token), AppAuthRest.PushTokenRequest(HEX, "OTHER")).statusCode,
        )
    }

    @Test
    fun `push token moves to the app that registers it last`() {
        val (_, token) = appDeviceDao.createDevice(getUser(TEST_USER), "old phone", "ios", Date(), null)
        val (second, secondToken) = appDeviceDao.createDevice(getUser(TEST_USER), "new phone", "ios", Date(), null)
        logonByAppToken(TEST_USER)
        appAuthRest.pushToken(appRequest(token), body(HEX))
        appAuthRest.pushToken(appRequest(secondToken), body(HEX))
        Assertions.assertEquals(second.id, appDeviceDao.getPushDevices(getUser(TEST_USER).id).single().id)
    }

    private fun body(token: String) = AppAuthRest.PushTokenRequest(token, "SANDBOX")

    private fun logonByAppToken(username: String) {
        logon(username)
        ThreadLocalUserContext.userContext!!.loggedInByAuthenticationToken = true
    }

    private fun appRequest(token: String) = MockHttpServletRequest("POST", "/rs/appAuth/pushToken").also {
        it.addHeader("Authorization", "PFApp $token")
    }

    companion object {
        private val HEX = "ab12".repeat(16)
    }
}
