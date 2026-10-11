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

package org.projectforge.business.task

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * The task tree is read by many requests while an insert or move changes it: a reader walking the children
 * recursively (durations, dates, consumption) must not fail on a child added or removed meanwhile.
 */
class TaskNodeConcurrencyTest {
    @Test
    fun `walking the children while they change does not fail`() {
        val root = node(1, "root")
        val parent = node(2, "parent").also { it.setParent(root); root.addChild(it) }
        repeat(50) { i -> child(parent, 100L + i) }
        val failure = AtomicReference<Throwable?>()
        val writer = thread {
            try {
                repeat(5_000) { i ->
                    val child = child(parent, 1_000L + i)
                    parent.removeChild(child)
                }
            } catch (ex: Throwable) {
                failure.compareAndSet(null, ex)
            }
        }
        try {
            while (writer.isAlive) {
                // Stats are set on every node, so the tree is never asked to read them from the data base.
                root.getDuration(null, true)
                root.getLatestTimesheetStopDate(null, true)
                root.descendantIds
            }
        } catch (ex: Throwable) {
            failure.compareAndSet(null, ex)
        }
        writer.join()
        Assertions.assertNull(failure.get())
        Assertions.assertEquals(51, root.descendantIds.size)
        Assertions.assertEquals(50L * 60, root.getDuration(null, true))
    }

    private fun child(parent: TaskNode, id: Long): TaskNode =
        node(id, "child $id").also {
            it.setParent(parent)
            parent.addChild(it)
        }

    private fun node(id: Long, title: String): TaskNode =
        TaskNode().also { node ->
            node.setTask(TaskDO().also { it.id = id; it.title = title })
            node.setTimesheetStats(if (id >= 100) 60 else 0, null, null)
        }
}
