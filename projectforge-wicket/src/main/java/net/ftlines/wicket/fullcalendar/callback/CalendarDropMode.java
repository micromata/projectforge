/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
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
