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

package org.projectforge.business.fibu.kost;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.genome.db.jpa.history.api.WithHistory;

@Entity
@Indexed
@ClassBridge(name = "nummer", index = Index.YES /* TOKENIZED */, store = Store.NO,
    impl = HibernateSearchKost1Bridge.class)
@Table(name = "T_FIBU_KOST1", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "nummernkreis", "bereich", "teilbereich", "endziffer", "tenant_id" }) },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_fibu_kost1_tenant_id", columnList = "tenant_id")
    })
@WithHistory
public class Kost1DO extends DefaultBaseDO implements ShortDisplayNameCapable
{
  private static final long serialVersionUID = -6534347300453425760L;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  private KostentraegerStatus kostentraegerStatus;

  private int nummernkreis;

  private int bereich;

  private int teilbereich;

  private int endziffer;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  public KostentraegerStatus getKostentraegerStatus()
  {
    return kostentraegerStatus;
  }

  public void setKostentraegerStatus(final KostentraegerStatus kostentraegerStatus)
  {
    this.kostentraegerStatus = kostentraegerStatus;
  }

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
   * @see KostFormatter#getKostAsInt(int, int, int, int)
   */
  @Transient
  public Integer getNummer()
  {
    return KostFormatter.getKostAsInt(nummernkreis, bereich, teilbereich, endziffer);
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

  public void setNummernkreis(final int bereich)
  {
    this.nummernkreis = bereich;
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

  public void setBereich(final int bereich)
  {
    this.bereich = bereich;
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

  public void setTeilbereich(final int teilbereich)
  {
    this.teilbereich = teilbereich;
  }

  @Column(name = "endziffer")
  public int getEndziffer()
  {
    return endziffer;
  }

  public void setEndziffer(final int endziffer)
  {
    this.endziffer = endziffer;
  }

  /**
   * Optionale Kommentare zum Kostenträger.
   *
   * @return
   */
  @Column(length = 4000)
  public String getDescription()
  {
    return description;
  }

  public void setDescription(final String description)
  {
    this.description = description;
  }

  /**
   * return true if nummernkreis, bereich, teilbereich and endziffer is equal, otherwise false;
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */

  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof Kost1DO) {
      final Kost1DO other = (Kost1DO) o;
      return (this.nummernkreis == other.nummernkreis && this.bereich == other.bereich
          && this.teilbereich == other.teilbereich && this.endziffer == other.endziffer);
    }
    return false;
  }

  /**
   * Uses HashCodeBuilder with property nummernkreis, bereich, teilbereich and endziffer.
   *
   * @see java.lang.Object#hashCode()
   * @see HashCodeBuilder#append(int)
   */
  @Override
  public int hashCode()
  {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(this.nummernkreis).append(this.bereich).append(this.teilbereich).append(this.endziffer);
    return hcb.toHashCode();
  }
}
