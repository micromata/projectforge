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

package org.projectforge.jcr.store

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.random.Random

class FileSystemCipherTest {
    @Test
    fun `encrypted content is decrypted by any read pattern`() {
        val cipher = FileSystemCipher.create("key")!!
        val content = Random(1).nextBytes(200_000)
        val encrypted = encrypt(cipher, content)
        Assertions.assertEquals(content.size + FileSystemCipher.headerLength, encrypted.size)
        Assertions.assertFalse(encrypted.copyOfRange(FileSystemCipher.headerLength, encrypted.size).contentEquals(content))
        Assertions.assertFalse(encrypted.contentEquals(encrypt(cipher, content)), "Each file should get its own IV.")

        cipher.decrypt(encrypted.inputStream()).use {
            Assertions.assertArrayEquals(content, it.readBytes())
        }
        // Single bytes, odd buffer sizes and skip:
        cipher.decrypt(encrypted.inputStream()).use { istream ->
            val out = ByteArrayOutputStream()
            out.write(istream.read())
            val buffer = ByteArray(777)
            out.write(buffer, 0, istream.read(buffer, 0, 13))
            Assertions.assertEquals(1000, istream.skip(1000))
            out.write(ByteArray(1000))
            istream.copyTo(out, 4097)
            val result = out.toByteArray()
            Assertions.assertEquals(content.size, result.size)
            Assertions.assertArrayEquals(content.copyOfRange(0, 14), result.copyOfRange(0, 14))
            Assertions.assertArrayEquals(content.copyOfRange(1014, content.size), result.copyOfRange(1014, content.size))
        }
    }

    @Test
    fun `another key or plain content is rejected`() {
        val encrypted = encrypt(FileSystemCipher.create("key")!!, "Hello".toByteArray())
        Assertions.assertThrows(IOException::class.java) {
            FileSystemCipher.create("other key")!!.decrypt(encrypted.inputStream())
        }
        Assertions.assertThrows(IOException::class.java) {
            FileSystemCipher.create("key")!!.decrypt("Plain content, not encrypted at all.".byteInputStream())
        }
        Assertions.assertNull(FileSystemCipher.create(" "))
    }

    private fun encrypt(cipher: FileSystemCipher, content: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        cipher.encrypt(out).use { it.write(content) }
        return out.toByteArray()
    }
}
