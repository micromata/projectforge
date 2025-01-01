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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Request;
import org.joda.time.DateMidnight;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.projectforge.business.teamcal.filter.ViewType;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

/**
 * A base callback that passes back calendar's starting date
 *
 * @author igor
 */
public abstract class ViewDisplayCallback extends AbstractAjaxCallback implements CallbackWithHandler
{
  private static final long serialVersionUID = 7815962750349404797L;

  @Override
  protected String configureCallbackScript(final String script, final String urlTail)
  {
    return script
        .replace(
            urlTail,
            "&view=\"+v.name+\"&start=\"+fullCalendarExtIsoDate(v.start)+\"&end=\"+fullCalendarExtIsoDate(v.end)+\"&visibleStart=\"+fullCalendarExtIsoDate(v.visStart)+\"&visibleEnd=\"+fullCalendarExtIsoDate(v.visEnd)+\"");
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
        return String.format("function(v) {%s;}", getCallbackScript());
      }
    };
  }

  @Override
  protected void respond(final AjaxRequestTarget target)
  {
    final Request r = target.getPage().getRequest();
    final ViewType type = ViewType.forCode(r.getRequestParameters().getParameterValue("view").toString());
    final DateTimeFormatter fmt = ISODateTimeFormat.dateTimeParser().withZone(ThreadLocalUserContext.getDateTimeZone());
    final DateMidnight start = fmt.parseDateTime(r.getRequestParameters().getParameterValue("start").toString())
        .toDateMidnight();
    final DateMidnight end = fmt.parseDateTime(r.getRequestParameters().getParameterValue("end").toString())
        .toDateMidnight();
    final DateMidnight visibleStart = fmt.parseDateTime(
        r.getRequestParameters().getParameterValue("visibleStart").toString()).toDateMidnight();
    final DateMidnight visibleEnd = fmt
        .parseDateTime(r.getRequestParameters().getParameterValue("visibleEnd").toString()).toDateMidnight();
    final View view = new View(type, start, end, visibleStart, visibleEnd);
    final CalendarResponse response = new CalendarResponse(getCalendar(), target);
    onViewDisplayed(view, response);
  }

  protected abstract void onViewDisplayed(View view, CalendarResponse response);
}
