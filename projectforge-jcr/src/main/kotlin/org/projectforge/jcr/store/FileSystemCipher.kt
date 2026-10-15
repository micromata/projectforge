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
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts the files of the file system storage at rest ([EncryptionType.AES_CTR]), so backups of the file system
 * don't contain them in plain text. The key is derived once from `projectforge.files.fileSystemEncryptionKey`.
 *
 * Format of an encrypted file: magic `PFE1` (4 bytes), fingerprint of the key (8 bytes), random IV (16 bytes),
 * followed by the content encrypted with AES-256/CTR (same length as the plain content, no padding).
 *
 * Both directions are streamed with small buffers, so files of 10 GB need no more memory than small ones. There is
 * no authentication tag: the integrity is verified by the checksum of the content (see [FileStore.checkSanity]) and
 * by the CRC of compressed files.
 */
internal class FileSystemCipher private constructor(private val key: SecretKey) {
    private val random = SecureRandom()

    /**
     * Identifies the key, so a file encrypted with another key is detected instead of being returned as garbage.
     */
    private val fingerprint: ByteArray = Mac.getInstance("HmacSHA256").let { mac ->
        mac.init(key)
        mac.doFinal(FINGERPRINT_INFO.toByteArray()).copyOf(FINGERPRINT_LENGTH)
    }

    /**
     * Writes the header and returns a stream encrypting all bytes written to it into the given stream. Closing the
     * returned stream closes the given one.
     */
    fun encrypt(out: OutputStream): OutputStream {
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }
        out.write(MAGIC)
        out.write(fingerprint)
        out.write(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        return CipherOutputStream(out, cipher)
    }

    /**
     * Reads and checks the header and returns a stream decrypting the given one. Closing the returned stream closes the
     * given one.
     * @throws IOException if the header is missing or the file was encrypted with another key.
     */
    fun decrypt(input: InputStream): InputStream {
        val header = input.readNBytes(HEADER_LENGTH)
        if (header.size < HEADER_LENGTH || !header.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw IOException("Content isn't encrypted by the file store (header missing).")
        }
        if (!MessageDigest.isEqual(header.copyOfRange(MAGIC.size, MAGIC.size + FINGERPRINT_LENGTH), fingerprint)) {
            throw IOException("Content was encrypted with another key (projectforge.files.fileSystemEncryptionKey changed?).")
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(header.copyOfRange(HEADER_LENGTH - IV_LENGTH, HEADER_LENGTH)))
        return DecryptingInputStream(input, cipher)
    }

    /**
     * [javax.crypto.CipherInputStream] reads only 512 bytes per call, this one reads as many as requested (up to
     * [BUFFER_SIZE]). CTR is a stream mode: each input byte results in exactly one output byte.
     */
    private class DecryptingInputStream(input: InputStream, private val cipher: Cipher) : FilterInputStream(input) {
        private val buffer = ByteArray(BUFFER_SIZE)

        override fun read(): Int {
            val b = ByteArray(1)
            while (true) {
                val n = read(b, 0, 1)
                if (n < 0) {
                    return -1
                }
                if (n == 1) {
                    return b[0].toInt() and 0xFF
                }
            }
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len == 0) {
                return 0
            }
            val n = `in`.read(buffer, 0, minOf(len, buffer.size))
            if (n < 0) {
                return -1
            }
            return cipher.update(buffer, 0, n, b, off)
        }

        /**
         * Skipping the underlying stream would desynchronize the key stream, so the bytes are read and discarded.
         */
        override fun skip(n: Long): Long {
            var remaining = n
            val discard = ByteArray(minOf(n, BUFFER_SIZE.toLong()).toInt().coerceAtLeast(1))
            while (remaining > 0) {
                val read = read(discard, 0, minOf(remaining, discard.size.toLong()).toInt())
                if (read < 0) {
                    break
                }
                remaining -= read
            }
            return n - remaining
        }

        override fun markSupported(): Boolean = false

        override fun mark(readlimit: Int) {}

        override fun reset() {
            throw IOException("mark/reset not supported.")
        }
    }

    companion object {
        private const val TRANSFORMATION = "AES/CTR/NoPadding"
        private val MAGIC = "PFE1".toByteArray()
        private const val FINGERPRINT_LENGTH = 8
        private const val IV_LENGTH = 16
        private val HEADER_LENGTH = MAGIC.size + FINGERPRINT_LENGTH + IV_LENGTH
        private const val FINGERPRINT_INFO = "ProjectForge file store key fingerprint"

        /**
         * There is only one key per installation, so a fixed salt is sufficient (it only prevents precomputed tables).
         */
        private const val SALT = "ProjectForge file system storage"
        private const val ITERATIONS = 210_000
        private const val BUFFER_SIZE = 64 * 1024

        /**
         * Size of the header written in front of the encrypted content.
         */
        val headerLength: Int
            get() = HEADER_LENGTH

        /**
         * Derives the key (PBKDF2, takes some 100 ms, so call it once).
         * @return null, if no key is given (no encryption).
         */
        fun create(passphrase: String?): FileSystemCipher? {
            if (passphrase.isNullOrBlank()) {
                return null
            }
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(passphrase.toCharArray(), SALT.toByteArray(), ITERATIONS, 256)
            try {
                return FileSystemCipher(SecretKeySpec(factory.generateSecret(spec).encoded, "AES"))
            } finally {
                spec.clearPassword()
            }
        }
    }
}
