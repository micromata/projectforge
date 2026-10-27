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


package org.projectforge.rest.pub.next

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.service.PasswordResetService
import org.springframework.beans.factory.annotation.Autowired

class PasswordResetNextRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var passwordResetNextRest: PasswordResetNextRest

    @Autowired
    private lateinit var passwordResetService: PasswordResetService

    @BeforeEach
    @AfterEach
    fun clearRateLimits() {
        passwordResetService.clearRateLimits()
    }

    /**
     * The 11th request of a client within an hour is refused, whatever the user is. Up to then, the answer is the
     * same for an existing and an unknown user (no user enumeration).
     */
    @Test
    fun `password reset mails are throttled per client`() {
        val request = mockRequest("203.0.113.77")
        repeat(10) {
            val usernameEmail = if (it % 2 == 0) TEST_USER else "unknown-user-$it"
            val result = passwordResetNextRest.requestMail(request, NextPasswordForgottenPostData(usernameEmail)).body!!
            assertTrue(result.success, "Request #${it + 1} for '$usernameEmail'.")
        }
        val refused = passwordResetNextRest.requestMail(request, NextPasswordForgottenPostData("unknown-user")).body!!
        assertFalse(refused.success)
        assertEquals(translate("password.forgotten.tooManyRequests"), refused.message)
        val otherClient = passwordResetNextRest.requestMail(
            mockRequest("203.0.113.78"),
            NextPasswordForgottenPostData("unknown-user"),
        ).body!!
        assertTrue(otherClient.success, "Other clients aren't affected.")
    }

    private fun mockRequest(remoteAddr: String): HttpServletRequest {
        val request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.remoteAddr).thenReturn(remoteAddr)
        return request
    }
}
