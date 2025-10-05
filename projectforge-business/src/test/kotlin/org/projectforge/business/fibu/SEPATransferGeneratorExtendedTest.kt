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

package org.projectforge.business.fibu

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

/**
 * Extended test suite for SEPATransferGenerator to ensure migration safety.
 * Creates golden files (reference XMLs) that can be used to validate the migration.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class SEPATransferGeneratorExtendedTest : AbstractTestBase() {

    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    private lateinit var generator: SEPATransferGenerator

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        generator = SEPATransferGenerator()
        val param = configurationDao.getEntry(ConfigurationParam.ORGANIZATION)
        param?.stringValue = "Test Organization GmbH"
        param?.let { configurationDao.update(it, false) }
    }

    @Test
    fun `generate single invoice with German IBAN`() {
        val invoice = createInvoice(
            receiver = "Max Mustermann",
            iban = "DE89370400440532013000",
            bic = null,
            reference = "Invoice 2025-001 Payment",
            amount = BigDecimal("1250.50")
        )

        val result = generator.format(invoice)
        assertTrue(result.isSuccessful, "SEPA generation should succeed")
        assertNotNull(result.xml, "XML should be generated")

        val xmlBytes = result.xml ?: fail("XML should not be null")
        val xml = String(xmlBytes, StandardCharsets.UTF_8)
        validateXmlStructure(xml)
        assertXmlContains(xml, "DE89370400440532013000", "Max Mustermann", "1250.50")

        // Save as golden file
        saveGoldenFile("single_invoice_german_iban.xml", xmlBytes)
    }

    @Test
    fun `generate single invoice with foreign IBAN and BIC`() {
        val invoice = createInvoice(
            receiver = "Jean Dupont",
            iban = "FR1420041010050500013M02606",
            bic = "BNPAFRPPXXX",
            reference = "Consulting services Q1 2025",
            amount = BigDecimal("5000.00")
        )

        val result = generator.format(invoice)
        assertTrue(result.isSuccessful, "SEPA generation should succeed")
        assertNotNull(result.xml, "XML should be generated")

        val xmlBytes = result.xml ?: fail("XML should not be null")
        val xml = String(xmlBytes, StandardCharsets.UTF_8)
        validateXmlStructure(xml)
        assertXmlContains(xml, "FR1420041010050500013M02606", "Jean Dupont", "5000.00", "BNPAFRPPXXX")

        saveGoldenFile("single_invoice_foreign_iban_with_bic.xml", xmlBytes)
    }

    @Test
    fun `generate multiple invoices in single file`() {
        val invoices = listOf(
            createInvoice("Supplier A GmbH", "DE12500105170648489890", null, "Invoice A-123", BigDecimal("100.00")),
            createInvoice("Supplier B AG", "DE98765432109876543210", null, "Invoice B-456", BigDecimal("250.75")),
            createInvoice("Supplier C KG", "DE11222333444555666777", null, "Invoice C-789", BigDecimal("1500.25"))
        )

        val result = generator.format(invoices)
        assertTrue(result.isSuccessful, "SEPA generation should succeed")
        assertNotNull(result.xml, "XML should be generated")

        val xmlBytes = result.xml ?: fail("XML should not be null")
        val xml = String(xmlBytes, StandardCharsets.UTF_8)
        validateXmlStructure(xml)

        // Verify total amount
        val totalAmount = BigDecimal("100.00") + BigDecimal("250.75") + BigDecimal("1500.25")
        assertXmlContains(xml, totalAmount.toString())

        // Verify number of transactions
        assertTrue(xml.contains("<NbOfTxs>3</NbOfTxs>"), "Should contain 3 transactions")

        saveGoldenFile("multiple_invoices.xml", xmlBytes)
    }

    @Test
    fun `generate invoice with special characters in reference`() {
        val invoice = createInvoice(
            receiver = "Müller & Söhne GmbH",
            iban = "DE89370400440532013000",
            bic = null,
            reference = "Rechnung für Café-Ausstattung: Möbel & Stühle (2025)",
            amount = BigDecimal("999.99")
        )

        val result = generator.format(invoice)
        assertTrue(result.isSuccessful, "SEPA generation should succeed")
        assertNotNull(result.xml, "XML should be generated")

        val xmlBytes = result.xml ?: fail("XML should not be null")
        val xml = String(xmlBytes, StandardCharsets.UTF_8)
        validateXmlStructure(xml)

        saveGoldenFile("invoice_with_special_characters.xml", xmlBytes)
    }

    @Test
    fun `generate invoice with maximum amounts`() {
        val invoice = createInvoice(
            receiver = "Big Customer Corp",
            iban = "DE89370400440532013000",
            bic = null,
            reference = "Large project payment",
            amount = BigDecimal("999999.99")
        )

        val result = generator.format(invoice)
        assertTrue(result.isSuccessful, "SEPA generation should succeed")
        assertNotNull(result.xml, "XML should be generated")

        val xmlBytes = result.xml ?: fail("XML should not be null")
        val xml = String(xmlBytes, StandardCharsets.UTF_8)
        validateXmlStructure(xml)
        assertXmlContains(xml, "999999.99")

        saveGoldenFile("invoice_with_maximum_amount.xml", xmlBytes)
    }

    @Test
    fun `generate invoice with minimal amount`() {
        val invoice = createInvoice(
            receiver = "Small Vendor",
            iban = "DE89370400440532013000",
            bic = null,
            reference = "Small service charge",
            amount = BigDecimal("0.01")
        )

        val result = generator.format(invoice)
        assertTrue(result.isSuccessful, "SEPA generation should succeed")
        assertNotNull(result.xml, "XML should be generated")

        val xmlBytes = result.xml ?: fail("XML should not be null")
        val xml = String(xmlBytes, StandardCharsets.UTF_8)
        validateXmlStructure(xml)
        assertXmlContains(xml, "0.01")

        saveGoldenFile("invoice_with_minimal_amount.xml", xmlBytes)
    }

    @Test
    fun `roundtrip test - format and parse`() {
        val invoice = createInvoice(
            receiver = "Roundtrip Test GmbH",
            iban = "DE89370400440532013000",
            bic = null,
            reference = "Roundtrip test payment",
            amount = BigDecimal("1234.56")
        )

        val result = generator.format(invoice)
        assertTrue(result.isSuccessful)
        assertNotNull(result.xml)

        val xmlBytes = result.xml ?: fail("XML should not be null")

        // Parse back
        val document = generator.parse(xmlBytes)
        assertNotNull(document, "Document should be parseable")

        val pmtInf = document.cstmrCdtTrfInitn.pmtInf[0]
        val cdtTrfTxInf = pmtInf.cdtTrfTxInf[0]

        assertEquals("Roundtrip Test GmbH", cdtTrfTxInf.cdtr.nm)
        assertEquals("DE89370400440532013000", cdtTrfTxInf.cdtrAcct.id.iban)
        assertEquals("Roundtrip test payment", cdtTrfTxInf.rmtInf.ustrd)
        assertEquals(BigDecimal("1234.56").setScale(2), cdtTrfTxInf.amt.instdAmt.value)
    }

    @Test
    fun `validate all golden files against schema`() {
        // Generate all golden files first
        val goldenFiles = listOf(
            "single_invoice_german_iban.xml",
            "single_invoice_foreign_iban_with_bic.xml",
            "multiple_invoices.xml",
            "invoice_with_special_characters.xml",
            "invoice_with_maximum_amount.xml",
            "invoice_with_minimal_amount.xml"
        )

        val goldenDir = File("src/test/resources/sepa/golden")
        if (!goldenDir.exists()) {
            println("Golden files directory does not exist yet: ${goldenDir.absolutePath}")
            return
        }

        val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        val xsdUrl = javaClass.classLoader.getResource("misc/pain.001.003.03.xsd")
        assertNotNull(xsdUrl, "XSD file should be available")
        val schema = schemaFactory.newSchema(xsdUrl)
        val validator = schema.newValidator()

        for (fileName in goldenFiles) {
            val file = File(goldenDir, fileName)
            if (file.exists()) {
                assertDoesNotThrow({
                    validator.validate(StreamSource(file))
                }, "Golden file $fileName should be valid against schema")
            }
        }
    }

    private fun createInvoice(
        receiver: String,
        iban: String,
        bic: String?,
        reference: String,
        amount: BigDecimal
    ): EingangsrechnungDO {
        val invoice = EingangsrechnungDO()
        invoice.id = System.currentTimeMillis()
        invoice.paymentType = PaymentType.BANK_TRANSFER
        invoice.receiver = receiver
        invoice.iban = iban
        invoice.bic = bic
        invoice.referenz = reference

        val position = EingangsrechnungsPositionDO()
        position.einzelNetto = amount
        position.menge = BigDecimal.ONE
        invoice.positionen = mutableListOf(position)
        invoice.recalculate()

        return invoice
    }

    private fun validateXmlStructure(xml: String) {
        assertTrue(xml.contains("<?xml version=\"1.0\""), "Should have XML declaration")
        assertTrue(xml.contains("<Document"), "Should have Document root element")
        assertTrue(xml.contains("<CstmrCdtTrfInitn>"), "Should have CstmrCdtTrfInitn element")
        assertTrue(xml.contains("<GrpHdr>"), "Should have GrpHdr element")
        assertTrue(xml.contains("<PmtInf>"), "Should have PmtInf element")
        assertTrue(xml.contains("</Document>"), "Should have closing Document tag")
    }

    private fun assertXmlContains(xml: String, vararg expectedStrings: String) {
        for (expected in expectedStrings) {
            assertTrue(
                xml.contains(expected),
                "XML should contain: $expected"
            )
        }
    }

    private fun saveGoldenFile(fileName: String, content: ByteArray) {
        val goldenDir = File("src/test/resources/sepa/golden")
        goldenDir.mkdirs()
        val file = File(goldenDir, fileName)
        file.writeBytes(content)
        println("✓ Golden file saved: ${file.absolutePath}")
    }
}
