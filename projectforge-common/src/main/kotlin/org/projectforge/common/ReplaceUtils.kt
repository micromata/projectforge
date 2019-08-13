/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.StringUtils
import java.util.*

/**
 * Code re-used from merlin: https://github.com/micromata/Merlin.
 */
object ReplaceUtils {
    val ALLOWED_FILENAME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-"
    val PRESERVED_FILENAME_CHARS = "\"*/:<>?\\|"
    val FILENAME_REPLACE_CHAR = '_'

    private val umlautReplacementMap: MutableMap<Char, String>

    init {
        umlautReplacementMap = HashMap()
        umlautReplacementMap['Ä'] = "Ae"
        umlautReplacementMap['Ö'] = "Oe"
        umlautReplacementMap['Ü'] = "Ue"
        umlautReplacementMap['ä'] = "ae"
        umlautReplacementMap['ö'] = "oe"
        umlautReplacementMap['ü'] = "ue"
        umlautReplacementMap['ß'] = "ss"

    }

    /**
     * Preserved characters (Windows): 0x00-0x1F 0x7F " * / : &lt; &gt; ? \ |
     * Preserved characters (Mac OS): ':'
     * Preserved characters (Unix): '/'
     * Max length: 255
     *
     * @param filename         The filename to encode.
     * @param reducedCharsOnly if true, only [.ALLOWED_FILENAME_CHARS] are allowed and German Umlaute are replaced
     * 'Ä'-&gt;'Ae' etc. If not, all characters excluding [.PRESERVED_FILENAME_CHARS] are allowed and
     * all white spaces will be replaced by ' ' char.
     * @return The encoded filename.
     */

    fun encodeFilename(filename: String?, reducedCharsOnly: Boolean = true): String {
        if (filename.isNullOrEmpty()) {
            return "file"
        }
        var result = if (reducedCharsOnly)
            replaceGermanUmlauteAndAccents(filename)
        else
            filename
        val sb = StringBuilder()
        val charArray = result.toCharArray()
        for (i in charArray.indices) {
            val ch = charArray[i]
            if (reducedCharsOnly) {
                if (ALLOWED_FILENAME_CHARS.indexOf(ch) >= 0) {
                    sb.append(ch)
                } else {
                    sb.append(FILENAME_REPLACE_CHAR)
                }
            } else {
                if (ch.toInt() <= 31 || ch.toInt() == 127) { // Not 0x00-0x1F and not 0x7F
                    sb.append(FILENAME_REPLACE_CHAR)
                } else if (PRESERVED_FILENAME_CHARS.indexOf(ch) >= 0) {
                    sb.append(FILENAME_REPLACE_CHAR)
                } else if (Character.isWhitespace(ch)) {
                    sb.append(' ')
                } else {
                    sb.append(ch)
                }
            }
        }
        result = sb.toString()
        return if (result.length > 255) {
            result.substring(0, 255)
        } else result
    }

    fun replaceGermanUmlauteAndAccents(text: String?): String {
        if (text == null) {
            return "untitled"
        }
        val sb = StringBuilder()
        val charArray = text.toCharArray()
        for (i in charArray.indices) {
            val ch = charArray[i]
            if (umlautReplacementMap.containsKey(ch)) {
                sb.append(umlautReplacementMap[ch])
            } else {
                sb.append(ch)
            }
        }
        return StringUtils.stripAccents(sb.toString())
    }
}
