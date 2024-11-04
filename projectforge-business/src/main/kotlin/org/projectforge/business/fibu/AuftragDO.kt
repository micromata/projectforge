/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import jakarta.persistence.*
import org.apache.commons.lang3.StringUtils
import org.hibernate.annotations.ListIndexBase
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.jcr.AttachmentsInfo
import org.projectforge.framework.persistence.candh.CandHIgnore
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.history.NoHistory
import org.projectforge.framework.persistence.history.PersistenceBehavior
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.xmlstream.XmlObjectReader
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Repräsentiert einen Auftrag oder ein Angebot. Ein Angebot kann abgelehnt oder durch ein anderes ersetzt werden, muss
 * also nicht zum tatsächlichen Auftrag werden. Wichtig ist: Alle Felder sind historisiert, so dass Änderungen wertvolle
 * Informationen enthalten, wie beispielsweise die Beauftragungshistorie: LOI am 05.03.08 durch Herrn Müller und
 * schriftlich am 04.04.08 durch Beschaffung.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
    name = "t_fibu_auftrag",
    uniqueConstraints = [UniqueConstraint(columnNames = ["nummer"])],
    indexes = [jakarta.persistence.Index(
        name = "idx_fk_t_fibu_auftrag_contact_person_fk",
        columnList = "contact_person_fk"
    ),
        jakarta.persistence.Index(name = "idx_fk_t_fibu_auftrag_projectManager_fk", columnList = "projectmanager_fk"),
        jakarta.persistence.Index(
            name = "idx_fk_t_fibu_auftrag_headofbusinessmanager_fk",
            columnList = "headofbusinessmanager_fk"
        ),
        jakarta.persistence.Index(name = "idx_fk_t_fibu_auftrag_salesmanager_fk", columnList = "salesmanager_fk"),
        jakarta.persistence.Index(name = "idx_fk_t_fibu_auftrag_kunde_fk", columnList = "kunde_fk"),
        jakarta.persistence.Index(name = "idx_fk_t_fibu_auftrag_projekt_fk", columnList = "projekt_fk")]
)
/*@WithHistory(
  noHistoryProperties = ["lastUpdate", "created"],
  nestedEntities = [AuftragsPositionDO::class, PaymentScheduleDO::class]
)*/
@NamedQueries(
    NamedQuery(
        name = AuftragDO.SELECT_MIN_MAX_DATE,
        query = "select min(angebotsDatum), max(angebotsDatum) from AuftragDO"
    ),
    NamedQuery(name = AuftragDO.FIND_BY_NUMMER, query = "from AuftragDO where nummer=:nummer"),
    NamedQuery(name = AuftragDO.FIND_OTHER_BY_NUMMER, query = "from AuftragDO where nummer=:nummer and id!=:id")
)
open class AuftragDO : DefaultBaseDO(), DisplayNameCapable, AttachmentsInfo {

    override val displayName: String
        @Transient
        get() = "$nummer: $titel"

    /**
     * Auftragsnummer ist eindeutig und wird fortlaufend erzeugt.
     */
    @PropertyInfo(i18nKey = "fibu.auftrag.nummer")
    @GenericField
    @get:Column(nullable = false)
    open var nummer: Int? = null

    /**
     * Dies sind die alten Auftragsnummern oder Kundenreferenzen.
     */
    @PropertyInfo(i18nKey = "fibu.common.customer.reference")
    @GenericField
    @FullTextField(name = "referenz_tokenized") // was: @FullTextFields(Field(name = "referenz_tokenized"), Field(analyze = Analyze.NO))
    @get:Column(length = 255)
    open var referenz: String? = null

