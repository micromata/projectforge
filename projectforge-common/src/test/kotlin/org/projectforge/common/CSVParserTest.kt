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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.InputStreamReader

class CSVParserTest {

    private fun loadCsvFromResources(filename: String): CSVParser {
        val inputStream = javaClass.classLoader.getResourceAsStream("csv/$filename")
            ?: throw IllegalArgumentException("CSV file not found: csv/$filename")
        return CSVParser(InputStreamReader(inputStream))
    }

    @Test
    fun parseSimpleCsvWithHeaders() {
        val parser = loadCsvFromResources("simple.csv")

        // Parse headers
        val headers = parser.parseLine()
        Assertions.assertNotNull(headers)
        Assertions.assertEquals(3, headers!!.size)
        Assertions.assertEquals("Name", headers[0])
        Assertions.assertEquals("Age", headers[1])
        Assertions.assertEquals("City", headers[2])

        // Parse first data row
        val row1 = parser.parseLine()
        Assertions.assertNotNull(row1)
        Assertions.assertEquals(3, row1!!.size)
        Assertions.assertEquals("John", row1[0])
        Assertions.assertEquals("25", row1[1])
        Assertions.assertEquals("New York", row1[2])

        // Parse second data row
        val row2 = parser.parseLine()
        Assertions.assertNotNull(row2)
        Assertions.assertEquals(3, row2!!.size)
        Assertions.assertEquals("Jane", row2[0])
        Assertions.assertEquals("30", row2[1])
        Assertions.assertEquals("Berlin", row2[2])

        // Should be end of file
        val row3 = parser.parseLine()
        Assertions.assertNull(row3)
    }

    @Test
    fun parseMultilineCsvContent() {
        val parser = loadCsvFromResources("multiline.csv")

        // Parse headers
        val headers = parser.parseLine()
        Assertions.assertNotNull(headers)
        Assertions.assertEquals(3, headers!!.size)
        Assertions.assertEquals("Name", headers[0])
        Assertions.assertEquals("Description", headers[1])
        Assertions.assertEquals("Notes", headers[2])

        // Parse first data row with multiline content
        val row1 = parser.parseLine()
        Assertions.assertNotNull(row1)
        Assertions.assertEquals(3, row1!!.size)
        Assertions.assertEquals("Product A", row1[0])
        Assertions.assertTrue(row1[1].contains("\n"), "Should contain actual newline characters")
        Assertions.assertEquals("High quality\nproduct with\nmultiple features", row1[1])
        Assertions.assertEquals("Good", row1[2])

        // Parse second data row
        val row2 = parser.parseLine()
        Assertions.assertNotNull(row2)
        Assertions.assertEquals("Product B", row2!![0])
        Assertions.assertEquals("Simple product", row2[1])
        Assertions.assertEquals("Average", row2[2])
    }

    @Test
    fun parseEscapedQuotesInCsv() {
        val parser = loadCsvFromResources("escaped_quotes.csv")

        // Parse headers
        val headers = parser.parseLine()
        Assertions.assertNotNull(headers)
        Assertions.assertEquals(3, headers!!.size)
        Assertions.assertEquals("Company", headers[0])
        Assertions.assertEquals("Slogan", headers[1])
        Assertions.assertEquals("Industry", headers[2])

        // Parse first data row with escaped quotes
        val row1 = parser.parseLine()
        Assertions.assertNotNull(row1)
        Assertions.assertEquals(3, row1!!.size)
        Assertions.assertEquals("Company \"ABC\" Ltd", row1[0])
        Assertions.assertEquals("We are \"the best\"", row1[1])
        Assertions.assertEquals("Software", row1[2])

        // Parse second data row
        val row2 = parser.parseLine()
        Assertions.assertNotNull(row2)
        Assertions.assertEquals("Normal Corp", row2!![0])
        Assertions.assertEquals("Simple slogan", row2[1])
        Assertions.assertEquals("Consulting", row2[2])
    }

    @Test
    fun parseComplexInvoiceCsv() {
        val parser = loadCsvFromResources("invoice_sample.csv")

        // Parse headers
        val headers = parser.parseLine()
        Assertions.assertNotNull(headers)
        Assertions.assertEquals(5, headers!!.size)
        Assertions.assertEquals("Belegart", headers[0])
        Assertions.assertEquals("Geschäftspartner-Name", headers[1])
        Assertions.assertEquals("Rechnungsbetrag", headers[2])
        Assertions.assertEquals("Ware/Leistung", headers[3])
        Assertions.assertEquals("Notiz", headers[4])

        // Parse first invoice row
        val row1 = parser.parseLine()
        Assertions.assertNotNull(row1)
        Assertions.assertEquals(5, row1!!.size)
        Assertions.assertEquals("R", row1[0])
        Assertions.assertEquals("Software Company GmbH", row1[1])
        Assertions.assertEquals("1200,78", row1[2])
        Assertions.assertTrue(row1[3].contains("\n"), "Service description should contain newlines")
        Assertions.assertEquals("12x Creative Cloud\nLicense for period", row1[3])
        Assertions.assertTrue(row1[4].contains("\n"), "Notes should contain newlines")
        Assertions.assertEquals("KK KL\ngepr. KL", row1[4])

        // Parse second invoice row
        val row2 = parser.parseLine()
        Assertions.assertNotNull(row2)
        Assertions.assertEquals(5, row2!!.size)
        Assertions.assertEquals("R", row2[0])
        Assertions.assertEquals("Cloud Provider Inc", row2[1])
        Assertions.assertEquals("565,27", row2[2])
        Assertions.assertEquals("AWS-Account Prod Environment", row2[3])
        Assertions.assertTrue(row2[4].contains("\n"), "Notes should contain newlines")
        Assertions.assertEquals("wi AB\nchecked by admin", row2[4])
    }

