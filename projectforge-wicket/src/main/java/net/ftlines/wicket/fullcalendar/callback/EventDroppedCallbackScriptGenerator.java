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
package net.ftlines.wicket.fullcalendar.callback;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.util.template.PackageTextTemplate;

/**
 * Just the Javascript generator helper class to provide the event dropped javascript
 * 
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * 
 */
public class EventDroppedCallbackScriptGenerator
{
  public static final String NO_CONTEXTMENU_INDICATOR = "noContextMenu";

  private static final String MOVE_SAVE = CalendarDropMode.MOVE_SAVE.getI18nKey();

  private static final String MOVE_EDIT = CalendarDropMode.MOVE_EDIT.getI18nKey();

  private static final String COPY_SAVE = CalendarDropMode.COPY_SAVE.getI18nKey();

  private static final String COPY_EDIT = CalendarDropMode.COPY_EDIT.getI18nKey();

  private static final String CANCEL = CalendarDropMode.CANCEL.getI18nKey();

  private static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private static final PackageTextTemplate JS_TEMPLATE = new PackageTextTemplate(EventDroppedCallbackScriptGenerator.class,
      "EventDroppedCallbackScriptGenerator.js.template");

  /**
   * 
   * @param component
   * @param script
   * @return
   */
  private static Map<String, String> buildMap(final Component component, final String script)
  {
    final Map<String, String> result = new HashMap<String, String>();
    result.put("NO_CONTEXTMENU_INDICATOR", NO_CONTEXTMENU_INDICATOR);
    result.put("ORIGINAL_CALLBACK", script);
    result.put("MOVE_SAVE", component.getString(MOVE_SAVE));
    result.put("MOVE_SAVE_TARGET", CalendarDropMode.MOVE_SAVE.getAjaxTarget());
    result.put("MOVE_EDIT", component.getString(MOVE_EDIT));
    result.put("MOVE_EDIT_TARGET", CalendarDropMode.MOVE_EDIT.getAjaxTarget());
    result.put("COPY_SAVE", component.getString(COPY_SAVE));
    result.put("COPY_SAVE_TARGET", CalendarDropMode.COPY_SAVE.getAjaxTarget());
    result.put("COPY_EDIT", component.getString(COPY_EDIT));
    result.put("COPY_EDIT_TARGET", CalendarDropMode.COPY_EDIT.getAjaxTarget());
    result.put("CANCEL", component.getString(CANCEL));
    result.put("NONE_TARGET", CalendarDropMode.MOVE_EDIT.getAjaxTarget());
    return result;
  }

  /**
   * 
   * @param component
   * @param script
   * @param urlTail
   * @return
   */
  public static String getEventDroppedJavascript(final Component component, final String url, final String script, final String urlTail)
  {
    return JS_TEMPLATE.asString(buildMap(component, script.replace(urlTail, url + "&which=\"+which+\""))).replace(LINE_SEPARATOR, "")
        .replace("  ", " ") + "; hideAllTooltips();";
  }
}
