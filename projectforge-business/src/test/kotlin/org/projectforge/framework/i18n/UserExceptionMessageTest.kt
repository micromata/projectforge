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

package org.projectforge.framework.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.Constants
import org.projectforge.common.i18n.MessageParam
import org.projectforge.common.i18n.MessageParamType
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.i18n.I18nHelper.addBundleName

/**
 * What a [UserException] reads like when it reaches the user — [translateMsg].
 *
 * The case worth a test is the param that is an i18n key itself: an invoice saved as issued without a
 * number is refused with `validation.required.valueNotPresent` and the *label of the field* as its param,
 * and the whole point of that param type is that the label is translated too. Wicket does it
 * (`AbstractSecuredBasePage.translateParams`), the REST toast did not — it showed
 * "Wert 'fibu.rechnung.nummer' nicht gegeben".
 */
class UserExceptionMessageTest {

    @BeforeEach
    fun addBundle() {
        addBundleName(Constants.RESOURCE_BUNDLE_NAME)
    }

    @Test
    fun `a param that is an i18n key names the field, not the key`() {
        val ex = UserException(
            "validation.required.valueNotPresent",
            MessageParam("fibu.rechnung.nummer", MessageParamType.I18N_KEY),
        )

        val message = translateMsg(ex)

        // Spelled out through the bundle rather than as a literal: the expectation is the message with the
        // *label* in it, in whatever language the test runs in.
        assertEquals(
            translateMsg("validation.required.valueNotPresent", translate("fibu.rechnung.nummer")),
            message,
        )
        // And said again the other way round, because the line above would also hold if the bundle were
        // missing and both sides fell back to the key - which is the very bug this is about.
        assertFalse(message.contains("fibu.rechnung.nummer"), "The key must not reach the user: $message")
    }

    @Test
    fun `a plain param goes into the message as it is`() {
        val ex = UserException("validation.required.valueNotPresent", MessageParam("4711"))

        // MessageParamType.VALUE: nothing to look up, and nothing that may be looked up - a value that
        // happens to read like a key is still the value.
        assertEquals(
            translateMsg("validation.required.valueNotPresent", "4711"),
            translateMsg(ex),
        )
    }
}
