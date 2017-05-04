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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.IndexColumn;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.projectforge.common.anots.PropertyInfo;

import de.micromata.genome.db.jpa.history.api.WithHistory;

/**
 * Geplante und gestellte Rechnungen.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "t_fibu_rechnung",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "nummer", "tenant_id" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_fibu_rechnung_konto_id", columnList = "konto_id"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_rechnung_kunde_id", columnList = "kunde_id"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_rechnung_projekt_id", columnList = "projekt_id"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_rechnung_tenant_id", columnList = "tenant_id")
    })
@WithHistory(noHistoryProperties = { "lastUpdate", "created" }, nestedEntities = { RechnungsPositionDO.class })
public class RechnungDO extends AbstractRechnungDO<RechnungsPositionDO> implements Comparable<RechnungDO>
{
  private static final long serialVersionUID = 8143023040624332677L;

  @PropertyInfo(i18nKey = "fibu.rechnung.nummer")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO,
      bridge = @FieldBridge(impl = IntegerBridge.class))
  private Integer nummer;

  @PropertyInfo(i18nKey = "fibu.kunde")
  @IndexedEmbedded(depth = 1)
  private KundeDO kunde;

  @PropertyInfo(i18nKey = "fibu.kunde")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String kundeText;

  @PropertyInfo(i18nKey = "fibu.projekt")
  @IndexedEmbedded(depth = 2)
  private ProjektDO projekt;

  @PropertyInfo(i18nKey = "fibu.rechnung.status")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  private RechnungStatus status;

  @PropertyInfo(i18nKey = "fibu.rechnung.typ")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private RechnungTyp typ;

  @PropertyInfo(i18nKey = "fibu.periodOfPerformance.from")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date periodOfPerformanceBegin;

  @PropertyInfo(i18nKey = "fibu.periodOfPerformance.to")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date periodOfPerformanceEnd;

  /**
   * Rechnungsempfänger. Dieser Kunde kann vom Kunden, der mit dem Projekt verbunden ist abweichen.
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "kunde_id", nullable = true)
  public KundeDO getKunde()
  {
    return kunde;
  }

  public RechnungDO setKunde(final KundeDO kunde)
  {
    this.kunde = kunde;
    return this;
  }

  @Transient
  public Integer getKundeId()
  {
    if (this.kunde == null) {
      return null;
    }
    return kunde.getId();
  }

  /**
   * Freitextfeld, falls Kunde nicht aus Liste gewählt werden kann bzw. für Rückwärtskompatibilität mit alten Kunden.
   */
  @Column(name = "kunde_text")
  public String getKundeText()
  {
    return kundeText;
  }

  public RechnungDO setKundeText(final String kundeText)
  {
    this.kundeText = kundeText;
    return this;
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "projekt_id", nullable = true)
  public ProjektDO getProjekt()
  {
    return projekt;
  }

  public RechnungDO setProjekt(final ProjektDO projekt)
  {
    this.projekt = projekt;
    return this;
  }

  @Transient
  public Integer getProjektId()
  {
    if (this.projekt == null) {
      return null;
    }
    return projekt.getId();
  }

  @Column(nullable = true)
  public Integer getNummer()
  {
    return nummer;
  }

  public RechnungDO setNummer(final Integer nummer)
  {
    this.nummer = nummer;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  public RechnungStatus getStatus()
  {
    return status;
  }

  public RechnungDO setStatus(final RechnungStatus status)
  {
    this.status = status;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 40)
  public RechnungTyp getTyp()
  {
    return typ;
  }

  public RechnungDO setTyp(final RechnungTyp typ)
  {
    this.typ = typ;
    return this;
  }

  @Column(name = "period_of_performance_begin")
  public Date getPeriodOfPerformanceBegin()
  {
    return periodOfPerformanceBegin;
  }

  public RechnungDO setPeriodOfPerformanceBegin(final Date periodOfPerformanceBegin)
  {
    this.periodOfPerformanceBegin = periodOfPerformanceBegin;
    return this;
  }

  @Column(name = "period_of_performance_end")
  public Date getPeriodOfPerformanceEnd()
  {
    return periodOfPerformanceEnd;
  }

  public RechnungDO setPeriodOfPerformanceEnd(final Date periodOfPerformanceEnd)
  {
    this.periodOfPerformanceEnd = periodOfPerformanceEnd;
    return this;
  }

  /**
   * (this.status == RechnungStatus.BEZAHLT && this.bezahlDatum != null && this.zahlBetrag != null)
   */
  @Override
  @Transient
  public boolean isBezahlt()
  {
    if (this.getNetSum() == null || this.getNetSum().compareTo(BigDecimal.ZERO) == 0) {
      return true;
    }
    return (this.status == RechnungStatus.BEZAHLT && this.bezahlDatum != null && this.zahlBetrag != null);
  }

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "rechnung")
  @IndexColumn(name = "number", base = 1)
  @Override
  public List<RechnungsPositionDO> getPositionen()
  {
    return this.positionen;
  }

  @Transient
  public Set<AuftragsPositionVO> getAuftragsPositionVOs()
  {
    if (this.positionen == null) {
      return null;
    }
    Set<AuftragsPositionVO> set = null;
    for (final RechnungsPositionDO pos : this.positionen) {
      if (pos.getAuftragsPosition() == null) {
        continue;
      } else if (set == null) {
        set = new TreeSet<AuftragsPositionVO>();
      }
      set.add(new AuftragsPositionVO(pos.getAuftragsPosition()));
    }
    return set;
  }

  /**
   * @see KundeFormatter#formatKundeAsString(KundeDO, String)
   */
  @Transient
  public String getKundeAsString()
  {
    return KundeFormatter.formatKundeAsString(this.kunde, this.kundeText);
  }

  @Override
  public int compareTo(final RechnungDO o)
  {
    if (this.datum != null && o.datum != null) {
      final int r = o.datum.compareTo(this.datum);
      if (r != 0) {
        return r;
      }

    }
    if (this.nummer == null) {
      return (o.nummer == null) ? 0 : 1;
    }
    if (o.nummer == null) {
      return -1;
    }
    return this.nummer.compareTo(o.nummer);
  }
}
