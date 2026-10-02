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

import org.apache.pdfbox.Loader
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.configuration.ConfigurationService
import java.io.ByteArrayOutputStream

class LibreOfficeServiceTest {
    private val configurationService = Mockito.mock(ConfigurationService::class.java)

    @Test
    fun `disabled LibreOffice converts nothing, so the caller falls back`() {
        val service = LibreOfficeService(configurationService, "disabled")
        assertFalse(service.isAvailable)
        assertNull(service.convertDocxToPdf(createDocx()))
    }

    @Test
    fun `a missing executable converts nothing`() {
        assertNull(LibreOfficeService(configurationService, "/does/not/exist/soffice").convertDocxToPdf(createDocx()))
    }

    /**
     * Skipped where LibreOffice isn't installed.
     */
    @Test
    fun `Word document is converted to PDF A-3`() {
        val service = LibreOfficeService(configurationService, null)
        assumeTrue(service.isAvailable, "LibreOffice isn't installed.")
        val pdf = service.convertDocxToPdf(createDocx())
        assertNotNull(pdf)
        Loader.loadPDF(pdf!!).use { document ->
            assertTrue(document.numberOfPages > 0)
            val metadata = document.documentCatalog.metadata?.exportXMPMetadata()?.use { String(it.readAllBytes(), Charsets.UTF_8) }
            assertTrue(metadata?.contains("pdfaid:part>3<") == true || metadata?.contains("pdfaid:part=\"3\"") == true,
                "PDF/A-3 is declared: $metadata")
        }
    }

    private fun createDocx(): ByteArray = ByteArrayOutputStream().use { baos ->
        XWPFDocument().use { doc ->
            doc.createParagraph().createRun().setText("Stornorechnung")
            doc.write(baos)
        }
        baos.toByteArray()
    }
}
