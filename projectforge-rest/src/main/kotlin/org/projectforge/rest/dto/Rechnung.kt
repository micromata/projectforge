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

import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.*
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.Attachment
import java.math.BigDecimal
import java.time.LocalDate

class Rechnung(
    var nummer: Int? = null,
    var customer: Customer? = null,
    var kundeText: String? = null,
    var project: Project? = null,
    var status: RechnungStatus? = null,
    var typ: RechnungTyp? = null,
    var customerref1: String? = null,
    var attachment: String? = null,
    var customerAddress: String? = null,
    var periodOfPerformanceBegin: LocalDate? = null,
    var periodOfPerformanceEnd: LocalDate? = null,
    var datum: LocalDate? = null,
    var betreff: String? = null,
    var bemerkung: String? = null,
    var besonderheiten: String? = null,
    var faelligkeit: LocalDate? = null,
    var ueberfaellig: Boolean? = null,
    var zahlungsZielInTagen: Int? = null,
    var discountZahlungsZielInTagen: Int? = null,
    var bezahlDatum: LocalDate? = null,
    override val currency: String? = null,
    override var zahlBetrag: BigDecimal? = null,
    var konto: Konto? = null,
    var discountPercent: BigDecimal? = null,
    var discountMaturity: LocalDate? = null,
    override var attachmentsCounter: Int? = null,
    override var attachmentsSize: Long? = null,
    override var attachments: List<Attachment>? = null,
) : BaseDTO<RechnungDO>(), IRechnung, AttachmentsSupport {
    override var positionen: MutableList<RechnungsPosition>? = null

    var netSum: BigDecimal = BigDecimal.ZERO

    var vatAmountSum: BigDecimal = BigDecimal.ZERO

    var grossSum: BigDecimal = BigDecimal.ZERO

    var grossSumWithDiscount: BigDecimal = BigDecimal.ZERO

    var statusAsString: String? = null

    var kost1List: String? = null

    var kost1Info: String? = null

    var kost2List: String? = null

    var kost2Info: String? = null

    val isBezahlt: Boolean
        get() = if (this.netSum.compareTo(BigDecimal.ZERO) == 0) {
            true
        } else this.bezahlDatum != null && this.zahlBetrag != null

    override fun copyFrom(src: RechnungDO) {
        super.copyFrom(src)
        src.projekt?.let { p ->
            project = Project()
            project?.copyFromMinimal(p)
        }
        src.kunde?.let { c ->
            customer = Customer()
            customer?.copyFromMinimal(c)
        }
        this.netSum = src.info.netSum
        this.vatAmountSum = src.info.vatAmount
        this.grossSum = src.info.grossSum
        this.grossSumWithDiscount = src.info.grossSumWithDiscount
        ueberfaellig = src.info.isUeberfaellig
        src.status?.let {
            statusAsString = translate(it.i18nKey)
        }
    }

    /**
     * The lean row of the hand built next list: the columns of `invoice.page.tsx` and nothing else, so
     * `JsonInclude.Include.NON_NULL` keeps the rest off the wire (see [BaseDTO.copyFrom4ListRow]).
     *
     * What [copyFrom] would add and no column reads: the customer's and the project's whole entity behind
     * a cell showing one name, the address block the e-invoice needs (eight fields), `besonderheiten`,
     * `customerref1`, `attachment`, the discount fields and the two sums the list has no column for
     * (`vatAmountSum`, `grossSum`).
     *
     * The kost columns are filled here rather than in `OutgoingInvoiceEntityRest.transformFromDB`, which
     * this path does not run through - the row is built by [BaseDTO.copyFrom4ListRow] alone.
     *
     * Costs no query: [PfCaches.initializeWithoutPositions] answers the two relations and the [RechnungInfo]
     * from the caches, which the statistics of the same result set need anyway. Not the full
     * [PfCaches.initialize] - that one walks the lazy `positionen`, which is a query per row and no column
     * of this list reads a position entity.
     */
    override fun copyFrom4ListRow(src: RechnungDO) {
        PfCaches.instance.initializeWithoutPositions(src)
        id = src.id
        deleted = src.deleted
        // Two columns every next list offers, hidden until the user switches them on
        // (`lib/page-def/audit-columns.ts`).
        copyAuditFieldsFrom(src)
        nummer = src.nummer
        // The name only, and the free text as the fallback of an invoice naming no customer of the list -
        // the same fallback `KundeFormatter` makes for the Wicket list.
        customer = Customer(displayName = src.kunde?.displayName ?: src.kundeText)
        project = src.projekt?.let { Project(displayName = it.displayName) }
        // The account of the invoice itself, not the one inherited from customer or project: that is what
        // the Wicket list's column shows too (`RechnungDO.konto`), while `KontoCache.getKonto(invoice)`
        // falls back through the project - a fallback the export uses and the column doesn't.
        konto = PfCaches.instance.getKontoIfNotInitialized(src.konto)?.let { account ->
            // The name only ("11400 - Debitoren"), which is the whole cell - `displayName` is a computed
            // getter of KontoDO, so it is not in the constructor.
            Konto().also { it.displayName = account.displayName }
        }
        betreff = src.betreff
        bemerkung = src.bemerkung
        status = src.status
        typ = src.typ
        datum = src.datum
        faelligkeit = src.faelligkeit
        bezahlDatum = src.bezahlDatum
        zahlBetrag = src.zahlBetrag
        periodOfPerformanceBegin = src.periodOfPerformanceBegin
        periodOfPerformanceEnd = src.periodOfPerformanceEnd
        attachmentsCounter = src.attachmentsCounter
        attachmentsSize = src.attachmentsSize
        val info = src.ensuredInfo
        netSum = info.netSum
        grossSumWithDiscount = info.grossSumWithDiscount
        // Both are row colours rather than columns: overdue reads red, unpaid blue (see invoice.page.tsx).
        ueberfaellig = info.isUeberfaellig
        src.status?.let { statusAsString = translate(it.i18nKey) }
        val kost1Sorted = info.sortedKost1
        kost1List = RechnungInfo.numbersAsString(kost1Sorted)
        kost1Info = RechnungInfo.detailsAsString(kost1Sorted)
        val kost2Sorted = info.sortedKost2
        kost2List = RechnungInfo.numbersAsString(kost2Sorted)
        kost2Info = RechnungInfo.detailsAsString(kost2Sorted)
    }

    fun copyPositionenFrom(src: RechnungDO) {
        val list = positionen ?: mutableListOf()
        src.positionen?.forEach {
            list.add(RechnungsPosition(it))
        }
        src.projekt?.let {
            project = Project()
            project?.copyFromMinimal(it)
        }
        kundeText = src.kundeAsString
        src.konto?.let {
            konto = Konto()
            konto?.copyFromMinimal(it)
        }
        positionen = list
    }

    override fun copyTo(dest: RechnungDO) {
        super.copyTo(dest)
        val list = dest.positionen ?: mutableListOf()
        positionen?.forEach {
            val pos = RechnungsPositionDO()
            it.copyTo(pos)
            list.add(pos)
        }
        dest.positionen = list
    }
}
