/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.genome.db.jpa.history.api.NoHistory
import de.micromata.genome.db.jpa.history.api.WithHistory
import org.apache.commons.lang3.StringUtils
import org.hibernate.annotations.ListIndexBase
import org.hibernate.search.annotations.*
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.api.PFPersistancyBehavior
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.framework.xstream.XmlObjectReader
import java.math.BigDecimal
import java.sql.Date
import java.util.*
import javax.persistence.*

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
@Table(name = "t_fibu_auftrag",
        uniqueConstraints = [UniqueConstraint(columnNames = ["nummer", "tenant_id"])],
        indexes = [javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_contact_person_fk", columnList = "contact_person_fk"),
            javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_projectManager_fk", columnList = "projectmanager_fk"),
            javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_headofbusinessmanager_fk", columnList = "headofbusinessmanager_fk"),
            javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_salesmanager_fk", columnList = "salesmanager_fk"),
            javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_kunde_fk", columnList = "kunde_fk"),
            javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_projekt_fk", columnList = "projekt_fk"),
            javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_tenant_id", columnList = "tenant_id")])
@WithHistory(noHistoryProperties = ["lastUpdate", "created"],
        nestedEntities = [AuftragsPositionDO::class, PaymentScheduleDO::class])
class AuftragDO : DefaultBaseDO() {

    private val log = org.slf4j.LoggerFactory.getLogger(AuftragDO::class.java)

    /**
     * Auftragsnummer ist eindeutig und wird fortlaufend erzeugt.
     */
    @PropertyInfo(i18nKey = "fibu.auftrag.nummer")
    @get:Column(nullable = false)
    var nummer: Int? = null

    /**
     * Dies sind die alten Auftragsnummern oder Kundenreferenzen.
     */
    @PropertyInfo(i18nKey = "fibu.auftrag.customer.reference")
    @Fields(Field(name = "referenz_tokenized"), Field(analyze = Analyze.NO))
    @get:Column(length = 255)
    var referenz: String? = null

    @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
    @IndexedEmbedded(depth = 1)
    @get:OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "auftrag")
    @get:OrderColumn(name = "number") // was IndexColumn(name = "number", base = 1)
    @get:ListIndexBase(1)
    var positionen: MutableList<AuftragsPositionDO>? = null

    @Field
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "status", length = 30)
    var auftragsStatus: AuftragsStatus? = null

    @PropertyInfo(i18nKey = "contactPerson")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "contact_person_fk", nullable = true)
    var contactPerson: PFUserDO? = null

    @PropertyInfo(i18nKey = "fibu.kunde")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "kunde_fk", nullable = true)
    var kunde: KundeDO? = null

    /**
     * Freitextfeld, falls Kunde nicht aus Liste gewählt werden kann bzw. für Rückwärtskompatibilität mit alten Kunden.
     */
    @Field
    @get:Column(name = "kunde_text", length = 1000)
    var kundeText: String? = null

    @PropertyInfo(i18nKey = "fibu.projekt")
    @IndexedEmbedded(depth = 2)
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "projekt_fk", nullable = true)
    var projekt: ProjektDO? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.titel")
    @Field
    @get:Column(name = "titel", length = 1000)
    var titel: String? = null

    @PropertyInfo(i18nKey = "comment")
    @Field
    @get:Column(length = 4000)
    var bemerkung: String? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.statusBeschreibung")
    @Field
    @get:Column(length = 4000, name = "status_beschreibung")
    var statusBeschreibung: String? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.angebot.datum")
    @Field
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "angebots_datum")
    var angebotsDatum: Date? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.erfassung.datum")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "erfassungs_datum")
    var erfassungsDatum: Date? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.entscheidung.datum")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "entscheidungs_datum")
    var entscheidungsDatum: Date? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.bindungsFrist")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "bindungs_frist")
    var bindungsFrist: Date? = null

    /**
     * Wer hat wann und wie beauftragt? Z. B. Beauftragung per E-Mail durch Herrn Müller.
     */
    @get:Column(name = "beauftragungs_beschreibung", length = 4000)
    var beauftragungsBeschreibung: String? = null

    /**
     * Wann wurde beauftragt? Beachte: Alle Felder historisiert, so dass hier ein Datum z. B. mit dem LOI und später das
     * Datum der schriftlichen Beauftragung steht.
     */
    @PropertyInfo(i18nKey = "fibu.auftrag.beauftragungsdatum")
    @get:Column(name = "beauftragungs_datum")
    var beauftragungsDatum: Date? = null

    private var fakturiertSum: BigDecimal? = null

    /**
     * The user interface status of an order. The [AuftragUIStatus] is stored as XML.
     *
     * @return the XML representation of the uiStatus.
     * @see AuftragUIStatus
     */
    @get:NoHistory
    @get:Column(name = "ui_status_as_xml", length = 10000)
    var uiStatusAsXml: String? = null

    private var uiStatus: AuftragUIStatus? = null

    /**
     * Get the payment schedule entries for this object.
     */
    @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
    @get:OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "auftrag")
    @get:OrderColumn(name = "number") // was IndexColumn(name = "number", base = 1)
    @get:ListIndexBase(1)
    var paymentSchedules: MutableList<PaymentScheduleDO>? = null

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.from")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "period_of_performance_begin")
    var periodOfPerformanceBegin: Date? = null

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.to")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "period_of_performance_end")
    var periodOfPerformanceEnd: Date? = null

    @PropertyInfo(i18nKey = "fibu.probabilityOfOccurrence")
    @get:Column(name = "probability_of_occurrence")
    var probabilityOfOccurrence: Int? = null

    @PropertyInfo(i18nKey = "fibu.projectManager")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "projectmanager_fk")
    var projectManager: PFUserDO? = null

    @PropertyInfo(i18nKey = "fibu.headOfBusinessManager")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "headofbusinessmanager_fk")
    var headOfBusinessManager: PFUserDO? = null

    @PropertyInfo(i18nKey = "fibu.salesManager")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "salesmanager_fk")
    var salesManager: PFUserDO? = null

    /**
     * Adds all net sums of the positions (without not ordered positions) and return the total sum.
     */
    val nettoSumme: BigDecimal
        @Transient
        get() {
            if (positionen == null) {
                return BigDecimal.ZERO
            }
            var sum = BigDecimal.ZERO
            for (position in positionen!!) {
                if (position.isDeleted) {
                    continue
                }
                val nettoSumme = position.nettoSumme
                if (nettoSumme != null && position.status != AuftragsPositionsStatus.ABGELEHNT && position.status != AuftragsPositionsStatus.ERSETZT) {
                    sum = sum.add(nettoSumme)
                }
            }
            return sum
        }

    /**
     * Adds all net sums of the positions (only ordered positions) and return the total sum.
     */
    val beauftragtNettoSumme: BigDecimal
        @Transient
        get() {
            if (positionen == null) {
                return BigDecimal.ZERO
            }
            var sum = BigDecimal.ZERO
            for (position in positionen!!) {
                if (position.isDeleted) {
                    continue
                }
                val nettoSumme = position.nettoSumme
                if (nettoSumme != null
                        && position.status != null
                        && position.status!!.isIn(AuftragsPositionsStatus.ABGESCHLOSSEN, AuftragsPositionsStatus.BEAUFTRAGT)) {
                    sum = sum.add(nettoSumme)
                }
            }

            return sum
        }

    /**
     * @return FAKTURIERT if isVollstaendigFakturiert == true, otherwise AuftragsStatus as String.
     */
    val auftragsStatusAsString: String?
        @Transient
        get() {
            if (isVollstaendigFakturiert) {
                return I18nHelper.getLocalizedMessage("fibu.auftrag.status.fakturiert")
            }
            return if (auftragsStatus != null) I18nHelper.getLocalizedMessage(auftragsStatus!!.i18nKey) else null
        }

    val kundeId: Int?
        @Transient
        get() = if (this.kunde == null) {
            null
        } else kunde!!.id

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
        get() {
            val buf = StringBuffer()
            var first = true
            if (this.projekt != null) {
                if (projekt!!.kunde != null) {
                    if (first) {
                        first = false
                    } else {
                        buf.append("; ")
                    }
                    buf.append(projekt!!.kunde.name)
                }
                if (StringUtils.isNotBlank(projekt!!.name)) {
                    if (!first) {
                        buf.append(" - ")
                    }
                    buf.append(projekt!!.name)
                }
            }
            return buf.toString()
        }

    val projektId: Int?
        @Transient
        get() = if (this.projekt == null) {
            null
        } else projekt!!.id

    val contactPersonId: Int?
        @Transient
        get() = if (this.contactPerson == null) {
            null
        } else contactPerson!!.id

    /**
     * @return true wenn alle Auftragspositionen vollständig fakturiert sind.
     * @see AuftragsPositionDO.isVollstaendigFakturiert
     */
    val isVollstaendigFakturiert: Boolean
        @Transient
        get() {
            if (positionen == null || auftragsStatus != AuftragsStatus.ABGESCHLOSSEN) {
                return false
            }
            for (position in positionen!!) {
                if (position.isDeleted) {
                    continue
                }
                if (!position.vollstaendigFakturiert && (position.status == null || !position.status!!.isIn(AuftragsPositionsStatus.ABGELEHNT, AuftragsPositionsStatus.ERSETZT))) {
                    return false
                }
            }
            return true
        }

    val isAbgeschlossenUndNichtVollstaendigFakturiert: Boolean
        @Transient
        get() {
            if (this.auftragsStatus!!.isIn(AuftragsStatus.ABGESCHLOSSEN) && !isVollstaendigFakturiert) {
                return true
            }
            if (positionenIncludingDeleted != null) {
                for (pos in positionenIncludingDeleted!!) {
                    if (pos.isDeleted) {
                        continue
                    }
                    if (pos.status == AuftragsPositionsStatus.ABGESCHLOSSEN && !pos.vollstaendigFakturiert) {
                        return true
                    }
                }
            }
            return false
        }

    /**
     * Get list of AuftragsPosition including elements that are marked as deleted.
     * **Attention: Changes in this list will be persisted**.
     *
     * @return Returns the full list of linked AuftragsPositionen.
     */
    val positionenIncludingDeleted: List<AuftragsPositionDO>?
        @Transient
        get() = this.positionen

    /**
     * Get list of AuftragsPosition excluding elements that are marked as deleted.
     * **Attention: Changes in this list will not be persisted.**
     *
     * @return Returns a filtered list of AuftragsPosition excluding marked as deleted elements.
     */
    val positionenExcludingDeleted: List<AuftragsPositionDO>
        @Transient
        get() = positionen?.filter { !it.isDeleted } ?: emptyList()

    /**
     * @return The sum of person days of all positions.
     */
    val personDays: BigDecimal
        @Transient
        get() {
            var result = BigDecimal.ZERO
            if (this.positionen != null) {
                for (pos in this.positionen!!) {
                    if (pos.isDeleted) {
                        continue
                    }
                    if (pos.personDays != null && pos.status != AuftragsPositionsStatus.ABGELEHNT && pos.status != AuftragsPositionsStatus.ERSETZT) {
                        result = result.add(pos.personDays)
                    }
                }
            }
            return result
        }

    val zuFakturierenSum: BigDecimal
        @Transient
        get() {
            var `val` = BigDecimal.ZERO
            if (positionen != null) {
                for (pos in positionen!!) {
                    if (pos.isDeleted) {
                        continue
                    }
                    if (pos.status != null) {
                        if (pos.status == AuftragsPositionsStatus.ABGELEHNT || pos.status == AuftragsPositionsStatus.ERSETZT || pos.status == AuftragsPositionsStatus.OPTIONAL) {
                            continue
                        }
                    }
                    var net: BigDecimal? = pos.nettoSumme
                    if (net == null) {
                        net = BigDecimal.ZERO
                    }
                    var invoiced: BigDecimal? = pos.fakturiertSum
                    if (invoiced == null) {
                        invoiced = BigDecimal.ZERO
                    }
                    `val` = `val`.add(net!!).subtract(invoiced!!)
                }
            }
            return `val`
        }

    val isZahlplanAbgeschlossenUndNichtVollstaendigFakturiert: Boolean
        @Transient
        get() {
            if (this.paymentSchedules != null) {
                for (pos in this.paymentSchedules!!) {
                    if (pos.isReached == true && pos.isVollstaendigFakturiert) {
                        return true
                    }
                }
            }
            return false
        }

    val projectManagerId: Int?
        @Transient
        get() = if (projectManager != null) projectManager!!.id else null

    val headOfBusinessManagerId: Int?
        @Transient
        get() = if (headOfBusinessManager != null) headOfBusinessManager!!.id else null

    val salesManagerId: Int?
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

    private fun addUser(result: ArrayList<String>, user: PFUserDO?) {
        if (user != null)
            result.add(user.getFullname())
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

    fun ensureAndGetPositionen(): List<AuftragsPositionDO> {
        if (this.positionen == null) {
            this.positionen = ArrayList()
        }
        return positionen!!
    }

    /**
     * Sums all positions. Must be set in all positions before usage. The value is not calculated automatically!
     *
     * @see AuftragDao.calculateInvoicedSum
     */
    @Transient
    fun getFakturiertSum(): BigDecimal {
        if (this.fakturiertSum == null) {
            this.fakturiertSum = BigDecimal.ZERO
            if (positionen != null) {
                for (pos in positionen!!) {
                    if (pos.isDeleted) {
                        continue
                    }
                    if (NumberHelper.isNotZero(pos.fakturiertSum)) {
                        this.fakturiertSum = this.fakturiertSum!!.add(pos.fakturiertSum)
                    }
                }
            }
        }
        return this.fakturiertSum!!
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

    fun ensureAndGetPaymentSchedules(): List<PaymentScheduleDO>? {
        if (this.paymentSchedules == null) {
            this.paymentSchedules = ArrayList()
        }
        return this.paymentSchedules
    }
}
