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

package org.projectforge.common;

import org.junit.jupiter.api.Test;
import org.projectforge.framework.utils.IntRanges;
import org.projectforge.framework.utils.Range;

import static org.junit.jupiter.api.Assertions.*;

public class IntRangesTest
{
  @SuppressWarnings("unchecked")
  @Test
  public void testRanges()
  {
    testListSizes(new IntRanges(null), 0, 0);
    testListSizes(new IntRanges(""), 0, 0);
    testListSizes(new IntRanges(" "), 0, 0);
    testListSizes(new IntRanges(",  , "), 0, 0);

    testValueList(new IntRanges("5-7"), (int[]) null);
    testValueList(new IntRanges("-5"), -5);
    testValueList(new IntRanges("-5,-5--3,-8,0"), -5, -8, 0);

    testRangeList(new IntRanges("2,5"), (Range<Integer>[]) null);
    testRangeList(new IntRanges("2-5"), r(2, 5));
    testRangeList(new IntRanges("-2--5"), r(-5, -2));
    testRangeList(new IntRanges("-5,-5--3,-8,0"), r(-5, -3));
    testRangeList(new IntRanges("2-5,-5--3,-8,0"), r(2, 5), r(-5, -3));

    assertEquals("", new IntRanges(null).toString());
    assertEquals("1", new IntRanges("1").toString());
    assertEquals("-10--5", new IntRanges("-5--10").toString());
    assertEquals("2-5,-5--3,-8,0", new IntRanges("2-5,-5--3,-8,0").toString());

    checkExceptions("-5-");
    checkExceptions("-");
    checkExceptions("--");

    assertFalse(new IntRanges("").doesMatch(null));
    doesNotMatch(new IntRanges(""), 0, 42, -1);

    doesMatch(new IntRanges("42"), 42);
    doesNotMatch(new IntRanges("42"), 0, -1);

    doesMatch((IntRanges) new IntRanges("").setNullRangeMatchesAlways(true), 0, 42);
    doesNotMatch(new IntRanges(""), 0, -1);

    doesMatch(new IntRanges("42-44,-3--5,-8,0"), 42, 43, 44, -5, -4, -3, -8, 0);
    doesNotMatch(new IntRanges("42-44,-3--5,-8,0"), 41, 45, -6, -2, -7, -9, -1, 1);
  }

  private void doesMatch(final IntRanges ranges, final int... values)
  {
    for (final int value : values) {
      assertTrue(ranges.doesMatch(value));
    }
  }

  private void doesNotMatch(final IntRanges ranges, final int... values)
  {
    for (final int value : values) {
      assertFalse(ranges.doesMatch(value));
    }
  }

  private Range<Integer> r(final int minValue, final int maxValue)
  {
    return new Range<Integer>(minValue, maxValue);
  }

  private void testListSizes(final IntRanges ranges, final int rangesSize, final int valuesSize)
  {
    assertEquals(rangesSize, ranges.getRanges().size());
    assertEquals(valuesSize, ranges.getValues().size());
  }

  private void testValueList(final IntRanges ranges, final int... values)
  {
    if (values == null) {
      assertEquals(0, ranges.getValues().size());
      return;
    }
    assertEquals(values.length, ranges.getValues().size());
    for (int i = 0; i < values.length; i++) {
      assertEquals(values[i], (int) ranges.getValues().get(i));
    }
  }

  private void testRangeList(final IntRanges intRanges, final Range<Integer>... ranges)
  {
    if (ranges == null) {
      assertEquals(0, intRanges.getRanges().size());
      return;
    }
    assertEquals(ranges.length, intRanges.getRanges().size());
    for (int i = 0; i < ranges.length; i++) {
      final Range<Integer> range = intRanges.getRanges().get(i);
      assertEquals(ranges[i].getMinValue(), range.getMinValue());
      assertEquals(ranges[i].getMaxValue(), range.getMaxValue());
    }
  }

  private void checkExceptions(final String rangesString)
  {
    try {
      testValueList(new IntRanges(rangesString), -5);
      fail("Exception excepted for string: " + rangesString);
    } catch (final IllegalArgumentException ex) {
      // OK
    }
  }
}
