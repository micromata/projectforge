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

package org.projectforge.common.i18n;

import org.apache.commons.lang3.StringUtils;

/**
 * Needed for example to give tasks a priority.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum Priority implements I18nEnum
{
  LEAST("least"), LOW("low"), MIDDLE("middle"), HIGH("high"), HIGHEST("highest");

  private String key;

  /**
   * @see #ordinal()
   */
  public int getOrdinal()
  {
    return ordinal();
  }

  public static Priority getPriority(String s)
  {
    if (StringUtils.isEmpty(s) == true) {
      return null;
    }
    if ("LEAST".equals(s) == true) {
      return LEAST;
    } else if ("LOW".equals(s) == true) {
      return LOW;
    } else if ("MIDDLE".equals(s) == true) {
      return MIDDLE;
    } else if ("HIGH".equals(s) == true) {
      return HIGH;
    } else if ("HIGHEST".equals(s) == true) {
      return HIGHEST;
    }
    throw new UnsupportedOperationException("Unknown Priority: '" + s + "'");
  }

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }

  @Override
  public String getI18nKey()
  {
    return "priority." + key;
  }

  Priority(String key)
  {
    this.key = key;
  }
}
