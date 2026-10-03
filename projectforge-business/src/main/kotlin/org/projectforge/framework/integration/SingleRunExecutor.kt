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
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

private val log = KotlinLogging.logger {}

/**
 * Runs background jobs of one kind (e.g. the LDAP sync after a cache refresh) in one named daemon thread
 * instead of a new thread per trigger. At most one job runs and one waits: a trigger while a job is waiting
 * replaces the waiting job, because the newest job has the newest data (e.g. the users and groups of the last
 * cache refresh). So jobs never overlap, the last trigger is never lost and the number of threads stays fixed,
 * however often the trigger fires.
 */
class SingleRunExecutor(private val name: String) {
    private val threadCounter = AtomicInteger()

    private val pending = AtomicReference<(() -> Unit)?>()

    // At most one runner is queued (only if no job was pending), so the queue never grows.
    private val executor = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(),
        { runnable ->
            Thread(runnable, "$name-${threadCounter.incrementAndGet()}").also { it.isDaemon = true }
        },
    )

    /**
     * @return false, if the job replaced another one, which was still waiting.
     */
    fun trigger(job: () -> Unit): Boolean {
        if (pending.getAndSet(job) != null) {
            log.debug { "$name: waiting job replaced by a newer one." }
            return false
        }
        executor.execute {
            val next = pending.getAndSet(null) ?: return@execute
            try {
                next()
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
        get() = executor.activeCount > 0 || pending.get() != null

    fun shutdown() {
        executor.shutdownNow()
    }
}
