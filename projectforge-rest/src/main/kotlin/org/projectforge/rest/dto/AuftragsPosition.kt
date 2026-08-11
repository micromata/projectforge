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

package org.projectforge.rest.dto

import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragForecastType
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.AuftragsPositionsArt
import org.projectforge.business.fibu.AuftragsPositionsPaymentType
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.business.fibu.ModeOfPaymentType
import org.projectforge.business.fibu.PeriodOfPerformanceType
import org.projectforge.business.fibu.RechnungCache
import org.projectforge.business.fibu.RechnungDao
import java.math.BigDecimal
import java.time.LocalDate

/**
 * One position of an order, as the edit form of `/next/order` sends and receives it.
 *
 * A position is never edited on its own — it has no `AbstractPagesRest` and no url. It travels as part
 * of [Auftrag.positionen], and [number] is what identifies it there: the collection handler of the
 * persistence layer matches a posted position against the database row by `number` (see
 * [AuftragsPositionDO.equals]), not only by [id]. A position the client sends without a number would
 * therefore be written as a new row.
 *
 * Deleted positions stay in the list with `deleted = true`. `AuftragDO.positionen` carries
 * `autoUpdateCollectionEntries = true` but no `@SoftDeleteCollection`, so a position missing from the
 * posted collection is deleted **physically**, together with its history — and its number is then free
 * again, which the unique constraint on (auftrag_fk, number) would collide with.
 */
class AuftragsPosition(
    /**
     * Position number inside the order, 1-based and stable. Assigned by the backend for new positions
     * (see `AuftragPagesRest.transformForDB`); an existing position is never renumbered.
     */
    var number: Short = 0,
    var titel: String? = null,
    var art: AuftragsPositionsArt? = null,
    var paymentType: AuftragsPositionsPaymentType? = null,
    var forecastType: AuftragForecastType? = null,
    var status: AuftragsStatus? = null,
    var nettoSumme: BigDecimal? = null,
    var personDays: BigDecimal? = null,
    var bemerkung: String? = null,
    /**
     * Only writable with `FIBU_AUSGANGSRECHNUNGEN = READWRITE` and only for finished positions; the
     * `AuftragRight` of the DAO is the authority (see [Auftrag.vollstaendigFakturiertWriteAccess]).
     */
    var vollstaendigFakturiert: Boolean? = false,
    var periodOfPerformanceType: PeriodOfPerformanceType? = PeriodOfPerformanceType.SEEABOVE,
    var periodOfPerformanceBegin: LocalDate? = null,
    var periodOfPerformanceEnd: LocalDate? = null,
    var modeOfPaymentType: ModeOfPaymentType? = null,
    var task: Task? = null,
) : BaseDTO<AuftragsPositionDO>() {

    /**
     * Net sum already invoiced for this position, from [RechnungCache]. Read-only: it is the sum of the
     * invoice positions pointing here, which this form cannot change.
     */
    var invoicedSum: BigDecimal? = null

    /**
     * Net sum still to be invoiced, as [org.projectforge.business.fibu.OrderPositionInfo.notYetInvoiced]
     * calculates it. Read-only.
     */
    var notInvoicedSum: BigDecimal? = null

    /**
     * The invoices this position was invoiced with, one entry per invoice (an invoice may hold several
     * positions pointing here, their net sums summed up). Read-only, shown as links.
     */
    var invoices: List<InvoiceRef>? = null

    /**
     * True if an invoice position points at this position. The frontend then hides the delete button, as
     * Wicket does (`AuftragEditForm.positionInInvoiceExists`): deleting the position would leave the
     * invoice pointing at nothing.
     */
    var invoicedElsewhere: Boolean = false

    /**
     * An invoice this position was (partly) invoiced with. Enough to render a link and its tooltip.
     */
    class InvoiceRef(
        var id: Long? = null,
        /**
         * The invoice number, i.e. what the link shows. Null for a planned invoice without a number,
         * which is why such invoices are left out.
         */
        var nummer: Int? = null,
        var date: LocalDate? = null,
        /** Net sum of this invoice's positions pointing at the order position. */
        var netSum: BigDecimal? = null,
    )

    override fun copyFrom(src: AuftragsPositionDO) {
        super.copyFrom(src)
        task = src.task?.let { Task(it) }
        // The invoiced sums and links come from the cache, not from the position itself: an order
        // position doesn't know its invoices, the invoice positions point at it.
        val invoicePositions = RechnungCache.instance.getRechnungsPosInfosByAuftragsPositionId(src.id)
        invoicedElsewhere = !invoicePositions.isNullOrEmpty()
        invoicedSum = RechnungDao.getNettoSumme(invoicePositions)
        invoices = invoicePositions
            ?.filter { it.rechnungInfo?.nummer != null }
            // One entry per invoice, so an invoice holding two positions of this order position is
            // listed once with their sum — same as Wicket's InvoicePositionsPanel does.
            ?.groupBy { it.rechnungInfo!!.id }
            ?.map { (_, positions) ->
                val info = positions.first().rechnungInfo!!
                InvoiceRef(
                    id = info.id,
                    nummer = info.nummer,
                    date = info.date,
                    netSum = positions.sumOf { it.netSum },
                )
            }
            ?.sortedBy { it.nummer }
    }

    /**
     * Also assigns the back reference [AuftragsPositionDO.auftrag], because the collection handler
     * matches by (number, auftrag.id) — a position without it would look removed and be deleted.
     * Called by [Auftrag.copyTo], not by the framework.
     */
    fun copyTo(dest: AuftragsPositionDO, order: AuftragDO) {
        copyTo(dest)
        dest.auftrag = order
    }
}
