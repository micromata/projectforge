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

package org.projectforge.framework.integration

import mu.KotlinLogging
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private val log = KotlinLogging.logger {}

/**
 * Runs background jobs of one kind (e.g. the LDAP sync after a cache refresh) in one named daemon thread
 * instead of a new thread per trigger. At most one job runs and one waits: further triggers while one is
 * waiting are dropped, because the waiting job will see their changes anyway. So no trigger is lost, jobs
 * never overlap and the number of threads stays fixed, however often the trigger fires.
 */
class SingleRunExecutor(private val name: String) {
    private val threadCounter = AtomicInteger()

    private val executor = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(1),
        { runnable ->
            Thread(runnable, "$name-${threadCounter.incrementAndGet()}").also { it.isDaemon = true }
        },
        ThreadPoolExecutor.DiscardPolicy(),
    )

    /**
     * @return false, if the job was dropped, because another one is already waiting.
     */
    fun trigger(job: () -> Unit): Boolean {
        if (executor.queue.isNotEmpty()) {
            log.debug { "$name: job already pending, trigger coalesced." }
            return false
        }
        executor.execute {
            try {
                job()
            } catch (e: Throwable) {
                log.error(e) { "$name: job failed: ${e.message}" }
            }
        }
        return true
    }

    /**
     * True, if a job is running or waiting.
     */
    val busy: Boolean
        get() = executor.activeCount > 0 || executor.queue.isNotEmpty()

    fun shutdown() {
        executor.shutdownNow()
    }
}
