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

import java.io.FilterInputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Counts the bytes read and calculates the SHA-256 checksum of them. Throws [SizeLimitExceeded] as soon as more than
 * [maxSize] bytes are read, so oversized uploads are aborted early instead of being stored completely.
 */
internal class MeasuringInputStream(input: InputStream, private val maxSize: Long) : FilterInputStream(input) {
    private val digest = MessageDigest.getInstance("SHA-256")

    var count: Long = 0
        private set

    /**
     * Formatted as in the JCR: "SHA256: <hex>".
     */
    val checksum: String
        get() = "SHA256: ${digest.digest().joinToString("") { "%02x".format(it) }}"

    override fun read(): Int {
        val b = super.read()
        if (b >= 0) {
            digest.update(b.toByte())
            increment(1)
        }
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = super.read(b, off, len)
        if (n > 0) {
            digest.update(b, off, n)
            increment(n)
        }
        return n
    }

    override fun skip(n: Long): Long {
        throw UnsupportedOperationException("skip isn't supported, checksum would be wrong.")
    }

    override fun markSupported(): Boolean = false

    private fun increment(n: Int) {
        count += n
        if (count > maxSize) {
            throw SizeLimitExceeded(count)
        }
    }

    class SizeLimitExceeded(val size: Long) : RuntimeException("Maximum file size exceeded (at least $size bytes).")
}
