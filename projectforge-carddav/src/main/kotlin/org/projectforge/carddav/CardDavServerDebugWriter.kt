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
import org.projectforge.carddav.CardDavServerDebugWriter.sanitizeContent
import org.projectforge.common.CSVWriter
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.utils.RequestLog
import org.projectforge.rest.utils.ResponseUtils
import java.io.File
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

private val log = KotlinLogging.logger {}

/**
 * For debugging purposes. If a debug user is given (in projectforge.properties), all requests and responses are written to log files.
 * @see CardDavConfig
 */
internal object CardDavServerDebugWriter {
    /**
     * Set by [CardDavConfig] to enable test mode.
     */
    var debugUser = ""

    private val logFile: File by lazy {
        prepareLogFile("projectforge-carddav-$debugUser-calls.log", writeCsvHeadLine())
    }

    private val logDetailFile: File by lazy {
        val text = """**********************************************************************************
                     |**********************************************************************************
                     |**
                     |** ${PFDateTime.now().isoStringSeconds}: ProjectForge ${ProjectForgeVersion.VERSION} CardDav test log started...
                     |**
                     |**********************************************************************************
                     |**********************************************************************************
                     |""".trimMargin()
        prepareLogFile("projectforge-carddav-$debugUser-detail.log", text)
    }

    fun writeRequestResponseLogInTestMode(
        requestWrapper: RequestWrapper,
        response: HttpServletResponse,
        responseContent: String = "",
    ) {
        if (debugUser.isBlank()) {
            return
        }
        val loggedInUser = ThreadLocalUserContext.loggedInUser
        if (loggedInUser?.username != debugUser) {
            return
        }
        val date = PFDateTime.now().isoStringSeconds
        val sb = StringBuilder()
        sb.appendLine("---------------------------")
        sb.appendLine("-- new request: $date")
        sb.appendLine("--              user-agent=${requestWrapper.request.getHeader("User-Agent")}")
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
        append(logDetailFile, sb.toString())
        append(logFile, writeCsvLine(date, requestWrapper))
    }

    private fun writeCsvHeadLine(): String {
        return """"date";"uri";"method";"user-agent","auth-type","body""""
    }

    private fun writeCsvLine(date: String, requestWrapper: RequestWrapper): String {
        val uri = requestWrapper.requestURI
        val method = requestWrapper.method
        val userAgent = requestWrapper.request.getHeader("User-Agent")
        val authType = requestWrapper.request.authType
        return """${asCsvValue(date)};${asCsvValue(uri)};${asCsvValue(method)};${asCsvValue(userAgent)},${asCsvValue(authType)};${asCsvValue(requestWrapper.body)}"""
    }

    internal fun asCsvValue(text: String?): String {
        if (text == null) {
            return ""
        }
        val sb = StringBuilder()
        sb.append("\"")
        text.forEach { c ->
            when (c) {
                '"' -> sb.append("\"\"")
                '\r' -> sb.append("\\r")
                '\n' -> sb.append("\\n")
                else -> sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    private fun append(file: File, text: String) {
        Files.write(
            Paths.get(file.absolutePath),
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
                // PHOTO;ENCODING=b;TYPE=JPEG:
                //  9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB
                //  AQEBAQEB...
                line.startsWith("PHOTO;", true) && line.contains("encoding=b", ignoreCase = true) -> {
                    result.append(line)
                    skipFollowingLines = true // Folgende Zeilen ignorieren
                    result.appendLine("...")
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
    }

    private fun prepareLogFile(filename: String, introText: String): File {
        val tempDir = ConfigXml.getInstance().tempDirectory
        val file = File(tempDir, filename)
        if (!file.exists()) {
            file.writeText(introText)
        } else {
            file.appendText(introText)
        }
        log.info { "Writing CardDav requests to ${file.absolutePath}." }
        return file
    }
}

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
