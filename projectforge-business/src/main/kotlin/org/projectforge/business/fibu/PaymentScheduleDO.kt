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
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.math.BigDecimal
import java.time.LocalDate
import jakarta.persistence.*
import org.projectforge.framework.json.IdOnlySerializer

/**
 * @author Werner Feder (werner.feder@t-online.de)
 */
@Entity
@Table(
  name = "T_FIBU_PAYMENT_SCHEDULE",
  uniqueConstraints = [UniqueConstraint(columnNames = ["auftrag_id", "number"])],
  indexes = [Index(name = "idx_fk_t_fibu_payment_schedule_auftrag_id", columnList = "auftrag_id")]
)
open class PaymentScheduleDO : DefaultBaseDO(), DisplayNameCapable {

  override val displayName: String
    @Transient
    get() = "$auftrag?.id:$number"

  /**
   * Not used as object due to performance reasons.
   */
  // @JsonIgnore needed due to circular references.
  @JsonIgnore
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "auftrag_id", nullable = false)
  @JsonSerialize(using = IdOnlySerializer::class)
  open var auftrag: AuftragDO? = null

  /**
   * The position's number this payment schedule is assigned to. (It is not the id/pk!!!)
   */
  @get:Column(name = "position_number")
  open var positionNumber: Short? = null

  @get:Column
  open var number: Short = 0

  @PropertyInfo(i18nKey = "fibu.rechnung.datum.short")
  @get:Column(name = "schedule_date")
  open var scheduleDate: LocalDate? = null

  @PropertyInfo(i18nKey = "fibu.common.betrag")
  @get:Column(scale = 2, precision = 12)
  open var amount: BigDecimal? = null

  @PropertyInfo(i18nKey = "comment")
  @get:Column
  open var comment: String? = null

  @PropertyInfo(i18nKey = "fibu.common.reached")
  @get:Column
  open var reached: Boolean = false

  /**
   * Dieses Flag wird manuell von der FiBu gesetzt und kann nur für abgeschlossene Aufträge gesetzt werden.
   */
  @PropertyInfo(i18nKey = "fibu.auftrag.vollstaendigFakturiert")
  @get:Column(name = "vollstaendig_fakturiert", nullable = false)
  open var vollstaendigFakturiert: Boolean = false

  val isEmpty: Boolean
    @Transient
    get() {
      if (!StringUtils.isBlank(comment)) {
        return false
      }
      return if (amount != null && amount!!.compareTo(BigDecimal.ZERO) != 0) {
        false
      } else scheduleDate == null
    }

  override fun equals(other: Any?): Boolean {
    if (other is PaymentScheduleDO) {
      val o = other as PaymentScheduleDO?
      if (this.number != o!!.number) {
        return false
      }
      return this.auftrag?.id == o.auftrag?.id
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
}
