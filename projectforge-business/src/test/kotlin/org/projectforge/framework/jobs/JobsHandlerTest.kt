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
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO

class JobsHandlerTest {
  @Test
  fun jobExecutionTest() {
    ThreadLocalUserContext.setUser(PFUserDO())
    val jobHandler = JobHandler()
    var onBeforeCancel = false
    var onAfterCancel = false
    val job = jobHandler.addJob(object : AbstractJob("Sleep job 1.000ms") {
      override suspend fun run() {
        Assertions.assertEquals(Status.RUNNING, status)
        repeat(100) { i -> // Paranoia counter
          delay(100L)
        }
      }

      override fun writeAccess(user: PFUserDO?): Boolean {
        return true
      }

      override fun onBeforeCancel() {
        Assertions.assertEquals(Status.RUNNING, status)
        onBeforeCancel = true
      }

      override fun onAfterCancel() {
        Assertions.assertEquals(Status.CANCELLED, status)
        onAfterCancel = true
      }
    })
    runBlocking {
      for (i in 0..20) {
        delay(100)
        if (job.status == AbstractJob.Status.RUNNING) {
          break
        }
      }
    }
    jobHandler.shutdownJobHandler()
    Assertions.assertTrue(onBeforeCancel)
    Assertions.assertTrue(onAfterCancel)
  }

  @Test
  fun failureTest() {
    ThreadLocalUserContext.setUser(PFUserDO())
    val jobHandler = JobHandler()
    var onAfterException = false
    val job = jobHandler.addJob(object : AbstractJob("Sleep job 1.000ms") {
      override suspend fun run() {
        throw Exception("job failed.")
      }

      override fun writeAccess(user: PFUserDO?): Boolean {
        return true
      }

      override fun onAfterException(ex: Exception) {
        Assertions.assertEquals(Status.FAILED, status)
        Assertions.assertEquals("job failed.", ex.message)
        onAfterException = true
      }
    })
    runBlocking {
      for (i in 0..20) {
        delay(100)
        if (job.status == AbstractJob.Status.FAILED) {
          break
        }
      }
    }
    Assertions.assertTrue(onAfterException)
  }

  companion object {
    @JvmStatic
    fun madin(args: Array<String>) {
      val jobHandler = JobHandler()
      val job1 = jobHandler.addJob(object : AbstractJob("Sleep job 1.000ms") {
        override suspend fun run() {
          repeat(1000) { i ->
            println("job1: I'm sleeping $i ...")
            delay(500L)
          }
        }

        override fun writeAccess(user: PFUserDO?): Boolean {
          return true
        }

        override fun onAfterCancel() {
          println("Job 1 cancelled")
        }
      })
      val job2 = jobHandler.addJob(object : AbstractJob("Sleep job 200ms") {
        override suspend fun run() {
          repeat(1000) { i ->
            println("job2: I'm sleeping $i ...")
            delay(200L)
          }
        }

        override fun writeAccess(user: PFUserDO?): Boolean {
          return true
        }

        override fun onAfterCancel() {
          println("Job 2 is going to be cancelled...")
          Thread.sleep(1000)
          println("Job 2 cancelled")
        }
      })
      println("end")
      Thread.sleep(2000)
      jobHandler.cancelJob(job1)
      Thread.sleep(2000)
      jobHandler.shutdownJobHandler()
      println("exit")
    }
  }
}
