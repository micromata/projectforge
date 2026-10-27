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

package org.projectforge.menu.builder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class MenuItemDefConcurrencyTest {
    /**
     * Plugins register their menu entries on the ApplicationReadyEvent, while requests already build menus: iterating
     * the children must not fail while entries are added.
     */
    @Test
    fun `children are iterable while entries are added`() {
        val parent = MenuItemDef("MISC", "menu.misc")
        parent.add(MenuItemDef("POLL", "menu.poll"))
        val failure = AtomicReference<Throwable?>()
        val reader = thread {
            try {
                repeat(20_000) {
                    parent.children?.forEach { it.id }
                }
            } catch (ex: Throwable) {
                failure.set(ex)
            }
        }
        repeat(2_000) { parent.add(MenuItemDef("PLUGIN_$it", "plugin.$it")) }
        reader.join()
        assertNull(failure.get(), "Iterating the children failed: ${failure.get()}")
        assertEquals(2_001, parent.children!!.size)
    }
}
