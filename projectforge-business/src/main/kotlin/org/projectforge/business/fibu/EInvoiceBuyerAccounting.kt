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

import org.mustangproject.Invoice
import org.mustangproject.ZUGFeRD.IExportableTransaction
import org.mustangproject.ZUGFeRD.IXMLProvider
import org.mustangproject.XMLTools
import org.mustangproject.ZUGFeRD.ZUGFeRD2PullProvider
import org.mustangproject.ZUGFeRD.ZUGFeRDExporterFromA3

/**
 * An [Invoice] that knows the buyer accounting reference (BT-19, e.g. the cost center). Mustang
 * supports it on the level of the invoice lines only (BT-133), which is not what a customer asks for.
 */
internal class EInvoiceTransaction : Invoice() {
    var buyerAccountingReference: String? = null
}

/**
 * Writes the buyer accounting reference of an [EInvoiceTransaction] to the settlement header of the CII
 * XML (`ReceivableSpecifiedTradeAccountingAccount`, BT-19), after the XML Mustang generates.
 */
internal class EInvoiceXmlProvider : ZUGFeRD2PullProvider() {
    override fun generateXML(trans: IExportableTransaction) {
        super.generateXML(trans)
        val reference = (trans as? EInvoiceTransaction)?.buyerAccountingReference?.trim()
        if (reference.isNullOrEmpty()) {
            return
        }
        val xml = String(zugferdData, Charsets.UTF_8)
        val closing = "</ram:ApplicableHeaderTradeSettlement>"
        val index = xml.lastIndexOf(closing)
        if (index < 0) {
            return
        }
        // Last child of the settlement header, after the monetary summation and the referenced invoices.
        val account = "<ram:ReceivableSpecifiedTradeAccountingAccount><ram:ID>${XMLTools.encodeXML(reference)}" +
                "</ram:ID></ram:ReceivableSpecifiedTradeAccountingAccount>"
        zugferdData = (xml.substring(0, index) + account + xml.substring(index)).toByteArray(Charsets.UTF_8)
    }
}

/** The ZUGFeRD exporter with [EInvoiceXmlProvider] (Mustang offers no public way to set a provider). */
internal class EInvoiceZugferdExporter : ZUGFeRDExporterFromA3() {
    /** Replaces the provider; call after [setZUGFeRDVersion], which installs Mustang's own. */
    fun useProvider(provider: IXMLProvider): EInvoiceZugferdExporter {
        setXMLProvider(provider)
        return this
    }
}
