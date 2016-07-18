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

package org.projectforge.plugins.liquidityplanning;

import java.io.Serializable;

import org.projectforge.business.fibu.AmountType;
import org.projectforge.business.fibu.PaymentStatus;
import org.projectforge.framework.persistence.api.BaseSearchFilter;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class LiquidityFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = -6069385642823972160L;

  private int nextDays;

  private PaymentStatus paymentStatus = PaymentStatus.ALL;

  private AmountType amountType = AmountType.ALL;

  public LiquidityFilter()
  {
  }

  public LiquidityFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }

  /**
   * @return the nextDays
   */
  public int getNextDays()
  {
    return nextDays;
  }

  /**
   * @param nextDays the nextDays to set
   * @return this for chaining.
   */
  public LiquidityFilter setNextDays(final int nextDays)
  {
    this.nextDays = nextDays;
    return this;
  }

  /**
   * @return the paymentStatus
   */
  public PaymentStatus getPaymentStatus()
  {
    return paymentStatus;
  }

  /**
   * @param paymentStatus the paymentStatus to set
   * @return this for chaining.
   */
  public LiquidityFilter setPaymentStatus(final PaymentStatus paymentStatus)
  {
    if (paymentStatus == null) {
      this.paymentStatus = PaymentStatus.ALL;
    } else {
      this.paymentStatus = paymentStatus;
    }
    return this;
  }

  /**
   * @return the amountType
   */
  public AmountType getAmountType()
  {
    return amountType;
  }

  /**
   * @param amountType the amountType to set
   * @return this for chaining.
   */
  public LiquidityFilter setAmountType(final AmountType amountType)
  {
    if (amountType == null) {
      this.amountType = AmountType.ALL;
    } else {
      this.amountType = amountType;
    }
    return this;
  }
}
