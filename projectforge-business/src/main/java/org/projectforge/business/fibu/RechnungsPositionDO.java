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

import java.sql.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Resolution;
import org.projectforge.business.fibu.kost.KostZuweisungDO;

/**
 * Repräsentiert eine Position innerhalb eine Rechnung.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "t_fibu_rechnung_position",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "rechnung_fk", "number" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_fibu_rechnung_position_auftrags_position_fk",
            columnList = "auftrags_position_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_rechnung_position_rechnung_fk", columnList = "rechnung_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_rechnung_position_tenant_id", columnList = "tenant_id")
    })
public class RechnungsPositionDO extends AbstractRechnungsPositionDO
{
  private static final long serialVersionUID = 3889773820456424008L;

  private RechnungDO rechnung;

  @IndexedEmbedded(depth = 1)
  private AuftragsPositionDO auftragsPosition;

  private PeriodOfPerformanceType periodOfPerformanceType = PeriodOfPerformanceType.SEEABOVE;

  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date periodOfPerformanceBegin;

  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date periodOfPerformanceEnd;

  @Override
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "rechnung_fk", nullable = false)
  public RechnungDO getRechnung()
  {
    return rechnung;
  }

  @Override
  protected RechnungsPositionDO setRechnung(final AbstractRechnungDO<?> rechnung)
  {
    this.rechnung = (RechnungDO) rechnung;
    return this;
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "auftrags_position_fk")
  public AuftragsPositionDO getAuftragsPosition()
  {
    return auftragsPosition;
  }

  public RechnungsPositionDO setAuftragsPosition(final AuftragsPositionDO auftragsPosition)
  {
    this.auftragsPosition = auftragsPosition;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "period_of_performance_type", length = 10)
  public PeriodOfPerformanceType getPeriodOfPerformanceType()
  {
    return periodOfPerformanceType;
  }

  public RechnungsPositionDO setPeriodOfPerformanceType(final PeriodOfPerformanceType periodOfPerformanceType)
  {
    this.periodOfPerformanceType = periodOfPerformanceType;
    return this;
  }

  @Column(name = "period_of_performance_begin")
  public Date getPeriodOfPerformanceBegin()
  {
    return periodOfPerformanceBegin;
  }

  public RechnungsPositionDO setPeriodOfPerformanceBegin(final Date periodOfPerformanceBegin)
  {
    this.periodOfPerformanceBegin = periodOfPerformanceBegin;
    return this;
  }

  @Column(name = "period_of_performance_end")
  public Date getPeriodOfPerformanceEnd()
  {
    return periodOfPerformanceEnd;
  }

  public RechnungsPositionDO setPeriodOfPerformanceEnd(final Date periodOfPerformanceEnd)
  {
    this.periodOfPerformanceEnd = periodOfPerformanceEnd;
    return this;
  }

  @Override
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "rechnungs_pos_fk")
  @OrderColumn(name = "index")
  public List<KostZuweisungDO> getKostZuweisungen()
  {
    return kostZuweisungen;
  }

  @Transient
  @Override
  protected void setThis(KostZuweisungDO kostZuweisung)
  {
    kostZuweisung.setRechnungsPosition(this);
  }

  @Override
  protected AbstractRechnungsPositionDO newInstance()
  {
    return new RechnungsPositionDO();
  }
}
