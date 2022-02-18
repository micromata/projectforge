/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package net.ftlines.wicket.fullcalendar.callback;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * 
 */
public enum CalendarDropMode
{
  MOVE_SAVE("calendar.dd.move.save", "MoveSave"), //
  MOVE_EDIT("calendar.dd.move.edit", "MoveEdit"), //
  COPY_SAVE("calendar.dd.copy.save", "CopySave"), //
  COPY_EDIT("calendar.dd.copy.edit", "CopyEdit"), //
  CANCEL("cancel", "Cancel");

  private String i18nKey;

  private String ajaxTarget;

  /**
   * @param i18nKey
   * @param ajaxTarget
   */
  private CalendarDropMode(final String i18nKey, final String ajaxTarget)
  {
    this.i18nKey = i18nKey;
    this.ajaxTarget = ajaxTarget;
  }

  /**
   * @return the i18nKey
   */
  public String getI18nKey()
  {
    return this.i18nKey;
  }

  /**
   * @return the ajaxTarget
   */
  public String getAjaxTarget()
  {
    return this.ajaxTarget;
  }

  public static CalendarDropMode fromAjaxTarget(final String ajaxTarget)
  {
    CalendarDropMode result = null;
    for (final CalendarDropMode mode : values()) {
      if (mode.getAjaxTarget().equals(ajaxTarget)) {
        result = mode;
        break;
      }
    }
    return result;
  }
}
