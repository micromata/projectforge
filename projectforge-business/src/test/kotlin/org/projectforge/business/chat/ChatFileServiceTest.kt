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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import javax.imageio.ImageIO

/**
 * The files of the chat: refused for a host without files, the type of an image by content, metadata removed, huge
 * images refused before decoding. That files are taken where allowed shows `DataTransferChatAreaTest`.
 */
class ChatFileServiceTest {
    @Test
    fun `the type is detected by the content`() {
        assertEquals(ChatFileService.ImageType.PNG, ChatFileService.detectType(png(4, 3)))
        assertEquals(null, ChatFileService.detectType("<svg onload=alert(1)>".toByteArray()))
    }

    @Test
    fun `an image is encoded again, a huge one is refused without decoding`() {
        val small = png(4, 3)
        assertEquals(12L, ChatFileService.pixels(small))
        val stripped = ChatFileService.stripMetadata(small, ChatFileService.ImageType.PNG)
        assertEquals(4, ImageIO.read(stripped.inputStream()).width)

        // A header only, declaring 50000 x 50000 pixels (about 10 GB decoded).
        val bomb = pngHeader(50_000, 50_000)
        assertEquals(2_500_000_000L, ChatFileService.pixels(bomb))
        assertThrows(UserException::class.java) {
            ChatFileService.stripMetadata(bomb, ChatFileService.ImageType.PNG)
        }
        assertFalse(ChatFileService.pixels("no image".toByteArray()) != null)
    }

    @Test
    fun `a posted file is refused by a host without files, before anything is stored`() {
        val conversation = ChatConversationDO().also { it.id = 1; it.hostId = 2; it.channel = "internal" }
        val context = ChatAccess.Context(conversation, NoFilesHost(), PFUserDO(), "case", canWrite = true, isModerator = false)
        val service = ChatFileService()
        assertFalse(service.filesAllowed(context))
        val ex = assertThrows(UserException::class.java) {
            service.store(context, listOf(ChatFileService.Upload("notes.txt", "notes".toByteArray())))
        }
        assertEquals(ChatFileService.ERROR_NOT_ALLOWED, ex.i18nKey)
        assertEquals(emptyList<ChatAttachment>(), service.store(context, emptyList()), "a message without files passes")
    }

    /** A host like the discussion of a CVE case: written by everybody, no files. */
    private class NoFilesHost : ChatHost {
        override val hostType = "noFiles"
        override val searchableAcrossConversations = false
        override val usesOwnFileArea = false
        override fun channels(hostId: Long) = listOf("internal")
        override fun title(hostId: Long, channel: String) = "case"
        override fun link(hostId: Long): String? = null
        override fun hasReadAccess(user: PFUserDO, hostId: Long, channel: String) = true
        override fun hasWriteAccess(user: PFUserDO, hostId: Long, channel: String) = true
        override fun isModerator(user: PFUserDO, hostId: Long, channel: String) = false
        override fun participantIds(hostId: Long, channel: String) = emptySet<Long>()
        override fun filesAllowed(hostId: Long, channel: String) = false
    }

    private fun png(width: Int, height: Int): ByteArray {
        val out = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", out)
        return out.toByteArray()
    }

    /** The PNG signature and the header chunk (IHDR), no image data. */
    private fun pngHeader(width: Int, height: Int): ByteArray {
        val data = ByteBuffer.allocate(13).putInt(width).putInt(height)
            .put(8).put(2).put(0).put(0).put(0).array() // 8 bit RGB, no interlace
        val type = "IHDR".toByteArray()
        val crc = CRC32().also { it.update(type); it.update(data) }.value.toInt()
        return ByteBuffer.allocate(8 + 4 + 4 + 13 + 4)
            .put(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            .putInt(13).put(type).put(data).putInt(crc).array()
    }
}
