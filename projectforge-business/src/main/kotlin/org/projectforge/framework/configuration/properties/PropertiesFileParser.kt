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

package org.projectforge.framework.configuration.properties

/**
 * One logical line of a properties file. [rawLines] are the physical lines as found in the file (more than one for
 * a value continued by a trailing backslash), so an untouched line can be written back byte by byte.
 */
sealed class PropertiesFileLine(val rawLines: List<String>) {
    class Blank(raw: String) : PropertiesFileLine(listOf(raw))

    /** A comment line (`#` or `!`). [text] is the comment without the comment char and one following space. */
    class Comment(raw: String, val text: String) : PropertiesFileLine(listOf(raw)) {
        /** `### Title` lines start a group in the bundled application.properties. */
        val groupTitle: String?
            get() = rawLines[0].trimStart().takeIf { it.startsWith("### ") }?.substring(4)?.trim()?.ifEmpty { null }
    }

    /**
     * A key/value pair. [commentedOut] is true for a comment of the form `#key=value` (an inactive, optional
     * parameter); such a line is a [Comment] too, as far as Java's Properties are concerned.
     */
    class Entry(
        rawLines: List<String>,
        val key: String,
        val value: String,
        val commentedOut: Boolean,
    ) : PropertiesFileLine(rawLines)
}

/**
 * A parser of properties files, which, unlike [java.util.Properties], keeps the comments, blank lines and the
 * order of the lines. The content must be decoded as ISO-8859-1, like Spring and [java.util.Properties] do.
 */
object PropertiesFileParser {
    /** A commented-out key/value pair: `#key=value` or `# key=value`, the key without white spaces. */
    private val commentedOutRegex = Regex("""^([A-Za-z][A-Za-z0-9._\-\[\]]*)\s*=(.*)$""")

    fun parse(content: String): List<PropertiesFileLine> {
        val physical = content.split("\r\n", "\n", "\r").let {
            // A trailing line break doesn't start another (empty) line.
            if (it.isNotEmpty() && it.last().isEmpty()) it.dropLast(1) else it
        }
        val result = mutableListOf<PropertiesFileLine>()
        var i = 0
        while (i < physical.size) {
            val line = physical[i++]
            val trimmed = line.trimStart()
            if (trimmed.isEmpty()) {
                result.add(PropertiesFileLine.Blank(line))
                continue
            }
            if (trimmed[0] == '#' || trimmed[0] == '!') {
                var text = trimmed.substring(1)
                if (text.startsWith(" ")) {
                    text = text.substring(1)
                }
                val match = commentedOutRegex.find(text)
                if (match != null) {
                    val value = unescape(match.groupValues[2].trimStart())
                    result.add(PropertiesFileLine.Entry(listOf(line), match.groupValues[1], value, commentedOut = true))
                } else {
                    result.add(PropertiesFileLine.Comment(line, text))
                }
                continue
            }
            val rawLines = mutableListOf(line)
            val logical = StringBuilder(trimmed)
            while (endsWithContinuation(logical) && i < physical.size) {
                logical.setLength(logical.length - 1)
                val next = physical[i++]
                rawLines.add(next)
                logical.append(next.trimStart())
            }
            if (endsWithContinuation(logical)) {
                logical.setLength(logical.length - 1) // Continuation at the end of the file.
            }
            val (key, value) = splitKeyValue(logical.toString())
            result.add(PropertiesFileLine.Entry(rawLines, key, value, commentedOut = false))
        }
        return result
    }

    /** An odd number of trailing backslashes continues the line. */
    private fun endsWithContinuation(sb: CharSequence): Boolean {
        var count = 0
        var pos = sb.length - 1
        while (pos >= 0 && sb[pos] == '\\') {
            count++
            pos--
        }
        return count % 2 == 1
    }

    /** Splits a logical line like [java.util.Properties.load] does and unescapes key and value. */
    internal fun splitKeyValue(line: String): Pair<String, String> {
        var keyEnd = 0
        var escaped = false
        while (keyEnd < line.length) {
            val c = line[keyEnd]
            if (escaped) {
                escaped = false
            } else if (c == '\\') {
                escaped = true
            } else if (c == '=' || c == ':' || c.isWhitespace()) {
                break
            }
            keyEnd++
        }
        var valueStart = keyEnd
        while (valueStart < line.length && line[valueStart].isWhitespace()) {
            valueStart++
        }
        if (valueStart < line.length && (line[valueStart] == '=' || line[valueStart] == ':')) {
            valueStart++
            while (valueStart < line.length && line[valueStart].isWhitespace()) {
                valueStart++
            }
        }
        return unescape(line.substring(0, keyEnd)) to unescape(line.substring(valueStart))
    }

    fun unescape(str: String): String {
        if (!str.contains('\\')) {
            return str
        }
        val sb = StringBuilder(str.length)
        var i = 0
        while (i < str.length) {
            val c = str[i++]
            if (c != '\\' || i >= str.length) {
                sb.append(c)
                continue
            }
            when (val next = str[i++]) {
                't' -> sb.append('\t')
                'n' -> sb.append('\n')
                'r' -> sb.append('\r')
                'f' -> sb.append('\u000c')
                'u' -> {
                    val hex = str.substring(i, minOf(i + 4, str.length))
                    val code = hex.takeIf { it.length == 4 }?.toIntOrNull(16)
                        ?: throw IllegalArgumentException("Malformed \\uxxxx encoding: '\\u$hex'.")
                    sb.append(code.toChar())
                    i += 4
                }

                else -> sb.append(next)
            }
        }
        return sb.toString()
    }

    fun escapeKey(key: String): String = escape(key, isKey = true)

    fun escapeValue(value: String): String = escape(value, isKey = false)

    /** Escapes like [java.util.Properties.store]: non-ASCII as `\uXXXX`, so the file stays ISO-8859-1 safe. */
    private fun escape(str: String, isKey: Boolean): String {
        val sb = StringBuilder(str.length + 8)
        str.forEachIndexed { index, c ->
            when {
                c == '\\' -> sb.append("\\\\")
                c == '\t' -> sb.append("\\t")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\u000c' -> sb.append("\\f")
                c == ' ' && (isKey || index == 0) -> sb.append("\\ ")
                isKey && (c == '=' || c == ':') -> sb.append('\\').append(c)
                (c == '#' || c == '!') && index == 0 -> sb.append('\\').append(c)
                c.code < 0x20 || c.code > 0x7e -> sb.append(String.format("\\u%04x", c.code))
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    fun formatEntry(key: String, value: String, commentedOut: Boolean = false): String {
        return "${if (commentedOut) "#" else ""}${escapeKey(key)}=${escapeValue(value)}"
    }
}
