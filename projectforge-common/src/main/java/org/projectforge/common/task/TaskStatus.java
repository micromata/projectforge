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

package org.projectforge.common.task;

import org.projectforge.common.i18n.I18nEnum;

/**
 * Every task has a task status: N - not opened, O - opened, C - closed.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum TaskStatus implements I18nEnum
{
  N("notOpened"), O("opened"), C("closed");

  private String key;

  public static TaskStatus getTaskStatus(String s)
  {
    if ("N".equals(s) == true) {
      return N;
    } else if ("O".equals(s) == true) {
      return O;
    } else if ("C".equals(s) == true) {
      return C;
    }
    throw new UnsupportedOperationException("Unknown TaskStatus: '" + s + "'");
  }

  public boolean isIn(TaskStatus... status)
  {
    for (TaskStatus st : status) {
      if (this == st) {
        return true;
      }
    }
    return false;
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
    return "task.status." + key;
  }

  TaskStatus(String key)
  {
    this.key = key;
  }
}
