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
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

/**
 * Compiles and runs real Kotlin scripts through the scripting engine (class path mode, not the fat jar mode of
 * JarExtractor). The engine depends on the embedded Kotlin compiler, so this test guards Kotlin upgrades.
 */
class KotlinScriptExecutionTest : AbstractTestBase() {
    @Autowired
    private lateinit var scriptDao: ScriptDao

    @Test
    fun `example scripts compile and run`() {
        logon(TEST_FINANCE_USER)
        listOf("helloWorld.kts", "simpleExcelExport.kts", "advancedExcelExport.kts").forEach { filename ->
            val source = this::class.java.getResource("/example-scripts/$filename")!!.readText()
            val result = execute(source)
            assertSuccess(filename, result)
        }
    }

    @Test
    fun `script with parameter and return value`() {
        logon(TEST_FINANCE_USER)
        val script = ScriptDO().apply {
            type = ScriptDO.ScriptType.KOTLIN
            scriptAsString = """
                val numbers = (1..count!!.toInt()).map { it * 2 }
                log.info("numbers: ${'$'}numbers")
                numbers.sum()
            """.trimIndent()
            parameter1Name = "count"
            parameter1Type = ScriptParameterType.INTEGER
        }
        val param = ScriptParameter("count", ScriptParameterType.INTEGER).apply { intValue = 4 }
        val result = scriptDao.execute(script, listOf(param), emptyMap(), null, ScriptLogger())
        assertSuccess("parameter script", result)
        Assertions.assertEquals(20, result.result)
    }

    @Test
    fun `compile error is reported`() {
        logon(TEST_FINANCE_USER)
        val result = execute("val x: Int = \"no int\"")
        Assertions.assertTrue(result.scriptLogger.hasErrors, "Compile error expected in script log.")
        // Line numbers refer to the effective script (incl. auto imports and bindings), so check the marked source:
        Assertions.assertTrue(
            result.scriptLogger.messages.any { it.message?.contains(""">>>"no int"<<<""") == true },
            "Compile error should mark the erroneous code: ${messages(result)}",
        )
    }

    private fun execute(source: String): ScriptExecutionResult {
        val script = ScriptDO().apply {
            type = ScriptDO.ScriptType.KOTLIN
            scriptAsString = source
        }
        return scriptDao.execute(script, emptyList(), emptyMap(), null, ScriptLogger())
    }

    private fun assertSuccess(name: String, result: ScriptExecutionResult) {
        Assertions.assertNull(result.exception, "$name: ${result.exception}")
        Assertions.assertFalse(result.scriptLogger.hasErrors, "$name: ${messages(result)}")
        Assertions.assertNotNull(result.result, "$name: no result. ${messages(result)}")
        Assertions.assertFalse(result.result is Throwable, "$name: ${result.result}")
    }

    private fun messages(result: ScriptExecutionResult): String {
        return result.scriptLogger.messages.joinToString("\n") { "${it.level}: ${it.message}" }
    }
}
