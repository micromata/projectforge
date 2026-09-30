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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonManagedReference
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.hibernate.annotations.ListIndexBase
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.jcr.AttachmentsInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.history.NoHistory
import org.projectforge.framework.persistence.history.PersistenceBehavior
import java.time.LocalDate

/**
 * Geplante und gestellte Rechnungen.
 *
 * @author Kai Reinhard
 */
@Entity
@Indexed
//@Cacheable
//@Cache(region = "invoices", usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
//@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(
    name = "t_fibu_rechnung",
    indexes = [
        jakarta.persistence.Index(name = "idx_fk_t_fibu_rechnung_konto_id", columnList = "konto_id"),
        jakarta.persistence.Index(name = "idx_fk_t_fibu_rechnung_kunde_id", columnList = "kunde_id"),
        jakarta.persistence.Index(name = "idx_fk_t_fibu_rechnung_projekt_id", columnList = "projekt_id"),
        jakarta.persistence.Index(name = "idx_fk_t_fibu_rechnung_original_rechnung", columnList = "original_rechnung_fk")]
)
/*@WithHistory(
  noHistoryProperties = ["lastUpdate", "created"],
  nestedEntities = [RechnungsPositionDO::class]
)*/
@NamedQueries(
    NamedQuery(name = RechnungDO.SELECT_MIN_MAX_DATE, query = "select min(datum), max(datum) from RechnungDO where deleted = false"),
    NamedQuery(name = RechnungDO.FIND_OTHER_BY_NUMMER, query = "from RechnungDO where nummer=:nummer and id!=:id"),
    NamedQuery(name = RechnungDO.FIND_BY_NUMMER, query = "from RechnungDO where nummer=:nummer and deleted=false"),
    NamedQuery(
        name = RechnungDO.FIND_CANCELLATIONS_OF,
        query = "from RechnungDO where originalRechnung.id=:originalId and deleted=false"
    ),
    NamedQuery(
        name = RechnungDO.SELECT_TYP_AND_ORIGINAL_ID,
        query = "select r.typ as typ, o.id as originalId from RechnungDO r left join r.originalRechnung o where r.id=:id"
    ),
)
open class RechnungDO : AbstractRechnungDO(), Comparable<RechnungDO>, AttachmentsInfo {
    override val displayName: String
        @Transient
        get() = "${belegNummer ?: nummer}"

    @PropertyInfo(i18nKey = "fibu.rechnung.nummer")
    @GenericField // was: @FullTextField(analyze = Analyze.NO, bridge = FieldBridge(impl = IntegerBridge::class))
    @get:Column(nullable = true)
    open var nummer: Int? = null

