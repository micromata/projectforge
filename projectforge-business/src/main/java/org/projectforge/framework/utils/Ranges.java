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

package org.projectforge.framework.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.projectforge.common.StringHelper;
import org.springframework.util.CollectionUtils;

/**
 * Holds number ranges, such as "10,20-25,30-35,50" (comma separated ranges and values).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public abstract class Ranges<T extends Comparable<T>> implements Serializable
{
  private static final long serialVersionUID = 844349548492107372L;

  private List<Range<T>> ranges;

  private List<T> values;

  private final String separatorChars = ";,";

  private final char rangesSeparatorChar = '-';

  private boolean nullRangeMatchesAlways;

  /**
   * Number ranges, such as "10,20-25,30-35,50" (comma separated ranges and values).
   * @param rangesString
   */
  public Ranges(final String rangesString)
  {
    setRanges(rangesString);
  }

  public boolean doesMatch(final T value)
  {
    if (value == null) {
      return false;
    }
    if (CollectionUtils.isEmpty(ranges) == true && CollectionUtils.isEmpty(values) == true) {
      return nullRangeMatchesAlways;
    }
    for (final Range<T> range : ranges) {
      if (range.doesMatch(value) == true) {
        return true;
      }
    }
    for (final T val : values) {
      if (value.compareTo(val) == 0) {
        return true;
      }
    }
    return false;
  }

  protected abstract T parseValue(String value);

  /**
   * Number ranges, such as "10,20-25,30-35,50" (comma separated ranges and values).
   * @param rangesString
   */
  public Ranges<T> setRanges(final String rangesString)
  {
    ranges = new ArrayList<Range<T>>();
    values = new ArrayList<T>();
    if (StringUtils.isBlank(rangesString) == true) {
      // No ranges given.
      return this;
    }
    final String[] rangeStrings = StringUtils.split(rangesString, separatorChars);
    for (final String rangeString : rangeStrings) {
      if (StringUtils.isBlank(rangeString) == true) {
        // No range given.
        continue;
      }
      final String str = rangeString.trim();
      final int pos = str.indexOf(rangesSeparatorChar, 1); // Ignore any leading and trailing '-'
      if (pos > 0) {
        if (pos >= str.length()) {
          throw new IllegalArgumentException("Couldn't parse range: '" + rangesString + "'.");
        }
        final String from = str.substring(0, pos);
        final String to = str.substring(pos + 1);
        try {
          final Range<T> range = new Range<T>(parseValue(from.trim()), parseValue(to.trim()));
          ranges.add(range);
        } catch (final NumberFormatException ex) {
          throw new IllegalArgumentException("Couldn't parse range: '" + rangesString + "'." + ex.getMessage(), ex);
        }
      } else {
        try {
          values.add(parseValue(str));
        } catch (final NumberFormatException ex) {
          throw new IllegalArgumentException("Couldn't parse range: '" + rangesString + "'." + ex.getMessage(), ex);
        }
      }
    }
    return this;
  }

  /**
   * If set to true, all values will match if no range is given.
   * @param nullRangeMatchesAlways the nullRangeMatchesAlways to set
   * @return this for chaining.
   */
  public Ranges<T> setNullRangeMatchesAlways(final boolean nullRangeMatchesAlways)
  {
    this.nullRangeMatchesAlways = nullRangeMatchesAlways;
    return this;
  }

  /**
   * @return the values
   */
  public List<T> getValues()
  {
    return values;
  }

  /**
   * @return the ranges
   */
  public List<Range<T>> getRanges()
  {
    return ranges;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    if (ranges != null) {
      for (final Range<T> range : ranges) {
        first = StringHelper.append(buf, first, String.valueOf(range.getMinValue()) + rangesSeparatorChar + range.getMaxValue(), ",");
      }
    }
    if (values != null) {
      for (final T value : values) {
        first = StringHelper.append(buf, first, String.valueOf(value), ",");
      }
    }
    return buf.toString();
  }
}
