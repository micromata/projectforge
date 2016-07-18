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

package org.projectforge.business.teamcal.event.model;

import org.projectforge.common.i18n.I18nEnum;

/**
 * To define if alert duration type is minute, hour or day.
 * 
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 *
 */
public enum ReminderDurationUnit implements I18nEnum
{
  MINUTES("M", "plugins.teamcal.event.reminder.MINUTES_BEFORE"), //
  HOURS("H", "plugins.teamcal.event.reminder.HOURS_BEFORE"), //
  DAYS("D", "plugins.teamcal.event.reminder.DAYS_BEFORE");

  private final String type;
  private final String i18n;

  private ReminderDurationUnit(final String type, final String i18n)
  {
    this.type = type;
    this.i18n = i18n;
  }

  /**
   * @return the type
   */
  public String getType()
  {
    return type;
  }

  /**
   * @see org.projectforge.common.i18n.I18nEnum#getI18nKey()
   */
  @Override
  public String getI18nKey()
  {
    return i18n;
  }

  public static ReminderDurationUnit getbyType(String type)
  {
    for (ReminderDurationUnit rat : ReminderDurationUnit.values()) {
      if (rat.getType().equals(type)) {
        return rat;
      }
    }
    return null;
  }
}
