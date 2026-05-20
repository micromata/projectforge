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

import java.math.BigDecimal

data class EInvoiceData(
    val invoiceNumber: String? = null,
    val issueDate: String? = null,
    val dueDate: String? = null,
    val currency: String? = null,
    val documentTypeCode: String? = null,
    val buyerReference: String? = null,
    val orderReference: String? = null,
    val paymentTerms: String? = null,
    val deliveryDate: String? = null,
    val seller: EInvoiceParty? = null,
    val buyer: EInvoiceParty? = null,
    val totalNetAmount: BigDecimal? = null,
    val totalGrossAmount: BigDecimal? = null,
    val totalTaxAmount: BigDecimal? = null,
    val amountDue: BigDecimal? = null,
    val lineItems: List<EInvoiceLineItem> = emptyList(),
    val attachments: List<EInvoiceAttachment> = emptyList(),
    val validationErrors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val format: String? = null,
    val profile: String? = null,
)

data class EInvoiceParty(
    val name: String? = null,
    val street: String? = null,
    val zip: String? = null,
    val city: String? = null,
    val country: String? = null,
    val vatId: String? = null,
    val email: String? = null,
    val contactName: String? = null,
    val iban: String? = null,
    val bic: String? = null,
)

data class EInvoiceLineItem(
    val id: Int = 0,
    val position: Int,
    val description: String? = null,
    val quantity: BigDecimal? = null,
    val unit: String? = null,
    val unitPrice: BigDecimal? = null,
    val netAmount: BigDecimal? = null,
    val vatPercent: BigDecimal? = null,
)

data class EInvoiceAttachment(
    val id: Int = 0,
    val filename: String,
    val mimeType: String? = null,
    val description: String? = null,
    val size: Int = 0,
    val index: Int = 0,
)
