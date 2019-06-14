/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.i18n;

import org.projectforge.common.i18n.I18nEnum;

/**
 * I18n params are params for localized message which will be localized itself, if paramType == VALUE (default).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MessageParam
{
  private final Object value;

  private final MessageParamType paramType;

  /**
   */
  public MessageParam(final Object value)
  {
    this.value = value;
    this.paramType = MessageParamType.VALUE;
  }

  /**
   */
  public MessageParam(final I18nEnum value)
  {
    this.value = value.getI18nKey();
    this.paramType = MessageParamType.I18N_KEY;
  }

  /**
   * Will be interpreted as value.
   */
  public MessageParam(final String value)
  {
    this.value = value;
    this.paramType = MessageParamType.VALUE;
  }

  /**
   * @value Value or i18n key, if paramType = I18N_KEY
   * @paramType
   */
  public MessageParam(final String value, final MessageParamType paramType)
  {
    this.value = value;
    this.paramType = paramType;
  }

  /**
   * @return The key for the localized message.
   * @throws IllegalArgumentException if paramType is not I18N_KEY or the value is not an instance of java.lang.String.
   */
  public String getI18nKey()
  {
    if (isI18nKey() == true) {
      return (String) value;
    }
    throw new IllegalArgumentException(
        "getI18nKey is called, but paramType is not I18N_KEY or value is not an instance of java.lang.String");
  }

  /**
   * @return The value for the localized message.
   */
  public Object getValue()
  {
    return value;
  }

  /**
   * @return True, if paramType is I18N_KEY and the value is an instance of java.lang.String
   */
  public boolean isI18nKey()
  {
    return paramType == MessageParamType.I18N_KEY && value instanceof String;
  }

  @Override
  public String toString()
  {
    return String.valueOf(value);
  }
}
