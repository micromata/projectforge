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
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.convert.IConverter;

/**
 * Supports only €.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TimeZoneConverter implements IConverter
{
  private static final long serialVersionUID = 2554286471459716772L;

  @Override
  public Object convertToObject(final String value, final Locale locale)
  {
    if (StringUtils.isEmpty(value) == true) {
      return null;
    }
    final int ind = value.indexOf(" (");
    final String id = ind >= 0 ? value.substring(0, ind) : value;
    final TimeZone timeZone = TimeZone.getTimeZone(id);
    if (timeZone.getID().toLowerCase(locale).equals(id.toLowerCase(locale)) == false) {
      error();
      return null;
    }
    return timeZone;
  }

  @Override
  public String convertToString(final Object value, final Locale locale)
  {
    if (value == null) {
      return null;
    }
    final TimeZone timeZone = (TimeZone) value;
    return timeZone.getID() + " (" + timeZone.getDisplayName(locale) + ")";
  }

  /**
   * Will be called if convert to Object fails. Does nothing at default.
   */
  protected void error()
  {
  }
}
