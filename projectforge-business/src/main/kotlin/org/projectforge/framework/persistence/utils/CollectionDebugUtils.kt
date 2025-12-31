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

package org.projectforge.framework.persistence.utils

import name.fraser.neil.plaintext.DiffMatchPatch
import org.projectforge.common.ConsoleUtils
import org.projectforge.common.extensions.shortenMiddle
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.utils.CollectionUtils.compareCollections
import org.projectforge.framework.persistence.utils.CollectionUtils.idObjectsEqual
import org.projectforge.framework.persistence.utils.CollectionUtils.joinToString

object CollectionDebugUtils {

    fun showCompareDiff(
        src: Collection<Any?>?,
        dest: Collection<Any?>?,
        withKept: Boolean = false,
        prefix: String = ""
    ): String? {
        val result = compareCollections(src, dest, withKept)
        var differs = false
        val sb = StringBuilder()
        differs = append(sb, result.added, differs, "added  ", prefix)
        differs = append(sb, result.removed, differs, "removed", prefix)
        if (withKept) {
            result.kept?.forEach {
                val srcEntry = findEntry(src, it)
                val destEntry = findEntry(dest, it)
                val srcJson = JsonUtils.toJson(srcEntry)
                val destJson = JsonUtils.toJson(destEntry)
                if (srcJson != destJson) {
                    if (!differs) {
                        sb.appendLine()
                    }
                    differs = true
                    // sb.appendLine("${prefix} diff.src =[$srcJson]")
                    // sb.appendLine("${prefix} diff.dest=[$destJson]")
                    sb.appendLine("${prefix} diff.short=[${diffStrings(srcJson, destJson, true)}]")
                    sb.appendLine("${prefix} diff.long =[${diffStrings(srcJson, destJson)}]")
                }
            }
        }
        return if (differs) {
            sb.toString()
        } else {
            null
        }
    }

    private fun findEntry(col: Collection<Any?>?, entry: Any): Any? {
        if (col.isNullOrEmpty()) {
            return null
        }
        if (entry is IdObject<*>) {
            return col.firstOrNull {
                (it as? IdObject<*>)?.let { idObjectsEqual(it, entry) } ?: false
            }
        }
        if (entry is Comparable<*>) {
            return col.firstOrNull {
                @Suppress("UNCHECKED_CAST")
                (it as? Comparable<Any>)?.let { comparableItem -> (entry as Comparable<Any>).compareTo(comparableItem) == 0 }
                    ?: false
            }
        }
        return col.firstOrNull { it == entry }
    }

    fun diffStrings(str1: String, str2: String, shortenMiddle: Boolean = false): String {
        val diffMatchPatch = DiffMatchPatch()
        val diffs = diffMatchPatch.diff_main(str1, str2)
        diffMatchPatch.diff_cleanupSemantic(diffs)
        val sb = StringBuilder()
        diffs.forEach { aDiff ->
            val text = if (shortenMiddle) aDiff.text.shortenMiddle(20) else aDiff.text
            when (aDiff.operation) {
                DiffMatchPatch.Operation.INSERT -> ConsoleUtils.appendText(sb, "+[$text]", ConsoleUtils.AnsiColor.BLUE)
                DiffMatchPatch.Operation.DELETE -> ConsoleUtils.appendText(sb, "-[$text]", ConsoleUtils.AnsiColor.RED)
                DiffMatchPatch.Operation.EQUAL -> sb.append(text)
                else -> sb.append("")
            }
        }
        return sb.toString()
    }

    private fun append(
        sb: StringBuilder,
        col: Collection<Any?>?,
        differs: Boolean,
        list: String,
        prefix: String
    ): Boolean {
        var newDiffers = differs
        if (col?.isNotEmpty() == true) {
            sb.append(prefix)
            newDiffers = true
            sb.appendLine("$list=[${joinToString(col)}]")
        }
        return newDiffers
    }
}
