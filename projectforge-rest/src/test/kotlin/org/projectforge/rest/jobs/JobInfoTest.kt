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

package org.projectforge.rest.jobs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * The cancel button of a job (JobInfo.cancelId) is offered only to a user allowed to cancel it, and whether a job
 * is over doesn't depend on it.
 */
class JobInfoTest : AbstractTestBase() {
    private class TestJob(private val writable: Boolean) : AbstractJob("Test job", ownerId = null) {
        override suspend fun run() {}
        override fun readAccess(user: PFUserDO?): Boolean = true
        override fun writeAccess(user: PFUserDO?): Boolean = writable
    }

    @Test
    fun `cancel offered only with write access`() {
        // Read access only (TEST_USER watching somebody else's job): no cancel button, still not terminated.
        logon(TEST_USER)
        TestJob(writable = false).let { job ->
            assertEquals(AbstractJob.Status.WAITING, job.status)
            val info = JobInfo.create(job)
            assertNull(info.cancelId)
            assertFalse(info.terminated)
        }
        // Write access: cancellable.
        TestJob(writable = true).let { job ->
            val info = JobInfo.create(job)
            assertEquals(job.id, info.cancelId)
            assertFalse(info.terminated)
        }
    }
}
