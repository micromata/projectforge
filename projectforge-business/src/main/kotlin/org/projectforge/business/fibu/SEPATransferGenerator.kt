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

import org.apache.commons.lang3.StringUtils
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.time.PFDateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.xml.sax.SAXException
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import jakarta.annotation.PostConstruct
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory

/**
 * This component generates and reads transfer files in pain.001.003.03 format.
 * Migrated from JAXB to manual XML generation using javax.xml APIs.
 *
 * @author Stefan Niemczyk (s.niemczyk@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de) - JAXB migration
 */
@Component
class SEPATransferGenerator {

    enum class SEPATransferError {
        NO_INPUT, SUM, BANK_TRANSFER, BIC, IBAN, RECEIVER, REFERENCE, INVOICE_OR_DEBITOR_NOTEXISTING
    }

    companion object {
        private val log = LoggerFactory.getLogger(SEPATransferGenerator::class.java)
        private const val PAIN_001_003_03_XSD = "misc/pain.001.003.03.xsd"
        private val MESSAGE_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm")
    }

    private var painSchema: Schema? = null
    private val patternBic = Pattern.compile("[A-Z]{6,6}[A-Z2-9][A-NP-Z0-9]([A-Z0-9]{3,3}){0,1}")
    private val patternIBAN = Pattern.compile("[A-Z]{2,2}[0-9]{2,2}[a-zA-Z0-9]{1,30}")

    @Value("\${projectforge.fibu.sepa.defaultIBAN:DE87200500001234567890}")
    private var defaultIBAN: String = "DE87200500001234567890"

    @Value("\${projectforge.fibu.sepa.defaultBIC:BANKDEFFXXX}")
    private var defaultBIC: String = "BANKDEFFXXX"

    @PostConstruct
    fun init() {
        val xsdUrl = javaClass.classLoader.getResource(PAIN_001_003_03_XSD)
        if (xsdUrl == null) {
            log.error("pain.001.003.03.xsd file not found, transfer export not possible without it.")
            return
        }

        try {
            val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
            painSchema = schemaFactory.newSchema(xsdUrl)
        } catch (e: SAXException) {
            log.error("An error occurred while reading pain.001.003.03.xsd -> transfer export not possible.", e)
        }
    }

    /**
     * Generates a transfer xml for the given invoice in pain.001.003.03 format.
     */
    fun format(invoice: EingangsrechnungDO): SEPATransferResult {
        return format(listOf(invoice))
    }

    /**
     * Generates a transfer xml for the given invoices in pain.001.003.03 format.
     */
    fun format(invoices: List<EingangsrechnungDO>?): SEPATransferResult {
        val result = SEPATransferResult()

        if (invoices == null || invoices.isEmpty()) {
            val error = "Invoices are null or empty"
            log.warn("A problem occurred while exporting invoices: $error")
            throw UserException("error", "A problem occurred while exporting invoices: $error")
        }

        // Generate message ID and get debtor info
        val now = PFDateTime.now()
        val msgId = "transfer-${now.dateTime.format(MESSAGE_ID_FORMATTER)}"
        val debitor = Configuration.instance.getStringValue(ConfigurationParam.ORGANIZATION)

        // Validate all invoices and create transactions
        val transactions = mutableListOf<SEPAXmlBuilder.TransactionInfo>()
        var amount = BigDecimal.ZERO

        for ((index, invoice) in invoices.withIndex()) {
            val errors = validateInvoice(invoice)
            if (errors.isNotEmpty()) {
                (result.errors as MutableMap)[invoice] = errors
                continue
            }

            RechnungCalculator.calculate(invoice)
            val invoiceAmount = invoice.info.grossSumWithDiscount ?: BigDecimal.ZERO
            amount = amount.add(invoiceAmount)

            val iban = invoice.iban?.replace("\\s".toRegex(), "")?.uppercase() ?: ""
            transactions.add(
                SEPAXmlBuilder.TransactionInfo(
                    creditorName = SEPATransferGeneratorUtils.eraseUnsuportedChars(invoice.receiver ?: ""),
                    creditorIban = SEPATransferGeneratorUtils.eraseUnsuportedChars(iban),
                    creditorBic = if (!iban.startsWith("DE")) {
                        invoice.bic?.let { SEPATransferGeneratorUtils.eraseUnsuportedChars(it.uppercase()) }
                    } else null,
                    amount = invoiceAmount,
                    remittanceInfo = SEPATransferGeneratorUtils.eraseUnsuportedChars(invoice.referenz ?: "")
                )
            )
        }

        if (result.errors.isNotEmpty()) {
            return result
        }

        amount = amount.setScale(2, RoundingMode.HALF_UP)

        // Format dates using java.time
        val creationDateTime = now.dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val requiredExecutionDate = now.dateTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)

        // Build XML
        try {
            val xmlBytes = SEPAXmlBuilder.buildXml(
                messageId = msgId,
                creationDateTime = creationDateTime,
                initiatingPartyName = SEPATransferGeneratorUtils.eraseUnsuportedChars(debitor),
                debtorName = SEPATransferGeneratorUtils.eraseUnsuportedChars(debitor),
                debtorIban = SEPATransferGeneratorUtils.eraseUnsuportedChars(defaultIBAN),
                debtorBic = SEPATransferGeneratorUtils.eraseUnsuportedChars(defaultBIC),
                numberOfTransactions = transactions.size,
                controlSum = amount,
                requiredExecutionDate = requiredExecutionDate,
                transactions = transactions
            )

            // Validate against schema
            painSchema?.let { schema ->
                try {
                    val validator = schema.newValidator()
                    validator.validate(StreamSource(ByteArrayInputStream(xmlBytes)))
                } catch (e: Exception) {
                    log.error("Generated XML failed schema validation", e)
                }
            }

            result.xml = xmlBytes
        } catch (e: Exception) {
            log.error("An error occurred while building SEPA XML.", e)
        }

        return result
    }


    private fun validateInvoice(invoice: EingangsrechnungDO): List<SEPATransferError> {
        val errors = mutableListOf<SEPATransferError>()

        RechnungCalculator.calculate(invoice)

        val grossSum = invoice.info.grossSumWithDiscount
        if (grossSum == null || grossSum.compareTo(BigDecimal.ZERO) == 0) {
            errors.add(SEPATransferError.SUM)
        }

        if (invoice.paymentType != PaymentType.BANK_TRANSFER) {
            errors.add(SEPATransferError.BANK_TRANSFER)
        }

        var iban = invoice.iban
        if (StringUtils.isNotBlank(iban)) {
            iban = iban!!.replace("\\s".toRegex(), "").uppercase()
        }

        if (iban != null && !iban.startsWith("DE")) {
            val bic = invoice.bic
            if (bic == null || !patternBic.matcher(bic.uppercase()).matches()) {
                errors.add(SEPATransferError.BIC)
            }
        }

        if (iban == null || !patternIBAN.matcher(iban).matches()) {
            errors.add(SEPATransferError.IBAN)
        }

        if (invoice.receiver == null || invoice.receiver!!.length < 1 || invoice.receiver!!.length > 70) {
            errors.add(SEPATransferError.RECEIVER)
        }

        if (invoice.referenz == null || invoice.referenz!!.length < 1 || invoice.referenz!!.length > 140) {
            errors.add(SEPATransferError.REFERENCE)
        }

        return errors
    }
}
