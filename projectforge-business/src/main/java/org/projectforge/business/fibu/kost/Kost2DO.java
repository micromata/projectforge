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

package org.projectforge.business.fibu.kost;

import java.math.BigDecimal;

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

import java.util.Objects;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.genome.db.jpa.history.api.WithHistory;

@Entity
@Indexed
@ClassBridge(name = "nummer", index = Index.YES /* TOKENIZED */, store = Store.NO,
    impl = HibernateSearchKost2Bridge.class)
@Table(name = "T_FIBU_KOST2",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "nummernkreis", "bereich", "teilbereich", "kost2_art_id", "tenant_id" }),
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_fibu_kost2_kost2_art_id", columnList = "kost2_art_id"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_kost2_projekt_id", columnList = "projekt_id"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_kost2_tenant_id", columnList = "tenant_id")
    })
@WithHistory
public class Kost2DO extends DefaultBaseDO implements ShortDisplayNameCapable, Comparable<Kost2DO>
{
  private static final long serialVersionUID = -6534347300453425760L;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private KostentraegerStatus kostentraegerStatus;

  private int nummernkreis;

  private int bereich;

  private int teilbereich;

  private Kost2ArtDO kost2Art;

  private BigDecimal workFraction;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String description;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String comment;

  @IndexedEmbedded(depth = 2)
  private ProjektDO projekt;

  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  public KostentraegerStatus getKostentraegerStatus()
  {
    return kostentraegerStatus;
  }

  public Kost2DO setKostentraegerStatus(final KostentraegerStatus kostentraegerStatus)
  {
    this.kostentraegerStatus = kostentraegerStatus;
    return this;
  }

  /**
   * @see KostFormatter#getKostAsInt(int, int, int, int)
   */
  @Transient
  public Integer getNummer()
  {
    return KostFormatter.getKostAsInt(nummernkreis, bereich, teilbereich, kost2Art.getId());
  }

  /**
   * @see KostFormatter#format(Kost2DO)
   * @see org.projectforge.framework.persistence.api.ShortDisplayNameCapable#getShortDisplayName()
   */
  @Override
  @Transient
  public String getShortDisplayName()
  {
    return KostFormatter.format(this);
  }

  /**
   * Format: #.###.##.##
   * 
   * @see KostFormatter#format(Kost2DO)
   */
  @Transient
  public String getFormattedNumber()
  {
    return KostFormatter.format(this);
  }

  /**
   * @see KostFormatter#formatToolTip(Kost2DO)
   */
  @Transient
  public String getToolTip()
  {
    return KostFormatter.formatToolTip(this);
  }

  @Transient
  public boolean isEqual(final int nummernkreis, final int bereich, final int teilbereich, final int kost2Art)
  {
    return this.nummernkreis == nummernkreis
        && this.bereich == bereich
        && this.teilbereich == teilbereich
        && this.kost2Art.getId() == kost2Art;
  }

  /**
   * Nummernkreis entspricht der ersten Ziffer.
   * 
   * @return
   */
  @Column(name = "nummernkreis")
  public int getNummernkreis()
  {
    return nummernkreis;
  }

  public Kost2DO setNummernkreis(final int nummernkreis)
  {
    this.nummernkreis = nummernkreis;
    return this;
  }

  /**
   * Bereich entspricht der 2.-4. Ziffer.
   * 
   * @return
   */
  @Column(name = "bereich")
  public int getBereich()
  {
    return bereich;
  }

  public Kost2DO setBereich(final int bereich)
  {
    this.bereich = bereich;
    return this;
  }

  /**
   * Teilbereich entspricht der 5.-6. Ziffer.
   * 
   * @return
   */
  @Column(name = "teilbereich")
  public int getTeilbereich()
  {
    return teilbereich;
  }

  public Kost2DO setTeilbereich(final int teilbereich)
  {
    this.teilbereich = teilbereich;
    return this;
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "kost2_art_id", nullable = false)
  public Kost2ArtDO getKost2Art()
  {
    return kost2Art;
  }

  public Kost2DO setKost2Art(final Kost2ArtDO kost2Art)
  {
    this.kost2Art = kost2Art;
    return this;
  }

  @Transient
  public Integer getKost2ArtId()
  {
    if (this.kost2Art == null) {
      return null;
    }
    return kost2Art.getId();
  }

  @Column(name = "work_fraction", scale = 5, precision = 10)
  public BigDecimal getWorkFraction()
  {
    return workFraction;
  }

  public Kost2DO setWorkFraction(final BigDecimal workFraction)
  {
    this.workFraction = workFraction;
    return this;
  }

  @Column(length = 4000)
  public String getDescription()
  {
    return description;
  }

  public Kost2DO setDescription(final String description)
  {
    this.description = description;
    return this;
  }

  /**
   * Optionale Kommentare zum Kostenträger.
   * 
   * @return
   */
  @Column(length = 4000)
  public String getComment()
  {
    return comment;
  }

  public Kost2DO setComment(final String comment)
  {
    this.comment = comment;
    return this;
  }

  /**
   * Projekt kann gegeben sein. Wenn Kostenträger zu einem Projekt hinzugehört, dann sind auf jeden Fall die ersten 6
   * Ziffern identisch mit der Projektnummer.
   * 
   * @return
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "projekt_id")
  public ProjektDO getProjekt()
  {
    return projekt;
  }

  public Kost2DO setProjekt(final ProjektDO projekt)
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

  /**
   * return true if nummernkreis, bereich, teilbereich and kost2Art is equal, otherwise false;
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof Kost2DO) {
      final Kost2DO other = (Kost2DO) o;
      if (this.nummernkreis == other.nummernkreis && this.bereich == other.bereich
          && this.teilbereich == other.teilbereich) {
        return Objects.equals(this.kost2Art, other.kost2Art);
      }
    }
    return false;
  }

  /**
   * Uses HashCodeBuilder with property nummernkreis, bereich, teilbereich and kost2Art.
   * 
   * @see java.lang.Object#hashCode()
   * @see HashCodeBuilder#append(int)
   * @see HashCodeBuilder#append(Object)
   */
  @Override
  public int hashCode()
  {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(this.nummernkreis).append(this.bereich).append(this.teilbereich).append(this.kost2Art);
    return hcb.toHashCode();
  }

  /**
   * Compares shortDisplayName.
   * 
   * @see #getShortDisplayName()
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final Kost2DO o)
  {
    return this.getShortDisplayName().compareTo(o.getShortDisplayName());
  }
}