    @Test
    fun parseEmptyCells() {
        val parser = loadCsvFromResources("empty_cells.csv")

        // Parse headers
        val headers = parser.parseLine()
        Assertions.assertNotNull(headers)
        Assertions.assertEquals(3, headers!!.size)
        Assertions.assertEquals("A", headers[0])
        Assertions.assertEquals("B", headers[1])
        Assertions.assertEquals("C", headers[2])

        // Parse row with empty cells
        val row1 = parser.parseLine()
        Assertions.assertNotNull(row1)
        Assertions.assertEquals(3, row1!!.size)
        Assertions.assertEquals("", row1[0])
        Assertions.assertEquals("Value2", row1[1])
        Assertions.assertEquals("", row1[2])

        val row2 = parser.parseLine()
        Assertions.assertNotNull(row2)
        Assertions.assertEquals(3, row2!!.size)
        Assertions.assertEquals("Value1", row2[0])
        Assertions.assertEquals("", row2[1])
        Assertions.assertEquals("Value3", row2[2])
    }

    @Test
    fun parseRealWorldDatevExport() {
        val parser = loadCsvFromResources("datev_export.csv")

        // Parse headers
        val headers = parser.parseLine()
        Assertions.assertNotNull(headers)
        Assertions.assertEquals(9, headers!!.size)
        Assertions.assertEquals("Belegart", headers[0])
        Assertions.assertEquals("Geschäftspartner-Name", headers[1])
        Assertions.assertEquals("Geschäftspartner-Konto", headers[2])
        Assertions.assertEquals("Rechnungsbetrag", headers[3])
        Assertions.assertEquals("Ware/Leistung", headers[7])
        Assertions.assertEquals("Notiz", headers[8])

        // Parse simple invoice
        val row1 = parser.parseLine()
        Assertions.assertNotNull(row1)
        Assertions.assertEquals(9, row1!!.size)
        Assertions.assertEquals("R", row1[0])
        Assertions.assertEquals("Marketing Service GmbH", row1[1])
        Assertions.assertEquals("384,16", row1[3])
        Assertions.assertEquals("Geschäftsbericht, Material Frühlingsfest", row1[7])

        // Parse multiline invoice
        val row2 = parser.parseLine()
        Assertions.assertNotNull(row2)
        Assertions.assertEquals(9, row2!!.size)
        Assertions.assertEquals("R", row2[0])
        Assertions.assertEquals("Software Solutions Ltd", row2[1])
        Assertions.assertEquals("1200,78", row2[3])
        Assertions.assertTrue(row2[7].contains("\n"), "Service description should span multiple lines")
        Assertions.assertTrue(row2[8].contains("\n"), "Notes should span multiple lines")

        // Parse credit note
        val row3 = parser.parseLine()
        Assertions.assertNotNull(row3)
        Assertions.assertEquals("G", row3!![0])
        Assertions.assertEquals("Marketing Service GmbH", row3[1])
        Assertions.assertEquals("-54,15", row3[3])
        Assertions.assertEquals("zu Rechnung Nr. 24984", row3[8])
    }

    @Test
    fun parseEdgeCaseWithQuotesAndSeparators() {
        val parser = loadCsvFromResources("edge_cases.csv")

        // Parse headers
        val headers = parser.parseLine()
        Assertions.assertNotNull(headers)
        Assertions.assertEquals(3, headers!!.size)
        Assertions.assertEquals("Field1", headers[0])
        Assertions.assertEquals("Field2", headers[1])
        Assertions.assertEquals("Field3", headers[2])

        // Parse first row with semicolons in quoted fields
        val row1 = parser.parseLine()
        Assertions.assertNotNull(row1)
        Assertions.assertEquals(3, row1!!.size)
        Assertions.assertEquals("Value with ; semicolon", row1[0])
        Assertions.assertEquals("Value with \"quotes\" and ; semicolon", row1[1])
        Assertions.assertEquals("Normal", row1[2])

        // Parse multiline row
        val row2 = parser.parseLine()
        Assertions.assertNotNull(row2)
        Assertions.assertEquals(3, row2!!.size)
        Assertions.assertEquals("Multiline\nwith separators", row2[0])
        Assertions.assertEquals("Simple", row2[1])
        Assertions.assertEquals("End", row2[2])
    }

    @Test
    fun parseSimpleCsvWithBOM() {
        val parser = loadCsvFromResources("simple_with_bom.csv")

        // Parse headers
        val headers = parser.parseLine()
        Assertions.assertNotNull(headers)
        Assertions.assertEquals(3, headers!!.size)
        Assertions.assertEquals("Name", headers[0])
        Assertions.assertEquals("Age", headers[1])
        Assertions.assertEquals("City", headers[2])

        // Parse first data row
        val row1 = parser.parseLine()
        Assertions.assertNotNull(row1)
        Assertions.assertEquals(3, row1!!.size)
        Assertions.assertEquals("John", row1[0])
        Assertions.assertEquals("25", row1[1])
        Assertions.assertEquals("New York", row1[2])

        // Parse second data row
        val row2 = parser.parseLine()
        Assertions.assertNotNull(row2)
        Assertions.assertEquals(3, row2!!.size)
        Assertions.assertEquals("Jane", row2[0])
        Assertions.assertEquals("30", row2[1])
        Assertions.assertEquals("Berlin", row2[2])

        // Should be end of file
        val row3 = parser.parseLine()
        Assertions.assertNull(row3)
    }
}