    /**
     * The invoice a cancellation invoice ([RechnungTyp.CANCELLATION]) cancels, null for every other type.
     *
     * Eager, not lazy: [belegNummer] (and with it [displayName]) is derived from the number of the original,
     * and is read outside a session as well (history, lists, exports), where a lazy proxy would throw. The
     * reference is one level deep only - an original is a [RechnungTyp.RECHNUNG] and references nothing
     * itself (see `RechnungDao.validateCancellation`).
     */
    @PropertyInfo(i18nKey = "fibu.rechnung.originalRechnung")
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "original_rechnung_fk", nullable = true)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var originalRechnung: RechnungDO? = null

    /**
     * Whether this invoice can be cancelled by a cancellation invoice: a stored, undeleted invoice with a
     * number. A missing type is an invoice, too (as in the e-invoice export): old rows may have none.
     */
    val isCancellable: Boolean
        @Transient
        get() = id != null && !deleted && nummer != null && (typ == null || typ == RechnungTyp.RECHNUNG)

    /**
     * The number of the document as printed on it and written into the e-invoice (BT-1): [nummer] for an
     * invoice, and the number of the original with the suffix [CANCELLATION_SUFFIX] for a cancellation
     * invoice (e.g. `16956-S`), which has no number of its own.
     */
    val belegNummer: String?
        @Transient
        get() = if (typ == RechnungTyp.CANCELLATION) {
            originalRechnung?.nummer?.let { "$it$CANCELLATION_SUFFIX" }
        } else {
            nummer?.toString()
        }

    /**
     * Rechnungsempfänger. Dieser Kunde kann vom Kunden, der mit dem Projekt verbunden ist abweichen.
     */
    @PropertyInfo(i18nKey = "fibu.kunde")
    @IndexedEmbedded(includeDepth = 1)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "kunde_id", nullable = true)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var kunde: KundeDO? = null

    /**
     * Freitextfeld, falls Kunde nicht aus Liste gewählt werden kann bzw. für Rückwärtskompatibilität mit alten Kunden.
     */
    @PropertyInfo(i18nKey = "fibu.kunde.text")
    @FullTextField
    @get:Column(name = "kunde_text")
    open var kundeText: String? = null

    @PropertyInfo(i18nKey = "fibu.projekt")
    @IndexedEmbedded(includeDepth = 2)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "projekt_id", nullable = true)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var projekt: ProjektDO? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.status")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 30)
    open var status: RechnungStatus? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.typ")
    @FullTextField
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 40)
    open var typ: RechnungTyp? = null

    @PropertyInfo(i18nKey = "fibu.customerref1")
    @FullTextField
    @get:Column(name = "customerref1")
    open var customerref1: String? = null

    @PropertyInfo(i18nKey = "fibu.attachment")
    @FullTextField
    @get:Column(name = "attachment")
    open var attachment: String? = null

    @PropertyInfo(i18nKey = "fibu.konto.contactPerson")
    @get:Column(name = "customer_contact_person", length = 255)
    open var customerContactPerson: String? = null

    @PropertyInfo(i18nKey = "fibu.konto.street")
    @FullTextField
    @get:Column(name = "customeraddress")
    open var customerAddress: String? = null

    @PropertyInfo(i18nKey = "fibu.konto.zipCode")
    @get:Column(name = "customer_zip_code", length = 10)
    open var customerZipCode: String? = null

    @PropertyInfo(i18nKey = "fibu.konto.city")
    @get:Column(name = "customer_city", length = 100)
    open var customerCity: String? = null

    @PropertyInfo(i18nKey = "fibu.konto.country")
    @get:Column(name = "customer_country", length = 2)
    open var customerCountry: String? = null

    @PropertyInfo(i18nKey = "fibu.konto.vatId")
    @get:Column(name = "customer_vat_id", length = 20)
    open var customerVatId: String? = null

    @PropertyInfo(i18nKey = "fibu.konto.leitwegId")
    @get:Column(name = "customer_leitweg_id", length = 50)
    open var customerLeitwegId: String? = null

    @PropertyInfo(i18nKey = "fibu.konto.eInvoiceEmail")
    @get:Column(name = "customer_e_invoice_email", length = 255)
    open var customerEInvoiceEmail: String? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.sellerBankAccount")
    @get:Column(name = "seller_bank_account", length = 34)
    open var sellerBankAccount: String? = null

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.from")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "period_of_performance_begin")
    open var periodOfPerformanceBegin: LocalDate? = null

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.to")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "period_of_performance_end")
    open var periodOfPerformanceEnd: LocalDate? = null

    @PersistenceBehavior(autoUpdateCollectionEntries = true)
    @JsonManagedReference
    @IndexedEmbedded(includeDepth = 3)
    @get:OneToMany(
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH],
        orphanRemoval = false,
        fetch = FetchType.LAZY,
        mappedBy = "rechnung",
        targetEntity = RechnungsPositionDO::class,
    )
    @get:OrderColumn(name = "number") // was IndexColumn(name = "number", base = 1)
    @get:ListIndexBase(1)
    override var positionen: MutableList<RechnungsPositionDO>? = null

    override val abstractPositionen: List<AbstractRechnungsPositionDO>?
        @Transient
        get() = positionen

    /**
     * The positions the invoice actually consists of, i.e. without the ones marked as deleted.
     *
     * What any document or sum of the invoice has to iterate: a deleted position stays in [positionen] (it is
     * only flagged, so it can be restored and so its history survives), but it is no part of the invoice any
     * more. [RechnungCalculator] skips it and therefore never fills its `info` — reading `position.info.netSum`
     * of a deleted position throws, which is how a single deleted position used to make the Word and e-invoice
     * exports fail altogether.
     *
     * @return The undeleted positions, empty if there are none.
     */
    val positionenExcludingDeleted: List<RechnungsPositionDO>
        @Transient
        get() = positionen?.filter { !it.deleted } ?: emptyList()

    /**
     *  @return true if the invoice is valid: isn't deleted, status is not GEPLANT or STORNIERT and it is no
     *  cancellation. A cancellation is left out wherever the cancelled invoice (status STORNIERT) is, so both
     *  together count 0 there - as they do everywhere else, where the cancellation's negative amounts balance
     *  the original's.
     */
    override val isValid: Boolean
        @Transient
        get() = !deleted && status?.isIn(RechnungStatus.GEPLANT, RechnungStatus.STORNIERT) == false &&
                typ != RechnungTyp.CANCELLATION

    override fun ensureAndGetPositionen(): MutableList<out AbstractRechnungsPositionDO> {
        if (this.positionen == null) {
            positionen = mutableListOf()
        }
        return positionen!!
    }

    override fun addPositionWithoutCheck(position: AbstractRechnungsPositionDO) {
        position as RechnungsPositionDO
        this.positionen!!.add(position)
        position.rechnung = this
    }

    override fun setAbstractRechnung(position: AbstractRechnungsPositionDO) {
        position as RechnungsPositionDO
        position.rechnung = this
    }

    /**
     * @see KundeFormatter.formatKundeAsString
     */
    val kundeAsString: String
        @Transient
        get() = KundeFormatter.formatKundeAsString(this.kunde, this.kundeText)

    fun setRechnung(position: RechnungsPositionDO) {
        position.rechnung = this
    }

    override fun compareTo(other: RechnungDO): Int {
        val cmp = compareValues(this.datum, other.datum)
        if (cmp != 0) return cmp
        return compareValues(this.nummer, other.nummer)
    }

    @JsonIgnore
    @FullTextField
    @NoHistory
    @get:Column(length = 10000, name = "attachments_names")
    override var attachmentsNames: String? = null

    @JsonIgnore
    @FullTextField
    @NoHistory
    @get:Column(length = 10000, name = "attachments_ids")
    override var attachmentsIds: String? = null

    @JsonIgnore
    @NoHistory
    @get:Column(name = "attachments_counter")
    override var attachmentsCounter: Int? = null

    @JsonIgnore
    @NoHistory
    @get:Column(name = "attachments_size")
    override var attachmentsSize: Long? = null

    @PropertyInfo(i18nKey = "attachment")
    @JsonIgnore
    @get:Column(length = 10000, name = "attachments_last_user_action")
    override var attachmentsLastUserAction: String? = null

    companion object {
        internal const val SELECT_MIN_MAX_DATE = "RechnungDO_SelectMinMaxDate"
        internal const val FIND_OTHER_BY_NUMMER = "RechnungDO_FindOtherByNummer"
        internal const val FIND_BY_NUMMER = "RechnungDO_FindByNummer"
        internal const val FIND_CANCELLATIONS_OF = "RechnungDO_FindCancellationsOf"
        internal const val SELECT_TYP_AND_ORIGINAL_ID = "RechnungDO_SelectTypAndOriginalId"

        /** Appended to the number of the original to form the number of a cancellation invoice. */
        const val CANCELLATION_SUFFIX = "-S"
    }
}
