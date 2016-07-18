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
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.projectforge.common.anots.PropertyInfo;

import de.micromata.genome.db.jpa.history.api.WithHistory;

/**
 * Eingehende Rechnungen.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "t_fibu_eingangsrechnung",
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_fibu_eingangsrechnung_konto_id", columnList = "konto_id"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_eingangsrechnung_tenant_id", columnList = "tenant_id")
    })
// @AssociationOverride(name="positionen", joinColumns=@JoinColumn(name="eingangsrechnung_fk"))
@WithHistory(noHistoryProperties = { "lastUpdate", "created" }, nestedEntities = { EingangsrechnungsPositionDO.class })
public class EingangsrechnungDO extends AbstractRechnungDO<EingangsrechnungsPositionDO>
    implements Comparable<EingangsrechnungDO>
{
  private static final long serialVersionUID = -9085180505664149496L;

  @PropertyInfo(i18nKey = "fibu.common.reference")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String referenz;

  @PropertyInfo(i18nKey = "fibu.common.creditor")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String kreditor;

  @PropertyInfo(i18nKey = "fibu.payment.type")
  private PaymentType paymentType;

  @Column(length = 255)
  public String getKreditor()
  {
    return kreditor;
  }

  public void setKreditor(final String kreditor)
  {
    this.kreditor = kreditor;
  }

  /**
   * Referenz / Eingangsrechnungsnummer des Kreditors.
   * 
   * @return
   */
  @Column(length = 1000)
  public String getReferenz()
  {
    return referenz;
  }

  public void setReferenz(final String referenz)
  {
    this.referenz = referenz;
  }

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "eingangsrechnung",
      targetEntity = EingangsrechnungsPositionDO.class)
  @IndexColumn(name = "number", base = 1)
  @Override
  public List<EingangsrechnungsPositionDO> getPositionen()
  {
    return this.positionen;
  }

  /**
   * @return the paymentType
   */
  @Enumerated(EnumType.STRING)
  @Column(length = 20, name = "payment_type")
  public PaymentType getPaymentType()
  {
    return paymentType;
  }

  /**
   * @param paymentType the paymentType to set
   * @return this for chaining.
   */
  public EingangsrechnungDO setPaymentType(final PaymentType paymentType)
  {
    this.paymentType = paymentType;
    return this;
  }

  /**
   * (this.status == EingangsrechnungStatus.BEZAHLT && this.bezahlDatum != null && this.zahlBetrag != null)
   */
  @Override
  @Transient
  public boolean isBezahlt()
  {
    if (this.getNetSum() == null || this.getNetSum().compareTo(BigDecimal.ZERO) == 0) {
      return true;
    }
    return (this.bezahlDatum != null && this.zahlBetrag != null);
  }

  @Override
  public int compareTo(final EingangsrechnungDO o)
  {
    int r = this.datum.compareTo(o.datum);
    if (r != 0) {
      return -r;
    }
    String s1 = StringUtils.defaultString(this.kreditor);
    String s2 = StringUtils.defaultString(o.kreditor);
    r = s1.compareTo(s2);
    if (r != 0) {
      return -r;
    }
    s1 = StringUtils.defaultString(this.referenz);
    s2 = StringUtils.defaultString(o.referenz);
    return s1.compareTo(s2);
  }
}