    @PropertyInfo(i18nKey = "label.position.short")
    @PersistenceBehavior(autoUpdateCollectionEntries = true)
    @IndexedEmbedded(includeDepth = 1)
    @get:OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "auftrag")
    @get:OrderColumn(name = "number") // was IndexColumn(name = "number", base = 1)
    @get:ListIndexBase(1)
    open var positionen: MutableList<AuftragsPositionDO>? = null

    @PropertyInfo(i18nKey = "status")
    @FullTextField
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "status", length = 30)
    open var auftragsStatus: AuftragsStatus? = null

    @PropertyInfo(i18nKey = "contactPerson")
    @IndexedEmbedded(includeDepth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:JoinColumn(name = "contact_person_fk", nullable = true)
    open var contactPerson: PFUserDO? = null

    val contactPersonId: Long?
        @Transient
        get() = contactPerson?.id


    @PropertyInfo(i18nKey = "fibu.kunde")
    @IndexedEmbedded(includeDepth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:JoinColumn(name = "kunde_fk", nullable = true)
    open var kunde: KundeDO? = null

    val kundeId: Long?
        @Transient
        get() = kunde?.id

    /**
     * Freitextfeld, falls Kunde nicht aus Liste gewählt werden kann bzw. für Rückwärtskompatibilität mit alten Kunden.
     */
    @PropertyInfo(i18nKey = "fibu.kunde.text")
    @FullTextField
    @get:Column(name = "kunde_text", length = 1000)
    open var kundeText: String? = null

    @PropertyInfo(i18nKey = "fibu.projekt")
    @IndexedEmbedded(includeDepth = 2)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:JoinColumn(name = "projekt_fk", nullable = true)
    open var projekt: ProjektDO? = null

    val projektId: Long?
        @Transient
        get() = projekt?.id

    @PropertyInfo(i18nKey = "fibu.auftrag.titel")
    @FullTextField
    @get:Column(name = "titel", length = 1000)
    open var titel: String? = null

    @PropertyInfo(i18nKey = "comment")
    @FullTextField
    @get:Column(length = 4000)
    open var bemerkung: String? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.statusBeschreibung")
    @FullTextField
    @get:Column(length = 4000, name = "status_beschreibung")
    open var statusBeschreibung: String? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.angebot.datum")
    @GenericField
    @get:Column(name = "angebots_datum")
    open var angebotsDatum: LocalDate? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.erfassung.datum")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "erfassungs_datum")
    open var erfassungsDatum: LocalDate? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.entscheidung.datum")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "entscheidungs_datum")
    open var entscheidungsDatum: LocalDate? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.bindungsFrist")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "bindungs_frist")
    open var bindungsFrist: LocalDate? = null

    /**
     * Wer hat wann und wie beauftragt? Z. B. Beauftragung per E-Mail durch Herrn Müller.
     */
    @get:Column(name = "beauftragungs_beschreibung", length = 4000)
    open var beauftragungsBeschreibung: String? = null

    /**
     * Wann wurde beauftragt? Beachte: Alle Felder historisiert, so dass hier ein Datum z. B. mit dem LOI und später das
     * Datum der schriftlichen Beauftragung steht.
     */
    @PropertyInfo(i18nKey = "fibu.auftrag.beauftragungsdatum")
    @get:Column(name = "beauftragungs_datum")
    open var beauftragungsDatum: LocalDate? = null

    /**
     * The user interface status of an order. The [AuftragUIStatus] is stored as XML.
     *
     * @return the XML representation of the uiStatus.
     * @see AuftragUIStatus
     */
    @NoHistory
    @get:Column(name = "ui_status_as_xml", length = 10000)
    open var uiStatusAsXml: String? = null

    private var uiStatus: AuftragUIStatus? = null

    /**
     * Get the payment schedule entries for this object.
     */
    @PropertyInfo(i18nKey = "fibu.auftrag.paymentschedule")
    @PersistenceBehavior(autoUpdateCollectionEntries = true)
    @get:OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "auftrag")
    @get:OrderColumn(name = "number") // was IndexColumn(name = "number", base = 1)
    @get:ListIndexBase(1)
    open var paymentSchedules: MutableList<PaymentScheduleDO>? = null

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.from")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "period_of_performance_begin")
    open var periodOfPerformanceBegin: LocalDate? = null

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.to")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "period_of_performance_end")
    open var periodOfPerformanceEnd: LocalDate? = null

    @PropertyInfo(i18nKey = "fibu.probabilityOfOccurrence")
    @get:Column(name = "probability_of_occurrence")
    open var probabilityOfOccurrence: Int? = null

    @PropertyInfo(i18nKey = "fibu.projectManager")
    @IndexedEmbedded(includeDepth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:JoinColumn(name = "projectmanager_fk")
    open var projectManager: PFUserDO? = null

    @PropertyInfo(i18nKey = "fibu.headOfBusinessManager")
    @IndexedEmbedded(includeDepth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:JoinColumn(name = "headofbusinessmanager_fk")
    open var headOfBusinessManager: PFUserDO? = null

    @PropertyInfo(i18nKey = "fibu.salesManager")
    @IndexedEmbedded(includeDepth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:JoinColumn(name = "salesmanager_fk")
    open var salesManager: PFUserDO? = null

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
    @get:Column(length = 10000, name = "attachments_counter")
    override var attachmentsCounter: Int? = null

    @JsonIgnore
    @NoHistory
    @get:Column(length = 10000, name = "attachments_size")
    override var attachmentsSize: Long? = null

    @PropertyInfo(i18nKey = "attachment")
    @JsonIgnore
    @get:Column(length = 10000, name = "attachments_last_user_action")
    override var attachmentsLastUserAction: String? = null


    /**
     * @see ProjektFormatter.formatProjektKundeAsString
     */
    val projektKundeAsString: String
        @Transient
        get() = ProjektFormatter.formatProjektKundeAsString(this.projekt, this.kunde, this.kundeText)

    /**
     * @see KundeFormatter.formatKundeAsString
     */
    val kundeAsString: String
        @Transient
        get() = KundeFormatter.formatKundeAsString(this.kunde, this.kundeText)

    val projektAsString: String
        @Transient
        get() = ProjektFormatter.formatProjektKundeAsString(this.projekt, this.kunde, this.kundeText)

    /**
     * Get list of AuftragsPosition including elements that are marked as deleted.
     *
     * @return Returns the full list of linked AuftragsPositionen.
     */
    val positionenIncludingDeleted: List<AuftragsPositionDO>?
        @Transient
        get() = this.positionen

    /**
     * Get list of AuftragsPosition excluding elements that are marked as deleted.
     *
     * @return Returns a filtered list of AuftragsPosition excluding marked as deleted elements.
     */
    val positionenExcludingDeleted: List<AuftragsPositionDO>
        @Transient
        get() = positionen?.filter { !it.deleted } ?: emptyList()

    /**
     * Get list of PaymentScheduleDO excluding elements that are marked as deleted.
     *
     * @return Returns a filtered list of PaymentScheduleDO excluding marked as deleted elements.
     */
    val paymentSchedulesExcludingDeleted: List<PaymentScheduleDO>
        @Transient
        get() = paymentSchedules?.filter { !it.deleted } ?: emptyList()

    val projectManagerId: Long?
        @Transient
        get() = if (projectManager != null) projectManager!!.id else null

    val headOfBusinessManagerId: Long?
        @Transient
        get() = if (headOfBusinessManager != null) headOfBusinessManager!!.id else null

    val salesManagerId: Long?
        @Transient
        get() = if (salesManager != null) salesManager!!.id else null

    val assignedPersons: String
        @Transient
        get() {
            val result = ArrayList<String>()
            addUser(result, projectManager)
            addUser(result, headOfBusinessManager)
            addUser(result, salesManager)
            addUser(result, contactPerson)
            return result.joinToString("; ")
        }

    /**
     * Must be calculated before use.
     * @see OrderInfo.calculateAll
     */
    @get:Transient
    @JsonIgnore
    @CandHIgnore
    var info = OrderInfo(this)

    @Deprecated("Use info.netSum instead.")
    @get:Transient
    val nettoSumme: BigDecimal
        get() = info.netSum

    @Deprecated("Use info.orderedNetSum instead.")
    @get:Transient
    val beauftragtNettoSumme: BigDecimal
        get() = info.orderedNetSum

    @Deprecated("Use info.invoicedSum instead.")
    @get:Transient
    val invoicedSum: BigDecimal
        get() = info.invoicedSum

    @Deprecated("Use info.notYetInvoicedSum instead.")
    @get:Transient
    val notYetInvoicedSum: BigDecimal
        get() = info.notYetInvoicedSum

    /**
     * @return FAKTURIERT if isVollstaendigFakturiert == true, otherwise AuftragsStatus as String.
     */
    @get:Transient
    val auftragsStatusAsString: String?
        get() {
            if (info.isVollstaendigFakturiert) {
                return I18nHelper.getLocalizedMessage("fibu.auftrag.status.fakturiert")
            }
            return if (auftragsStatus != null) I18nHelper.getLocalizedMessage(auftragsStatus!!.i18nKey) else null
        }

    private fun addUser(result: ArrayList<String>, user: PFUserDO?) {
        UserGroupCache.getInstance().getUser(user?.id)?.let { result.add(it.getFullname()) }
    }

    /**
     * @param number
     * @return AuftragsPositionDO with given position number or null (iterates through the list of positions and compares
     * the number), if not exist.
     */
    fun getPosition(number: Short): AuftragsPositionDO? {
        if (positionen == null || positionen!!.size < 1) {
            return null
        }
        for (position in this.positionen!!) {
            if (position.number == number) {
                return position
            }
        }
        return null
    }

    fun addPosition(position: AuftragsPositionDO): AuftragDO {
        ensureAndGetPositionen()
        var number: Short = 1
        for (pos in positionen!!) {
            if (pos.number >= number) {
                number = pos.number
                number++
            }
        }
        position.number = number
        position.auftrag = this
        this.positionen!!.add(position)
        return this
    }

    fun ensureAndGetPositionen(): MutableList<AuftragsPositionDO> {
        if (this.positionen == null) {
            this.positionen = ArrayList()
        }
        return positionen!!
    }

    /**
     * @return the rechungUiStatus
     */
    @Transient
    fun getUiStatus(): AuftragUIStatus {
        if (uiStatus == null && StringUtils.isEmpty(uiStatusAsXml)) {
            uiStatus = AuftragUIStatus()
        } else if (uiStatus == null) {
            val reader = XmlObjectReader()
            reader.initialize(AuftragUIStatus::class.java)
            uiStatus = reader.read(uiStatusAsXml) as AuftragUIStatus
        }

        return uiStatus as AuftragUIStatus
    }

    /**
     * @param number
     * @return PaymentScheduleDO with given position number or null (iterates through the list of payment schedules and
     * compares the number), if not exist.
     */
    fun getPaymentSchedule(number: Short): PaymentScheduleDO? {
        if (paymentSchedules == null) {
            return null
        }
        for (schedule in this.paymentSchedules!!) {
            if (schedule.number == number) {
                return schedule
            }
        }
        return null
    }

    fun addPaymentSchedule(paymentSchedule: PaymentScheduleDO): AuftragDO {
        ensureAndGetPaymentSchedules()
        var number: Short = 1
        for (pos in paymentSchedules!!) {
            if (pos.number >= number) {
                number = pos.number
                number++
            }
        }
        paymentSchedule.number = number
        paymentSchedule.auftrag = this
        this.paymentSchedules!!.add(paymentSchedule)
        return this
    }

    fun ensureAndGetPaymentSchedules(): MutableList<PaymentScheduleDO> {
        if (this.paymentSchedules == null) {
            this.paymentSchedules = ArrayList()
        }
        return this.paymentSchedules!!
    }

    companion object {
        internal const val SELECT_MIN_MAX_DATE = "AuftragDO_SelectMinMaxDate"
        internal const val FIND_BY_NUMMER = "AuftragDO_FindByNummer"
        internal const val FIND_OTHER_BY_NUMMER = "AuftragDO_FindOtherByNummer"
    }
}
