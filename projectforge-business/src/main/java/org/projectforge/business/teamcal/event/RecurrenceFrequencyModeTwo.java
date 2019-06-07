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

package org.projectforge.business.teamcal.event;

import org.projectforge.common.i18n.I18nEnum;

/**
 * Created by fdesel on 03.08.17.
 */
public enum RecurrenceFrequencyModeTwo implements I18nEnum
{
  MONDAY("monday"), TUESDAY("tuesday"), WEDNESDAY("wednesday"), THURSDAY("thursday"), FRIDAY("friday"), SATURDAY("saturday"), SUNDAY("sunday"),
  DAY("day"), WEEKDAY("weekday"), WEEKEND("weekend");

  private String key;

  @Override
  public String getI18nKey()
  {
    return "plugins.teamcal.event.recurrence." + key;
  }

  public String getKey()
  {
    return key;
  }

  RecurrenceFrequencyModeTwo(final String key)
  {
    this.key = key;
  }
}
