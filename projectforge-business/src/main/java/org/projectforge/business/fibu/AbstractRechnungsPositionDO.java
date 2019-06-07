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

package org.projectforge.business.fibu;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.apache.commons.collections.CollectionUtils;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.projectforge.business.fibu.kost.KostZuweisungDO;
import org.projectforge.framework.persistence.api.PFPersistancyBehavior;
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.utils.CurrencyHelper;
import org.projectforge.framework.utils.NumberHelper;

/**
 * Repräsentiert eine Position innerhalb eine Rechnung.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@MappedSuperclass
public abstract class AbstractRechnungsPositionDO extends DefaultBaseDO implements ShortDisplayNameCapable
{
  private static final long serialVersionUID = 4132530394057069876L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(AbstractRechnungsPositionDO.class);

  protected short number;

  protected String text;

  protected BigDecimal menge;

  protected BigDecimal einzelNetto;

  protected BigDecimal vat;

  @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
  protected List<KostZuweisungDO> kostZuweisungen = null;

  @Column
  public short getNumber()
  {
    return number;
  }

  public AbstractRechnungsPositionDO setNumber(final short number)
  {
    this.number = number;
    return this;
  }

  @Column(scale = 5, precision = 18)
  public BigDecimal getMenge()
  {
    return menge;
  }

  public AbstractRechnungsPositionDO setMenge(final BigDecimal menge)
  {
    this.menge = menge;
    return this;
  }

  @Column(name = "einzel_netto", scale = 2, precision = 18)
  public BigDecimal getEinzelNetto()
  {
    return einzelNetto;
  }

  public AbstractRechnungsPositionDO setEinzelNetto(final BigDecimal einzelNetto)
  {
    this.einzelNetto = einzelNetto;
    return this;
  }

  @Column(scale = 5, precision = 10)
  public BigDecimal getVat()
  {
    return vat;
  }

  public AbstractRechnungsPositionDO setVat(final BigDecimal vat)
  {
    if (vat != null) {
      this.vat = vat.stripTrailingZeros();
    } else {
      this.vat = null;
    }
    return this;
  }

  @Transient
  public BigDecimal getNetSum()
  {
    if (this.menge != null) {
      if (this.einzelNetto != null) {
        return CurrencyHelper.multiply(this.menge, this.einzelNetto);
      } else {
        return BigDecimal.ZERO;
      }
    } else {
      return this.einzelNetto != null ? this.einzelNetto : BigDecimal.ZERO;
    }
  }

  @Transient
  public BigDecimal getBruttoSum()
  {
    final BigDecimal netSum = getNetSum();
    if (vat != null) {
      return netSum.add(CurrencyHelper.multiply(netSum, vat));
    } else {
      return netSum;
    }
  }

  @Transient
  public BigDecimal getVatAmount()
  {
    final BigDecimal netSum = getNetSum();
    if (vat != null) {
      return CurrencyHelper.multiply(netSum, vat);
    } else {
      return BigDecimal.ZERO;
    }
  }

  @Column(name = "s_text", length = 1000)
  public String getText()
  {
    return text;
  }

  public AbstractRechnungsPositionDO setText(final String text)
  {
    this.text = text;
    return this;
  }

  /**
   * Get the position entries for this object.
   */
  @Transient
  public abstract List<KostZuweisungDO> getKostZuweisungen();

  public AbstractRechnungsPositionDO setKostZuweisungen(final List<KostZuweisungDO> kostZuweisungen)
  {
    this.kostZuweisungen = kostZuweisungen;
    return this;
  }

  /**
   * @param idx Index of the cost assignment not index of collection.
   * @return KostZuweisungDO with given index or null, if not exist.
   */
  public KostZuweisungDO getKostZuweisung(final int index)
  {
    if (kostZuweisungen == null) {
      log.error("Can't get cost assignment with index " + index + " because no cost assignments given.");
      return null;
    }
    for (final KostZuweisungDO zuweisung : kostZuweisungen) {
      if (index == zuweisung.getIndex()) {
        return zuweisung;
      }
    }
    log.error("Can't found cost assignment with index " + index);
    return null;
  }

  public AbstractRechnungsPositionDO addKostZuweisung(final KostZuweisungDO kostZuweisung)
  {
    ensureAndGetKostzuweisungen();
    short index = 0;
    for (final KostZuweisungDO zuweisung : kostZuweisungen) {
      if (zuweisung.getIndex() >= index) {
        index = zuweisung.getIndex();
        index++;
      }
    }
    kostZuweisung.setIndex(index);
    setThis(kostZuweisung);
    this.kostZuweisungen.add(kostZuweisung);
    return this;
  }

  /**
   * @return The total net sum of all assigned cost entries multiplied with the vat of this position.
   */
  @Transient
  public BigDecimal getKostZuweisungGrossSum()
  {
    return CurrencyHelper.getGrossAmount(getKostZuweisungsNetSum(), vat);
  }

  /**
   * kostZuweisung.setEingangsrechnungsPosition(this);
   *
   * @param kostZuweisung
   */
  protected abstract void setThis(final KostZuweisungDO kostZuweisung);

  protected abstract AbstractRechnungsPositionDO newInstance();

  /**
   * this.getEingangsrechnung()
   */
  @Transient
  protected abstract AbstractRechnungDO<?> getRechnung();

  @Transient
  public Integer getRechnungId()
  {
    if (getRechnung() == null) {
      return null;
    } else {
      return getRechnung().getId();
    }
  }

  /**
   * setEingangsrechnung(rechnung)
   */
  protected abstract AbstractRechnungsPositionDO setRechnung(final AbstractRechnungDO<?> rechnung);

  /**
   * Does only work for not already persisted entries (meaning entries without an id / pk) and only the last entry of
   * the list. Otherwise this method logs an error message and do nothing else.
   *
   * @param idx
   * @see #isKostZuweisungDeletable(KostZuweisungDO)
   */
  public AbstractRechnungsPositionDO deleteKostZuweisung(final int idx)
  {
    final KostZuweisungDO zuweisung = getKostZuweisung(idx);
    if (zuweisung == null) {
      return this;
    }
    if (isKostZuweisungDeletable(zuweisung) == false) {
      log
          .error(
              "Deleting of cost assignements which are already persisted (a id / pk already exists) or not are not the last entry is not supported. Do nothing.");
      return this;
    }
    this.kostZuweisungen.remove(zuweisung);
    return this;
  }

  /**
   * Only the last entry of cost assignments is deletable if not already persisted (no id/pk given).
   *
   * @param zuweisung
   * @return
   */
  public boolean isKostZuweisungDeletable(final KostZuweisungDO zuweisung)
  {
    if (zuweisung == null) {
      return false;
    }
    if (this instanceof EingangsrechnungsPositionDO && Objects.equals(zuweisung.getEingangsrechnungsPositionId(), this.getId()) == false
        || this instanceof RechnungsPositionDO && Objects.equals(zuweisung.getRechnungsPositionId(), this.getId()) == false) {
      log.error("Oups, given cost assignment is not assigned to this invoice position.");
      return false;
    }
    if (zuweisung.getId() != null) {
      return false;
    }
    // if (zuweisung.getIndex() + 1 < this.kostZuweisungen.size()) {
    // return false;
    // }
    return true;
  }

  public List<KostZuweisungDO> ensureAndGetKostzuweisungen()
  {
    if (this.kostZuweisungen == null) {
      setKostZuweisungen(new ArrayList<KostZuweisungDO>());
    }
    return getKostZuweisungen();
  }

  /**
   * @return The net value as sum of all cost assignements.
   */
  @Transient
  public BigDecimal getKostZuweisungsNetSum()
  {
    BigDecimal sum = BigDecimal.ZERO;
    if (CollectionUtils.isNotEmpty(this.kostZuweisungen) == true) {
      for (final KostZuweisungDO zuweisung : this.kostZuweisungen) {
        sum = NumberHelper.add(sum, zuweisung.getNetto());
      }
    }
    return sum;
  }

  @Transient
  public BigDecimal getKostZuweisungNetFehlbetrag()
  {
    return getKostZuweisungsNetSum().subtract(getNetSum());
  }

  /**
   * Clones this including cost assignments and order position (without id's).
   *
   * @return
   */
  public AbstractRechnungsPositionDO newClone()
  {
    final AbstractRechnungsPositionDO rechnungsPosition = newInstance();
    rechnungsPosition.copyValuesFrom(this, "id", "kostZuweisungen");
    if (this.getKostZuweisungen() != null) {
      for (final KostZuweisungDO origKostZuweisung : this.getKostZuweisungen()) {
        final KostZuweisungDO kostZuweisung = origKostZuweisung.newClone();
        rechnungsPosition.addKostZuweisung(kostZuweisung);
      }
    }
    return rechnungsPosition;
  }

  @Transient
  public boolean isEmpty()
  {
    if (StringUtils.isBlank(text) == false) {
      return false;
    }
    return (NumberHelper.isNotZero(einzelNetto) == false);
  }

  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof AbstractRechnungsPositionDO) {
      final AbstractRechnungsPositionDO other = (AbstractRechnungsPositionDO) o;
      if (Objects.equals(this.getNumber(), other.getNumber()) == false) {
        return false;
      }
      if (Objects.equals(this.getRechnungId(), other.getRechnungId()) == false) {
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
    if (getRechnung() != null) {
      hcb.append(getRechnung().getId());
    }
    return hcb.toHashCode();
  }

  @Override
  @Transient
  public String getShortDisplayName()
  {
    return String.valueOf(number);
  }
}
