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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URI
import javax.imageio.ImageIO

class ChatLinkPreviewTest {
    @Test
    fun `the first http link is taken, not of code, mail or the own host`() {
        assertEquals(
            "https://example.com/a",
            ChatLinkPreview.firstUrl("""<p>see <a href="mailto:a@b.de">mail</a> and <a href="https://example.com/a">this</a></p>"""),
        )
        assertEquals(
            "https://example.org/b",
            ChatLinkPreview.firstUrl("""<p><code>https://example.com/x</code> <a href="https://pf.acme.com/next/task/1">task</a> https://example.org/b.</p>""", "pf.acme.com"),
        )
        assertEquals("https://example.com/plain?q=1", ChatLinkPreview.firstUrl("<p>(see https://example.com/plain?q=1)</p>"))
        assertEquals("https://www.example.com/w", ChatLinkPreview.firstUrl("<p>at www.example.com/w, really</p>"))
        assertNull(ChatLinkPreview.firstUrl("<p>no link</p>"))
        assertNull(ChatLinkPreview.firstUrl("""<p><a href="http://example.com:8080/">non-standard port</a></p>"""))
        assertNull(ChatLinkPreview.firstUrl(null))
    }

    @Test
    fun `open graph data is preferred, the title and description are the fallback`() {
        val og = ChatLinkPreview.parse(
            """<html><head><title>Ignored</title>
               <meta property="og:title" content="  The   title &amp; more ">
               <meta property="og:description" content="Description">
               <meta property="og:site_name" content="Site">
               <meta property="og:image" content="/img/preview.png">
               </head><body></body></html>""".toByteArray(),
            "text/html; charset=utf-8",
            URI("https://example.com/page/1"),
        )!!
        assertEquals("The title & more", og.title)
        assertEquals("Description", og.description)
        assertEquals("Site", og.siteName)
        assertEquals("https://example.com/img/preview.png", og.imageUrl)

        val plain = ChatLinkPreview.parse(
            """<html><head><title>Plain title</title><meta name="description" content="Text"></head></html>""".toByteArray(),
            null,
            URI("https://example.com/"),
        )!!
        assertEquals("Plain title", plain.title)
        assertEquals("Text", plain.description)
        assertNull(plain.imageUrl)

        val image = ChatLinkPreview.parse(
            """<html><head><title>T</title><meta property="og:image" content="file:///etc/passwd"></head></html>""".toByteArray(),
            null,
            URI("https://example.com/"),
        )!!
        assertNull(image.imageUrl)
        assertNull(ChatLinkPreview.parse("<html><body>nothing</body></html>".toByteArray(), null, URI("https://example.com/")))
    }

    @Test
    fun `thumbnails are small jpegs, other content is refused`() {
        val png = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(1600, 800, BufferedImage.TYPE_INT_ARGB), "png", it) }
        val thumbnail = ChatLinkPreviewService.thumbnail(png.toByteArray())
        assertNotNull(thumbnail)
        assertEquals(ChatFileService.ImageType.JPEG, ChatFileService.detectType(thumbnail!!.content))
        val image = ImageIO.read(thumbnail.content.inputStream())
        assertEquals(image.width, thumbnail.width)
        assertEquals(image.height, thumbnail.height)
        assertEquals(ChatLinkPreviewService.THUMBNAIL_SIZE, image.width)
        assertEquals(ChatLinkPreviewService.THUMBNAIL_SIZE / 2, image.height)
        assertNull(ChatLinkPreviewService.thumbnail("<svg onload=alert(1)>".toByteArray()))
        assertTrue(ChatLinkPreviewService.thumbnail(ByteArray(0)) == null)
        // A small image (a logo) isn't enlarged.
        val logo = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(120, 90, BufferedImage.TYPE_INT_RGB), "png", it) }
        assertEquals(120, ChatLinkPreviewService.thumbnail(logo.toByteArray())!!.width)
    }
}
