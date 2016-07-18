/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.business.fibu;

import java.math.BigDecimal;
import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

/**
 * @author Werner Feder (werner.feder@t-online.de)
 * 
 */
@Entity
@Table(name = "T_FIBU_PAYMENT_SCHEDULE",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "auftrag_id", "number" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_fibu_payment_schedule_auftrag_id", columnList = "auftrag_id"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_payment_schedule_tenant_id", columnList = "tenant_id")
    })
public class PaymentScheduleDO extends DefaultBaseDO implements ShortDisplayNameCapable
{
  private static final long serialVersionUID = -8024212050762584171L;

  private AuftragDO auftrag;

  private short number;

  @PropertyInfo(i18nKey = "date")
  private Date scheduleDate;

  @PropertyInfo(i18nKey = "fibu.common.betrag")
  private BigDecimal amount = null;

  @PropertyInfo(i18nKey = "comment")
  private String comment;

  @PropertyInfo(i18nKey = "fibu.common.reached")
  private boolean reached;

  @PropertyInfo(i18nKey = "fibu.auftrag.vollstaendigFakturiert")
  private boolean vollstaendigFakturiert;

  /**
   * Not used as object due to performance reasons.
   * 
   * @return AuftragDO
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "auftrag_id", nullable = false)
  public AuftragDO getAuftrag()
  {
    return auftrag;
  }

  public PaymentScheduleDO setAuftrag(final AuftragDO auftrag)
  {
    this.auftrag = auftrag;
    return this;
  }

  @Transient
  public Integer getAuftragId()
  {
    if (this.auftrag == null) {
      return null;
    }
    return auftrag.getId();
  }

  @Column
  public short getNumber()
  {
    return number;
  }

  public PaymentScheduleDO setNumber(final short number)
  {
    this.number = number;
    return this;
  }

  @Column(name = "schedule_date")
  public Date getScheduleDate()
  {
    return scheduleDate;
  }

  public PaymentScheduleDO setScheduleDate(final Date scheduleDate)
  {
    this.scheduleDate = scheduleDate;
    return this;
  }

  @Column(scale = 2, precision = 12)
  public BigDecimal getAmount()
  {
    return amount;
  }

  public PaymentScheduleDO setAmount(final BigDecimal amount)
  {
    this.amount = amount;
    return this;
  }

  @Column
  public String getComment()
  {
    return comment;
  }

  public PaymentScheduleDO setComment(final String comment)
  {
    this.comment = comment;
    return this;
  }

  @Column
  public boolean isReached()
  {
    return reached;
  }

  public PaymentScheduleDO setReached(final boolean reached)
  {
    this.reached = reached;
    return this;
  }

  /**
   * Dieses Flag wird manuell von der FiBu gesetzt und kann nur für abgeschlossene Aufträge gesetzt werden.
   */
  @Column(name = "vollstaendig_fakturiert", nullable = false)
  public boolean isVollstaendigFakturiert()
  {
    return vollstaendigFakturiert;
  }

  public PaymentScheduleDO setVollstaendigFakturiert(final boolean vollstaendigFakturiert)
  {
    this.vollstaendigFakturiert = vollstaendigFakturiert;
    return this;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof PaymentScheduleDO) {
      final PaymentScheduleDO other = (PaymentScheduleDO) o;
      if (ObjectUtils.equals(this.getNumber(), other.getNumber()) == false) {
        return false;
      }
      if (ObjectUtils.equals(this.getAuftragId(), other.getAuftragId()) == false) {
        return false;
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(getNumber());
    if (getAuftrag() != null) {
      hcb.append(getAuftrag().getId());
    }
    return hcb.toHashCode();
  }

  @Transient
  public boolean isEmpty()
  {
    if (StringUtils.isBlank(comment) == false) {
      return false;
    }
    if (amount != null && amount.compareTo(BigDecimal.ZERO) != 0) {
      return false;
    }
    return (scheduleDate == null);
  }

  /**
   * @see org.projectforge.framework.persistence.api.ShortDisplayNameCapable#getShortDisplayName()
   */
  @Override
  @Transient
  public String getShortDisplayName()
  {
    return getAuftragId() == null ? Short.toString(number) : getAuftragId().toString() + ":" + Short.toString(number);
  }

}
