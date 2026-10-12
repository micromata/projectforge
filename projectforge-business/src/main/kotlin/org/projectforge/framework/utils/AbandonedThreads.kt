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

package org.projectforge.framework.utils

import java.lang.ref.WeakReference

/**
 * Threads of terminated cache refreshes or scheduler runs (system dashboard, "Terminate"). A terminated run is
 * interrupted and abandoned: its lock or running flag is released at once, so a new run may start. A thread not
 * reacting to the interrupt (e.g. blocked in a JDBC call) can't be stopped on Java 20+ ([Thread.stop] is gone) and
 * lives on until it returns by itself; it may no longer change the state of its cache or job. Such threads are listed
 * here as long as they are alive, so the dashboard can show them.
 */
class AbandonedThreads {
    private val threads = mutableListOf<WeakReference<Thread>>()

    @Synchronized
    fun add(thread: Thread) {
        prune()
        threads.add(WeakReference(thread))
    }

    @Synchronized
    fun contains(thread: Thread): Boolean = threads.any { it.get() === thread }

    /** The number of abandoned threads still alive. */
    @Synchronized
    fun aliveCount(): Int {
        prune()
        return threads.size
    }

    private fun prune() {
        threads.removeAll { it.get()?.isAlive != true }
    }

    companion object {
        /** The stack trace of the thread for the log, one line per frame. */
        fun stackTraceOf(thread: Thread?): String =
            thread?.stackTrace?.joinToString(separator = "") { "\n\tat $it" } ?: " <thread already gone>"
    }
}
