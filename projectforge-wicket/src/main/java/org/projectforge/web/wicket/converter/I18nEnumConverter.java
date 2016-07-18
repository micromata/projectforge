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
import org.apache.wicket.Component;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.common.i18n.I18nEnum;

/**
 * Format I18nEnums.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class I18nEnumConverter implements IConverter
{
  private static final long serialVersionUID = -2840912843477849172L;

  private Component parent;

  private I18nEnum[] supportedValues;

  /**
   * @param parent Needed for i18n.
   * @param clazz Class type of i18n enum.
   * @see Component#getString(String)
   */
  public I18nEnumConverter(final Component parent, final I18nEnum[] supportedValues)
  {
    this.parent = parent;
    this.supportedValues = supportedValues;
  }

  public String convertToString(Object value, Locale locale)
  {
    if (value == null) {
      return "";
    }
    if (value instanceof I18nEnum) {
      return parent.getString(((I18nEnum) value).getI18nKey());
    } else {
      return String.valueOf(value);
    }
  }

  @Override
  public Object convertToObject(final String value, final Locale locale)
  {
    if (StringUtils.isEmpty(value) == true) {
      return null;
    }
    for (final I18nEnum i18nEnum : supportedValues) {
      if (value.equals(parent.getString(i18nEnum.getI18nKey())) == true) {
        return i18nEnum;
      }
    }
    return null;
  }
}
