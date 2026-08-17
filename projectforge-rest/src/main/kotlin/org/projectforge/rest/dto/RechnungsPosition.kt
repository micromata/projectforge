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

import org.projectforge.business.fibu.*
import org.projectforge.business.fibu.kost.KostZuweisungDO
import java.math.BigDecimal
import java.time.LocalDate

/**
 * One position of an outgoing invoice, as the edit form of `/next/invoice` sends and receives it.
 *
 * A position is never edited on its own — it has no `AbstractPagesRest` and no url. It travels as part of
 * [Rechnung.positionen], and [number] is what identifies it there: `RechnungsPositionDO` carries
 * `@OrderColumn(name = "number")` with `@ListIndexBase(1)` and a unique constraint on
 * (rechnung_fk, number), so a position the client sends without a number would be written as a new row.
 *
 * Deleted positions stay in the list with `deleted = true`. `RechnungDO.positionen` carries
 * `autoUpdateCollectionEntries = true` but no `@SoftDeleteCollection` (only `EingangsrechnungDO.positionen`
 * has that), so a position missing from the posted collection is deleted **physically**, together with its
 * history — and its number is then free again, which the unique constraint would collide with.
 */
class RechnungsPosition(
    /**
     * Position number inside the invoice, 1-based and stable. Assigned by the backend for new positions
     * (see `OutgoingInvoiceEntityRest.transformForDB`); an existing position is never renumbered.
     */
    var number: Short = 0,
    var text: String? = null,
    override var menge: BigDecimal? = null,
    override var einzelNetto: BigDecimal? = null,
    override var vat: BigDecimal? = null,
    /** The order position this one invoices, if any. Read-only for now — there is no picker for it yet. */
    var auftragsPosition: OrderPositionRef? = null,
    var periodOfPerformanceType: PeriodOfPerformanceType? = PeriodOfPerformanceType.SEEABOVE,
    var periodOfPerformanceBegin: LocalDate? = null,
    var periodOfPerformanceEnd: LocalDate? = null,
) : BaseDTO<RechnungsPositionDO>(), IRechnungsPosition {

    var kostZuweisungen: MutableList<KostZuweisung>? = null

    /**
     * The sums [RechnungCalculator] computes for this position — net, VAT amount, gross, the net sum of the
     * cost assignments and the difference between the two.
     *
     * Read-only: they follow from `menge`, `einzelNetto`, `vat` and the assignments, and how a position is
     * rounded before it enters a sum is the calculator's rule (`roundPositionsBeforeSum`). The form asks
     * `POST /rs/outgoingInvoice/recalculate` for them while it is being edited. Filled by [assignSums].
     */
    var netSum: BigDecimal? = null
    var vatAmountSum: BigDecimal? = null
    var grossSum: BigDecimal? = null
    var kostZuweisungNetSum: BigDecimal? = null

    /**
     * How much of this position's net sum is not assigned to a cost unit yet
     * ([RechnungPosInfo.kostZuweisungNetFehlbetrag]), which the form shows in red as Wicket does.
     *
     * A hint only: `RechnungDao` performs no validation of the cost assignment sums, so an invoice with a
     * difference saves fine.
     */
    var kostZuweisungNetFehlbetrag: BigDecimal? = null

    /**
     * The order position an invoice position points at, as a link needs it: enough to show
     * "1234.5" and to navigate to the order.
     */
    class OrderPositionRef(
        var id: Long? = null,
        /** Id of the order the position belongs to — the link's target. */
        var auftragId: Long? = null,
        var auftragNummer: Int? = null,
        var number: Short? = null,
    )

    /**
     * @see copyFrom
     */
    constructor(src: RechnungsPositionDO) : this() {
        copyFrom(src)
    }

    override fun copyFrom(src: RechnungsPositionDO) {
        super.copyFrom(src)
        // Set by hand: BaseDTO.copy maps a *DO -> *DTO relation only between fields of the same type
        // family, and OrderPositionRef is no BaseDTO (it carries the order's number, which the position
        // itself doesn't know).
        auftragsPosition = src.auftragsPosition?.let { position ->
            val orderInfo = AuftragsCache.instance.getOrderPositionInfo(position.id)
            OrderPositionRef(
                id = position.id,
                auftragId = orderInfo?.auftragId,
                auftragNummer = orderInfo?.auftragNummer,
                number = orderInfo?.number,
            )
        }
        kostZuweisungen = src.kostZuweisungen?.map { zuweisung ->
            KostZuweisung().also { it.copyFrom(zuweisung) }
        }?.toMutableList()
    }

    /**
     * Fills the read-only sums from the [RechnungPosInfo] the invoice's [RechnungInfo] holds for this
     * position, called by [Rechnung.copyFromWithCollections].
     *
     * Not read from `RechnungsPositionDO.info` in [copyFrom]: that is a `lateinit` whose
     * `isInfoInitialized` guard is `internal` to projectforge-business, so a position nobody calculated
     * would throw here. The invoice's info is the one place that knows whether the sums exist at all.
     */
    fun assignSums(posInfo: RechnungPosInfo) {
        netSum = posInfo.netSum
        vatAmountSum = posInfo.vatAmount
        grossSum = posInfo.grossSum
        kostZuweisungNetSum = posInfo.kostZuweisungNetSum
        kostZuweisungNetFehlbetrag = posInfo.kostZuweisungNetFehlbetrag
    }

    /**
     * Rebuilds [RechnungsPositionDO.kostZuweisungen] instead of appending to it, and assigns the back
     * reference [RechnungsPositionDO.rechnung] — see [Rechnung.copyTo] for the whole story.
     *
     * Called by [Rechnung.copyTo], not by the framework. Deliberately not an override of the one-argument
     * `copyTo`: without the invoice a position cannot be written at all (`rechnung_fk` is `nullable = false`).
     */
    fun copyTo(dest: RechnungsPositionDO, invoice: RechnungDO) {
        copyTo(dest)
        dest.rechnung = invoice
        // By hand, because [OrderPositionRef] is no [BaseDTO] and [BaseDTO.copy] therefore skips it — and
        // skipping here would mean losing the reference on every save of a position that had one.
        dest.auftragsPosition = auftragsPosition?.id?.let { id ->
            AuftragsPositionDO().also { it.id = id }
        }
        dest.kostZuweisungen = kostZuweisungen?.map { dto ->
            KostZuweisungDO().also { dto.copyTo(it, dest) }
        }?.toMutableList()
    }
}
