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
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import java.util.Objects;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.fibu.EingangsrechnungsPositionDO;
import org.projectforge.business.fibu.EmployeeSalaryDO;
import org.projectforge.business.fibu.RechnungsPositionDO;
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.utils.Constants;
import org.projectforge.framework.utils.CurrencyHelper;

/**
 * Rechnungen (Ein- und Ausgang) sowie Gehaltssonderzahlungen werden auf Kost1 und Kost2 aufgeteilt. Einer Rechnung
 * können mehrere KostZuweisungen zugeordnet sein. Die Summe aller Einzelkostzuweisung sollte dem Betrag der
 * Rechnung/Gehaltszahlung entsprechen.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_FIBU_KOST_ZUWEISUNG",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "index", "rechnungs_pos_fk", "kost1_fk", "kost2_fk" }),
        @UniqueConstraint(columnNames = { "index", "eingangsrechnungs_pos_fk", "kost1_fk", "kost2_fk" }),
        @UniqueConstraint(columnNames = { "index", "employee_salary_fk", "kost1_fk", "kost2_fk" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_fibu_kost_zuweisung_eingangsrechnungs_pos_fk",
            columnList = "eingangsrechnungs_pos_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_kost_zuweisung_employee_salary_fk",
            columnList = "employee_salary_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_kost_zuweisung_kost1_fk", columnList = "kost1_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_kost_zuweisung_kost2_fk", columnList = "kost2_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_kost_zuweisung_rechnungs_pos_fk",
            columnList = "rechnungs_pos_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_kost_zuweisung_tenant_id", columnList = "tenant_id")
    })
public class KostZuweisungDO extends DefaultBaseDO implements ShortDisplayNameCapable
{
  private static final long serialVersionUID = 7680349296575044993L;

  private short index;

  private BigDecimal netto;

  @IndexedEmbedded(depth = 1)
  private Kost1DO kost1;

  @IndexedEmbedded(depth = 1)
  private Kost2DO kost2;

  @IndexedEmbedded(depth = 1)
  private RechnungsPositionDO rechnungsPosition;

  @IndexedEmbedded(depth = 1)
  private EingangsrechnungsPositionDO eingangsrechnungsPosition;

  @IndexedEmbedded(depth = 1)
  private EmployeeSalaryDO employeeSalary;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String comment;

  /**
   * Die Kostzuweisungen sind als Array organisiert. Dies stellt den Index der Kostzuweisung dar. Der Index ist für
   * Gehaltszahlungen ohne Belang.
   *
   * @return
   */
  @Column
  public short getIndex()
  {
    return index;
  }

  public void setIndex(final short index)
  {
    this.index = index;
  }

  @Column(scale = 2, precision = 12)
  public BigDecimal getNetto()
  {
    return netto;
  }

  /**
   * @param netto
   * @return this for chaining.
   */
  public KostZuweisungDO setNetto(final BigDecimal netto)
  {
    this.netto = netto;
    return this;
  }

  /**
   * Calculates gross amount using the vat from the invoice position.
   *
   * @return Gross amount if vat found otherwise net amount.
   * @see #getRechnungsPosition()
   * @see #getEingangsrechnungsPosition()
   */
  @Transient
  public BigDecimal getBrutto()
  {
    final BigDecimal vat;
    if (this.rechnungsPosition != null) {
      vat = this.rechnungsPosition.getVat();
    } else if (this.eingangsrechnungsPosition != null) {
      vat = this.eingangsrechnungsPosition.getVat();
    } else {
      vat = null;
    }
    return CurrencyHelper.getGrossAmount(this.netto, vat);
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "kost1_fk", nullable = false)
  public Kost1DO getKost1()
  {
    return kost1;
  }

  /**
   * @param kost1
   * @return this for chaining.
   */
  public KostZuweisungDO setKost1(final Kost1DO kost1)
  {
    this.kost1 = kost1;
    return this;
  }

  @Transient
  public Integer getKost1Id()
  {
    if (this.kost1 == null) {
      return null;
    }
    return kost1.getId();
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "kost2_fk", nullable = false)
  public Kost2DO getKost2()
  {
    return kost2;
  }

  /**
   * @param kost2
   * @return this for chaining.
   */
  public KostZuweisungDO setKost2(final Kost2DO kost2)
  {
    this.kost2 = kost2;
    return this;
  }

  @Transient
  public Integer getKost2Id()
  {
    if (this.kost2 == null) {
      return null;
    }
    return kost2.getId();
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "rechnungs_pos_fk", nullable = true)
  public RechnungsPositionDO getRechnungsPosition()
  {
    return rechnungsPosition;
  }

  /**
   * @param rechnungsPosition
   * @return this for chaining.
   * @throws IllegalStateException if eingangsRechnung or employeeSalary is already given.
   */
  public KostZuweisungDO setRechnungsPosition(final RechnungsPositionDO rechnungsPosition)
  {
    if (rechnungsPosition != null && (this.eingangsrechnungsPosition != null || this.employeeSalary != null)) {
      throw new IllegalStateException("eingangsRechnung or employeeSalary already given!");
    }
    this.rechnungsPosition = rechnungsPosition;
    return this;
  }

  @Transient
  public Integer getRechnungsPositionId()
  {
    if (this.rechnungsPosition == null) {
      return null;
    }
    return rechnungsPosition.getId();
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "eingangsrechnungs_pos_fk", nullable = true)
  public EingangsrechnungsPositionDO getEingangsrechnungsPosition()
  {
    return eingangsrechnungsPosition;
  }

  /**
   * @param eingangsrechnung
   * @return this for chaining.
   * @throws IllegalStateException if rechnung or employeeSalary is already given.
   */
  public KostZuweisungDO setEingangsrechnungsPosition(final EingangsrechnungsPositionDO eingangsrechnungsPosition)
  {
    if (eingangsrechnungsPosition != null && (this.rechnungsPosition != null || this.employeeSalary != null)) {
      throw new IllegalStateException("rechnungsPosition or employeeSalary already given!");
    }
    this.eingangsrechnungsPosition = eingangsrechnungsPosition;
    return this;
  }

  @Transient
  public Integer getEingangsrechnungsPositionId()
  {
    if (this.eingangsrechnungsPosition == null) {
      return null;
    }
    return eingangsrechnungsPosition.getId();
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "employee_salary_fk", nullable = true)
  public EmployeeSalaryDO getEmployeeSalary()
  {
    return employeeSalary;
  }

  /**
   * @param employeeSalary
   * @return this for chaining.
   * @throws IllegalStateException if rechnung or eingangsRechnung is already given.
   */
  public KostZuweisungDO setEmployeeSalary(final EmployeeSalaryDO employeeSalary)
  {
    if (employeeSalary != null && (this.eingangsrechnungsPosition != null || this.rechnungsPosition != null)) {
      throw new IllegalStateException("eingangsRechnung or rechnungsPosition already given!");
    }
    this.employeeSalary = employeeSalary;
    return this;
  }

  @Transient
  public Integer getEmployeeSalaryId()
  {
    if (this.employeeSalary == null) {
      return null;
    }
    return employeeSalary.getId();
  }

  @Column(length = Constants.COMMENT_LENGTH)
  public String getComment()
  {
    return comment;
  }

  /**
   * @param comment
   * @return this for chaining.
   */
  public KostZuweisungDO setComment(final String comment)
  {
    this.comment = comment;
    return this;
  }

  /**
   * @return true if betrag is zero or not given.
   */
  @Transient
  public boolean isEmpty()
  {
    return netto == null || netto.compareTo(BigDecimal.ZERO) == 0;
  }

  /**
   * If empty then no error will be returned.
   *
   * @return error message (i18n key) or null if no error is given.
   */
  @Transient
  public String hasErrors()
  {
    if (isEmpty() == true) {
      return null;
    }
    int counter = 0;
    if (getRechnungsPositionId() != null) {
      counter++;
    }
    if (getEingangsrechnungsPositionId() != null) {
      counter++;
    }
    if (getEmployeeSalaryId() != null) {
      counter++;
    }
    if (counter != 1) {
      return "fibu.kostZuweisung.error.genauEinFinanzobjektErwartet"; // i18n key
    }
    return null;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof KostZuweisungDO) {
      final KostZuweisungDO other = (KostZuweisungDO) o;
      if (Objects.equals(this.getIndex(), other.getIndex()) == false) {
        return false;
      }
      if (Objects.equals(this.getRechnungsPositionId(), other.getRechnungsPositionId()) == false) {
        return false;
      }
      if (Objects.equals(this.getEingangsrechnungsPositionId(), other.getEingangsrechnungsPositionId()) == false) {
        return false;
      }
      if (Objects.equals(this.getEmployeeSalaryId(), other.getEmployeeSalaryId()) == false) {
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
    hcb.append(getIndex());
    if (getRechnungsPosition() != null) {
      hcb.append(getRechnungsPositionId());
    }
    if (getEingangsrechnungsPosition() != null) {
      hcb.append(getEingangsrechnungsPositionId());
    }
    if (getEmployeeSalary() != null) {
      hcb.append(getEmployeeSalaryId());
    }
    return hcb.toHashCode();
  }

  @Override
  @Transient
  public String getShortDisplayName()
  {
    return String.valueOf(index);
  }

  /**
   * Clones this cost assignment (without id's).
   *
   * @return
   */
  public KostZuweisungDO newClone()
  {
    final KostZuweisungDO kostZuweisung = new KostZuweisungDO();
    kostZuweisung.copyValuesFrom(this, "id");
    return kostZuweisung;
  }
}
