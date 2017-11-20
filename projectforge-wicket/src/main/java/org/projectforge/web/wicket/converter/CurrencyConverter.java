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
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.util.convert.converter.BigDecimalConverter;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.utils.NumberHelper;


/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class CurrencyConverter extends BigDecimalConverter
{
  private static final long serialVersionUID = -7006507810932242601L;

  private final String currency;

  private BigDecimal totalAmount;

  public CurrencyConverter()
  {
    currency = ConfigXml.getInstance().getCurrencySymbol();
  }

  /**
   * Use this constructor to set the total amount. If total amount is given the converter supports a percentage value. A percentage value
   * will be calculated.
   * @param totalAmount
   */
  public CurrencyConverter(final BigDecimal totalAmount)
  {
    this();
    this.totalAmount = totalAmount;
  }

  /**
   * If total amount is given also a percentage value is supported.
   * @see org.projectforge.framework.xstream.converter.converters.BigDecimalConverter#convertToObject(java.lang.String, java.util.Locale)
   */
  @Override
  public BigDecimal convertToObject(String value, final Locale locale)
  {
    value = StringUtils.trimToEmpty(value);
    if (value.endsWith(currency) == true) {
      value = value.substring(0, value.length() - 1).trim();
    } else if (totalAmount != null && value.endsWith("%") == true) {
      value = value.substring(0, value.length() - 1).trim();
      final BigDecimal percentage = super.convertToObject(value, locale);
      return totalAmount.multiply(percentage).divide(NumberHelper.HUNDRED, RoundingMode.HALF_UP);
    }
    return super.convertToObject(value, locale);
  }

  @Override
  public String convertToString(final BigDecimal value, final Locale locale)
  {
    if (value == null) {
      return "";
    }
    NumberFormat nf;
    if (locale != null) {
      nf = NumberHelper.getCurrencyFormat(locale);
    } else {
      nf = NumberHelper.getCurrencyFormat(ThreadLocalUserContext.getLocale());
    }
    return nf.format(value) + " " + currency;
  }
}
