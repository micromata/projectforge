/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package net.ftlines.wicket.fullcalendar.callback;

import net.ftlines.wicket.fullcalendar.CalendarResponse;
import net.ftlines.wicket.fullcalendar.Config;
import net.ftlines.wicket.fullcalendar.Event;
import net.ftlines.wicket.fullcalendar.EventSource;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Request;

public abstract class EventDroppedCallback extends AbstractAjaxCallbackWithClientsideRevert implements CallbackWithHandler
{
  private static final long serialVersionUID = 9220878749378414280L;

  private final Config config;

  /**
   * @param config
   */
  public EventDroppedCallback(final Config config)
  {
    this.config = config;
  }

  @Override
  protected String configureCallbackScript(final String script, final String urlTail)
  {
    final String url = "&eventId=\"+event.id+\"&sourceId=\"+event.source.data."
        + EventSource.Const.UUID
        + "+\"&dayDelta=\"+dayDelta+\"&minuteDelta=\"+minuteDelta+\"&allDay=\"+allDay+\"";
    if (config.isEnableContextMenu() == false) { // do not show context menu
      return script.replace(urlTail, url);
    } else { // do show context menu
      return EventDroppedCallbackScriptGenerator.getEventDroppedJavascript(getComponent(), url, script, urlTail);
    }
  }

  @SuppressWarnings("serial")
  @Override
  public IModel<String> getHandlerScript()
  {
    return new IModel<String>()
    {
      @Override
      public String getObject()
      {
        return "function(event, dayDelta, minuteDelta, allDay, revertFunc, originalEvent) { " + getCallbackScript() + "}";
      }
    };
  }

  @Override
  protected boolean onEvent(final AjaxRequestTarget target)
  {
    final Request r = getCalendar().getRequest();
    final String eventId = r.getRequestParameters().getParameterValue("eventId").toString();
    final String sourceId = r.getRequestParameters().getParameterValue("sourceId").toString();

    final EventSource source = getCalendar().getEventManager().getEventSource(sourceId);
    final Event event = source.getEventProvider().getEventForId(eventId);

    final int dayDelta = r.getRequestParameters().getParameterValue("dayDelta").toInt();
    final int minuteDelta = r.getRequestParameters().getParameterValue("minuteDelta").toInt();
    final boolean allDay = r.getRequestParameters().getParameterValue("allDay").toBoolean();

    return onEventDropped(new DroppedEvent(source, event, dayDelta, minuteDelta, allDay), new CalendarResponse(getCalendar(), target));
  }

  protected abstract boolean onEventDropped(DroppedEvent event, CalendarResponse response);

  @Override
  protected String getRevertScript()
  {
    return "revertFunc();";
  }

}
