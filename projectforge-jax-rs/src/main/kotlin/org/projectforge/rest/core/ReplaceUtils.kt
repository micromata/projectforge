package org.projectforge.rest.core

import org.apache.commons.lang3.StringUtils
import java.util.*

/**
 * Code re-used from merlin: https://github.com/micromata/Merlin.
 */
object ReplaceUtils {
    val ALLOWED_FILENAME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-"
    val PRESERVED_FILENAME_CHARS = "\"*/:<>?\\|"
    val FILENAME_REPLACE_CHAR = '_'

    private var umlautReplacementMap: MutableMap<Char, String>? = null

    init {
        umlautReplacementMap = HashMap()
        umlautReplacementMap!!['Ä'] = "Ae"
        umlautReplacementMap!!['Ö'] = "Oe"
        umlautReplacementMap!!['Ü'] = "Ue"
        umlautReplacementMap!!['ä'] = "ae"
        umlautReplacementMap!!['ö'] = "oe"
        umlautReplacementMap!!['ü'] = "ue"
        umlautReplacementMap!!['ß'] = "ss"
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
        var filename = filename
        if (StringUtils.isEmpty(filename)) {
            return "file"
        }
        if (reducedCharsOnly) {
            filename = replaceGermanUmlauteAndAccents(filename)
        }
        val sb = StringBuilder()
        val charArray = filename!!.toCharArray()
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
        val result = sb.toString()
        return if (result.length > 255) {
            result.substring(0, 255)
        } else result
    }

    fun replaceGermanUmlauteAndAccents(text: String?): String? {
        if (text == null) {
            return null
        }
        val sb = StringBuilder()
        val charArray = text.toCharArray()
        for (i in charArray.indices) {
            val ch = charArray[i]
            if (umlautReplacementMap!!.containsKey(ch)) {
                sb.append(umlautReplacementMap!![ch])
            } else {
                sb.append(ch)
            }
        }
        return StringUtils.stripAccents(sb.toString())
    }
}
