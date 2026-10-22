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

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.login.LoginProtection
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.rest.core.RestCsrfProtection
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession

/**
 * Only refused changes: a successful one would change the password of a test user shared with other tests.
 */
class ChangePasswordRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var changePasswordRest: ChangePasswordRest

    @Autowired
    private lateinit var changeWlanPasswordRest: ChangeWlanPasswordRest

    @Autowired
    private lateinit var sessionCsrfService: SessionCsrfService

    @AfterEach
    fun clearLoginProtection() {
        // A wrong old password is a failed login attempt of the test user, shared with other tests.
        LoginProtection.instance().clearAll()
    }

    @Test
    fun `the init of the own password page`() {
        logon(TEST_USER)
        val init = changePasswordRest.getInit(null)
        assertEquals(getUserId(TEST_USER), init.userId)
        assertTrue(init.changeOwn)
        assertTrue(init.supported)
        assertNotNull(init.passwordQualityHint)
        // The default login handler can't change the WLAN password, and its rules aren't checked anyway.
        val wlan = changeWlanPasswordRest.getInit(null)
        assertFalse(wlan.supported)
        assertNull(wlan.passwordQualityHint)
    }

    @Test
    fun `only admins may open the page of another user`() {
        logon(TEST_ADMIN_USER)
        val init = changePasswordRest.getInit(getUserId(TEST_USER).toString())
        assertFalse(init.changeOwn)
        logon(TEST_USER)
        assertThrows<AccessException> { changePasswordRest.getInit(getUserId(TEST_ADMIN_USER).toString()) }
        assertThrows<AccessException> {
            changePasswordRest.change(requestWithToken(), postData(userId = getUserId(TEST_ADMIN_USER)))
        }
    }

    @Test
    fun `a change without session or csrf token is refused`() {
        logon(TEST_USER)
        assertEquals(HttpStatus.FORBIDDEN, changePasswordRest.change(MockHttpServletRequest(), postData()).statusCode)
        val withoutToken = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
        assertEquals(HttpStatus.FORBIDDEN, changePasswordRest.change(withoutToken, postData()).statusCode)
        val wrongToken = requestWithToken("x".repeat(30))
        assertEquals(HttpStatus.FORBIDDEN, changePasswordRest.change(wrongToken, postData()).statusCode)
    }

    @Test
    fun `the errors are answered on their fields`() {
        logon(TEST_USER)
        assertEquals(
            listOf("loginPassword", "newPassword"),
            fieldIds(changePasswordRest.change(requestWithToken(), postData(login = "", new = "", repeat = ""))),
        )
        assertEquals(
            listOf("passwordRepeat"),
            fieldIds(changePasswordRest.change(requestWithToken(), postData(repeat = "other-Pass42!"))),
        )
        assertEquals(
            listOf("loginPassword"),
            fieldIds(changePasswordRest.change(requestWithToken(), postData(login = "wrong"))),
        )
        LoginProtection.instance().clearAll()
        // The right old password, but a new one breaking the quality rules (too short, no non-letter): nothing is
        // changed, and every broken rule is on the field.
        assertEquals(
            listOf("newPassword", "newPassword"),
            fieldIds(
                changePasswordRest.change(
                    requestWithToken(), postData(login = String(TEST_USER_PASSWORD), new = "a", repeat = "a"),
                )
            ),
        )
    }

    @Test
    fun `a wrong old password locks the change like a failed login`() {
        logon(TEST_USER)
        val wrong = changePasswordRest.change(requestWithToken(), postData(login = "wrong"))
        assertEquals(listOf("user.changePassword.error.oldPasswordWrong"), messageIds(wrong))
        // Right away again, now with the right password: refused without checking it, on the field of the old one.
        val locked = changePasswordRest.change(
            requestWithToken(), postData(login = String(TEST_USER_PASSWORD), new = "a", repeat = "a"),
        )
        assertEquals(listOf(ChangePasswordRest.MESSAGE_KEY_TIME_OFFSET), messageIds(locked))
        assertEquals(listOf("loginPassword"), fieldIds(locked))
        // The admin changing another user's password enters none, so isn't locked by the user's attempts.
        logon(TEST_ADMIN_USER)
        val byAdmin = changePasswordRest.change(
            requestWithToken(), postData(userId = getUserId(TEST_USER), new = "a", repeat = "a"),
        )
        assertEquals(listOf("newPassword", "newPassword"), fieldIds(byAdmin))
    }

    @Test
    fun `a not supported change is refused`() {
        logon(TEST_USER)
        val response = changeWlanPasswordRest.change(requestWithToken(), postData(login = String(TEST_USER_PASSWORD)))
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.statusCode)
        val error = (response.body as ResponseAction).validationErrors!!.single()
        assertEquals(ChangePasswordRest.MESSAGE_KEY_NOT_SUPPORTED, error.messageId)
        assertNull(error.fieldId)
    }

    @Test
    fun `the error keys of the user service belong to their fields`() {
        assertEquals("loginPassword", ChangePasswordRest.fieldIdOf("user.changePassword.error.oldPasswordWrong"))
        assertEquals("loginPassword", ChangePasswordRest.fieldIdOf("user.changeWlanPassword.error.loginPasswordWrong"))
        assertEquals("newPassword", ChangePasswordRest.fieldIdOf("user.changePassword.error.notMinLength"))
        assertEquals("newPassword", ChangePasswordRest.fieldIdOf("user.changePassword.error.oldPasswdEqualsNew"))
        assertNull(ChangePasswordRest.fieldIdOf("something.else"))
    }

    /**
     * A request of a session with a csrf token, sending that token or [token] instead.
     */
    private fun requestWithToken(token: String? = null): MockHttpServletRequest {
        val request = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
        val sessionToken = sessionCsrfService.createServerData(request as HttpServletRequest).csrfToken!!
        request.addHeader(RestCsrfProtection.CSRF_TOKEN_HEADER, token ?: sessionToken)
        return request
    }

    private fun postData(
        userId: Long? = null,
        login: String = "wrong",
        new: String = "new-Pass42!",
        repeat: String = new,
    ) = PostData(
        ChangePasswordData(
            userId = userId,
            loginPassword = login.toCharArray(),
            newPassword = new.toCharArray(),
            passwordRepeat = repeat.toCharArray(),
        ),
        watchFieldsTriggered = null,
        serverData = null,
    )

    private fun messageIds(response: ResponseEntity<*>): List<String?> {
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.statusCode)
        return (response.body as ResponseAction).validationErrors!!.map { it.messageId }
    }

        private fun fieldIds(response: ResponseEntity<*>): List<String?> {
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.statusCode)
        return (response.body as ResponseAction).validationErrors!!.map { it.fieldId }
    }
}
