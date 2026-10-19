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
 * A change of one parameter: [value] sets it, [remove] removes it from projectforge.properties (the default of
 * application.properties applies again).
 */
class PropertyChange(val key: String, val value: String? = null, val remove: Boolean = false)

/**
 * Builds the new content of projectforge.properties: the parameters are sorted by the groups and the order of the
 * bundled application.properties ([PropertiesMetadata]), unknown keys at the end (group `Other`). The admin's own
 * comments stay above the parameter they were written above, the file's header (the first comment block followed by
 * a blank line) stays at the top and comments at the end of the file stay at the end. Untouched parameters are written
 * back unchanged (same raw lines).
 */
class ProjectForgePropertiesWriter(private val metadata: PropertiesMetadata) {
    /**
     * A parameter of the admin's file with the comment lines above it. A [commentOnly] unit is a paragraph of
     * comments with commented-out unknown keys (e.g. `#mail.smtp.user=...`), followed by a blank line: it is sorted
     * by its first key instead of being glued to whatever parameter follows it.
     */
    private class Unit(
        val key: String,
        var commentedOut: Boolean,
        val commentLines: List<String>,
        var entryLines: List<String>,
        val fileIndex: Int,
        val commentOnly: Boolean = false,
    )

    /**
     * The file parsed as the writer sees it: [units] are the parameters (active and commented-out known ones),
     * each with the comment lines above it.
     */
    private class ParsedFile(val header: List<String>, val units: MutableList<Unit>, val trailer: List<String>)

    fun write(content: String, changes: List<PropertyChange>): String {
        val parsed = parse(content)
        val units = parsed.units
        // Duplicates of an active key: the last one wins (as in java.util.Properties), the former ones are commented out.
        units.filter { !it.commentedOut }.groupBy { it.key }.values.filter { it.size > 1 }.forEach { duplicates ->
            duplicates.dropLast(1).forEach { commentOut(it, it.key) }
        }
        changes.forEach { change -> apply(units, change) }
        return format(parsed)
    }

    private fun apply(units: MutableList<Unit>, change: PropertyChange) {
        val key = change.key
        val active = units.lastOrNull { it.key == key && !it.commentedOut }
        if (change.remove) {
            active?.let { commentOut(it, key) }
            return
        }
        val value = change.value ?: return
        val line = PropertiesFileParser.formatEntry(key, value)
        if (active != null) {
            active.entryLines = listOf(line)
            return
        }
        // A commented-out line of the key is activated (keeping its comment), otherwise a new unit is appended.
        val commentedOut = units.lastOrNull { it.key == key && !it.commentOnly }
        if (commentedOut != null) {
            commentedOut.commentedOut = false
            commentedOut.entryLines = listOf(line)
        } else {
            units.add(Unit(key, false, emptyList(), listOf(line), Int.MAX_VALUE))
        }
    }

    /** The removed value stays as a comment, except of secrets: they mustn't be kept anywhere. */
    private fun commentOut(unit: Unit, key: String) {
        val value = if (PropertiesMetadata.isSecret(key)) {
            ""
        } else {
            (PropertiesFileParser.parse(unit.entryLines.joinToString("\n")).single() as PropertiesFileLine.Entry).value
        }
        unit.commentedOut = true
        unit.entryLines = listOf(PropertiesFileParser.formatEntry(key, value, commentedOut = true))
    }

    private fun parse(content: String): ParsedFile {
        val lines = PropertiesFileParser.parse(content)
        val generatedHeaders = metadata.groups.map { "### ${it.title}" }.toSet() + "### $OTHER_GROUP_TITLE"
        var index = 0
        val header = mutableListOf<String>()
        // The header: leading comment lines followed by a blank line.
        val firstNonComment = lines.indexOfFirst { it !is PropertiesFileLine.Comment }
        if (firstNonComment > 0 && lines[firstNonComment] is PropertiesFileLine.Blank &&
            lines.take(firstNonComment).none { (it as PropertiesFileLine.Comment).groupTitle != null }
        ) {
            lines.take(firstNonComment).forEach { header.addAll(it.rawLines) }
            index = firstNonComment
        }
        val units = mutableListOf<Unit>()
        val pending = mutableListOf<String>()
        var pendingKey: String? = null // First commented-out unknown key of the pending comment lines.
        while (index < lines.size) {
            val line = lines[index++]
            when {
                line is PropertiesFileLine.Blank && pendingKey != null -> {
                    units.add(Unit(pendingKey, true, emptyList(), trimBlankLines(pending), units.size, commentOnly = true))
                    pending.clear()
                    pendingKey = null
                }

                line is PropertiesFileLine.Comment && line.rawLines[0].trim() in generatedHeaders -> {
                    // Written by this writer, will be written again.
                }

                line is PropertiesFileLine.Entry && (!line.commentedOut || metadata.contains(line.key)) -> {
                    units.add(Unit(line.key, line.commentedOut, trimBlankLines(pending), line.rawLines, units.size))
                    pending.clear()
                    pendingKey = null
                }

                else -> {
                    if (line is PropertiesFileLine.Entry && pendingKey == null) {
                        pendingKey = line.key
                    }
                    pending.addAll(line.rawLines)
                }
            }
        }
        return ParsedFile(header, units, trimBlankLines(pending))
    }

    private fun format(parsed: ParsedFile): String {
        data class Sorted(val unit: Unit, val group: PropertiesMetadata.Group?, val position: Double)

        val sorted = parsed.units.map { unit ->
            val related = metadata.findRelatedKey(unit.key)
            val position = related?.let { metadata.getPosition(it) }
            if (related != null && position != null) {
                // Unknown keys related to a known one follow it.
                Sorted(unit, metadata.getGroup(related), position + if (related == unit.key) 0.0 else 0.5)
            } else {
                Sorted(unit, null, Double.MAX_VALUE)
            }
        }.sortedWith(compareBy<Sorted> { it.position }.thenBy { it.unit.fileIndex })
        val out = mutableListOf<String>()
        if (parsed.header.isNotEmpty()) {
            out.addAll(parsed.header)
        }
        var currentGroup: String? = null
        var previous: Sorted? = null
        sorted.forEach { item ->
            val groupTitle = item.group?.title ?: OTHER_GROUP_TITLE
            if (groupTitle != currentGroup) {
                if (out.isNotEmpty()) {
                    out.add("")
                }
                out.add("### $groupTitle")
                out.add("")
                currentGroup = groupTitle
            } else if (item.unit.commentLines.isNotEmpty() || !sameBlock(previous?.unit?.key, item.unit.key)) {
                out.add("")
            }
            out.addAll(item.unit.commentLines)
            out.addAll(item.unit.entryLines)
            previous = item
        }
        if (parsed.trailer.isNotEmpty()) {
            out.add("")
            out.addAll(parsed.trailer)
        }
        return out.joinToString("\n", postfix = "\n")
    }

    /** Keys of the same block of application.properties are written without a blank line between them. */
    private fun sameBlock(key1: String?, key2: String): Boolean {
        key1 ?: return false
        val related1 = metadata.findRelatedKey(key1) ?: return false
        val related2 = metadata.findRelatedKey(key2) ?: return false
        return metadata.groups.any { group -> group.blocks.any { block -> block.params.any { it.key == related1 } && block.params.any { it.key == related2 } } }
    }

    private fun trimBlankLines(lines: List<String>): List<String> =
        lines.dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }

    companion object {
        const val OTHER_GROUP_TITLE = "Other"
    }
}
