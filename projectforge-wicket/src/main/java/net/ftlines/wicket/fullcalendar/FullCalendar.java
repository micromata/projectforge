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

package net.ftlines.wicket.fullcalendar;

import net.ftlines.wicket.fullcalendar.callback.*;
import org.apache.wicket.behavior.IBehaviorListener;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.collections.MicroMap;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.apache.wicket.util.template.TextTemplate;
import org.projectforge.framework.ToStringUtil;

import java.util.UUID;

public class FullCalendar extends AbstractFullCalendar implements IBehaviorListener {
  private static final long serialVersionUID = 6517344280923639300L;

  /**
   * Field can't be static final because otherwise a severe Exception is thrown on the server's startup due to loading sessions from
   * persistent storage.
   */
  private static TextTemplate EVENTS;

  private final Config config;

  private EventDroppedCallback eventDropped;

  private EventResizedCallback eventResized;

  private GetEventsCallback getEvents;

  private DateRangeSelectedCallback dateRangeSelected;

  private EventClickedCallback eventClicked;

  private ViewDisplayCallback viewDisplay;

  public FullCalendar(final String id, final Config config) {
    super(id);
    if (EVENTS == null) {
      EVENTS = new PackageTextTemplate(FullCalendar.class, "FullCalendar.events.tpl");
    }
    this.config = config;
    setVersioned(false);
  }

  public Config getConfig() {
    return config;
  }

  public EventManager getEventManager() {
    return new EventManager(this);
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    for (final EventSource source : config.getEventSources()) {

      final String uuid = UUID.randomUUID().toString().replaceAll("[^A-Za-z0-9]", "");
      source.setUuid(uuid);
    }
  }

  @Override
  protected void onBeforeRender() {
    super.onBeforeRender();
    setupCallbacks();
  }

  @SuppressWarnings("serial")
  private void setupCallbacks() {

    if (getEvents != null)
      return;

    getEvents = new GetEventsCallback();
    add(getEvents);
    for (final EventSource source : config.getEventSources()) {
      source.setEventsModel(new AbstractReadOnlyModel<String>() {
        @Override
        public String getObject() {
          return EVENTS.asString(new MicroMap<String, String>("url", getEvents.getUrl(source)));
        }
      });
    }

    if (Strings.isEmpty(config.getEventClick()) == true) {
      add(eventClicked = new EventClickedCallback() {
        @Override
        protected void onClicked(final ClickedEvent event, final CalendarResponse response) {
          onEventClicked(event, response);
        }
      });
      config.setEventClickModel(eventClicked.getHandlerScript());
    }

    if (Strings.isEmpty(config.getSelect()) == true) {
      add(dateRangeSelected = new DateRangeSelectedCallback(config.isIgnoreTimezone()) {
        @Override
        protected void onSelect(final SelectedRange range, final CalendarResponse response) {
          FullCalendar.this.onDateRangeSelected(range, response);
        }
      });
      config.setSelectModel(dateRangeSelected.getHandlerScript());
    }

    if (Strings.isEmpty(config.getEventDrop()) == true) {
      add(eventDropped = new EventDroppedCallback(config) {

        @Override
        protected boolean onEventDropped(final DroppedEvent event, final CalendarResponse response) {
          return FullCalendar.this.onEventDropped(event, response);
        }
      });
      config.setEventDropModel(eventDropped.getHandlerScript());
    }

    if (Strings.isEmpty(config.getEventResize()) == true) {
      add(eventResized = new EventResizedCallback() {

        @Override
        protected boolean onEventResized(final ResizedEvent event, final CalendarResponse response) {
          return FullCalendar.this.onEventResized(event, response);
        }

      });

      config.setEventResizeModel(eventResized.getHandlerScript());
    }

    if (Strings.isEmpty(config.getViewDisplay()) == true) {
      add(viewDisplay = new ViewDisplayCallback() {
        @Override
        protected void onViewDisplayed(final View view, final CalendarResponse response) {
          FullCalendar.this.onViewDisplayed(view, response);
        }
      });
      config.setViewDisplayModel(viewDisplay.getHandlerScript());
    }

    getPage().dirty();
  }

  @Override
  public void renderHead(final IHeaderResponse response) {
    super.renderHead(response);

    String configuration = "$(\"#" + getMarkupId() + "\").fullCalendarExt(";
    configuration += ToStringUtil.toJsonString(config);
    configuration += ");";
    response.render(OnDomReadyHeaderItem.forScript(configuration));

  }

  protected boolean onEventDropped(final DroppedEvent event, final CalendarResponse response) {
    return false;
  }

  protected boolean onEventResized(final ResizedEvent event, final CalendarResponse response) {
    return false;
  }

  protected void onDateRangeSelected(final SelectedRange range, final CalendarResponse response) {

  }

  protected void onEventClicked(final ClickedEvent event, final CalendarResponse response) {

  }

  protected void onViewDisplayed(final View view, final CalendarResponse response) {

  }

  public AjaxConcurrency getAjaxConcurrency() {
    return AjaxConcurrency.QUEUE;
  }

  @Override
  public void onRequest() {
    getEvents.onRequest();

  }

}
