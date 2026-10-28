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

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * The text of a version of projectforge.properties as the properties editor's diff compares it, values of secrets
 * masked ([maskSecret]).
 *
 * With [ignoreOrder], the text is canonical, so that only real changes of values and comments remain in the diff
 * of two versions, not the re-sorting done by the editor ([ProjectForgePropertiesWriter]) or by hand:
 * - every parameter, active or commented out, is one unit with the comment lines above it (since the former
 *   parameter, across blank lines), and the units are sorted by key;
 * - the file's header (the first comment block followed by a blank line) stays at the top, the comments after the
 *   last parameter stay at the end;
 * - parameters are written uniformly (`key=value`, same escaping), blank lines and the group titles written by the
 *   editor (`### General`) are left out.
 */
object PropertiesDiffText {
    fun build(content: String, metadata: PropertiesMetadata, ignoreOrder: Boolean): String {
        val lines = PropertiesFileParser.parse(content)
        val out = if (ignoreOrder) canonical(lines, metadata) else lines.flatMap { masked(it) ?: it.rawLines }
        return out.joinToString("\n", postfix = "\n")
    }

    private class Unit(val commentLines: List<String>, val entry: PropertiesFileLine.Entry) {
        val entryLine = masked(entry)?.single() ?: PropertiesFileParser.formatEntry(entry.key, entry.value, entry.commentedOut)
    }

    private fun canonical(lines: List<PropertiesFileLine>, metadata: PropertiesMetadata): List<String> {
        val generatedHeaders = metadata.groups.map { "### ${it.title}" }.toSet() +
            "### ${ProjectForgePropertiesWriter.OTHER_GROUP_TITLE}"
        var start = 0
        val header = mutableListOf<String>()
        // The header, as the writer keeps it: leading comment lines followed by a blank line.
        val firstNonComment = lines.indexOfFirst { it !is PropertiesFileLine.Comment }
        if (firstNonComment > 0 && lines[firstNonComment] is PropertiesFileLine.Blank &&
            lines.take(firstNonComment).none { (it as PropertiesFileLine.Comment).groupTitle != null }
        ) {
            lines.take(firstNonComment).forEach { header.add(it.rawLines[0].trim()) }
            start = firstNonComment
        }
        val units = mutableListOf<Unit>()
        val pending = mutableListOf<String>()
        lines.drop(start).forEach { line ->
            when (line) {
                is PropertiesFileLine.Blank -> {}
                is PropertiesFileLine.Comment -> {
                    val text = line.rawLines[0].trim()
                    if (text !in generatedHeaders) {
                        pending.add(text)
                    }
                }

                is PropertiesFileLine.Entry -> {
                    units.add(Unit(pending.toList(), line))
                    pending.clear()
                }
            }
        }
        // Commented-out lines of a key before its active one, duplicates in a stable order.
        val sorted = units.sortedWith(
            compareBy<Unit>({ it.entry.key }, { !it.entry.commentedOut }, { it.entryLine }, { it.commentLines.joinToString("\n") })
        )
        val out = mutableListOf<String>()
        out.addAll(header)
        sorted.forEach { unit ->
            if (unit.commentLines.isNotEmpty() && out.isNotEmpty()) {
                // Easier to read: a comment starts a paragraph.
                out.add("")
            }
            out.addAll(unit.commentLines)
            out.add(unit.entryLine)
        }
        if (pending.isNotEmpty()) {
            if (out.isNotEmpty()) {
                out.add("")
            }
            out.addAll(pending)
        }
        return out
    }

    /** The line of an entry of a secret with the value masked, null if it isn't one of a secret. */
    private fun masked(line: PropertiesFileLine): List<String>? {
        if (line !is PropertiesFileLine.Entry || !PropertiesMetadata.isSecret(line.key)) {
            return null
        }
        return listOf(PropertiesFileParser.formatEntry(line.key, maskSecret(line.value), line.commentedOut))
    }

    /**
     * A secret's value never leaves the server, but whether it differs between two versions must be visible: the
     * mask carries a fingerprint of the value, an HMAC with a random key of this JVM. Without the key, the value
     * can't be guessed from it; equal values have equal fingerprints (until the next restart).
     */
    internal fun maskSecret(value: String): String {
        if (value.isEmpty()) {
            return ""
        }
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(fingerprintKey, HMAC_ALGORITHM))
        val fingerprint = mac.doFinal(value.toByteArray(Charsets.UTF_8)).take(4).joinToString("") { "%02x".format(it) }
        return "$SECRET_MASK [$fingerprint]"
    }

    private const val SECRET_MASK = "******"

    private const val HMAC_ALGORITHM = "HmacSHA256"

    private val fingerprintKey: ByteArray by lazy { ByteArray(32).also { SecureRandom().nextBytes(it) } }
}
