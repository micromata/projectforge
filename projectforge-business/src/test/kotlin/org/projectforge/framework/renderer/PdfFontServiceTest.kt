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
import de.micromata.merlin.word.WordDocument
import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions
import org.apache.pdfbox.Loader
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito
import org.projectforge.business.configuration.ConfigurationService
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path

class PdfFontServiceTest {
    @Test
    fun `Word's family name and style select the matching face`() {
        val index = PdfFontService.FontIndex()
        index.register("/r.ttf", "Montserrat", "Regular", listOf("Montserrat Regular", "Montserrat-Regular"))
        index.register("/b.ttf", "Montserrat", "Bold", listOf("Montserrat Bold", "Montserrat-Bold"))
        index.register("/i.ttf", "Montserrat", "Italic", listOf("Montserrat Italic", "Montserrat-Italic"))

        index.find("Montserrat", Font.NORMAL)!!.let {
            assertEquals("/r.ttf", it.first.path)
            assertEquals(Font.NORMAL, it.second)
        }
        index.find("Montserrat", Font.BOLD)!!.let {
            assertEquals("/b.ttf", it.first.path)
            assertEquals(Font.NORMAL, it.second, "The face is bold already, iText mustn't embolden it again.")
        }
        index.find("Montserrat", Font.BOLD or Font.ITALIC or Font.UNDERLINE)!!.let {
            assertEquals("/b.ttf", it.first.path, "No bold italic face: bold one, iText slants it.")
            assertEquals(Font.ITALIC or Font.UNDERLINE, it.second)
        }
        assertEquals("/r.ttf", index.find("montserrat", Font.NORMAL)!!.first.path, "Case doesn't matter.")
        assertEquals("/b.ttf", index.find("Montserrat-Bold", Font.NORMAL)!!.first.path, "PostScript name.")
        assertNull(index.find("Lucida Grande", Font.NORMAL))
    }

    @Test
    fun `the first registered font wins`() {
        val index = PdfFontService.FontIndex()
        index.register("/projectforge/m.ttf", "Montserrat", "Regular", emptyList())
        index.register("/usr/share/fonts/m.ttf", "Montserrat", "Regular", emptyList())
        assertEquals("/projectforge/m.ttf", index.find("Montserrat", Font.NORMAL)!!.first.path)
    }

    /**
     * Uses a font that is installed on the machine (no font files in the repository, licensing), skipped if none.
     */
    @Test
    fun `fonts of the fonts directory are embedded into the converted Word document`(@TempDir tempDir: Path) {
        val source = findInstalledTtf()
        assumeTrue(source != null, "No TrueType font installed on this machine.")
        source!!.copyTo(File(tempDir.toFile(), source.name))
        val family = BaseFont.getAllNameEntries(source.absolutePath, BaseFont.IDENTITY_H, null)
            .first { it[0] == "1" }[4]
        val configurationService = Mockito.mock(ConfigurationService::class.java)
        Mockito.`when`(configurationService.fontsDir).thenReturn(tempDir.toString())
        val fontService = PdfFontService(configurationService)

        val resolved = fontService.getFont(family, Font.NORMAL)
        assertNotNull(resolved, "Font '$family' is found.")
        assertTrue(resolved!!.baseFont.isEmbedded)

        val docx = ByteArrayOutputStream().use { baos ->
            XWPFDocument().use { doc ->
                // The converter requires a styles part and a section.
                doc.createStyles()
                doc.document.body.addNewSectPr().apply {
                    addNewPgSz().apply { w = 11906.toBigInteger(); h = 16838.toBigInteger() } // A4
                    addNewPgMar().apply {
                        top = 1440.toBigInteger(); bottom = 1440.toBigInteger()
                        left = 1440.toBigInteger(); right = 1440.toBigInteger()
                    }
                }
                doc.createParagraph().createRun().apply {
                    fontFamily = family
                    setText("Stornorechnung")
                }
                doc.write(baos)
            }
            baos.toByteArray()
        }
        val pdf = ByteArrayInputStream(docx).use { istream ->
            WordDocument(istream, "test.docx").use { word ->
                ByteArrayOutputStream().use { baos ->
                    val options = PdfOptions.create().fontProvider(PdfFontProvider(fontService))
                    PdfConverter.getInstance().convert(word.document, baos, options)
                    baos.toByteArray()
                }
            }
        }
        Loader.loadPDF(pdf).use { document ->
            val resources = document.getPage(0).resources
            val fonts = resources.fontNames.map { resources.getFont(it) }
            val postscriptName = resolved.baseFont.postscriptFontName
            val font = fonts.firstOrNull { it.name.endsWith(postscriptName) }
            assertNotNull(font, "PDF uses '$postscriptName', found: ${fonts.map { it.name }}")
            assertTrue(font!!.isEmbedded, "Font is embedded.")
        }
    }

    private fun findInstalledTtf(): File? {
        val home = System.getProperty("user.home")
        return listOf("/usr/share/fonts", "/System/Library/Fonts/Supplemental", "/Library/Fonts", "$home/Library/Fonts")
            .map { File(it) }
            .filter { it.isDirectory }
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension.lowercase() == "ttf" }.sortedBy { it.name }.toList() }
            .firstOrNull { file ->
                runCatching { BaseFont.createFont(file.absolutePath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED) }.isSuccess
            }
    }
}
