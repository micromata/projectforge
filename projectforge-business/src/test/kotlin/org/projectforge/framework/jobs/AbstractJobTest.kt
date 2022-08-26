/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.jobs

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.*

class AbstractJobTest {
  @Test
  fun timeoutReachedTest() {
    val job = object : AbstractJob("job1", timeoutSeconds = 10) {
      override suspend fun run() {
      }

      override fun writeAccess(user: PFUserDO?): Boolean {
        return true
      }
    }
    job.status = AbstractJob.Status.RUNNING
    job.startTime = Date()
    Assertions.assertFalse(job.timeoutReached)
    job.startTime = Date(System.currentTimeMillis() - 10001)
    Assertions.assertTrue(job.timeoutReached)
  }

  @Test
  fun keepTerminatedIntevallTest() {
    val jobHandler = JobHandler()
    var onAfterFinishedCalled = false
    val job = jobHandler.addJob(object : AbstractJob("job") {
      override suspend fun run() {
        // println("job2")
      }

      override fun writeAccess(user: PFUserDO?): Boolean {
        return true
      }

      override fun onAfterFinish() {
        // println("job2 finished")
        Assertions.assertEquals(Status.FINISHED, status)
        Assertions.assertFalse(terminatedForDeletion)
        Assertions.assertNotNull(jobHandler.getJobById(id))
        terminatedTime = Date(System.currentTimeMillis() - JobHandler.KEEP_TERMINATED_JOBS_INTERVALL_MS - 1)
        Assertions.assertTrue(terminatedForDeletion)
        jobHandler.tidyUp()
        Assertions.assertNull(jobHandler.getJobById(id))
        onAfterFinishedCalled = true
      }
    })
    runBlocking {
      for (i in 0..1000) {
        delay(100)
        if (job.status == AbstractJob.Status.FINISHED) {
          break
        }
      }
    }
    Assertions.assertTrue(onAfterFinishedCalled)
    // println(job.toString())
  }

  @Test
  fun toBeQueuedTest() {
    var job = createJob("job1", "area")
    var newJob = createJob("job1", "area")
    Assertions.assertFalse(job.isBlocking(newJob))
    newJob = createJob("job1", "area", queueStrategy = AbstractJob.QueueStrategy.PER_QUEUE)
    Assertions.assertTrue(job.isBlocking(newJob))
    newJob = createJob("job1", "area", userId = 5, queueStrategy = AbstractJob.QueueStrategy.PER_QUEUE)
    Assertions.assertTrue(job.isBlocking(newJob))

    newJob = createJob("job1", "area", queueStrategy = AbstractJob.QueueStrategy.PER_QUEUE_AND_USER)
    Assertions.assertTrue(job.isBlocking(newJob), "Both userId's are null / equal.")
    newJob = createJob("job1", "area", userId = 5, queueStrategy = AbstractJob.QueueStrategy.PER_QUEUE_AND_USER)
    Assertions.assertFalse(job.isBlocking(newJob))
    job = createJob("job1", "area", userId = 5)
    Assertions.assertTrue(job.isBlocking(newJob))
  }

  @Test
  fun displayDurationTest() {
    Assertions.assertEquals("00:00s", AbstractJob.displayDuration(Date(System.currentTimeMillis())))
    Assertions.assertEquals("00:01s", AbstractJob.displayDuration(Date(System.currentTimeMillis() - 1000)))
    Assertions.assertEquals("00:01s", AbstractJob.displayDuration(Date(System.currentTimeMillis() + 1000)))
    Assertions.assertEquals("00:59s", AbstractJob.displayDuration(Date(System.currentTimeMillis() - 59000)))
    Assertions.assertEquals("01:00s", AbstractJob.displayDuration(Date(System.currentTimeMillis() - 60000)))
    Assertions.assertEquals("01:00:00s", AbstractJob.displayDuration(Date(System.currentTimeMillis() - 1000 * 3600)))
    Assertions.assertEquals(
      "01:59:59s",
      AbstractJob.displayDuration(Date(System.currentTimeMillis() - 1000 * 3600 * 2 + 1000)),
    )
    Assertions.assertEquals(
      "109:59:59s",
      AbstractJob.displayDuration(Date(System.currentTimeMillis() - 1000 * 3600 * 110 + 1000)),
    )
  }

  private fun createJob(
    title: String,
    area: String? = null,
    userId: Int? = null,
    queueStrategy: AbstractJob.QueueStrategy = AbstractJob.QueueStrategy.NONE,
  ): AbstractJob {
    return object : AbstractJob(title, area = area, userId = userId, queueStrategy = queueStrategy) {
      override suspend fun run() {
      }

      override fun writeAccess(user: PFUserDO?): Boolean {
        return true
      }
    }
  }
}
