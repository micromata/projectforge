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


package org.projectforge.business.push

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.notification.NotificationDO

class PushNotifierTest {
    @Test
    fun `html becomes plain text with one line per block`() {
        assertEquals(
            "Anna wrote in Team:\nHello & welcome\nfirst\nsecond",
            PushNotifier.plainTeaser(
                "<p>Anna wrote in Team:</p><p>Hello &amp; <b>welcome</b></p><ul><li>first</li><li> second </li></ul>",
                240,
            ),
        )
        assertEquals("a\nb", PushNotifier.plainTeaser("a<br>b", 240))
        assertEquals(
            "Name Value\nCity Kassel",
            PushNotifier.plainTeaser("<table><tr><th>Name</th><td>Value</td></tr><tr><td>City</td><td>Kassel</td></tr></table>", 240),
        )
    }

    @Test
    fun `the content is the preview, else the sms text, else the body`() {
        val notification = NotificationDO().apply {
            title = "Invoice overdue"
            smsText = "Invoice 42 is overdue."
            body = "<p>The invoice <b>42</b> is overdue.</p>"
        }
        assertEquals(
            Pair("Invoice overdue", "Preview of 42"),
            PushNotifier.content(notification, true) { "Preview of 42" },
        )
        assertEquals(Pair("Invoice overdue", "Invoice 42 is overdue."), PushNotifier.content(notification, true) { null })
        notification.smsText = " "
        assertEquals(Pair("Invoice overdue", "The invoice 42 is overdue."), PushNotifier.content(notification, true) { null })
    }

    @Test
    fun `without content or text the push has the generic text`() {
        val notification = NotificationDO().apply {
            title = "Invoice overdue"
            body = "<p>The invoice 42 is overdue.</p>"
        }
        var previewed = false
        assertEquals(Pair(null, null), PushNotifier.content(notification, false) { previewed = true; "Preview" })
        assertFalse(previewed, "No preview is built without content.")
        notification.title = " "
        notification.body = "<p> </p>"
        assertEquals(Pair(null, null), PushNotifier.content(notification, true) { null })
    }

    @Test
    fun `empty html has no teaser`() {
        assertNull(PushNotifier.plainTeaser(null, 240))
        assertNull(PushNotifier.plainTeaser("<p> </p><div></div>", 240))
        assertNull(PushNotifier.teaser(" \n\t ", 240))
    }

    @Test
    fun `long texts are cut with an ellipsis`() {
        val teaser = PushNotifier.teaser("x".repeat(300), 240)!!
        assertEquals(240, teaser.length)
        assertTrue(teaser.endsWith("…"))
        assertEquals("short text", PushNotifier.teaser("short   text", 240))
        // Not in the middle of an emoji:
        val emoji = PushNotifier.teaser("x".repeat(8) + "😀😀", 10)!!
        assertEquals("x".repeat(8) + "…", emoji)
    }
}
