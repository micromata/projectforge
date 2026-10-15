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


package org.projectforge.framework.renderer

import com.lowagie.text.Font
import com.lowagie.text.pdf.BaseFont
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.configuration.ConfigurationService
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

/**
 * Fonts for the Word-to-PDF conversion (invoice PDF for ZUGFeRD, Merlin's PDF export).
 *
 * The font files are never part of the repository (licensing): they are installed locally on the server, either
 * in the ProjectForge fonts directory (`projectforge.fontsDirectory`, default `<home>/resources/fonts`) or in the
 * usual OS font directories. The ProjectForge directory wins if a font exists in both.
 *
 * Word asks for a font by family name plus bold/italic, so the fonts are indexed by family (name id 1) and
 * subfamily (name id 2, e.g. "Bold Italic"). The full and the PostScript name are indexed as well, as Merlin
 * templates used to reference fonts by them. All fonts are embedded (Identity-H), as PDF/A-3 requires it.
 *
 * The directories are scanned on first use, not at startup. A font file is parsed only when it is used.
 */
@Service
open class PdfFontService(private val configurationService: ConfigurationService) {
    internal class FontFile(val path: String, val bold: Boolean, val italic: Boolean)

    /**
     * Font found for a requested family and style.
     * @param style The style to pass to [Font]: the requested one without the bits the face already provides,
     * otherwise iText would embolden or slant an already bold or italic face once more.
     */
    class ResolvedFont(val baseFont: BaseFont, val style: Int)

    internal class FontIndex {
        /** canonical family name -> style key -> font file. */
        val families = mutableMapOf<String, MutableMap<Int, FontFile>>()

        /** canonical full or PostScript name -> font file. */
        val names = mutableMapOf<String, FontFile>()

        /**
         * First registration wins, so the directories have to be registered in order of precedence.
         */
        fun register(path: String, family: String, subfamily: String?, otherNames: Collection<String>) {
            val sub = subfamily?.lowercase() ?: ""
            val fontFile = FontFile(
                path,
                bold = sub.contains("bold"),
                italic = sub.contains("italic") || sub.contains("oblique"),
            )
            families.getOrPut(canonical(family)) { mutableMapOf() }
                .putIfAbsent(styleKey(fontFile.bold, fontFile.italic), fontFile)
            otherNames.forEach { names.putIfAbsent(canonical(it), fontFile) }
        }

        fun find(familyName: String, style: Int): Pair<FontFile, Int>? {
            val name = canonical(familyName)
            val bold = style and Font.BOLD != 0
            val italic = style and Font.ITALIC != 0
            families[name]?.let { faces ->
                faces[styleKey(bold, italic)]?.let { return it to clearStyle(style) }
                // Use what the family has, iText adds the missing bold/italic.
                (faces[styleKey(bold, false)] ?: faces[styleKey(false, italic)] ?: faces[styleKey(false, false)])
                    ?.let { return it to remainingStyle(it, style) }
                return faces.values.first().let { it to remainingStyle(it, style) }
            }
            return names[name]?.let { it to remainingStyle(it, style) }
        }

        val size: Int
            get() = families.values.sumOf { it.size }

        private fun styleKey(bold: Boolean, italic: Boolean): Int =
            (if (bold) Font.BOLD else 0) or (if (italic) Font.ITALIC else 0)

        private fun clearStyle(style: Int): Int =
            if (style == Font.UNDEFINED) style else style and (Font.BOLD or Font.ITALIC).inv()

        private fun remainingStyle(fontFile: FontFile, style: Int): Int {
            if (style == Font.UNDEFINED) return style
            var result = style
            if (fontFile.bold) result = result and Font.BOLD.inv()
            if (fontFile.italic) result = result and Font.ITALIC.inv()
            return result
        }
    }

    private val index by lazy { buildIndex() }

    private val baseFonts = ConcurrentHashMap<String, BaseFont>()

    private val reportedMissing = ConcurrentHashMap.newKeySet<String>()

    /**
     * @return The embedded font for the given family and style, or null if no installed font matches.
     */
    open fun getFont(familyName: String?, style: Int): ResolvedFont? {
        if (familyName.isNullOrBlank()) {
            return null
        }
        val (fontFile, remainingStyle) = index.find(familyName, style) ?: run {
            if (reportedMissing.add(canonical(familyName))) {
                log.warn { "Font '$familyName' isn't installed, the PDF uses a substitute. Install it in '${configurationService.fontsDir}'." }
            }
            return null
        }
        val baseFont = try {
            baseFonts.computeIfAbsent(fontFile.path) {
                BaseFont.createFont(it, BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
            }
        } catch (ex: Exception) {
            log.error(ex) { "Can't load font '${fontFile.path}': ${ex.message}" }
            return null
        }
        return ResolvedFont(baseFont, remainingStyle)
    }

    private fun buildIndex(): FontIndex {
        val index = FontIndex()
        fontDirs().forEach { dir ->
            val before = index.size
            dir.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in FONT_EXTENSIONS }
                .sortedBy { it.absolutePath }
                .forEach { file -> registerFile(index, file) }
            if (index.size > before) {
                log.info { "Registered ${index.size - before} font(s) from '${dir.absolutePath}'." }
            }
        }
        log.info { "PDF fonts: ${index.families.entries.sortedBy { it.key }.joinToString { "${it.key} (${it.value.size})" }}" }
        return index
    }

    private fun registerFile(index: FontIndex, file: File) {
        if (file.name.contains("VariableFont", ignoreCase = true)) {
            // Google Fonts ships variable fonts next to the static ones, iText can only use the static ones.
            log.info { "Skipping variable font '${file.absolutePath}', use the static font files instead." }
            return
        }
        try {
            val entries = BaseFont.getAllNameEntries(file.absolutePath, BaseFont.IDENTITY_H, null)
            // Entry: nameId, platformId, platformEncodingId, languageId, name.
            fun names(nameId: String): List<String> =
                entries.filter { it[0] == nameId }.sortedBy { if (it[1] == "3") 0 else 1 }.map { it[4] }
            val family = names("1").firstOrNull() ?: return
            index.register(file.absolutePath, family, names("2").firstOrNull(), names("4") + names("6"))
        } catch (ex: Exception) {
            log.warn { "Can't read font '${file.absolutePath}': ${ex.message}" }
        }
    }

    private fun fontDirs(): List<File> {
        val home = System.getProperty("user.home")
        return (listOfNotNull(configurationService.fontsDir) + SYSTEM_FONT_DIRS.map { it.replace("~", home) })
            .map { File(it) }
            .filter { it.isDirectory }
            .distinctBy { it.canonicalPath }
    }

    companion object {
        private val FONT_EXTENSIONS = setOf("ttf", "otf")

        private val SYSTEM_FONT_DIRS = listOf(
            "/usr/share/fonts",
            "/usr/local/share/fonts",
            "~/.fonts",
            "~/.local/share/fonts",
            "/Library/Fonts",
            "~/Library/Fonts",
        )

        internal fun canonical(name: String): String = name.replace("[ \\-._]".toRegex(), "").lowercase()
    }
}
