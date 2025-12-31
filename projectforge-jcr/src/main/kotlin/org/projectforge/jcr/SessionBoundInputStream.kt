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

package org.projectforge.jcr

import mu.KotlinLogging
import java.io.InputStream

private val log = KotlinLogging.logger {}

/**
 * InputStream wrapper that ensures proper cleanup of JCR Session resources.
 * When this stream is closed, it will also properly close the associated JCR session.
 */
internal class SessionBoundInputStream(
    private val delegate: InputStream,
    private val session: SessionWrapper
) : InputStream() {

    @Volatile
    private var closed = false

    override fun read(): Int {
        checkClosed()
        return delegate.read()
    }

    override fun read(b: ByteArray): Int {
        checkClosed()
        return delegate.read(b)
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        checkClosed()
        return delegate.read(b, off, len)
    }

    override fun skip(n: Long): Long {
        checkClosed()
        return delegate.skip(n)
    }

    override fun available(): Int {
        checkClosed()
        return delegate.available()
    }

    override fun mark(readlimit: Int) {
        if (!closed) {
            delegate.mark(readlimit)
        }
    }

    override fun reset() {
        checkClosed()
        delegate.reset()
    }

    override fun markSupported(): Boolean {
        return if (closed) false else delegate.markSupported()
    }

    override fun close() {
        if (!closed) {
            closed = true
            try {
                delegate.close()
            } catch (ex: Exception) {
                log.warn(ex) { "Error closing delegate InputStream" }
            } finally {
                try {
                    session.logout()
                } catch (ex: Exception) {
                    log.error(ex) { "Error closing JCR session" }
                }
            }
        }
    }

    private fun checkClosed() {
        if (closed) {
            throw IllegalStateException("Stream has been closed")
        }
    }
}
