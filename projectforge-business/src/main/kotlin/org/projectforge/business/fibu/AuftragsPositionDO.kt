/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.projectforge.business.task.TaskDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.math.BigDecimal
import java.time.LocalDate
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.search.ClassBridge

/**
 * Repräsentiert eine Position innerhalb eines Auftrags oder eines Angebots.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@TypeBinding(binder = TypeBinderRef(type = HibernateSearchAuftragsPositionTypeBinder::class))
@ClassBridge(name = "position") // position should be used in HibernateSearchAuftragsPositionBridge as field name.
//@ClassBridge(name = "position", analyze = Analyze.NO, impl = HibernateSearchAuftragsPositionBridge::class)
//@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
//@Cacheable
@Table(
  name = "t_fibu_auftrag_position",
  uniqueConstraints = [UniqueConstraint(columnNames = ["auftrag_fk", "number"])],
  indexes = [jakarta.persistence.Index(
    name = "idx_fk_t_fibu_auftrag_position_auftrag_fk",
    columnList = "auftrag_fk"
  ), jakarta.persistence.Index(name = "idx_fk_t_fibu_auftrag_position_task_fk", columnList = "task_fk")],
)
@NamedQueries(
  NamedQuery(name = AuftragsPositionDO.FIND_BY_ID, query = "from AuftragsPositionDO where id=:id")
)
open class AuftragsPositionDO : DefaultBaseDO(), DisplayNameCapable {

  override val displayName: String
    @Transient
    get() = "${auftrag?.nummer}.$number"

  @get:Column
  open var number: Short = 0

  // @JsonIgnore needed due to circular references.
  @JsonIgnore
  // @ContainedIn
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "auftrag_fk", nullable = false)
  @JsonSerialize(using = IdOnlySerializer::class)
  open var auftrag: AuftragDO? = null

  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "task_fk", nullable = true)
  @JsonSerialize(using = IdOnlySerializer::class)
  open var task: TaskDO? = null

  @PropertyInfo(i18nKey = "fibu.auftrag.position.art")
  @FullTextField
  @get:Enumerated(EnumType.STRING)
  @get:Column(name = "art", length = 30)
  open var art: AuftragsPositionsArt? = null

  @PropertyInfo(i18nKey = "fibu.auftrag.position.paymenttype")
  @get:Enumerated(EnumType.STRING)
  @get:Column(name = "paymentType", length = 30)
  open var paymentType: AuftragsPositionsPaymentType? = null

  @PropertyInfo(i18nKey = "fibu.auftrag.position.status")
  @FullTextField
  @get:Enumerated(EnumType.STRING)
  @get:Column(name = "status", length = 30)
  open var status: AuftragsStatus? = null

  @PropertyInfo(i18nKey = "fibu.auftrag.title")
  @FullTextField
  @get:Column(name = "titel", length = 255)
  open var titel: String? = null

  @PropertyInfo(i18nKey = "comment")
  @FullTextField
  @get:Column(length = 4000)
  open var bemerkung: String? = null

  @PropertyInfo(i18nKey = "fibu.auftrag.nettoSumme")
  @GenericField
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
   * Dieses Flag wird manuell von der FiBu gesetzt und kann nur für abgeschlossene Aufträge gesetzt werden.
   */
  @get:Column(name = "vollstaendig_fakturiert", nullable = false)
  open var vollstaendigFakturiert: Boolean? = false

  @get:Enumerated(EnumType.STRING)
  @get:Column(name = "period_of_performance_type", length = 10)
  open var periodOfPerformanceType: PeriodOfPerformanceType? = PeriodOfPerformanceType.SEEABOVE

  @get:Column(name = "period_of_performance_begin")
  open var periodOfPerformanceBegin: LocalDate? = null

  @get:Column(name = "period_of_performance_end")
  open var periodOfPerformanceEnd: LocalDate? = null

  @get:Enumerated(EnumType.STRING)
  @get:Column(name = "mode_of_payment_type", length = 13)
  open var modeOfPaymentType: ModeOfPaymentType? = null

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
    get() = "${auftrag?.nummer}.$number"

  /**
   * Throws UserException if vollstaendigFakturiert is true and status is not ABGESCHLOSSEN.
   */
  fun checkVollstaendigFakturiert() {
    if (vollstaendigFakturiert == true && (status?.isIn(AuftragsStatus.ABGESCHLOSSEN) != true)) {
      throw UserException(
        "fibu.auftrag.error.nurAbgeschlosseneAuftragsPositionenKoennenVollstaendigFakturiertSein"
      )
    }
  }

  override fun equals(other: Any?): Boolean {
    if (other is AuftragsPositionDO) {
      val o = other as AuftragsPositionDO?
      return this.number == o!!.number && this.auftrag?.id == o.auftrag?.id
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
  fun hasOwnPeriodOfPerformance(): Boolean {
    return periodOfPerformanceType == PeriodOfPerformanceType.OWN
  }

  companion object {
    internal const val FIND_BY_ID = "AuftragPositionDO_FindById"
  }
}
