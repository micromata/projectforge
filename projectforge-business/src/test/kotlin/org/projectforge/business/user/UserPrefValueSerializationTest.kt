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

package org.projectforge.business.user

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.framework.utils.RecentQueue

/**
 * Value types that used to be stored via XStream must round-trip through the JSON store ([UserPrefDao]) after the
 * XML->JSON migration. This guards against Jackson generics erosion for the legacy select-panel and task-tree prefs.
 */
class UserPrefValueSerializationTest {
    @Test
    fun recentQueueOfStringRoundTrip() {
        val queue = RecentQueue<String>()
        queue.append("first")
        queue.append("second")
        queue.append("third")

        val serialized = UserPrefDao.serialize(queue, compressBigContent = false)
        assertTrue(serialized.startsWith("^JSON:"), "Expected JSON marker: $serialized")

        @Suppress("UNCHECKED_CAST")
        val restored = UserPrefDao.fromJson(serialized, RecentQueue::class.java) as RecentQueue<String>?
        assertNotNull(restored)
        // append() prepends, so the newest entry is first:
        assertEquals(listOf("third", "second", "first"), restored!!.recentList)
    }

    @Test
    fun setOfLongRoundTrip() {
        val ids: Set<Long> = linkedSetOf(1L, 2L, 100000000000L)

        val serialized = UserPrefDao.serialize(ids, compressBigContent = false)
        assertTrue(serialized.startsWith("^JSON:"), "Expected JSON marker: $serialized")

        val restored = UserPrefDao.fromJson(serialized, ids.javaClass)
        assertNotNull(restored)
        // The JSON store cannot preserve the element type of a raw collection (no generic type info is persisted):
        // small values come back as Integer, larger ones as Long. Only the numeric values are guaranteed, which is
        // why callers reading Set<Long> prefs (e.g. TaskTreeExpansion) must normalize to Long.
        assertEquals(
            ids,
            restored!!.map { (it as Number).toLong() }.toSet(),
            "Set<Long> numeric content should survive the JSON round-trip (element type may widen).",
        )
    }
}
