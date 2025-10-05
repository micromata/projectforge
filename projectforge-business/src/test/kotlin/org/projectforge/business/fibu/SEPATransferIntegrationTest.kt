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
 * Integration tests for SEPA transfer generation with realistic multi-invoice scenarios.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class SEPATransferIntegrationTest : AbstractTestBase() {

    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    private lateinit var generator: SEPATransferGenerator

    @BeforeEach
    fun setup() {
        generator = SEPATransferGenerator()
        val param = configurationDao.getEntry(ConfigurationParam.ORGANIZATION)
        param?.stringValue = "ProjectForge GmbH"
        param?.let { configurationDao.update(it, false) }
    }

    @Test
    fun `generate realistic monthly payment batch with mixed SEPA countries`() {
        val invoices = listOf(
            // German suppliers (no BIC required)
            createInvoice(
                receiver = "Deutsche Consulting AG",
                iban = "DE89370400440532013000",
                bic = null,
                reference = "Beratungsleistung Januar 2025",
                amount = BigDecimal("15000.00")
            ),
            createInvoice(
                receiver = "Müller & Söhne GmbH",
                iban = "DE12500105170648489890",
                bic = null,
                reference = "Lieferung Büromaterial Q1/2025",
                amount = BigDecimal("2345.67")
            ),
            // Foreign suppliers (BIC required)
            createInvoice(
                receiver = "Consulting International SAS",
                iban = "FR1420041010050500013M02606",
                bic = "BNPAFRPPXXX",
                reference = "International project phase 1",
                amount = BigDecimal("25000.00")
            ),
            createInvoice(
                receiver = "Austrian Software GmbH",
                iban = "AT611904300234573201",
                bic = "BKAUATWWXXX",
                reference = "Software-Lizenz 2025",
                amount = BigDecimal("8999.99")
            ),
            // More German suppliers with special characters
            createInvoice(
                receiver = "IT-Dienstleistung für Große Projekte GmbH & Co. KG",
                iban = "DE98765432109876543210",
                bic = null,
                reference = "Server-Hosting & Wartung (inkl. Updates)",
                amount = BigDecimal("1234.50")
            ),
            // Small amounts
            createInvoice(
                receiver = "Kleinunternehmer Schmidt",
                iban = "DE11222333444555666777",
                bic = null,
                reference = "Kleine Reparatur",
                amount = BigDecimal("99.99")
            )
        )

        val result = generator.format(invoices)
        assertTrue(result.isSuccessful, "Multi-invoice generation should succeed")
        assertTrue(result.errors.isEmpty(), "Should have no errors")

        val xmlBytes = result.xml ?: fail("XML should not be null")
        val xml = String(xmlBytes, StandardCharsets.UTF_8)

        // Validate against schema
        validateAgainstSchema(xmlBytes)

        val doc = parseXml(xml)

        // Verify header information
        assertEquals("6", getXPathValue(doc, "//GrpHdr/NbOfTxs"), "Should have 6 transactions")

        // Verify total sum
        val expectedTotal = BigDecimal("15000.00")
            .add(BigDecimal("2345.67"))
            .add(BigDecimal("25000.00"))
            .add(BigDecimal("8999.99"))
            .add(BigDecimal("1234.50"))
            .add(BigDecimal("99.99"))

        val actualTotal = getXPathValue(doc, "//GrpHdr/CtrlSum")
        assertEquals(expectedTotal.setScale(2).toString(), actualTotal, "Total amount should match")

        // Verify initiating party
        assertEquals("ProjectForge GmbH", getXPathValue(doc, "//InitgPty/Nm"))

        // Verify individual transactions using XPath
        val xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath()
        val cdtTrfTxInfNodes = xpath.evaluate("//*[local-name()='CdtTrfTxInf']", doc, javax.xml.xpath.XPathConstants.NODESET) as org.w3c.dom.NodeList
        assertEquals(6, cdtTrfTxInfNodes.length, "Should have 6 credit transfer transactions")

        // Verify first German transaction (no BIC) - extract from first CdtTrfTxInf
        val firstTx = cdtTrfTxInfNodes.item(0) as org.w3c.dom.Element
        val firstCreditor = getElementText(firstTx, "Cdtr", "Nm")
        val firstIban = getElementText(firstTx, "CdtrAcct", "Id", "IBAN")
        assertEquals("Deutsche Consulting AG", firstCreditor)
        assertEquals("DE89370400440532013000", firstIban)

        // Verify French transaction (with BIC) - third transaction
        val thirdTx = cdtTrfTxInfNodes.item(2) as org.w3c.dom.Element
        val thirdCreditor = getElementText(thirdTx, "Cdtr", "Nm")
        val thirdIban = getElementText(thirdTx, "CdtrAcct", "Id", "IBAN")
        val thirdBic = getElementText(thirdTx, "CdtrAgt", "FinInstnId", "BIC")
        assertEquals("Consulting International SAS", thirdCreditor)
        assertEquals("FR1420041010050500013M02606", thirdIban)
        assertEquals("BNPAFRPPXXX", thirdBic)

        println("✓ Realistic monthly payment batch test passed")

        // Compare with golden file for integration test
        assertXmlEquals(File("src/test/resources/sepa/golden/integration_monthly_payment_batch.xml"), xml)
    }

    @Test
    fun `handle edge cases in batch processing`() {
        val invoices = listOf(
            // Maximum amount
            createInvoice(
                receiver = "Large Contract Corp",
                iban = "DE89370400440532013000",
                bic = null,
                reference = "Large project milestone payment",
                amount = BigDecimal("999999.99")
            ),
            // Minimum amount
            createInvoice(
                receiver = "Micro Service Provider",
                iban = "DE12500105170648489890",
                bic = null,
                reference = "Small adjustment",
                amount = BigDecimal("0.01")
            ),
            // Amount with many decimals (should be rounded)
            createInvoice(
                receiver = "Precise Calculations Inc",
                iban = "DE98765432109876543210",
                bic = null,
                reference = "Calculated amount",
                amount = BigDecimal("1234.567")
            )
        )

        val result = generator.format(invoices)
        assertTrue(result.isSuccessful)

        val xmlBytes = result.xml ?: fail("XML should not be null")
        validateAgainstSchema(xmlBytes)

        val doc = parseXml(xml = String(xmlBytes, StandardCharsets.UTF_8))

        // Verify amounts are properly formatted
        val xpath = XPathFactory.newInstance().newXPath()
        val amountNodes = xpath.evaluate("//*[local-name()='InstdAmt']/text()", doc, XPathConstants.NODESET) as org.w3c.dom.NodeList

        assertEquals(3, amountNodes.length, "Should have 3 InstdAmt nodes, got: ${amountNodes.length}")
        for (i in 0 until amountNodes.length) {
            val amountStr = amountNodes.item(i).nodeValue
            assertTrue(amountStr.matches(Regex("\\d+\\.\\d{2}")),
                "Amount should have exactly 2 decimals: $amountStr")
        }

        // Verify specific amounts
        assertEquals("999999.99", amountNodes.item(0).nodeValue)
        assertEquals("0.01", amountNodes.item(1).nodeValue)
        assertEquals("1234.57", amountNodes.item(2).nodeValue) // Rounded

        println("✓ Edge cases in batch processing test passed")
    }

    @Test
    fun `verify error handling for invalid invoices in batch`() {
        val invoices = listOf(
            // Valid invoice
            createInvoice(
                receiver = "Valid Supplier",
                iban = "DE89370400440532013000",
                bic = null,
                reference = "Valid payment",
                amount = BigDecimal("100.00")
            ),
            // Invalid: zero amount
            createInvoice(
                receiver = "Invalid Supplier 1",
                iban = "DE12500105170648489890",
                bic = null,
                reference = "Should fail",
                amount = BigDecimal("0.00")
            ),
            // Invalid: foreign IBAN without BIC
            createInvoice(
                receiver = "Invalid Supplier 2",
                iban = "FR1420041010050500013M02606",
                bic = null,
                reference = "Should fail - no BIC",
                amount = BigDecimal("100.00")
            )
        )

        val result = generator.format(invoices)
        assertFalse(result.isSuccessful, "Batch with invalid invoices should fail")
        assertTrue(result.errors.isNotEmpty(), "Should have errors")
        assertNull(result.xml, "XML should not be generated when errors exist")

        println("✓ Error handling for invalid invoices test passed")
    }

    @Test
    fun `generate and validate complex reference strings`() {
        val complexReferences = listOf(
            "Invoice #2025-001: Consulting Services (Jan-Mar)",
            "RE: Project \"Phoenix\" - Phase 1 & 2 Implementation",
            "Büromaterial für Café & Restaurant-Ausstattung",
            "Server-Miete 01.01.2025 - 31.12.2025 (inkl. 19% MwSt.)",
            "Zahlung gemäß Vereinbarung vom 15.01.2025"
        )

        val invoices = complexReferences.mapIndexed { index, reference ->
            createInvoice(
                receiver = "Supplier $index",
                iban = "DE89370400440532013000",
                bic = null,
                reference = reference,
                amount = BigDecimal("${(index + 1) * 100}.00")
            )
        }

        val result = generator.format(invoices)
        assertTrue(result.isSuccessful, "Should handle complex reference strings")

        val xmlBytes = result.xml ?: fail("XML should not be null")
        validateAgainstSchema(xmlBytes)

        val xml = String(xmlBytes, java.nio.charset.StandardCharsets.UTF_8)
        val doc = parseXml(xml)

        // Verify number of transactions using XPath
        val xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath()
        val ustrdNodes = xpath.evaluate("//*[local-name()='Ustrd']", doc, javax.xml.xpath.XPathConstants.NODESET) as org.w3c.dom.NodeList
        assertEquals(complexReferences.size, ustrdNodes.length)

        // Verify references are preserved (potentially cleaned of unsupported chars)
        for (i in 0 until ustrdNodes.length) {
            val reference = ustrdNodes.item(i).textContent
            assertNotNull(reference, "Reference should not be null")
            assertTrue(reference.isNotEmpty(), "Reference should not be empty")
        }

        println("✓ Complex reference strings test passed")
    }

    private fun createInvoice(
        receiver: String,
        iban: String,
        bic: String?,
        reference: String,
        amount: BigDecimal
    ): EingangsrechnungDO {
        val invoice = EingangsrechnungDO()
        invoice.id = System.currentTimeMillis() + Math.random().toLong()
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

    private fun parseXml(xml: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        return builder.parse(InputSource(StringReader(xml)))
    }

    private fun getXPathValue(doc: Document, xpathExpression: String): String {
        val xpath = XPathFactory.newInstance().newXPath()
        // Use local-name() to ignore namespaces
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

    private fun getElementText(parent: org.w3c.dom.Element, vararg path: String): String? {
        var current: org.w3c.dom.Element? = parent
        for (tagName in path) {
            if (current == null) return null
            current = getFirstChildElement(current, tagName)
        }
        return current?.textContent
    }

    private fun getFirstChildElement(parent: org.w3c.dom.Element, localName: String): org.w3c.dom.Element? {
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                val element = node as org.w3c.dom.Element
                if (element.localName == localName) {
                    return element
                }
            }
        }
        return null
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
}
