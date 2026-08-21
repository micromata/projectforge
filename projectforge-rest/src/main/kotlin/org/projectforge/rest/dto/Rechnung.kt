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
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.Attachment
import java.math.BigDecimal
import java.time.LocalDate

class Rechnung(
    var nummer: Int? = null,
    var customer: Customer? = null,
    /**
     * Free text customer, for customers not in the list. Nulled by the backend if [customer] is given (as
     * `RechnungEditPage.onSaveOrUpdate` does), so a customer chosen from the list always wins.
     */
    var kundeText: String? = null,
    var project: Project? = null,
    var status: RechnungStatus? = null,
    var typ: RechnungTyp? = null,
    var customerref1: String? = null,
    var attachment: String? = null,
    var customerContactPerson: String? = null,
    var customerAddress: String? = null,
    var customerZipCode: String? = null,
    var customerCity: String? = null,
    var customerCountry: String? = null,
    var customerVatId: String? = null,
    var customerLeitwegId: String? = null,
    var customerEInvoiceEmail: String? = null,
    /** IBAN of the seller's bank account, one of `EInvoiceSellerConfig.bankAccounts`. */
    var sellerBankAccount: String? = null,
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
    // `var`, not `val`: a form has to be able to send it back, and a read only property is silently
    // dropped by Jackson on the way in.
    override var currency: String? = null,
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

    /**
     * The orders the positions of this invoice bill, each order once and without its positions — the
     * "Aufträge" column of the next list, where every entry is a link to the order.
     *
     * Only on the list row (see [copyFrom4ListRow]); the edit form carries the reference per position
     * ([RechnungsPosition.auftragsPosition]), which names the position as well. Costs no query: the
     * positions come from the cached [RechnungInfo] and the order numbers from [AuftragsCache], which is
     * the same route Wicket's `OrderPositionsPanel` column takes.
     */
    var orders: List<OrderRef>? = null

    /** An order as a link to it needs it: the id to navigate to and the number to show. */
    class OrderRef(
        var id: Long? = null,
        var nummer: Int? = null,
    )

    /**
     * The net sum of all cost assignments of all positions, and how much of [netSum] is not assigned to a
     * cost unit yet ([RechnungInfo.kostZuweisungenFehlbetrag]). Read-only, and a hint only: `RechnungDao`
     * performs no validation of the cost assignment sums, so an invoice with a difference saves fine.
     *
     * The difference is on the list row as well (see [copyFrom4ListRow]), as the column that replaced
     * Wicket's `showKostZuweisungStatus` switch; the net sum is not, no list has a column for it.
     */
    var kostZuweisungenNetSum: BigDecimal? = null
    var kostZuweisungenFehlbetrag: BigDecimal? = null

    /**
     * Access flags, so the hand built next form knows what to offer. The `UILayout.UserAccess` the legacy
     * frontends use doesn't reach it: `GET /rs/outgoingInvoice/{id}` passes no user access, and the next
     * pages read none. The DAO stays the authority in every case — these only decide what is shown.
     */
    var writeAccess: Boolean = false
    var deleteAccess: Boolean = false

    /**
     * Whether cost accounting is configured at all ([org.projectforge.business.configuration.ConfigurationServiceImpl]
     * / `Configuration.isCostConfigured`). The form hides the cost assignments of a position when it is
     * false, as `AbstractRechnungEditForm` hides the whole table then.
     *
     * On the DTO rather than behind a config endpoint of its own: it is one boolean the edit page needs and
     * nothing else reads.
     */
    var costConfigured: Boolean = false

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
        // ensuredInfo, not info: the latter is a lateinit that throws for an invoice nobody calculated yet,
        // which is every invoice the recalculate endpoint and `newBaseDTO` build.
        val info = src.ensuredInfo
        this.netSum = info.netSum
        this.vatAmountSum = info.vatAmount
        this.grossSum = info.grossSum
        this.grossSumWithDiscount = info.grossSumWithDiscount
        this.kostZuweisungenNetSum = info.kostZuweisungenNetSum
        this.kostZuweisungenFehlbetrag = info.kostZuweisungenFehlbetrag
        ueberfaellig = info.isUeberfaellig
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
        // What Wicket's `showKostZuweisungStatus` switch appended to the first cell, as a column of its own
        // (`invoice.page.tsx`). Only where cost accounting is configured at all - otherwise no invoice has a
        // single assignment, and `JsonInclude.NON_NULL` keeps a column of "0.00" off the wire entirely, as
        // `AbstractRechnungListForm` hides the switch there.
        if (Configuration.instance.isCostConfigured) {
            kostZuweisungenFehlbetrag = info.kostZuweisungenFehlbetrag
        }
        src.status?.let { statusAsString = translate(it.i18nKey) }
        // The orders behind the positions, each one once and by number - what the Wicket list shows as
        // "Aufträge", without the positions. Null rather than an empty list for an invoice billing none,
        // so `JsonInclude.NON_NULL` keeps the column off the wire for it.
        orders = info.positions?.mapNotNull { posInfo ->
            AuftragsCache.instance.getOrderPositionInfo(posInfo.auftragsPositionId)
        }?.distinctBy { it.auftragId }
            ?.sortedBy { it.auftragNummer }
            ?.map { OrderRef(id = it.auftragId, nummer = it.auftragNummer) }
            ?.ifEmpty { null }
        val kost1Sorted = info.sortedKost1
        kost1List = RechnungInfo.numbersAsString(kost1Sorted)
        kost1Info = RechnungInfo.detailsAsString(kost1Sorted)
        val kost2Sorted = info.sortedKost2
        kost2List = RechnungInfo.numbersAsString(kost2Sorted)
        kost2Info = RechnungInfo.detailsAsString(kost2Sorted)
    }

    /**
     * [copyFrom] plus the positions with their cost assignments, for the edit page: it has to show every
     * row, and to send them all back on save.
     *
     * The deleted rows travel too. Neither `RechnungDO.positionen` nor `RechnungsPositionDO.kostZuweisungen`
     * carries `@SoftDeleteCollection` (only `EingangsrechnungDO.positionen` does), so the collection handler
     * physically removes — history and all — whatever a posted collection leaves out. See [RechnungsPosition].
     *
     * Replaces the former `copyPositionenFrom`, which appended to [positionen] instead of building it and
     * overwrote [kundeText] with `src.kundeAsString` — the merged display string, which the next save then
     * wrote into the raw column. That belongs to [copyFrom4ListRow], which puts it into
     * `customer.displayName` where it is read.
     */
    fun copyFromWithCollections(src: RechnungDO) {
        // First, so the RechnungInfo and with it every position's RechnungPosInfo exists (copyFrom goes
        // through ensuredInfo).
        copyFrom(src)
        positionen = src.positionen?.map { position ->
            RechnungsPosition().also { it.copyFrom(position) }
        }?.toMutableList()
        // Matched by number, not by id: a position of an unsaved invoice has none, and number is the key of
        // a position inside its invoice anyway. RechnungCalculator builds no info for a deleted position,
        // whose sums the form doesn't show either.
        val positionInfos = src.ensuredInfo.positions
        positionen?.forEach { position ->
            positionInfos?.find { it.number == position.number }?.let { position.assignSums(it) }
        }
    }

    /**
     * Rebuilds [RechnungDO.positionen] instead of appending to it: the destination is a fresh [RechnungDO]
     * per request, and appending would duplicate every row of an invoice that already carries positions.
     *
     * Each position keeps its `id`, `number` and `deleted` flag and gets the back reference to [dest] — that
     * pair is what `CollectionHandler` matches a posted row against its database row by, and `rechnung_fk` is
     * `nullable = false` anyway. [RechnungsPosition.copyTo] does the same for the cost assignments.
     *
     * The two relations are set by hand as well: [BaseDTO.copy] maps a `*DTO -> *DO` relation by id, but only
     * between fields of the same name, and these are named `customer`/`project` here and `kunde`/`projekt`
     * there. [konto] needs no such line — same name on both sides.
     */
    override fun copyTo(dest: RechnungDO) {
        super.copyTo(dest)
        dest.kunde = customer?.id?.let { id -> KundeDO().also { it.id = id } }
        dest.projekt = project?.id?.let { id -> ProjektDO().also { it.id = id } }
        dest.positionen = positionen?.map { dto ->
            RechnungsPositionDO().also { dto.copyTo(it, dest) }
        }?.toMutableList()
    }

    companion object {
        /**
         * The calculated sums of an invoice, computed from its own (possibly unsaved) positions without
         * touching the caches — [RechnungCalculator] would otherwise look a [RechnungPosInfo] up by a
         * position id the posted rows of an edit form do not have yet.
         *
         * The one code path the recalculate endpoint of `OutgoingInvoiceEntityRest` and a fresh DTO share,
         * as [Auftrag.calculateOrderInfo] is for orders.
         */
        fun calculateInvoiceInfo(src: RechnungDO): RechnungInfo {
            return RechnungCalculator.calculate(src, useCaches = false)
        }
    }
}
