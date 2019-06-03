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

import com.fasterxml.jackson.annotation.JsonBackReference;
import de.micromata.genome.db.jpa.history.api.WithHistory;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.*;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.projectforge.common.anots.PropertyInfo;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

  @PropertyInfo(i18nKey = "fibu.customerref1")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String customerref1;

  @PropertyInfo(i18nKey = "fibu.attachment")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String attachment;

  @PropertyInfo(i18nKey = "fibu.customer.address")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String customerAddress;

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

  @Column(name = "customerref1")
  public String getCustomerref1()
  {
    return customerref1;
  }

  public RechnungDO setCustomerref1(final String customerref1)
  {
    this.customerref1 = customerref1;
    return this;
  }

  @Column(name = "attachment")
  public String getAttachment()
  {
    return attachment;
  }

  public RechnungDO setAttachment(final String attachment)
  {
    this.attachment = attachment;
    return this;
  }

  @Column(name = "customeraddress")
  public String getCustomerAddress()
  {
    return customerAddress;
  }

  public RechnungDO setCustomerAddress(final String customerAddress)
  {
    this.customerAddress = customerAddress;
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
    return (this.status == RechnungStatus.BEZAHLT && this.getBezahlDatum() != null && this.getZahlBetrag() != null);
  }

  @JsonBackReference
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "rechnung")
  @IndexColumn(name = "number", base = 1)
  @Override
  public List<RechnungsPositionDO> getPositionen()
  {
    return super.getPositionen();
  }

  @Transient
  public Set<AuftragsPositionVO> getAuftragsPositionVOs()
  {
    if (this.getPositionen() == null) {
      return null;
    }
    Set<AuftragsPositionVO> set = null;
    for (final RechnungsPositionDO pos : this.getPositionen()) {
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
    if (this.getDatum() != null && o.getDatum() != null) {
      final int r = o.getDatum().compareTo(this.getDatum());
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
