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

package org.projectforge.business.gantt;

import org.projectforge.common.i18n.I18nEnum;

public enum GanttObjectType implements I18nEnum
{
  ACTIVITY("activity"), SUMMARY("summary"), MILESTONE("milestone");

  private String key;

  public boolean isIn(final GanttObjectType... types)
  {
    for (final GanttObjectType type : types) {
      if (this == type) {
        return true;
      }
    }
    return false;

  }

  /**
   * The key will be used e. g. for i18n.
   */
  public String getKey()
  {
    return key;
  }

  public String getI18nKey()
  {
    return "gantt.objectType." + key;
  }

  GanttObjectType(final String key)
  {
    this.key = key;
  }
}
