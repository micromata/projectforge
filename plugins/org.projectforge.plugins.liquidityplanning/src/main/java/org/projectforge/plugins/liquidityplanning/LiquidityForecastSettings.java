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
import java.math.BigDecimal;

public class LiquidityForecastSettings implements Serializable
{
  private static final long serialVersionUID = -6429410479048275707L;

  private BigDecimal startAmount = BigDecimal.ZERO;

  private int nextDays = 30;

  private int expectencyForRecentMonths = 12;

  /**
   * @return the startAmount if given or {@link BigDecimal#ZERO}.
   */
  public BigDecimal getStartAmount()
  {
    if (startAmount == null) {
      return BigDecimal.ZERO;
    }
    return startAmount;
  }

  /**
   * @param startAmount the startAmount to set
   * @return this for chaining.
   */
  public LiquidityForecastSettings setStartAmount(final BigDecimal startAmount)
  {
    this.startAmount = startAmount;
    return this;
  }

  /**
   * @return the nextDays (1-365)
   */
  public int getNextDays()
  {
    if (nextDays < 1 || nextDays > 365) {
      return 30;
    }
    return nextDays;
  }

  /**
   * @param nextDays the nextDays to set
   * @return this for chaining.
   */
  public LiquidityForecastSettings setNextDays(final int nextDays)
  {
    this.nextDays = nextDays;
    return this;
  }

  /**
   * For calculating the expected date of payment all paid invoices of an debitor of the last n month are analyzed.
   * @return the expectencyForRecentMonths
   */
  public int getExpectencyForRecentMonths()
  {
    return expectencyForRecentMonths;
  }

  /**
   * @param expectencyForRecentMonths the expectencyForRecentMonths to set
   * @return this for chaining.
   */
  public LiquidityForecastSettings setExpectencyForRecentMonths(final int expectencyForRecentMonths)
  {
    this.expectencyForRecentMonths = expectencyForRecentMonths;
    return this;
  }
}
