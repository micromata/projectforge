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

import mu.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Excel export service for outgoing invoices (Rechnung).
 * Uses de.micromata.merlin.excel for Excel generation.
 *
 * @author Kai Reinhard
 */
@Service
open class InvoiceExcelExporter {

    /**
     * Exports a list of outgoing invoices to Excel format.
     *
     * @param invoices List of invoices to export
     * @return ByteArray containing the Excel file, or null if export fails
     */
    fun exportInvoices(invoices: List<RechnungDO>): ByteArray? {
        log.info { "Export of ${invoices.size} outgoing invoices requested" }

        // TODO: Implement Excel export using de.micromata.merlin.excel
        // - Create ExcelWorkbook
        // - Add sheet with headers
        // - Add data rows with invoice details
        // - Return workbook.asByteArray

        log.warn { "InvoiceExcelExporter.exportInvoices() is not yet implemented" }
        return null
    }

    /**
     * Exports a list of outgoing invoices with cost assignments (Kostzuweisungen) to Excel format.
     * This is a more detailed export that includes cost center (Kost1) and cost type (Kost2) information.
     *
     * @param invoices List of invoices to export
     * @param sheetTitle Title for the Excel sheet
     * @return ByteArray containing the Excel file, or null if export fails
     */
    fun exportInvoicesWithCostAssignments(invoices: List<RechnungDO>, sheetTitle: String): ByteArray? {
        log.info { "Export of ${invoices.size} outgoing invoices with cost assignments requested" }

        // TODO: Implement Excel export with cost assignments using de.micromata.merlin.excel
        // - Create ExcelWorkbook
        // - Add sheet with extended headers including Kost1/Kost2
        // - Add data rows with invoice details and cost assignment information
        // - Include cost center and cost type breakdowns
        // - Return workbook.asByteArray

        log.warn { "InvoiceExcelExporter.exportInvoicesWithCostAssignments() is not yet implemented" }
        return null
    }
}
