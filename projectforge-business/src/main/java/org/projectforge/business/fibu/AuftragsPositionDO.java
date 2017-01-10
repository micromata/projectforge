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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.task.TaskDO;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

/**
 * Repräsentiert eine Position innerhalb eines Auftrags oder eines Angebots.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@ClassBridge(name = "position", index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO,
    impl = HibernateSearchAuftragsPositionBridge.class)
@Table(name = "t_fibu_auftrag_position",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "auftrag_fk", "number" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_position_auftrag_fk", columnList = "auftrag_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_position_task_fk", columnList = "task_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_position_tenant_id", columnList = "tenant_id")
    })
public class AuftragsPositionDO extends DefaultBaseDO implements ShortDisplayNameCapable
{
  private static final long serialVersionUID = 8011593885618879900L;

  private short number;

  private AuftragDO auftrag;

  private TaskDO task;

  private AuftragsPositionsArt art;

  private AuftragsPositionsPaymentType paymentType;

  private AuftragsPositionsStatus status;

  private String titel;

  private String bemerkung;

  private BigDecimal nettoSumme;

  private BigDecimal personDays;

  private BigDecimal fakturiertSum = null;

  private boolean vollstaendigFakturiert;

  private PeriodOfPerformanceType periodOfPerformanceType;

  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date periodOfPerformanceBegin;

  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date periodOfPerformanceEnd;

  private ModeOfPaymentType modeOfPaymentType;

  @Transient
  public boolean isAbgeschlossenUndNichtVollstaendigFakturiert()
  {
    if (getStatus() != null && getStatus().isIn(AuftragsPositionsStatus.ABGELEHNT, AuftragsPositionsStatus.ERSETZT)) {
      return false;
    }
    if (auftrag.getAuftragsStatus() != AuftragsStatus.ABGESCHLOSSEN && getStatus() != AuftragsPositionsStatus.ABGESCHLOSSEN) {
      return false;
    }
    return !isVollstaendigFakturiert();
  }

  @Column
  public short getNumber()
  {
    return number;
  }

  public AuftragsPositionDO setNumber(final short number)
  {
    this.number = number;
    return this;
  }

  @Column(name = "netto_summe", scale = 2, precision = 12)
  public BigDecimal getNettoSumme()
  {
    return nettoSumme;
  }

  public AuftragsPositionDO setNettoSumme(final BigDecimal nettoSumme)
  {
    this.nettoSumme = nettoSumme;
    return this;
  }

  /**
   * Person days (man days) for this order position. The value may differ from the calculated net sum because you spent
   * more or less person days to realize this order position.
   */
  @Column(name = "person_days", scale = 2, precision = 12)
  public BigDecimal getPersonDays()
  {
    return personDays;
  }

  public AuftragsPositionDO setPersonDays(final BigDecimal personDays)
  {
    this.personDays = personDays;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 30)
  public AuftragsPositionsStatus getStatus()
  {
    return status;
  }

  public AuftragsPositionDO setStatus(final AuftragsPositionsStatus status)
  {
    this.status = status;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "art", length = 30)
  public AuftragsPositionsArt getArt()
  {
    return art;
  }

  public AuftragsPositionDO setArt(final AuftragsPositionsArt art)
  {
    this.art = art;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "paymentType", length = 30)
  public AuftragsPositionsPaymentType getPaymentType()
  {
    return paymentType;
  }

  public AuftragsPositionDO setPaymentType(final AuftragsPositionsPaymentType paymentType)
  {
    this.paymentType = paymentType;
    return this;
  }

  @Column(name = "titel", length = 255)
  public String getTitel()
  {
    return titel;
  }

  public AuftragsPositionDO setTitel(final String titel)
  {
    this.titel = titel;
    return this;
  }

  @Column(length = 4000)
  public String getBemerkung()
  {
    return bemerkung;
  }

  public AuftragsPositionDO setBemerkung(final String bemerkung)
  {
    this.bemerkung = bemerkung;
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

  public AuftragsPositionDO setVollstaendigFakturiert(final boolean vollstaendigFakturiert)
  {
    this.vollstaendigFakturiert = vollstaendigFakturiert;
    return this;
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "auftrag_fk", nullable = false)
  public AuftragDO getAuftrag()
  {
    return auftrag;
  }

  public AuftragsPositionDO setAuftrag(final AuftragDO auftrag)
  {
    this.auftrag = auftrag;
    return this;
  }

  @Transient
  public Integer getAuftragId()
  {
    if (getAuftrag() == null) {
      return null;
    } else {
      return getAuftrag().getId();
    }
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_fk", nullable = true)
  public TaskDO getTask()
  {
    return task;
  }

  public AuftragsPositionDO setTask(final TaskDO task)
  {
    this.task = task;
    return this;
  }

  @Transient
  public Integer getTaskId()
  {
    return this.task != null ? task.getId() : null;
  }

  @Transient
  public boolean isEmpty()
  {
    if (StringUtils.isBlank(titel) == false) {
      return false;
    }
    if (nettoSumme != null && nettoSumme.compareTo(BigDecimal.ZERO) != 0) {
      return false;
    }
    return StringUtils.isBlank(bemerkung);
  }

  /**
   * Must be set in all positions before usage. The value is not calculated automatically!
   *
   * @see AuftragDao#calculateInvoicedSum(java.util.Collection)
   */
  @Transient
  public BigDecimal getFakturiertSum()
  {
    return fakturiertSum;
  }

  public AuftragsPositionDO setFakturiertSum(final BigDecimal fakturiertSum)
  {
    this.fakturiertSum = fakturiertSum;
    return this;
  }

  /**
   * @return the timeOfPerformanceBegin
   */
  @Column(name = "period_of_performance_begin")
  public Date getPeriodOfPerformanceBegin()
  {
    return periodOfPerformanceBegin;
  }

  /**
   * @param periodOfPerformanceBegin the periodOfPerformanceBegin to set
   * @return this for chaining.
   */
  public AuftragsPositionDO setPeriodOfPerformanceBegin(final Date periodOfPerformanceBegin)
  {
    this.periodOfPerformanceBegin = periodOfPerformanceBegin;
    return this;
  }

  /**
   * @return the timeOfPerformanceEnd
   */
  @Column(name = "period_of_performance_end")
  public Date getPeriodOfPerformanceEnd()
  {
    return periodOfPerformanceEnd;
  }

  /**
   * @param periodOfPerformanceEnd the periodOfPerformanceEnd to set
   * @return this for chaining.
   */
  public AuftragsPositionDO setPeriodOfPerformanceEnd(final Date periodOfPerformanceEnd)
  {
    this.periodOfPerformanceEnd = periodOfPerformanceEnd;
    return this;
  }

  /**
   * Throws UserException if vollstaendigFakturiert is true and status is not ABGESCHLOSSEN.
   */
  public void checkVollstaendigFakturiert()
  {
    if (vollstaendigFakturiert == true
        && (status == null || status.isIn(AuftragsPositionsStatus.ABGESCHLOSSEN) == false)) {
      throw new UserException(
          "fibu.auftrag.error.nurAbgeschlosseneAuftragsPositionenKoennenVollstaendigFakturiertSein");
    }
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "period_of_performance_type", length = 10)
  public PeriodOfPerformanceType getPeriodOfPerformanceType()
  {
    return periodOfPerformanceType;
  }

  public AuftragsPositionDO setPeriodOfPerformanceType(final PeriodOfPerformanceType periodOfPerformanceType)
  {
    this.periodOfPerformanceType = periodOfPerformanceType;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "mode_of_payment_type", length = 13)
  public ModeOfPaymentType getModeOfPaymentType()
  {
    return modeOfPaymentType;
  }

  public AuftragsPositionDO setModeOfPaymentType(final ModeOfPaymentType modeOfPaymentType)
  {
    this.modeOfPaymentType = modeOfPaymentType;
    return this;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof AuftragsPositionDO) {
      final AuftragsPositionDO other = (AuftragsPositionDO) o;
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

  @Override
  @Transient
  public String getShortDisplayName()
  {
    return (this.auftrag != null ? String.valueOf(this.auftrag.getNummer()) : "???") + "." + String.valueOf(number);
  }

  /**
   * @return Order number and position number: ###.## (&lt;order number&gt;.&lt;position number&gt;)
   */
  @Transient
  public String getFormattedNumber()
  {
    final StringBuffer buf = new StringBuffer();
    if (this.auftrag != null) {
      buf.append(this.auftrag.getNummer());
    }
    buf.append(".").append(this.number);
    return buf.toString();
  }
}
