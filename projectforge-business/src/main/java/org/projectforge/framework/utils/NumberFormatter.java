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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

public class NumberFormatter
{
  /**
   * Uses the scale of the BigDecimal. Uses the ThreadLocalUserContext locale.
   * @param value
   */
  public static String format(final int value)
  {
    final NumberFormat format = NumberFormat.getNumberInstance(ThreadLocalUserContext.getLocale());
    return format.format(value);
  }

  /**
   * Uses the scale of the BigDecimal. Uses the ThreadLocalUserContext locale.
   * @param value
   */
  public static String format(final long value)
  {
    final NumberFormat format = NumberFormat.getNumberInstance(ThreadLocalUserContext.getLocale());
    return format.format(value);
  }

  /**
   * Uses the scale of the BigDecimal. Uses the ThreadLocalUserContext locale.
   * @param value
   */
  public static String format(final BigDecimal value)
  {
    if (value == null) {
      return "";
    }
    return format(value, value.scale());
  }

  /**
   * Uses the scale of the BigDecimal.
   * @param value
   * @param locale
   */
  public static String format(final BigDecimal value, final Locale locale)
  {
    if (value == null) {
      return "";
    }
    return format(value, value.scale(), locale);
  }

  /**
   * Uses the scale of the BigDecimal. Uses the ThreadLocalUserContext locale.
   * @param value
   */
  public static String format(final BigDecimal value, final int scale)
  {
    return format(value, scale, ThreadLocalUserContext.getLocale());
  }

  /**
   * Uses the scale of the BigDecimal
   * @param value
   * @param locale
   */
  public static String format(final BigDecimal value, final int scale, final Locale locale)
  {
    if (value == null) {
      return "";
    }
    final NumberFormat format = NumberFormat.getNumberInstance(locale);
    format.setMaximumFractionDigits(scale);
    format.setMinimumFractionDigits(scale);
    return format.format(value);
  }

  /**
   * Strips trailing zeros.
   * @param value e. g. 0.19
   * @return e. g. 19% or empty string if value is null.
   * @see BigDecimal#stripTrailingZeros()
   */
  public static String formatPercent(final BigDecimal value)
  {
    if (value == null) {
      return "";
    }
    return format(value.multiply(NumberHelper.HUNDRED).stripTrailingZeros()) + "%";
  }
}
