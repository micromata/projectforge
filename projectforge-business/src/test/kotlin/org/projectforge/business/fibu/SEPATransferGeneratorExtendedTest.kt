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
        validateAgainstSchema(xmlBytes)
        validateXmlStructure(xml)
        assertXmlContains(xml, "DE89370400440532013000", "Max Mustermann", "1250.50")

        // Compare with golden file
        assertXmlEquals(File("src/test/resources/sepa/golden/single_invoice_german_iban.xml"), xml)
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
        validateAgainstSchema(xmlBytes)
        validateXmlStructure(xml)
        assertXmlContains(xml, "FR1420041010050500013M02606", "Jean Dupont", "5000.00", "BNPAFRPPXXX")

        assertXmlEquals(File("src/test/resources/sepa/golden/single_invoice_foreign_iban_with_bic.xml"), xml)
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
        validateAgainstSchema(xmlBytes)
        validateXmlStructure(xml)

        // Verify total amount
        val totalAmount = BigDecimal("100.00") + BigDecimal("250.75") + BigDecimal("1500.25")
        assertXmlContains(xml, totalAmount.toString())

        // Verify number of transactions
        assertTrue(xml.contains("<NbOfTxs>3</NbOfTxs>"), "Should contain 3 transactions")

        assertXmlEquals(File("src/test/resources/sepa/golden/multiple_invoices.xml"), xml)
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
        validateAgainstSchema(xmlBytes)
        validateXmlStructure(xml)

        assertXmlEquals(File("src/test/resources/sepa/golden/invoice_with_special_characters.xml"), xml)
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
        validateAgainstSchema(xmlBytes)
        validateXmlStructure(xml)
        assertXmlContains(xml, "999999.99")

        assertXmlEquals(File("src/test/resources/sepa/golden/invoice_with_maximum_amount.xml"), xml)
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
        validateAgainstSchema(xmlBytes)
        validateXmlStructure(xml)
        assertXmlContains(xml, "0.01")

        assertXmlEquals(File("src/test/resources/sepa/golden/invoice_with_minimal_amount.xml"), xml)
    }

    @Test
    fun `validate generated XML structure and content`() {
        val invoice = createInvoice(
            receiver = "Validation Test GmbH",
            iban = "DE89370400440532013000",
            bic = null,
            reference = "Validation test payment",
            amount = BigDecimal("1234.56")
        )

        val result = generator.format(invoice)
        assertTrue(result.isSuccessful)
        assertNotNull(result.xml)

        val xmlBytes = result.xml ?: fail("XML should not be null")
        validateAgainstSchema(xmlBytes)
        val xml = String(xmlBytes, StandardCharsets.UTF_8)

        // Validate XML structure and content using XPath
        val doc = parseXmlForValidation(xml)

        assertEquals("Validation Test GmbH", getXPathValue(doc, "//Cdtr/Nm"))
        assertEquals("DE89370400440532013000", getXPathValue(doc, "//CdtrAcct/Id/IBAN"))
        assertEquals("Validation test payment", getXPathValue(doc, "//Ustrd"))
        assertEquals("1234.56", getXPathValue(doc, "//InstdAmt"))
    }

    private fun parseXmlForValidation(xml: String): org.w3c.dom.Document {
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        return builder.parse(org.xml.sax.InputSource(java.io.StringReader(xml)))
    }

    private fun getXPathValue(doc: org.w3c.dom.Document, xpathExpression: String): String {
        val xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath()
        val converted = if (xpathExpression.contains("@")) {
            xpathExpression
        } else {
            xpathExpression.split("/").joinToString("/") { part ->
                if (part.isEmpty() || part == "*" || part.startsWith("@")) part
                else "*[local-name()='$part']"
            }
        }
        val result = xpath.evaluate(converted, doc, javax.xml.xpath.XPathConstants.STRING)
        return result.toString()
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

    private fun normalizeXml(xml: String): String {
        return xml
            .replace(Regex("<MsgId>.*?</MsgId>"), "<MsgId>NORMALIZED</MsgId>")
            .replace(Regex("<CreDtTm>.*?</CreDtTm>"), "<CreDtTm>NORMALIZED</CreDtTm>")
            .replace(Regex("<PmtInfId>.*?</PmtInfId>"), "<PmtInfId>NORMALIZED</PmtInfId>")
            .replace(Regex("<EndToEndId>.*?</EndToEndId>"), "<EndToEndId>NORMALIZED</EndToEndId>")
            .replace(Regex("<ReqdExctnDt>.*?</ReqdExctnDt>"), "<ReqdExctnDt>NORMALIZED</ReqdExctnDt>")
    }

    private fun assertXmlEquals(goldenFile: File, generatedXml: String, message: String = "") {
        assertTrue(goldenFile.exists(), "Golden file should exist: ${goldenFile.absolutePath}")
        val goldenXml = goldenFile.readText(StandardCharsets.UTF_8)
        assertEquals(normalizeXml(goldenXml), normalizeXml(generatedXml), message)
    }

    private fun validateAgainstSchema(xmlBytes: ByteArray) {
        val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        val xsdUrl = javaClass.classLoader.getResource("misc/pain.001.003.03.xsd")
        assertNotNull(xsdUrl, "XSD file should be available")
        val schema = schemaFactory.newSchema(xsdUrl)
        val validator = schema.newValidator()

        assertDoesNotThrow({
            validator.validate(StreamSource(xmlBytes.inputStream()))
        }, "Generated XML should be valid against pain.001.003.03 schema")
    }
}
