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

package org.projectforge.plugins.liquidityplanning;

import org.projectforge.framework.time.DayHolder;
import org.projectforge.framework.utils.NumberHelper;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class LiquidityEntriesStatistics implements Serializable
{
  private static final long serialVersionUID = 4818281174624971825L;

  private BigDecimal paid, open, total, overdue;

  private int counterPaid;

  private int counter;

  private final Date today;

  public LiquidityEntriesStatistics()
  {
    paid = open = total = BigDecimal.ZERO;
    counter = counterPaid = 0;
    today = new DayHolder().getDate();
  }

  public void add(final LiquidityEntryDO entry)
  {
    final BigDecimal amount = entry.getAmount();
    this.total = NumberHelper.add(total, amount);
    if (entry.getPaid()) {
      this.paid = NumberHelper.add(paid, amount);
      counterPaid++;
    } else {
      this.open = NumberHelper.add(open, amount);
      if (entry.getDateOfPayment() != null && entry.getDateOfPayment().before(today)) {
        this.overdue = NumberHelper.add(overdue, amount);
      }
    }
    counter++;
  }

  /**
   * @return the paid
   */
  public BigDecimal getPaid()
  {
    return paid;
  }

  /**
   * @return the open
   */
  public BigDecimal getOpen()
  {
    return open;
  }

  /**
   * @return the total
   */
  public BigDecimal getTotal()
  {
    return total;
  }

  /**
   * @return the overdue
   */
  public BigDecimal getOverdue()
  {
    return overdue;
  }

  public int getCounter()
  {
    return counter;
  }

  public int getCounterPaid()
  {
    return counterPaid;
  }
}
