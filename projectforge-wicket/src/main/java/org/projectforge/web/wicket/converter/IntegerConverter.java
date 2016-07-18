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
 * Format digits, e. g. "001" instead of "1".
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class IntegerConverter extends org.apache.wicket.util.convert.converter.IntegerConverter
{
  private static final long serialVersionUID = 8150882431021230194L;

  private final int digits;

  public IntegerConverter(final int digits)
  {
    this.digits = digits;
  }

  @Override
  public String convertToString(final Integer value, final Locale locale)
  {
    if (value == null) {
      return "";
    }
    return StringUtils.leftPad(value.toString(), digits, '0');
  }
}
