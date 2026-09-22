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

import org.projectforge.business.fibu.EingangsrechnungsPositionDO
import org.projectforge.business.fibu.IRechnungsPosition
import org.projectforge.business.fibu.RechnungPosInfo
import org.projectforge.business.fibu.kost.KostZuweisungDO
import java.math.BigDecimal

/**
 * One position of an incoming invoice, as the edit form of `/next/creditor-invoice` sends and receives it.
 *
 * The incoming sibling of [RechnungsPosition], trimmed: it has no order position and no period of
 * performance — an incoming invoice states neither. [number] is what identifies the position inside its
 * invoice, `EingangsrechnungsPositionDO` carrying the same `@OrderColumn(name = "number")` with
 * `@ListIndexBase(1)` and the unique constraint on (eingangsrechnung_fk, number).
 *
 * Deleted positions stay in the list with `deleted = true`; unlike the outgoing invoice,
 * `EingangsrechnungDO.positionen` carries `@SoftDeleteCollection`, so a position missing from the posted
 * collection is soft deleted rather than physically removed.
 */
class EingangsrechnungsPosition(
    /**
     * Position number inside the invoice, 1-based and stable. Assigned by the backend for new positions
     * (see `IncomingInvoiceEntityRest.transformForDB`); an existing position is never renumbered.
     */
    var number: Short = 0,
    var text: String? = null,
    override var menge: BigDecimal? = null,
    override var einzelNetto: BigDecimal? = null,
    override var vat: BigDecimal? = null,
) : BaseDTO<EingangsrechnungsPositionDO>(), IRechnungsPosition {
    var kostZuweisungen: MutableList<KostZuweisung>? = null

    /**
     * The sums [org.projectforge.business.fibu.RechnungCalculator] computes for this position — net, VAT
     * amount, gross, the net sum of the cost assignments and the difference between the two.
     *
     * Read-only, filled by [assignSums]; the form asks `POST /rs/incomingInvoice/recalculate` for them
     * while it is being edited. See [RechnungsPosition] for why they are not read from the DO's `info`.
     */
    var netSum: BigDecimal? = null
    var vatAmountSum: BigDecimal? = null
    var grossSum: BigDecimal? = null
    var kostZuweisungNetSum: BigDecimal? = null

    /**
     * How much of this position's net sum is not assigned to a cost unit yet, which the form shows in red
     * as Wicket does. A hint only: `EingangsrechnungDao` performs no validation of the cost assignment
     * sums, so an invoice with a difference saves fine.
     */
    var kostZuweisungNetFehlbetrag: BigDecimal? = null

    override fun copyFrom(src: EingangsrechnungsPositionDO) {
        super.copyFrom(src)
        kostZuweisungen = src.kostZuweisungen?.map { zuweisung ->
            KostZuweisung().also { it.copyFrom(zuweisung) }
        }?.toMutableList()
    }

    /**
     * Fills the read-only sums from the [RechnungPosInfo] the invoice's `RechnungInfo` holds for this
     * position, called by [Eingangsrechnung.copyFromWithCollections]. See [RechnungsPosition.assignSums].
     */
    fun assignSums(posInfo: RechnungPosInfo) {
        netSum = posInfo.netSum
        vatAmountSum = posInfo.vatAmount
        grossSum = posInfo.grossSum
        kostZuweisungNetSum = posInfo.kostZuweisungNetSum
        kostZuweisungNetFehlbetrag = posInfo.kostZuweisungNetFehlbetrag
    }

    /**
     * Rebuilds [EingangsrechnungsPositionDO.kostZuweisungen] instead of appending to it, and assigns the
     * back reference — see [Eingangsrechnung.copyTo] for the whole story.
     *
     * Called by [Eingangsrechnung.copyTo], not by the framework. Deliberately not an override of the
     * one-argument `copyTo`: without the invoice a position cannot be written at all
     * (`eingangsrechnung_fk` is `nullable = false`).
     */
    fun copyTo(dest: EingangsrechnungsPositionDO, invoice: org.projectforge.business.fibu.EingangsrechnungDO) {
        copyTo(dest)
        dest.eingangsrechnung = invoice
        dest.kostZuweisungen = kostZuweisungen?.map { dto ->
            KostZuweisungDO().also { dto.copyTo(it, dest) }
        }?.toMutableList()
    }
}
