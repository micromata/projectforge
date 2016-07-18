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

package org.projectforge.web.wicket.converter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.convert.converter.BigDecimalConverter;
import org.projectforge.framework.utils.NumberFormatter;
import org.projectforge.framework.utils.NumberHelper;

/**
 * Supports text fields with % symbol.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class BigDecimalPercentConverter extends BigDecimalConverter
{
  private static final long serialVersionUID = -1210802755722961881L;

  private boolean decimalFormat = true;

  public BigDecimalPercentConverter()
  {
  }

  /**
   * 
   * @param decimalFormat If true, then the percent value is represented by the decimal value, e. g. 19% as 0.19, otherwise 19% as 19.
   *          Default is false.
   */
  public BigDecimalPercentConverter(final boolean decimalFormat)
  {
    this.decimalFormat = decimalFormat;
  }

  @Override
  public BigDecimal convertToObject(String value, final Locale locale)
  {
    value = StringUtils.trimToEmpty(value);
    if (value.endsWith("%") == true) {
      value = value.substring(0, value.length() - 1).trim();
    }
    BigDecimal bd = super.convertToObject(value, locale);
    if (bd != null && decimalFormat == true) {
      bd = bd.divide(NumberHelper.HUNDRED, bd.scale() + 2, RoundingMode.HALF_UP);
    }
    return bd;
  }

  /**
   * @see org.apache.wicket.util.convert.converter.AbstractNumberConverter#convertToString(java.lang.Number, java.util.Locale)
   */
  @Override
  public String convertToString(final BigDecimal value, final Locale locale)
  {
    if (value == null) {
      return "%";
    }
    BigDecimal bd = value;
    if (decimalFormat == true) {
      bd = bd.multiply(NumberHelper.HUNDRED).stripTrailingZeros();
    }
    return NumberFormatter.format(bd) + "%";
  }
}
