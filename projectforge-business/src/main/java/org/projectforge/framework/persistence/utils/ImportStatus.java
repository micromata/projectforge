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

package org.projectforge.framework.persistence.utils;

import org.projectforge.common.i18n.I18nEnum;


/**
 */
public enum ImportStatus implements I18nEnum
{
  NOT_RECONCILED("notReconciled"), RECONCILED("reconciled"), HAS_ERRORS("hasErrors"), IMPORTED("imported"), NOTHING_TODO("nothingToDo");

  private String key;

  @Override
  public String getI18nKey()
  {
    return "common.import.status." + key;
  }

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }

  ImportStatus(String key)
  {
    this.key = key;
  }
  
  public boolean isIn(ImportStatus... status) {
    for (ImportStatus st : status) {
      if (this == st) {
        return true;
      }
    }
    return false;
  }
}
