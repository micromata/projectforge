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

package org.projectforge.business.chat

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ChatHtmlSanitizerTest {
    private fun sanitize(html: String) = ChatHtmlSanitizer.sanitize(html) { if (it == 42L) "Erika Mustermann" else null }

    @Test
    fun `scripts, handlers and foreign attributes are removed`() {
        assertEquals(
            """<p>Hi <a href="https://example.org" target="_blank" rel="noopener noreferrer">link</a></p>""",
            sanitize("""<p onclick="x()">Hi <a href="https://example.org" onmouseover="y()">link</a><script>z()</script></p>""").html,
        )
        assertEquals("<p>x</p>", sanitize("""<p>x<img src="x" onerror="alert(1)"></p>""").html)
        assertEquals("<p>evil</p>", sanitize("""<p><a href="javascript:alert(1)">evil</a></p>""").html)
        assertEquals("", sanitize("<p> </p><p></p>").html)
        assertEquals("1 &lt; 2 &amp; so", sanitize("1 < 2 & so").html)
    }

    @Test
    fun `mentions are kept for accepted users only, named by the caller`() {
        val result = sanitize(
            """<p><span data-type="mention" data-id="42" data-label="Fake" style="color: red">@Fake</span>
            |<span data-type="mention" data-id="7">@Other</span> <span data-id="42">no type</span></p>""".trimMargin()
        )
        assertEquals(listOf(42L), result.mentionedIds)
        assertEquals(
            """<p><span data-type="mention" data-id="42">@Erika Mustermann</span>
            |@Other no type</p>""".trimMargin(),
            result.html,
        )
        assertEquals("@Erika Mustermann @Other no type", result.text)
    }

    @Test
    fun `image types are detected by content`() {
        assertEquals(ChatImageService.ImageType.PNG, ChatImageService.detectType(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0)))
        assertEquals(ChatImageService.ImageType.JPEG, ChatImageService.detectType(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0)))
        assertEquals(ChatImageService.ImageType.GIF, ChatImageService.detectType("GIF89a...".toByteArray()))
        assertEquals(ChatImageService.ImageType.WEBP, ChatImageService.detectType("RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".toByteArray()))
        assertNull(ChatImageService.detectType("<svg onload=alert(1)>".toByteArray()))
        assertNull(ChatImageService.detectType("%PDF-1.7".toByteArray()))
        val gif = "GIF89a...".toByteArray()
        assertArrayEquals(gif, ChatImageService.stripMetadata(gif, ChatImageService.ImageType.GIF))
    }
}
