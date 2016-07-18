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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public abstract class AbstractAggregatedValues<T>
{
  protected final List<T> values = new LinkedList<T>();

  protected final List<T> weights = new LinkedList<T>();

  protected boolean averageDirty, weightedAverageDirty;

  protected T average, weightedAverage;

  /**
   * @return this for chaining.
   */
  public AbstractAggregatedValues<T> clear()
  {
    this.values.clear();
    return this;
  }

  /**
   * @param value
   * @return this for chaining.
   */
  public AbstractAggregatedValues<T> add(final T value)
  {
    values.add(value);
    averageDirty = weightedAverageDirty = true;
    return this;
  }

  /**
   * @param value
   * @return this for chaining.
   */
  public AbstractAggregatedValues<T> add(final T value, final T weight)
  {
    values.add(value);
    weights.add(weight);
    averageDirty = weightedAverageDirty = true;
    return this;
  }

  /**
   * @return The mean value of all entries.
   */
  public T getAverage()
  {
    if (average == null || averageDirty == true) {
      T sum = getZero();
      if (values.size() == 0) {
        average = sum;
      } else {
        for (final T value : values) {
          sum = sum(sum, value);
        }
        average = divide(sum, convert(values.size()));
      }
      averageDirty = false;
    }
    return average;
  }

  /**
   * @return The weighted mean value of all entries.
   */
  public T getWeightedAverage()
  {
    if (weightedAverage == null || weightedAverageDirty == true) {
      if (weights.size() != values.size()) {
        throw new IllegalArgumentException(
            "Size of weights not equals size of values, may-be add(value, weight) wasn't called at least once!");
      }
      T sum = getZero();
      T weightSum = getZero();
      if (values.size() == 0) {
        weightedAverage = sum;
      } else {
        final Iterator<T> valuesIt = values.iterator();
        final Iterator<T> weightsIt = weights.iterator();
        while (valuesIt.hasNext() == true) {
          final T value = valuesIt.next();
          final T weight = weightsIt.next();
          sum = sum(sum, multiply(value, weight));
          weightSum = sum(weightSum, weight);
        }
        if (isZero(weightSum) == true) {
          weightedAverage = getZero();
        } else {
          weightedAverage = divide(sum, weightSum);
        }
      }
      weightedAverageDirty = false;
    }
    return weightedAverage;
  }

  public int getNumberOfValues()
  {
    return values.size();
  }

  protected abstract T getZero();

  protected abstract T sum(T value1, T value2);

  protected abstract T convert(int value);

  protected abstract T divide(T value, T divisor);

  protected abstract T multiply(T value1, T value2);

  protected abstract boolean isZero(T value);
}
