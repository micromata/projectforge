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
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragForecastType
import org.projectforge.business.fibu.AuftragsCache
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.OrderInfo
import org.projectforge.business.fibu.OrderPositionInfo
import org.projectforge.business.fibu.PaymentScheduleDO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.utils.NumberFormatter
import java.math.BigDecimal
import java.time.LocalDate

class Auftrag(
    var nummer: Int? = null,
    var customer: Customer? = Customer(),
    var project: Project? = Project(),
    /**
     * Free text customer, for customers not in the list. Nulled by the backend if [customer] is given
     * (as `AuftragEditPage.onSaveOrUpdate` does), so a customer chosen from the list always wins.
     */
    var kundeText: String? = null,
    var titel: String? = null,
    var positionen: MutableList<AuftragsPosition>? = null,
    var paymentSchedules: MutableList<PaymentSchedule>? = null,
    var personDays: BigDecimal? = null,
    var referenz: String? = null,
    var assignedPersons: String? = null,
    var contactPerson: User? = null,
    var projectManager: User? = null,
    var headOfBusinessManager: User? = null,
    var salesManager: User? = null,
    var angebotsDatum: LocalDate? = null,
    var erfassungsDatum: LocalDate? = null,
    var entscheidungsDatum: LocalDate? = null,
    var bindungsFrist: LocalDate? = null,
    var beauftragungsDatum: LocalDate? = null,
    var beauftragungsBeschreibung: String? = null,
    var bemerkung: String? = null,
    var statusBeschreibung: String? = null,
    var forecastType: AuftragForecastType? = null,
    var nettoSumme: BigDecimal? = null,
    var beauftragtNettoSumme: BigDecimal? = null,
    var fakturiertSum: BigDecimal? = null,
    var zuFakturierenSum: BigDecimal? = null,
    /** True when at least one position or payment schedule is due to be invoiced — used for list row highlighting. */
    var toBeInvoiced: Boolean? = null,
    var periodOfPerformanceBegin: LocalDate? = null,
    var periodOfPerformanceEnd: LocalDate? = null,
    var probabilityOfOccurrence: Int? = null,
    var status: AuftragsStatus? = null,
    override var attachmentsCounter: Int? = null,
    override var attachmentsSize: Long? = null,
    override var attachments: List<Attachment>? = null,
) : BaseDTO<AuftragDO>(), AttachmentsSupport {
    var pos: String? = null

    var formattedNettoSumme: String? = null
    var formattedBeauftragtNettoSumme: String? = null
    var formattedFakturiertSum: String? = null
    var formattedZuFakturierenSum: String? = null

    /**
     * Whether the e-mail notification to the contact person should be sent on save. Not a persisted
     * field — the checkbox of the edit form, which `OrderEntityRest.onAfterSaveOrUpdate` reads.
     */
    var sendEMailNotification: Boolean = false

    /**
     * Access flags, so the hand built next form knows what to offer. The `UILayout.UserAccess` the
     * legacy frontends use doesn't reach it: `GET /rs/order/{id}` passes no user access, and the next
     * pages read none. The DAO stays the authority in every case — these only decide what is shown.
     */
    var writeAccess: Boolean = false
    var deleteAccess: Boolean = false

    /**
     * Whether the `vollstaendigFakturiert` checkboxes of the positions and payment schedules are shown.
     * Mirrors Wicket, which shows them for `FIBU_AUSGANGSRECHNUNGEN = READWRITE`
     * (`AuftragEditForm`) while `AuftragRight` enforces FINANCE group membership on write — an
     * asymmetry kept on purpose, so the form doesn't offer a field the DAO would refuse.
     */
    var vollstaendigFakturiertWriteAccess: Boolean = false

    /**
     * Everything a list row shows, and nothing that costs a query.
     *
     * [AuftragDO.positionen] and [AuftragDO.paymentSchedules] are `FetchType.LAZY`, so reading either one
     * is a select per order — and each position pulls its task, and each task its parents. Over a list of
     * some 7000 orders that is what turned `initialList` into 26 MB in 99 seconds, while Wicket's list
     * stayed fast: it reads the same sums from [AuftragsCache], which preloads positions and schedules in
     * three bulk selects (see its `refresh`) and never walks a lazy collection.
     *
     * So the sums *and* the position count come from the cache here, and the two collections are only
     * mapped when the caller asks for them — the edit page does, via [copyFromWithCollections].
     *
     * The six `ManyToOne`s are lazy as well, and mapping them reads more than an id: [User.copyFromMinimal]
     * reads the user's name, [Customer]/[Project] their display name. So each one is another select per
     * row, and [PfCaches.initialize] replaces all six with their cached instances first — which is exactly
     * what `AuftragListPage` does for the customer and the project, under the comment "Avoid lazy loading".
     */
    override fun copyFrom(src: AuftragDO) {
        // Before super.copyFrom, which is what would otherwise touch the proxies. Mutates src, as the
        // Wicket list does too: it swaps a proxy for the identical cached object, nothing more.
        PfCaches.instance.initialize(src)
        super.copyFrom(src)
        // super.copyFrom covers the scalars and every *DO -> *DTO relation (contact person and the three
        // managers included, mapped by BaseDTO.copy via copyFromMinimal). Only the collections and the
        // calculated sums are left, both of which it skips by design.
        this.customer = src.kunde?.let {
            Customer(it)
        }
        this.project = src.projekt?.let {
            Project(it)
        }
        val orderInfo = orderInfo(src)
        personDays = orderInfo.personDays
        assignedPersons = src.assignedPersons
        nettoSumme = orderInfo.netSum
        beauftragtNettoSumme = orderInfo.commissionedNetSum
        fakturiertSum = orderInfo.invoicedSum
        zuFakturierenSum = orderInfo.notYetInvoicedSum
        toBeInvoiced = if (orderInfo.toBeInvoiced) true else null
        formattedNettoSumme = NumberFormatter.formatCurrency(orderInfo.netSum)
        formattedBeauftragtNettoSumme = NumberFormatter.formatCurrency(orderInfo.commissionedNetSum)
        formattedFakturiertSum = NumberFormatter.formatCurrency(orderInfo.invoicedSum)
        formattedZuFakturierenSum = NumberFormatter.formatCurrency(orderInfo.notYetInvoicedSum)
        // From the info rather than from the collection, which counting would mean loading. Filtered
        // explicitly: the cache holds the non-deleted positions only, but [calculateOrderInfo] builds an
        // info for every posted position, deleted ones included.
        pos = "#" + (orderInfo.infoPositions?.count { !it.deleted } ?: 0)
    }

    /**
     * The lean row of the hand built next list: only the 19 columns of `order.page.tsx`, everything else
     * left null so `JsonInclude.Include.NON_NULL` keeps it off the wire.
     *
     * Not a [copyFrom] with a trimmed [Auftrag]: the legacy AG-Grid columns bind to the very fields this
     * leaves out — `formattedNettoSumme` & co., and the nested `contactPerson`/`customer`/`project`. Hence
     * two fillings of one DTO rather than two DTOs.
     *
     * Measured over the order book of a real installation (7132 rows), 1755 B/row become 741 B/row. What
     * is left out and why:
     * - the four `formatted*` sums (~141 B/row): the client formats currency in the user's locale
     *   (`lib/format.ts`), and a string column would sort "900,00" after "1.100,00" anyway.
     * - [contactPerson] and the three managers (~524 B/row): the column shows the derived
     *   [assignedPersons] string, nothing of the users themselves.
     * - [customer] and [project] as objects (~257 B/row): only their `displayName` is a column, so the row
     *   carries a [Customer]/[Project] holding that name and nothing else.
     * - `bemerkung`, `statusBeschreibung`, `bindungsFrist`, `beauftragungs*`, `kundeText`, `forecastType`,
     *   `angebotsDatum`: read by no column of the list.
     *
     * [created] and [lastUpdate] travel, ~44 B/row: every next list offers them as columns (see
     * [copyFrom4ListRow]), and the order book shows `lastUpdate` from the start.
     *
     * The four boolean flags ([sendEMailNotification], [writeAccess], [deleteAccess],
     * [vollstaendigFakturiertWriteAccess]) still travel, ~107 B/row: they are non-null `Boolean`s, so
     * `NON_NULL` cannot drop them, and making them nullable for the list's sake would push the
     * false-vs-absent distinction into the edit form. Left as it is until it is worth that.
     *
     * Costs no query beyond [copyFrom]'s: [PfCaches.initialize] for the two relations, [AuftragsCache] for
     * the sums and the position count. See the KDoc of [copyFrom] for what walking the lazy collections
     * instead once cost.
     */
    override fun copyFrom4ListRow(src: AuftragDO) {
        PfCaches.instance.initialize(src)
        id = src.id
        deleted = src.deleted
        // `created` and `lastUpdate`: two columns every next list offers, hidden until the user switches
        // them on (see [copyFrom4ListRow] and `lib/page-def/audit-columns.ts`).
        copyAuditFieldsFrom(src)
        nummer = src.nummer
        titel = src.titel
        referenz = src.referenz
        status = src.status
        // The display name only — a Customer/Project carrying its id and name, not the whole entity. The
        // free text customer is the fallback of an order naming a customer that is not in the list, as
        // `KundeFormatter` does it for the Wicket list.
        customer = Customer(displayName = src.kunde?.displayName ?: src.kundeText)
        project = src.projekt?.let { Project(displayName = it.displayName) }
        erfassungsDatum = src.erfassungsDatum
        entscheidungsDatum = src.entscheidungsDatum
        periodOfPerformanceBegin = src.periodOfPerformanceBegin
        periodOfPerformanceEnd = src.periodOfPerformanceEnd
        probabilityOfOccurrence = src.probabilityOfOccurrence
        assignedPersons = src.assignedPersons
        attachmentsCounter = src.attachmentsCounter
        attachmentsSize = src.attachmentsSize
        val orderInfo = orderInfo(src)
        personDays = orderInfo.personDays
        nettoSumme = orderInfo.netSum
        beauftragtNettoSumme = orderInfo.commissionedNetSum
        fakturiertSum = orderInfo.invoicedSum
        zuFakturierenSum = orderInfo.notYetInvoicedSum
        toBeInvoiced = if (orderInfo.toBeInvoiced) true else null
        pos = "#" + (orderInfo.infoPositions?.count { !it.deleted } ?: 0)
    }

    /**
     * [copyFrom] plus the two collections, for the edit page: it has to show every row, and to send them
     * all back on save.
     *
     * The deleted rows travel too. `AuftragDO.positionen` has `autoUpdateCollectionEntries` but no
     * `@SoftDeleteCollection`, so the collection handler physically removes — history and all — whatever
     * a posted collection leaves out. See [AuftragsPosition].
     */
    fun copyFromWithCollections(src: AuftragDO) {
        copyFrom(src)
        positionen = src.positionen?.map { position ->
            AuftragsPosition().also { it.copyFrom(position) }
        }?.toMutableList()
        paymentSchedules = src.paymentSchedules?.map { schedule ->
            PaymentSchedule().also { it.copyFrom(schedule) }
        }?.toMutableList()
        // Matched by number, not by id: a snapshot's position infos may carry no id, and number is the
        // key of a position inside its order anyway.
        val positionInfos = orderInfo(src).infoPositions
        positionen?.forEach { position ->
            position.notInvoicedSum = positionInfos?.find { it.number == position.number }?.notYetInvoiced
        }
    }

    /**
     * Rebuilds the two collections instead of appending to them: the destination is a fresh [AuftragDO]
     * per request, and appending (as `Rechnung.copyTo` does) would duplicate every row of an order that
     * already carries positions.
     *
     * Each row keeps its `id`, `number` and `deleted` flag and gets the back reference to [dest] —
     * that pair is what `CollectionHandler` matches a posted row against its database row by.
     *
     * The two relations are set by hand as well: [BaseDTO.copy] maps a `*DTO -> *DO` relation by id, but
     * only between fields of the same name, and these are named `customer`/`project` here and
     * `kunde`/`projekt` there.
     */
    override fun copyTo(dest: AuftragDO) {
        super.copyTo(dest)
        dest.kunde = customer?.id?.let { id -> KundeDO().also { it.id = id } }
        dest.projekt = project?.id?.let { id -> ProjektDO().also { it.id = id } }
        dest.positionen = positionen?.map { dto ->
            AuftragsPositionDO().also { dto.copyTo(it, dest) }
        }?.toMutableList()
        dest.paymentSchedules = paymentSchedules?.map { dto ->
            PaymentScheduleDO().also { dto.copyTo(it, dest) }
        }?.toMutableList()
    }

    companion object {
        /**
         * The calculated sums of an order, from the cache for a stored order and computed on the spot for
         * an unstored one. The same helper the recalculate endpoint of `OrderEntityRest` uses, so the
         * numbers a form shows before and after the first save come from one code path.
         *
         * [AuftragsCache.getOrderInfo] answers an empty [OrderInfo] (all sums 0.00) for an order without
         * an id, which is why the second branch exists at all.
         */
        fun orderInfo(src: AuftragDO): OrderInfo {
            if (src.id != null) {
                return AuftragsCache.instance.getOrderInfo(src)
            }
            return calculateOrderInfo(src)
        }

        /**
         * Calculates the sums of the given order from its own (possibly unsaved) positions, without
         * touching the cache.
         *
         * [OrderInfo.infoPositions] has to be assigned explicitly: its getter falls back to
         * [AuftragsCache] whenever the backing field is null, so leaving it unset would silently answer
         * the stored positions instead of the posted ones. The invoiced sums still come from
         * [org.projectforge.business.fibu.RechnungCache] per position id, so a position without an id
         * counts 0.00 — there can be no invoice for a position that was never saved.
         */
        fun calculateOrderInfo(src: AuftragDO): OrderInfo {
            val info = OrderInfo()
            // Before calculateAll, because OrderPositionInfo keeps the parent it is constructed with.
            src.info = info
            val positionInfos = src.positionen?.map { OrderPositionInfo(it, info) }
            info.infoPositions = positionInfos
            info.calculateAll(src, positionInfos, src.paymentSchedules)
            return info
        }
    }
}
