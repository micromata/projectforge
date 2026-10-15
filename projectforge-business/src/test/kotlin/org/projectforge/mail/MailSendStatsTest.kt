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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.integration.SyncStats
import java.net.ConnectException

class MailSendStatsTest {
    @Test
    fun `outcome of the last mail`() {
        val stats = MailSendStats()
        Assertions.assertNull(stats.toSync(), "No mail sent yet.")

        val start = System.currentTimeMillis()
        stats.recordSuccess(start)
        stats.toSync()!!.let {
            Assertions.assertEquals(MailSendStats.TYPE, it.type)
            Assertions.assertEquals(1, it.runs)
            Assertions.assertEquals(start, it.lastRun)
            Assertions.assertEquals(SyncStats.Status.SUCCESS, it.lastStatus)
            Assertions.assertNull(it.lastError)
        }

        stats.recordFailure(start, IllegalStateException("Recipient rejected"))
        stats.toSync()!!.let {
            Assertions.assertEquals(2, it.runs)
            Assertions.assertEquals(SyncStats.Status.ERRORS, it.lastStatus)
            Assertions.assertEquals("Recipient rejected", it.lastError)
            Assertions.assertNotNull(it.lastErrorDate)
        }

        stats.recordFailure(start, RuntimeException("send failed", ConnectException("Connection refused")))
        Assertions.assertEquals(SyncStats.Status.ABORTED, stats.toSync()!!.lastStatus, "Mail server unreachable.")

        stats.recordSuccess(start)
        stats.toSync()!!.let {
            Assertions.assertEquals(4, it.runs)
            Assertions.assertEquals(SyncStats.Status.SUCCESS, it.lastStatus)
            Assertions.assertEquals("send failed", it.lastError, "The last error is kept.")
        }
    }
}
