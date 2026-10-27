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

package org.projectforge.business.scripting

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

class ScriptLoggerTest {

    /**
     * The user's client reads the log (`AbstractScriptExecuteRest.refresh`) while the script still writes to it:
     * reading must not fail with a ConcurrentModificationException.
     */
    @Test
    fun readWhileLoggingTest() {
        val scriptLogger = ScriptLogger()
        var readError: Throwable? = null
        val writer = thread {
            repeat(LINES) { scriptLogger.debug { "line $it" } }
        }
        val reader = thread {
            try {
                while (writer.isAlive) {
                    scriptLogger.messages.map { it.message }
                    scriptLogger.hasErrors
                }
            } catch (ex: Throwable) {
                readError = ex
            }
        }
        writer.join()
        reader.join()
        Assertions.assertNull(readError)
        Assertions.assertEquals(LINES, scriptLogger.messages.size)
        Assertions.assertEquals("line ${LINES - 1}", scriptLogger.messages.last().message)
    }

    companion object {
        private const val LINES = 10_000
    }
}
