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

package org.projectforge.business.user;

import org.projectforge.common.i18n.I18nEnum;

/**
 * Every task has a task status: N - not opened, O - opened, C - closed.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum UserRightValue implements I18nEnum
{
  FALSE("false"), TRUE("true"), READONLY("readonly"), PARTLYREADWRITE("partlyReadwrite"), READWRITE("readwrite");

  private String key;

  public UserRightValue[] includedBy()
  {
    switch (this) {
      case READONLY:
        return new UserRightValue[] { READWRITE};
    }
    return null;
  }

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }

  public String getI18nKey()
  {
    return "access.right.value." + key;
  }

  public boolean isIn(UserRightValue... value)
  {
    for (UserRightValue val : value) {
      if (this == val) {
        return true;
      }
    }
    return false;
  }

  UserRightValue(String key)
  {
    this.key = key;
  }
}
