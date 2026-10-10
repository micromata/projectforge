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

package org.projectforge.rest.scripting

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.scripting.ScriptDO
import org.projectforge.business.scripting.ScriptDao
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class ScriptPageButtonsTest : AbstractTestBase() {
    @Autowired
    private lateinit var myScriptEntityRest: MyScriptEntityRest

    @Autowired
    private lateinit var scriptDao: ScriptDao

    @Autowired
    private lateinit var scriptPageTargets: ScriptPageTargets

    @Test
    fun `page buttons show the executable scripts of the target only`() {
        val testUserId = getUser(TEST_USER).id.toString()
        logon(TEST_FINANCE_USER)
        val visible = insert("visible", "list:order,orderStatistics:forecast", testUserId) {
            buttonTooltip = "Exports the order book."
        }
        val labelled = insert("labelled", "list:order", testUserId) {
            buttonLabel = "Export"
            description = "<p>The description</p>"
        }
        val hidden = listOf(
            insert("not executable", "list:order", null),
            insert("include", "list:order", testUserId) { type = ScriptDO.ScriptType.INCLUDE },
            // Contains the target as a substring only:
            insert("other target", "list:orderPosition", testUserId),
            insert("deleted", "list:order", testUserId).also { scriptDao.markAsDeleted(it) },
        )
        val ownIds = (hidden + visible + labelled).map { it.id }

        logon(TEST_USER)
        // Ordered by name, the label defaults to it:
        val buttons = myScriptEntityRest.getPageButtons("list:order").filter { it.id in ownIds }
        assertEquals(listOf("Export", "$PREFIX-visible"), buttons.map { it.label })
        assertEquals("Exports the order book.", buttons[1].tooltip)
        assertEquals("<p>The description</p>", buttons[0].tooltip, "the description as fallback")
        assertEquals(
            listOf(visible.id),
            myScriptEntityRest.getPageButtons("orderStatistics:forecast").map { it.id }.filter { it == visible.id },
        )
    }

    @Test
    fun `page targets are sanitized`() {
        logon(TEST_FINANCE_USER)
        val ids = scriptPageTargets.getAll().map { it.id }
        assertTrue(ids.containsAll(listOf("list:order", "orderStatistics:forecast", "orderStatistics:contributionMargin")))
        assertEquals(
            listOf("list:order", "orderStatistics:forecast"),
            scriptPageTargets.sanitize(listOf("list:order", " list:order", "list:unknown", "orderStatistics:unknown", "orderStatistics:forecast")),
        )
        assertEquals("/order", scriptPageTargets.getPageTarget("list:order")?.route)
        assertEquals("/finance/statistics?tab=forecast", scriptPageTargets.getPageTarget("orderStatistics:forecast")?.route)
    }

    @Test
    fun `the context of an unknown target is empty`() {
        logon(TEST_FINANCE_USER)
        listOf(null, "list:unknown", "bogus").forEach { target ->
            val context = scriptPageTargets.createContext(target)
            assertNull(context.target, "target $target")
            assertNull(context.filter, "target $target")
        }
        val context = scriptPageTargets.createContext("list:order")
        assertEquals("list:order", context.target)
    }

    private fun insert(name: String, pageTargets: String, executableByUserIds: String?, block: ScriptDO.() -> Unit = {}): ScriptDO {
        val script = ScriptDO().apply {
            this.name = "$PREFIX-$name"
            type = ScriptDO.ScriptType.KOTLIN
            scriptAsString = "\"$name\""
            this.pageTargets = pageTargets
            this.executableByUserIds = executableByUserIds
            block()
        }
        scriptDao.insert(script)
        return script
    }

    companion object {
        private const val PREFIX = "ScriptPageButtonsTest"
    }
}
