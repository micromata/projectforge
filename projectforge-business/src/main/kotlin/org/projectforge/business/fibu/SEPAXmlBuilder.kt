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

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Manual XML builder for SEPA pain.001.003.03 format.
 * Replaces JAXB marshalling with standard javax.xml APIs.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object SEPAXmlBuilder {

    private const val NAMESPACE = "urn:iso:std:iso:20022:tech:xsd:pain.001.003.03"

    /**
     * Build SEPA XML for a list of invoices.
     */
    fun buildXml(
        messageId: String,
        creationDateTime: String,
        initiatingPartyName: String,
        debtorName: String,
        debtorIban: String,
        debtorBic: String,
        numberOfTransactions: Int,
        controlSum: BigDecimal,
        requiredExecutionDate: String,
        transactions: List<TransactionInfo>
    ): ByteArray {
        val dbFactory = DocumentBuilderFactory.newInstance()
        dbFactory.isNamespaceAware = true
        val docBuilder = dbFactory.newDocumentBuilder()
        val doc = docBuilder.newDocument()

        // Create root element with namespace
        val document = doc.createElementNS(NAMESPACE, "Document")
        doc.appendChild(document)

        // CstmrCdtTrfInitn
        val cstmrCdtTrfInitn = doc.createElementNS(NAMESPACE, "CstmrCdtTrfInitn")
        document.appendChild(cstmrCdtTrfInitn)

        // Group Header
        buildGroupHeader(doc, cstmrCdtTrfInitn, messageId, creationDateTime, numberOfTransactions, controlSum, initiatingPartyName)

        // Payment Information
        buildPaymentInformation(
            doc, cstmrCdtTrfInitn, messageId, numberOfTransactions, controlSum,
            requiredExecutionDate, debtorName, debtorIban, debtorBic, transactions
        )

        // Convert to bytes
        return serializeDocument(doc)
    }

    private fun buildGroupHeader(
        doc: Document,
        parent: Element,
        messageId: String,
        creationDateTime: String,
        numberOfTransactions: Int,
        controlSum: BigDecimal,
        initiatingPartyName: String
    ) {
        val grpHdr = doc.createElementNS(NAMESPACE,"GrpHdr")
        parent.appendChild(grpHdr)

        addElement(doc, grpHdr, "MsgId", messageId)
        addElement(doc, grpHdr, "CreDtTm", creationDateTime)
        addElement(doc, grpHdr, "NbOfTxs", numberOfTransactions.toString())
        addElement(doc, grpHdr, "CtrlSum", controlSum.setScale(2, RoundingMode.HALF_UP).toString())

        val initgPty = doc.createElementNS(NAMESPACE,"InitgPty")
        grpHdr.appendChild(initgPty)
        addElement(doc, initgPty, "Nm", initiatingPartyName)
    }

    private fun buildPaymentInformation(
        doc: Document,
        parent: Element,
        messageId: String,
        numberOfTransactions: Int,
        controlSum: BigDecimal,
        requiredExecutionDate: String,
        debtorName: String,
        debtorIban: String,
        debtorBic: String,
        transactions: List<TransactionInfo>
    ) {
        val pmtInf = doc.createElementNS(NAMESPACE,"PmtInf")
        parent.appendChild(pmtInf)

        addElement(doc, pmtInf, "PmtInfId", "$messageId-1")
        addElement(doc, pmtInf, "PmtMtd", "TRF")
        addElement(doc, pmtInf, "BtchBookg", "true")
        addElement(doc, pmtInf, "NbOfTxs", numberOfTransactions.toString())
        addElement(doc, pmtInf, "CtrlSum", controlSum.setScale(2, RoundingMode.HALF_UP).toString())

        // Payment Type Information
        val pmtTpInf = doc.createElementNS(NAMESPACE,"PmtTpInf")
        pmtInf.appendChild(pmtTpInf)
        val svcLvl = doc.createElementNS(NAMESPACE,"SvcLvl")
        pmtTpInf.appendChild(svcLvl)
        addElement(doc, svcLvl, "Cd", "SEPA")

        addElement(doc, pmtInf, "ReqdExctnDt", requiredExecutionDate)

        // Debtor
        val dbtr = doc.createElementNS(NAMESPACE,"Dbtr")
        pmtInf.appendChild(dbtr)
        addElement(doc, dbtr, "Nm", debtorName)

        // Debtor Account
        val dbtrAcct = doc.createElementNS(NAMESPACE,"DbtrAcct")
        pmtInf.appendChild(dbtrAcct)
        val dbtrAcctId = doc.createElementNS(NAMESPACE,"Id")
        dbtrAcct.appendChild(dbtrAcctId)
        addElement(doc, dbtrAcctId, "IBAN", debtorIban)

        // Debtor Agent
        val dbtrAgt = doc.createElementNS(NAMESPACE,"DbtrAgt")
        pmtInf.appendChild(dbtrAgt)
        val dbtrFinInstnId = doc.createElementNS(NAMESPACE,"FinInstnId")
        dbtrAgt.appendChild(dbtrFinInstnId)
        addElement(doc, dbtrFinInstnId, "BIC", debtorBic)

        // Credit Transfer Transaction Information
        transactions.forEachIndexed { index, txInfo ->
            buildTransaction(doc, pmtInf, messageId, index + 1, txInfo)
        }
    }

    private fun buildTransaction(
        doc: Document,
        parent: Element,
        messageId: String,
        transactionIndex: Int,
        txInfo: TransactionInfo
    ) {
        val cdtTrfTxInf = doc.createElementNS(NAMESPACE,"CdtTrfTxInf")
        parent.appendChild(cdtTrfTxInf)

        // Payment ID
        val pmtId = doc.createElementNS(NAMESPACE,"PmtId")
        cdtTrfTxInf.appendChild(pmtId)
        addElement(doc, pmtId, "EndToEndId", "$messageId-1-$transactionIndex")

        // Amount
        val amt = doc.createElementNS(NAMESPACE,"Amt")
        cdtTrfTxInf.appendChild(amt)
        val instdAmt = doc.createElementNS(NAMESPACE,"InstdAmt")
        instdAmt.setAttribute("Ccy", "EUR")
        instdAmt.textContent = txInfo.amount.setScale(2, RoundingMode.HALF_UP).toString()
        amt.appendChild(instdAmt)

        // Creditor Agent (BIC) - only for non-German IBANs
        // IMPORTANT: CdtrAgt must come BEFORE Cdtr and CdtrAcct according to ISO 20022
        if (!txInfo.creditorIban.startsWith("DE") && txInfo.creditorBic != null) {
            val cdtrAgt = doc.createElementNS(NAMESPACE,"CdtrAgt")
            cdtTrfTxInf.appendChild(cdtrAgt)
            val cdtrFinInstnId = doc.createElementNS(NAMESPACE,"FinInstnId")
            cdtrAgt.appendChild(cdtrFinInstnId)
            addElement(doc, cdtrFinInstnId, "BIC", txInfo.creditorBic)
        }

        // Creditor
        val cdtr = doc.createElementNS(NAMESPACE,"Cdtr")
        cdtTrfTxInf.appendChild(cdtr)
        addElement(doc, cdtr, "Nm", txInfo.creditorName)

        // Creditor Account
        val cdtrAcct = doc.createElementNS(NAMESPACE,"CdtrAcct")
        cdtTrfTxInf.appendChild(cdtrAcct)
        val cdtrAcctId = doc.createElementNS(NAMESPACE,"Id")
        cdtrAcct.appendChild(cdtrAcctId)
        addElement(doc, cdtrAcctId, "IBAN", txInfo.creditorIban)

        // Remittance Information
        val rmtInf = doc.createElementNS(NAMESPACE,"RmtInf")
        cdtTrfTxInf.appendChild(rmtInf)
        addElement(doc, rmtInf, "Ustrd", txInfo.remittanceInfo)
    }

    private fun addElement(doc: Document, parent: Element, name: String, value: String) {
        val element = doc.createElementNS(NAMESPACE,name)
        element.textContent = value
        parent.appendChild(element)
    }

    private fun serializeDocument(doc: Document): ByteArray {
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no")

        val outputStream = ByteArrayOutputStream()
        transformer.transform(DOMSource(doc), StreamResult(outputStream))
        return outputStream.toByteArray()
    }

    /**
     * Transaction information for a single credit transfer.
     */
    data class TransactionInfo(
        val creditorName: String,
        val creditorIban: String,
        val creditorBic: String?,
        val amount: BigDecimal,
        val remittanceInfo: String
    )
}
