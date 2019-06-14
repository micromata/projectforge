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

package org.projectforge.common.task;

import org.projectforge.common.i18n.I18nEnum;

/**
 * Defines the status for time sheet booking for a task.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum TimesheetBookingStatus implements I18nEnum
{
  /**
   * Inherits from parent task.
   */
  INHERIT("inherit"),
  /**
   * Time sheet booking is allowed (but is perhaps denied by other rules, e. g. if a sub task has an assigned order position).
   */
  OPENED("opened"),
  /**
   * Time sheet booking is only allowed for leaf task nodes.
   */
  ONLY_LEAFS("onlyLeafs"),
  /**
   * No time sheet booking for this task allowed (for descendant task nodes time sheet booking is allowed if defined there).
   */
  NO_BOOKING("noBooking"),
  /**
   * No time sheet booking (inclusive all descendant task nodes) allowed.
   */
  TREE_CLOSED("treeClosed");
  
  public static TimesheetBookingStatus DEFAULT = INHERIT;

  private String key;

  public boolean isIn(final TimesheetBookingStatus... status)
  {
    for (TimesheetBookingStatus st : status) {
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
    return "task.timesheetBooking." + key;
  }

  TimesheetBookingStatus(String key)
  {
    this.key = key;
  }
}
