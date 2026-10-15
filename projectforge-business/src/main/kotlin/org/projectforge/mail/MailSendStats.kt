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

package org.projectforge.mail

import org.projectforge.framework.integration.IntegrationErrors
import org.projectforge.framework.integration.SubsystemSync
import org.projectforge.framework.integration.SyncStats

/**
 * The outcome of the mails sent since the start, for the mail tile of the problem dashboard
 * ([MailSubsystemStatusProvider]). Not a [SyncStats]: a mail isn't a sync run, its failures are already
 * reported by [MailLogEvents.SEND_FAILED].
 */
class MailSendStats {
    private var sent = 0
    private var lastRun: Long? = null
    private var lastStatus: SyncStats.Status? = null
    private var lastDurationMs: Long? = null
    private var lastError: String? = null
    private var lastErrorDate: Long? = null

    /** @param start The start of the sending, epoch millis. */
    @Synchronized
    fun recordSuccess(start: Long) {
        record(start, SyncStats.Status.SUCCESS)
    }

    /** An unreachable mail server is [SyncStats.Status.ABORTED], any other failure (e.g. a rejected recipient) not. */
    @Synchronized
    fun recordFailure(start: Long, ex: Exception) {
        val status = if (IntegrationErrors.isConnectionError(ex)) SyncStats.Status.ABORTED else SyncStats.Status.ERRORS
        record(start, status)
        lastError = ex.message ?: ex.javaClass.simpleName
        lastErrorDate = System.currentTimeMillis()
    }

    /** Null, if no mail was sent yet. */
    @Synchronized
    fun toSync(): SubsystemSync? {
        if (sent == 0) {
            return null
        }
        return SubsystemSync(
            type = TYPE,
            runs = sent,
            lastRun = lastRun,
            lastStatus = lastStatus,
            lastDurationMs = lastDurationMs,
            lastError = lastError,
            lastErrorDate = lastErrorDate,
        )
    }

    private fun record(start: Long, status: SyncStats.Status) {
        sent++
        lastRun = start
        lastStatus = status
        lastDurationMs = System.currentTimeMillis() - start
    }

    companion object {
        const val TYPE = "mail"
    }
}
