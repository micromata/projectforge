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

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

public class IntAggregatedValuesTest
{
  @Test
  public void testAverage()
  {
    assertValues(0, 0);
    assertValues(0, 1, 0);
    assertValues(42, 1, 42);
    assertValues(10, 3, 0, 10, 20);
  }

  @Test
  public void testWeightedAverage()
  {
    assertWeightetValues(0, 0, 0);
    assertWeightetValues(0, 0, 1, 0, 1);
    assertWeightetValues(42, 42, 1, 42, 1);
    assertWeightetValues(10, 10, 3, 0, 1, 10, 1, 20, 1);
    assertWeightetValues(10, 16, 3, 0, 0, 10, 2, 20, 3);
  }

  private void assertValues(final int expectedAverage, final int expectedNumberOfElement, final int... values)
  {
    final IntAggregatedValues stat = new IntAggregatedValues();
    if (values != null) {
      for (final int value : values) {
        stat.add(value);
      }
    }
    assertEquals(expectedAverage, (int) stat.getAverage());
    assertEquals(expectedNumberOfElement, stat.getNumberOfValues());
  }

  private void assertWeightetValues(final int expectedAverage, final int exptectedWeightedAverage,
      final int expectedNumberOfElement,
      final int... values)
  {
    final IntAggregatedValues stat = new IntAggregatedValues();
    if (values != null) {
      for (int i = 0; i < values.length - 1; i += 2) {
        stat.add(values[i], values[i + 1]);
      }
    }
    assertEquals(expectedAverage, (int) stat.getAverage());
    assertEquals(exptectedWeightedAverage, (int) stat.getWeightedAverage());
    assertEquals(expectedNumberOfElement, stat.getNumberOfValues());
  }
}
