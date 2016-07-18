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

import net.ftlines.wicket.fullcalendar.CalendarResponse;
import net.ftlines.wicket.fullcalendar.Event;
import net.ftlines.wicket.fullcalendar.EventSource;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Request;

public abstract class EventClickedCallback extends AbstractAjaxCallback implements CallbackWithHandler {
  private static final long serialVersionUID = -8024188125734141661L;

  @Override
  protected String configureCallbackScript(final String script, final String urlTail) {
    return script.replace(urlTail, "&eventId=\"+event.id+\"&sourceId=\"+event.source.data."
        + EventSource.Const.UUID + "+\"");
  }

  @SuppressWarnings("serial")
  @Override
  public IModel<String> getHandlerScript() {
    return new AbstractReadOnlyModel<String>() {
      /**
       * @see org.apache.wicket.model.AbstractReadOnlyModel#getObject()
       */
      @Override
      public String getObject()
      {
        return "function(event) { " + getCallbackScript() + "}";
      }
    };
  }

  @Override
  protected void respond(final AjaxRequestTarget target) {
    final Request r = getCalendar().getRequest();
    final String eventId = r.getRequestParameters().getParameterValue("eventId").toString();
    final String sourceId = r.getRequestParameters().getParameterValue("sourceId").toString();

    final EventSource source = getCalendar().getEventManager().getEventSource(sourceId);
    final Event event = source.getEventProvider().getEventForId(eventId);

    onClicked(new ClickedEvent(source, event), new CalendarResponse(getCalendar(), target));
  }

  protected abstract void onClicked(ClickedEvent event, CalendarResponse response);
}
