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

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.util.Objects;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.projectforge.framework.persistence.api.IManualIndex;
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO;

/**
 * Die letzten beiden Ziffern (Endziffern) eines Kostenträgers repräsentieren die Kostenart. Anhand der Endziffer kann
 * abgelesen werden, um welche Art von Kostenträger es sich handelt (fakturiert/nicht fakturiert, Akquise, Wartung etc.)
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_FIBU_KOST2ART", indexes = {
    @javax.persistence.Index(name = "idx_fk_t_fibu_kost2art_tenant_id", columnList = "tenant_id")
})
@Analyzer(impl = ClassicAnalyzer.class)
public class Kost2ArtDO extends AbstractHistorizableBaseDO<Integer> implements Comparable<Kost2ArtDO>, IManualIndex
{
  private static final long serialVersionUID = 2398122998160436266L;

  /**
   * Zweistellige Endziffer von KOST2
   */
  private Integer id;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String name;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String description;

  private boolean fakturiert;

  private BigDecimal workFraction;

  private boolean projektStandard;

  /**
   * Zweistellige Endziffer von KOST2
   */
  @Override
  @Id
  @Column(name = "pk")
  public Integer getId()
  {
    return id;
  }

  /**
   * Muss größer als 0 und kleiner als 100 sein, sonst wird ein Validierungsfehler geworfen.
   *
   * @param nummer
   */
  @Override
  public void setId(final Integer id)
  {
    this.id = id;
  }

  public Kost2ArtDO withId(final Integer id)
  {
    setId(id);
    return this;
  }

  @Column(length = 255, nullable = false)
  public String getName()
  {
    return name;
  }

  public Kost2ArtDO setName(final String name)
  {
    this.name = name;
    return this;
  }

  @Column(length = 5000)
  public String getDescription()
  {
    return description;
  }

  public Kost2ArtDO setDescription(final String description)
  {
    this.description = description;
    return this;
  }

  /**
   * Werden die Aufwendungen nach außen fakturiert, d. h. stehen den Ausgaben auch Einnahmen entgegen (i. d. R.
   * Kundenrechnungen oder Fördermaßnahmen).
   *
   * @return
   */
  @Column(nullable = false)
  public boolean isFakturiert()
  {
    return fakturiert;
  }

  public Kost2ArtDO setFakturiert(final boolean fakturiert)
  {
    this.fakturiert = fakturiert;
    return this;
  }

  @Column(name = "work_fraction", scale = 5, precision = 10)
  public BigDecimal getWorkFraction()
  {
    return workFraction;
  }

  public Kost2ArtDO setWorkFraction(final BigDecimal workFraction)
  {
    this.workFraction = workFraction;
    return this;
  }

  /**
   * Wenn true, dann wird diese Kostenart für Projekte als Standardendziffer für Kostenträger vorgeschlagen.
   */
  @Column(name = "projekt_standard")
  public boolean isProjektStandard()
  {
    return projektStandard;
  }

  public Kost2ArtDO setProjektStandard(final boolean projektStandard)
  {
    this.projektStandard = projektStandard;
    return this;
  }

  /**
   * return true if id is equal, otherwise false;
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof Kost2ArtDO) {
      final Kost2ArtDO other = (Kost2ArtDO) o;
      return (Objects.equals(this.id, other.id));
    }
    return false;
  }

  /**
   * Uses HashCodeBuilder with property id.
   *
   * @see java.lang.Object#hashCode()
   * @see HashCodeBuilder#append(int)
   */
  @Override
  public int hashCode()
  {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(this.id);
    return hcb.toHashCode();
  }

  @Override
  public int compareTo(final Kost2ArtDO o)
  {
    return id.compareTo(o.id);
  }
}
