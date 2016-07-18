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

import java.util.Locale;

import org.apache.commons.lang.StringUtils;

/**
 * Supports text fields with % symbol.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class IntegerPercentConverter extends IntegerConverter
{
  /**
   * @param digits
   */
  public IntegerPercentConverter(final int digits)
  {
    super(digits);
  }

  private static final long serialVersionUID = -1210802755722961881L;

  @Override
  public Integer convertToObject(String value, final Locale locale)
  {
    value = StringUtils.trimToEmpty(value);
    if (value.endsWith("%") == true) {
      value = value.substring(0, value.length() - 1).trim();
    }
    return super.convertToObject(value, locale);
  }

  @Override
  public String convertToString(final Integer value, final Locale locale)
  {
    if (value == null) {
      return "%";
    }
    return value + "%";
  }
}
