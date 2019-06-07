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

package org.projectforge.framework.utils;

import java.io.Serializable;

/**
 * Hold a single number ranges, such as "20-25" which fits als numbers between 20 and 25.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class Range<T extends Comparable<T>> implements Serializable
{
  private static final long serialVersionUID = 8796444756609759436L;

  private T minValue;

  private T maxValue;

  /**
   * The lower value will be the minValue, the higher value will be the maxValue.
   * @param value1
   * @param value2
   */
  public Range(final T value1, final T value2)
  {
    if (value1.compareTo(value2) < 0) {
      this.minValue = value1;
      this.maxValue = value2;
    } else {
      this.minValue = value2;
      this.maxValue = value1;
    }
  }

  public boolean doesMatch(final T value)
  {
    if (value == null) {
      return false;
    }
    return value.compareTo(minValue) >= 0 && value.compareTo(maxValue) <= 0;
  }

  /**
   * @return the minValue
   */
  public T getMinValue()
  {
    return minValue;
  }

  /**
   * @param minValue the minValue to set
   * @return this for chaining.
   */
  public Range<T> setMinValue(final T minValue)
  {
    this.minValue = minValue;
    return this;
  }

  /**
   * @return the maxValue
   */
  public T getMaxValue()
  {
    return maxValue;
  }

  /**
   * @param maxValue the maxValue to set
   * @return this for chaining.
   */
  public Range<T> setMaxValue(final T maxValue)
  {
    this.maxValue = maxValue;
    return this;
  }

}
