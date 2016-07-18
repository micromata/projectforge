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

package org.projectforge.statistics;

import java.io.Serializable;
import java.math.BigDecimal;


/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public  class BigDecimalAggregatedValues extends AbstractAggregatedValues<BigDecimal> implements Serializable
{
  private static final long serialVersionUID = 2998355183590649140L;

  /**
   * @see org.projectforge.statistics.AbstractAggregatedValues#getZero()
   */
  @Override
  protected BigDecimal getZero()
  {
    return BigDecimal.ZERO;
  }


  /**
   * @see org.projectforge.statistics.AbstractAggregatedValues#isZero(java.lang.Object)
   */
  @Override
  protected boolean isZero(final BigDecimal value)
  {
    return value == null || value.compareTo(BigDecimal.ZERO) == 0;
  }

  /**
   * @see org.projectforge.statistics.AbstractAggregatedValues#sum(java.lang.Object, java.lang.Object)
   */
  @Override
  protected BigDecimal sum(final BigDecimal value1, final BigDecimal value2)
  {
    return value1.add(value2);
  }

  /**
   * @see org.projectforge.statistics.AbstractAggregatedValues#divide(java.lang.Object, java.lang.Object)
   */
  @Override
  protected BigDecimal divide(final BigDecimal value, final BigDecimal divisor)
  {
    return value.divide(divisor);
  }

  /**
   * @see org.projectforge.statistics.AbstractAggregatedValues#convert(int)
   */
  @Override
  protected BigDecimal convert(final int value)
  {
    return new BigDecimal(value);
  }

  /**
   * @see org.projectforge.statistics.AbstractAggregatedValues#multiply(java.lang.Object, java.lang.Object)
   */
  @Override
  protected BigDecimal multiply(final BigDecimal value1, final BigDecimal value2)
  {
    return value1.multiply(value2);
  }
}
