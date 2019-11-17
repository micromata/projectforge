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

import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.search.annotations.*
import org.projectforge.business.task.TaskDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.i18n.UserException
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.math.BigDecimal
import java.sql.Date
import javax.persistence.*

/**
 * Repräsentiert eine Position innerhalb eines Auftrags oder eines Angebots.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@ClassBridge(name = "position", analyze = Analyze.NO, impl = HibernateSearchAuftragsPositionBridge::class)
@Cache(region = "orders", usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
//@Cacheable
@Table(name = "t_fibu_auftrag_position", uniqueConstraints = [UniqueConstraint(columnNames = ["auftrag_fk", "number"])], indexes = [javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_position_auftrag_fk", columnList = "auftrag_fk"), javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_position_task_fk", columnList = "task_fk"), javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_position_tenant_id", columnList = "tenant_id")])
open class AuftragsPositionDO : DefaultBaseDO(), ShortDisplayNameCapable {

    @get:Column
    open var number: Short = 0

    // @JsonIgnore needed due to circular references.
    @JsonIgnore
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "auftrag_fk", nullable = false)
    open var auftrag: AuftragDO? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "task_fk", nullable = true)
    open var task: TaskDO? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.position.art")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "art", length = 30)
    open var art: AuftragsPositionsArt? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.position.paymenttype")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "paymentType", length = 30)
    open var paymentType: AuftragsPositionsPaymentType? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.position.status")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "status", length = 30)
    open var status: AuftragsPositionsStatus? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.titel")
    @get:Column(name = "titel", length = 255)
    open var titel: String? = null

    @PropertyInfo(i18nKey = "comment")
    @get:Column(length = 4000)
    open var bemerkung: String? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.nettoSumme")
    @get:Column(name = "netto_summe", scale = 2, precision = 12)
    open var nettoSumme: BigDecimal? = null

    /**
     * Person days (man days) for this order position. The value may differ from the calculated net sum because you spent
     * more or less person days to realize this order position.
     */
    @PropertyInfo(i18nKey = "projectmanagement.personDays")
    @get:Column(name = "person_days", scale = 2, precision = 12)
    open var personDays: BigDecimal? = null

    /**
     * Must be set in all positions before usage. The value is not calculated automatically!
     *
     * @see AuftragDao.calculateInvoicedSum
     */
    @get:Transient
    open var fakturiertSum: BigDecimal? = null

    /**
     * Dieses Flag wird manuell von der FiBu gesetzt und kann nur für abgeschlossene Aufträge gesetzt werden.
     */
    @get:Column(name = "vollstaendig_fakturiert", nullable = false)
    open var vollstaendigFakturiert: Boolean? = false

    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "period_of_performance_type", length = 10)
    open var periodOfPerformanceType: PeriodOfPerformanceType? = PeriodOfPerformanceType.SEEABOVE

    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "period_of_performance_begin")
    open var periodOfPerformanceBegin: Date? = null

    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "period_of_performance_end")
    open var periodOfPerformanceEnd: Date? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "mode_of_payment_type", length = 13)
    open var modeOfPaymentType: ModeOfPaymentType? = null

    val isAbgeschlossenUndNichtVollstaendigFakturiert: Boolean
        @Transient
        get() {
            if (status != null && status!!.isIn(AuftragsPositionsStatus.ABGELEHNT, AuftragsPositionsStatus.ERSETZT)) {
                return false
            }
            return if (auftrag!!.auftragsStatus != AuftragsStatus.ABGESCHLOSSEN && status != AuftragsPositionsStatus.ABGESCHLOSSEN) {
                false
            } else vollstaendigFakturiert != true
        }

    val auftragId: Int?
        @Transient
        get() = auftrag?.id

    val taskId: Int?
        @Transient
        get() = task?.id

    val isEmpty: Boolean
        @Transient
        get() {
            if (StringUtils.isBlank(titel) == false) {
                return false
            }
            return if (nettoSumme != null && nettoSumme!!.compareTo(BigDecimal.ZERO) != 0) {
                false
            } else StringUtils.isBlank(bemerkung)
        }

    /**
     * @return Order number and position number: ###.## (&lt;order number&gt;.&lt;position number&gt;)
     */
    val formattedNumber: String
        @Transient
        get() {
            val buf = StringBuffer()
            if (this.auftrag != null) {
                buf.append(this.auftrag!!.nummer)
            }
            buf.append(".").append(this.number.toInt())
            return buf.toString()
        }

    /**
     * Throws UserException if vollstaendigFakturiert is true and status is not ABGESCHLOSSEN.
     */
    fun checkVollstaendigFakturiert() {
        if (vollstaendigFakturiert == true && (status == null || !status!!.isIn(AuftragsPositionsStatus.ABGESCHLOSSEN))) {
            throw UserException(
                    "fibu.auftrag.error.nurAbgeschlosseneAuftragsPositionenKoennenVollstaendigFakturiertSein")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is AuftragsPositionDO) {
            val o = other as AuftragsPositionDO?
            return this.number == o!!.number && this.auftragId == o.auftragId
        }
        return false
    }

    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        hcb.append(number)
        if (auftrag != null) {
            hcb.append(auftrag!!.id)
        }
        return hcb.toHashCode()
    }

    @Transient
    override fun getShortDisplayName(): String {
        return (if (this.auftrag != null) this.auftrag!!.nummer.toString() else "???") + "." + number.toString()
    }

    @Transient
    fun hasOwnPeriodOfPerformance(): Boolean {
        return periodOfPerformanceType == PeriodOfPerformanceType.OWN
    }
}
