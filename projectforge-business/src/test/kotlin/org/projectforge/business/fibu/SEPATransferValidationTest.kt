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
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.springframework.beans.factory.annotation.Autowired
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

/**
 * Validation test to ensure SEPA XMLs conform to pain.001.003.03 schema
 * and match golden reference files after migration.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class SEPATransferValidationTest : AbstractTestBase() {

    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    private lateinit var generator: SEPATransferGenerator

    @BeforeEach
    fun setup() {
        generator = SEPATransferGenerator()
        val param = configurationDao.getEntry(ConfigurationParam.ORGANIZATION)
        param?.stringValue = "Test Organization GmbH"
        param?.let { configurationDao.update(it, false) }
    }

    @Test
    fun `validate all golden files against pain schema`() {
        val goldenDir = File("src/test/resources/sepa/golden")
        assertTrue(goldenDir.exists(), "Golden files directory should exist")

        val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        val xsdUrl = javaClass.classLoader.getResource("misc/pain.001.003.03.xsd")
        assertNotNull(xsdUrl, "XSD file should be available")
        val schema = schemaFactory.newSchema(xsdUrl)
        val validator = schema.newValidator()

        val goldenFiles = goldenDir.listFiles { _, name -> name.endsWith(".xml") }
        assertNotNull(goldenFiles)
        assertTrue(goldenFiles!!.isNotEmpty(), "Should have golden files")

        for (file in goldenFiles) {
            assertDoesNotThrow({
                validator.validate(StreamSource(file))
            }, "Golden file ${file.name} should be valid against pain.001.003.03 schema")
            println("✓ Schema validation passed: ${file.name}")
        }
    }

    @Test
    fun `verify regenerated XMLs match structure of golden files`() {
        val testCases = listOf(
            TestCase(
                name = "single_invoice_german_iban.xml",
                invoice = createInvoice("Max Mustermann", "DE89370400440532013000", null,
                    "Invoice 2025-001 Payment", BigDecimal("1250.50"))
            ),
            TestCase(
                name = "single_invoice_foreign_iban_with_bic.xml",
                invoice = createInvoice("Jean Dupont", "FR1420041010050500013M02606", "BNPAFRPPXXX",
                    "Consulting services Q1 2025", BigDecimal("5000.00"))
            ),
            TestCase(
                name = "invoice_with_minimal_amount.xml",
                invoice = createInvoice("Small Vendor", "DE89370400440532013000", null,
                    "Small service charge", BigDecimal("0.01"))
            )
        )

        for (testCase in testCases) {
            val result = generator.format(testCase.invoice)
            assertTrue(result.isSuccessful, "${testCase.name}: Generation should succeed")

            val xmlBytes = result.xml ?: fail("XML should not be null")
            val regeneratedXml = String(xmlBytes, StandardCharsets.UTF_8)

            val goldenFile = File("src/test/resources/sepa/golden/${testCase.name}")
            if (goldenFile.exists()) {
                // Compare with golden file (ignoring timestamps via normalization)
                assertXmlEquals(goldenFile, regeneratedXml, testCase.name)
                println("✓ Golden file validation passed: ${testCase.name}")
            }
        }
    }

    @Test
    fun `verify critical SEPA fields in generated XML`() {
        val invoice = createInvoice(
            receiver = "Critical Test GmbH",
            iban = "DE89370400440532013000",
            bic = null,
            reference = "Critical field test",
            amount = BigDecimal("999.99")
        )

        val result = generator.format(invoice)
        assertTrue(result.isSuccessful)

        val xmlBytes = result.xml ?: fail("XML should not be null")
        val xml = String(xmlBytes, StandardCharsets.UTF_8)
        val doc = parseXml(xml)

        // Verify critical SEPA fields using XPath
        assertEquals("TRF", getXPathValue(doc, "//PmtMtd"), "Payment method should be TRF")
        assertEquals("SEPA", getXPathValue(doc, "//SvcLvl/Cd"), "Service level should be SEPA")
        assertEquals("EUR", getXPathValue(doc, "//@Ccy"), "Currency should be EUR")
        assertEquals("Test Organization GmbH", getXPathValue(doc, "//InitgPty/Nm"), "Initiating party should match")
        assertEquals("Critical Test GmbH", getXPathValue(doc, "//Cdtr/Nm"), "Creditor should match")
        assertEquals("DE89370400440532013000", getXPathValue(doc, "//CdtrAcct/Id/IBAN"), "IBAN should match")
        assertEquals("999.99", getXPathValue(doc, "//InstdAmt"), "Amount should match")
        assertEquals("Critical field test", getXPathValue(doc, "//Ustrd"), "Reference should match")

        println("✓ Critical SEPA fields validation passed")
    }

    @Test
    fun `verify amounts are correctly formatted with 2 decimals`() {
        val testAmounts = listOf(
            BigDecimal("100"),
            BigDecimal("100.5"),
            BigDecimal("100.50"),
            BigDecimal("100.556"), // Should round to 100.56
            BigDecimal("0.01"),
            BigDecimal("999999.99")
        )

        for (amount in testAmounts) {
            val invoice = createInvoice("Test", "DE89370400440532013000", null, "Test", amount)
            val result = generator.format(invoice)
            assertTrue(result.isSuccessful)

            val xmlBytes = result.xml ?: fail("XML should not be null")
            val xml = String(xmlBytes, StandardCharsets.UTF_8)
            val doc = parseXml(xml)

            val xmlAmount = getXPathValue(doc, "//InstdAmt")
            assertTrue(xmlAmount.matches(Regex("\\d+\\.\\d{2}")),
                "Amount should have exactly 2 decimals: $xmlAmount (from $amount)")
        }

        println("✓ Amount formatting validation passed")
    }

    @Test
    fun `verify roundtrip consistency`() {
        val invoice = createInvoice(
            receiver = "Roundtrip Corp",
            iban = "DE89370400440532013000",
            bic = null,
            reference = "Roundtrip consistency test",
            amount = BigDecimal("5432.10")
        )

        // First generation
        val result1 = generator.format(invoice)
        assertTrue(result1.isSuccessful)
        val xml1 = result1.xml ?: fail("First XML should not be null")

        // Verify generated XML using XPath
        val xml1String = String(xml1, StandardCharsets.UTF_8)
        val doc = parseXml(xml1String)

        assertEquals("Roundtrip Corp", getXPathValue(doc, "//Cdtr/Nm"))
        assertEquals("DE89370400440532013000", getXPathValue(doc, "//CdtrAcct/Id/IBAN"))
        assertEquals("Roundtrip consistency test", getXPathValue(doc, "//Ustrd"))
        assertEquals("5432.10", getXPathValue(doc, "//InstdAmt"))

        // Generate again with same input to verify consistency
        val result2 = generator.format(invoice)
        assertTrue(result2.isSuccessful)
        val xml2 = result2.xml ?: fail("Second XML should not be null")
        val xml2String = String(xml2, StandardCharsets.UTF_8)
        val doc2 = parseXml(xml2String)

        // Verify same key fields (ignoring timestamps and message IDs)
        assertEquals(getXPathValue(doc, "//Cdtr/Nm"), getXPathValue(doc2, "//Cdtr/Nm"))
        assertEquals(getXPathValue(doc, "//CdtrAcct/Id/IBAN"), getXPathValue(doc2, "//CdtrAcct/Id/IBAN"))
        assertEquals(getXPathValue(doc, "//Ustrd"), getXPathValue(doc2, "//Ustrd"))
        assertEquals(getXPathValue(doc, "//InstdAmt"), getXPathValue(doc2, "//InstdAmt"))

        println("✓ Roundtrip consistency validation passed")
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

    private fun parseXml(xml: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        return builder.parse(InputSource(StringReader(xml)))
    }

    private fun getXPathValue(doc: Document, xpathExpression: String): String {
        val xpath = XPathFactory.newInstance().newXPath()
        // Use local-name() to ignore namespaces
        val nsAgnosticExpression = xpathExpression.replace(
            Regex("/([A-Za-z])"),
            "/*[local-name()='$1"
        ).replace(Regex("([A-Za-z])>"), "$1']")

        // Simple approach: convert //Element to //*[local-name()='Element']
        val converted = if (xpathExpression.contains("@")) {
            xpathExpression // Attribute access doesn't need namespace handling
        } else {
            xpathExpression.split("/").joinToString("/") { part ->
                if (part.isEmpty() || part == "*" || part.startsWith("@")) {
                    part
                } else {
                    "*[local-name()='$part']"
                }
            }
        }

        val result = xpath.evaluate(converted, doc, XPathConstants.STRING)
        return result.toString()
    }

    private data class TestCase(
        val name: String,
        val invoice: EingangsrechnungDO
    )
}
