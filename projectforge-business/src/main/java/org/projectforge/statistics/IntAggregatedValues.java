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

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class IntAggregatedValues extends AbstractAggregatedValues<Integer> implements Serializable
{
  private static final long serialVersionUID = 3442619133267500263L;

  /**
   * @see org.projectforge.statistics.AbstractAggregatedValues#getZero()
   */
  @Override
  protected Integer getZero()
  {
    return 0;
  }

  /**
   * @see org.projectforge.statistics.AbstractAggregatedValues#isZero(java.lang.Object)
   */
  @Override
  protected boolean isZero(final Integer value)
  {
    return value == null || value == 0;
  }
  /**
   * @see org.projectforge.statistics.AbstractAggregatedValues#sum(java.lang.Object, java.lang.Object)
   */
  @Override
  protected Integer sum(final Integer value1, final Integer value2)
  {
    return value1 + value2;
  }

  /**
   * @see org.projectforge.statistics.AbstractAggregatedValues#divide(java.lang.Object, java.lang.Object)
   */
  @Override
  protected Integer divide(final Integer value, final Integer divisor)
  {
    return value / divisor;
  }

  /**
   * @see org.projectforge.statistics.AbstractAggregatedValues#convert(int)
   */
  @Override
  protected Integer convert(final int value)
  {
    return value;
  }

  /**
   * @see org.projectforge.statistics.AbstractAggregatedValues#multiply(java.lang.Object, java.lang.Object)
   */
  @Override
  protected Integer multiply(final Integer value1, final Integer value2)
  {
    return value1 * value2;
  }
}
