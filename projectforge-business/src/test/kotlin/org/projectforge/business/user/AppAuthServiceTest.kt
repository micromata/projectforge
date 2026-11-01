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


package org.projectforge.business.user

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.time.TimeUnit
import org.springframework.beans.factory.annotation.Autowired
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Date

class AppAuthServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var appAuthService: AppAuthService

    @Autowired
    private lateinit var appDeviceDao: AppDeviceDao

    @Test
    fun codeFlowTest() {
        val user = getUser(TEST_USER)
        appDeviceDao.deleteAll(user.id)
        val last2FA = System.currentTimeMillis()
        val url = authorize(user, last2FA)!!
        Assertions.assertTrue(url.startsWith("${AppAuthService.APP_REDIRECT_URL}?"), "Fixed callback of the app.")
        Assertions.assertEquals(STATE, param(url, "state"), "The state of the app is handed back.")
        val code = param(url, "code")

        val exchanged = appAuthService.exchange(code, VERIFIER, null)!!
        Assertions.assertEquals(user.id, exchanged.user.id)
        Assertions.assertEquals("iPhone", exchanged.device.deviceName)
        Assertions.assertEquals("ios", exchanged.device.platform)
        Assertions.assertEquals(last2FA, exchanged.device.last2FA?.time, "The second factor of the browser session.")
        val device = appDeviceDao.getValidDevice(exchanged.token)!!
        Assertions.assertEquals(exchanged.device.id, device.id)
        Assertions.assertNotEquals(exchanged.token, device.tokenHash, "Stored as a hash.")

        Assertions.assertNull(appAuthService.exchange(code, VERIFIER, null), "A code is good for one exchange only.")
        appDeviceDao.deleteAll(user.id)
    }

    /**
     * Another app may catch the code (same url scheme), but it has no verifier. A failed attempt consumes the code,
     * so it can't be tried again with another guess.
     */
    @Test
    fun wrongVerifierTest() {
        val user = getUser(TEST_USER)
        appDeviceDao.deleteAll(user.id)
        val code = param(authorize(user, System.currentTimeMillis())!!, "code")
        Assertions.assertNull(appAuthService.exchange(code, "x".repeat(43), null), "Wrong verifier.")
        Assertions.assertNull(appAuthService.exchange(code, VERIFIER, null), "Consumed by the failed attempt.")
        Assertions.assertNull(appAuthService.exchange(code, null, null))
        Assertions.assertNull(appAuthService.exchange("unknownCode", VERIFIER, null))
        Assertions.assertNull(appAuthService.exchange(null, VERIFIER, null))
        Assertions.assertTrue(appDeviceDao.getEntries(user.id).isEmpty(), "No device registered.")
    }

    @Test
    fun invalidChallengeOrStateTest() {
        val user = getUser(TEST_USER)
        val now = System.currentTimeMillis()
        Assertions.assertNull(appAuthService.authorize(user, now, "plain", STATE, null, null, null))
        Assertions.assertNull(appAuthService.authorize(user, now, null, STATE, null, null, null))
        Assertions.assertNull(appAuthService.authorize(user, now, CHALLENGE, "short", null, null, null))
        Assertions.assertNull(appAuthService.authorize(user, now, CHALLENGE, "evil&code=x#aaaaaaaaaa", null, null, null))
    }

    /**
     * A registered app logging in again keeps its row and gets a new token - but only its own user's row: a device
     * id of another user leads to a new device.
     */
    @Test
    fun renewDeviceTest() {
        val user = getUser(TEST_USER)
        val otherUser = getUser(TEST_USER2)
        appDeviceDao.deleteAll(user.id)
        appDeviceDao.deleteAll(otherUser.id)
        val (device, oldToken) = appDeviceDao.createDevice(user, "iPhone", "ios", Date(0), null)
        val (otherDevice, otherToken) = appDeviceDao.createDevice(otherUser, "Pixel", "android", Date(0), null)

        val now = System.currentTimeMillis()
        val renewed = exchange(appAuthService.authorize(user, now, CHALLENGE, STATE, "iPhone", "ios", device.id))!!
        Assertions.assertEquals(device.id, renewed.device.id, "Same device.")
        Assertions.assertEquals(now, renewed.device.last2FA?.time)
        Assertions.assertNull(appDeviceDao.getValidDevice(oldToken), "The old token is invalid.")
        Assertions.assertNotNull(appDeviceDao.getValidDevice(renewed.token))

        val foreign = exchange(appAuthService.authorize(user, now, CHALLENGE, STATE, "x", "ios", otherDevice.id))!!
        Assertions.assertNotEquals(otherDevice.id, foreign.device.id, "Not the other user's device.")
        Assertions.assertEquals(user.id, foreign.device.user?.id)
        Assertions.assertNotNull(appDeviceDao.getValidDevice(otherToken), "The other user's app stays logged in.")
        Assertions.assertEquals(Date(0), appDeviceDao.getValidDevice(otherToken)!!.last2FA)
        appDeviceDao.deleteAll(user.id)
        appDeviceDao.deleteAll(otherUser.id)
    }

    /**
     * The texts of the app end up in memory and log: cleaned and shortened right away. A user holds a few open codes
     * only, the oldest go.
     */
    @Test
    fun authorizeInputTest() {
        val user = getUser(TEST_USER)
        appDeviceDao.deleteAll(user.id)
        val now = System.currentTimeMillis()
        val first = param(appAuthService.authorize(user, now, CHALLENGE, STATE, "Pixel\n" + "x".repeat(500), "And\u0000roid", null)!!, "code")
        val exchanged = appAuthService.exchange(first, VERIFIER, null)!!
        Assertions.assertEquals(AppDeviceDO.MAX_DEVICE_NAME_LENGTH, exchanged.device.deviceName?.length)
        Assertions.assertTrue(exchanged.device.deviceName!!.startsWith("Pixelxxx"), "No line break.")
        Assertions.assertEquals("android", exchanged.device.platform)

        val codes = (1..AppAuthService.MAX_PENDING_CODES_PER_USER + 1).map { param(authorize(user, now)!!, "code") }
        Assertions.assertNull(appAuthService.exchange(codes.first(), VERIFIER, null), "The oldest code was dropped.")
        Assertions.assertNotNull(appAuthService.exchange(codes.last(), VERIFIER, null))
        appDeviceDao.deleteAll(user.id)
    }

    @Test
    fun deviceDaoTest() {
        val user = getUser(TEST_USER)
        val otherUser = getUser(TEST_USER2)
        appDeviceDao.deleteAll(user.id)
        val (device, token) = appDeviceDao.createDevice(user, "iPhone", "iOS", null, null)
        Assertions.assertEquals("ios", device.platform)
        Assertions.assertNull(appDeviceDao.getValidDevice(null))
        Assertions.assertNull(appDeviceDao.getValidDevice("tooShort"))
        Assertions.assertNull(appDeviceDao.getValidDevice("a".repeat(40)))
        Assertions.assertNull(appDeviceDao.renewDevice(otherUser.id, device.id, Date(), null), "Not the other's.")
        Assertions.assertNotNull(appDeviceDao.getValidDevice(token), "Untouched by the other user's renewal.")
        Assertions.assertEquals(0, appDeviceDao.deleteOfUser(otherUser.id, device.id), "Not the other's.")
        Assertions.assertNotNull(appDeviceDao.getValidDevice(token))

        setLastAccess(device.id!!, Date(System.currentTimeMillis() - 91 * TimeUnit.DAY.millis))
        Assertions.assertNull(appDeviceDao.getValidDevice(token), "Not used for more than 90 days.")
        Assertions.assertTrue(appDeviceDao.getEntries(user.id).isEmpty(), "The row is deleted.")

        val (second, secondToken) = appDeviceDao.createDevice(user, "iPad", "ios", null, null)
        Assertions.assertEquals(1, appDeviceDao.deleteOfUser(user.id, second.id))
        Assertions.assertNull(appDeviceDao.getValidDevice(secondToken), "Logged out.")
    }

    private fun authorize(user: org.projectforge.framework.persistence.user.entities.PFUserDO, last2FA: Long): String? {
        return appAuthService.authorize(user, last2FA, CHALLENGE, STATE, "iPhone", "ios", null)
    }

    private fun exchange(url: String?): AppAuthService.Exchanged? {
        return appAuthService.exchange(param(url!!, "code"), VERIFIER, null)
    }

    private fun param(url: String, name: String): String {
        return URI(url).rawQuery.split("&").map { it.split("=", limit = 2) }
            .first { it[0] == name }[1].let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
    }

    private fun setLastAccess(id: Long, lastAccess: Date) {
        persistenceService.runInTransaction { context ->
            context.em.find(AppDeviceDO::class.java, id).let { entry ->
                entry.lastAccess = lastAccess
                context.em.flush()
            }
        }
    }

    companion object {
        /** Example of RFC 7636, appendix B. */
        private const val VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        private const val CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
        private const val STATE = "state-1234567890abcdef"
    }
}
