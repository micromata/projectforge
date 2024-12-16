/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.carddav

import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.projectforge.ProjectForgeVersion
import org.projectforge.carddav.TestUtils.sanitizeContent
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.utils.RequestLog
import org.projectforge.rest.utils.ResponseUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

private val log = KotlinLogging.logger {}

internal object TestUtils {
    /**
     * Set by [CardDavConfig] to enable test mode.
     */
    var testMode = false

    private val logFile: File by lazy {
        val tempDir = ConfigXml.getInstance().tempDirectory
        val file = File(tempDir, "projectforge-carddav-test-${PFDateTime.now().format4Filenames()}.log")
        val text = "ProjectForge ${ProjectForgeVersion.VERSION} CardDav test log started..."
        Files.write(Paths.get(file.absolutePath), text.toByteArray())
        log.info { "Writing CardDav requests to ${file.absolutePath}." }
        file
    }

    fun writeRequestResponseLogInTestMode(
        requestWrapper: RequestWrapper,
        response: HttpServletResponse,
        responseContent: String = "",
    ) {
        if (!testMode) {
            return
        }
        val sb = StringBuilder()
        sb.appendLine("---------------------------")
        sb.appendLine("-- new request: ${PFDateTime.now().isoStringSeconds}")
        sb.appendLine("--              ${requestWrapper.method}: ${requestWrapper.requestURI}")
        sb.appendLine("-- ")
        sb.appendLine("Request:")
        sb.appendLine("request=${RequestLog.asJson(requestWrapper.request, longForm = true)}")
        sb.appendLine("body=[${requestWrapper.body}]")
        sb.appendLine()
        sb.appendLine("Response:")
        sb.appendLine("response=${ResponseUtils.asJson(response)}")
        sb.appendLine("content=[$responseContent]")
        sb.appendLine()
        append(sb.toString())
    }

    private fun append(text: String) {
        Files.write(
            Paths.get(logFile.absolutePath),
            sanitizeContent(text).toByteArray(),
            StandardOpenOption.APPEND
        )
    }

    /**
     * Removes the content of PHOTO lines from a vCard.
     */
    fun sanitizeContent(content: String): String {
        val result = StringBuilder()
        val lines = content.lines()
        var skipFollowingLines = false

        for (line in lines) {
            when {
                line.startsWith("PHOTO;", true) -> {
                    result.append(line)
                    result.appendLine("...")
                    skipFollowingLines = true // Folgende Zeilen ignorieren
                }
                line.startsWith(" ") && skipFollowingLines -> {
                    continue
                }
                else -> {
                    result.appendLine(line)
                    skipFollowingLines = false
                }
            }
        }

        return result.toString()
    }}

fun main() {
    val vcard = """
        EMAIL;TYPE=work:
        NOTE:
        PHOTO;ENCODING=b;TYPE=jpeg:iVBORw0KGgoAAAANSUhEUgAAAfQAAAIICAYAAACYbhx1AAAM
         Z2lDQ1BJQ0MgUHJvZmlsZQAASImVVwdYU8kWnluSkJDQAhGQEnoTpFcpIbQAAlIFGyEJJJQYEo
         aVb0yM+OORK3nvOVyBoKx9dfDgr0YWzDnwIyMDmX7jr/DbiFbMP/A+tHM8tp7kObAAAAAElFTk
         SuQmCC
        END:VCARD
        BEGIN:VCARD
        EMAIL;TYPE=work:
        NOTE:
        PHOTO;ENCODING=b;TYPE=jpeg:iVBORw0KGgoAAAANSUhEUgAAAfQAAAIICAYAAACYbhx1AAAM
         Z2lDQ1BJQ0MgUHJvZmlsZQAASImVVwdYU8kWnluSkJDQAhGQEnoTpFcpIbQAAlIFGyEJJJQYEo
         aVb0yM+OORK3nvOVyBoKx9dfDgr0YWzDnwIyMDmX7jr/DbiFbMP/A+tHM8tp7kObAAAAAElFTk
         SuQmCC
        END:VCARD
    """.trimIndent()

    val sanitizedVCard = sanitizeContent(vcard)
    println(sanitizedVCard)
}
