/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package net.ftlines.wicket.fullcalendar.callback;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Request;
import org.joda.time.DateMidnight;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.projectforge.business.teamcal.filter.ViewType;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

import net.ftlines.wicket.fullcalendar.CalendarResponse;

/**
 * A base callback that passes back calendar's starting date
 * 
 * @author igor
 * 
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
    return new AbstractReadOnlyModel<String>()
    {
      /**
       * @see org.apache.wicket.model.AbstractReadOnlyModel#getObject()
       */
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
