/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.common

import org.projectforge.framework.time.DateHelper
import java.util.*

object FilenameUtils {
    fun escapeFilename(
        str: String,
        maxlength: Int = 1_000,
        strict: Boolean = false,
        substituteUmlaute: Boolean = true,
    ): String {
        val sb = StringBuilder()
        escape(sb, str, maxlength = maxlength, strict = strict, substituteUmlaute = substituteUmlaute)
        return sb.toString()
    }

    /**
     * FileHelper.createSafeFilename("basename", ".pdf", 8, true)) -> "basename_2010-08-12.pdf".
     * @param filename The basename of the filename.
     * @param suffix The suffix to append to the filename. Default is null.
     * @param maxlength The maximum length of the filename. Default is 1000. Filename might be longer if suffix is appended.
     * @param appendTimestamp If true, the current date is appended to the filename.
     * @param substituteUmlaute If true, umlauts are substituted by ae, oe, ue, ss.
     * @return
     */
    @JvmOverloads
    @JvmStatic
    fun createSafeFilename(
        filename: String,
        suffix: String? = null,
        maxlength: Int = 1_000,
        appendTimestamp: Boolean = false,
        substituteUmlaute: Boolean = true,
    ): String {
        val sb = StringBuilder()
        escape(sb, filename, maxlength, strict = true, substituteUmlaute = substituteUmlaute)
        if (appendTimestamp) {
            sb.append('_').append(DateHelper.getDateAsFilenameSuffix(Date()))
        }
        if (suffix != null) {
            sb.append(suffix)
        }
        return sb.toString()
    }

    private fun escape(
        sb: StringBuilder,
        str: String,
        maxlength: Int,
        strict: Boolean = true,
        substituteUmlaute: Boolean = true
    ) {
        var escaped = false // Avoid escaping multiple times.
        var count = 0
        var i = 0
        while (i < str.length && count < maxlength) {
            val ch = str[i]
            if (substituteUmlaute && SUBSTITUTE_CHARS.indexOf(ch) >= 0) {
                val substitution = SUBSTITUTED_BY[SUBSTITUTE_CHARS.indexOf(ch)]
                val remain = maxlength - count
                if (substitution.length > remain) {
                    // String must be shortened to ensure max length.
                    sb.append(substitution.substring(0, remain))
                } else {
                    sb.append(substitution)
                }
                count += substitution.length
                escaped = false
                i++
                continue
            } else if (strict && ALLOWED_CHARS.indexOf(ch) >= 0 || !strict && DISALLOWED_CHARS.indexOf(ch) < 0) {
                sb.append(ch)
                count++
                escaped = false
                i++
                continue
            } else if (!escaped) {
                sb.append("_")
                count++
                escaped = true
            }
            i++
        }
    }

    private const val ALLOWED_CHARS: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-."

    private const val DISALLOWED_CHARS: String = "`\\/:*?\"<>"

    private const val SUBSTITUTE_CHARS: String = "ÄÖÜäöüß"

    private val SUBSTITUTED_BY: Array<String> = arrayOf("Ae", "Oe", "Ue", "ae", "oe", "ue", "ss")
}
