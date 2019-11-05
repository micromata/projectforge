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

package org.projectforge.plugins.poll.event;

import net.ftlines.wicket.fullcalendar.CalendarResponse;
import net.ftlines.wicket.fullcalendar.EventSource;
import net.ftlines.wicket.fullcalendar.callback.ClickedEvent;
import net.ftlines.wicket.fullcalendar.callback.DroppedEvent;
import net.ftlines.wicket.fullcalendar.callback.ResizedEvent;
import net.ftlines.wicket.fullcalendar.callback.SelectedRange;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.joda.time.DateTime;
import org.projectforge.plugins.poll.NewPollFrontendModel;
import org.projectforge.plugins.poll.NewPollPage;
import org.projectforge.plugins.poll.PollDO;
import org.projectforge.plugins.poll.attendee.PollAttendeePage;
import org.projectforge.web.calendar.MyFullCalendar;
import org.projectforge.web.calendar.MyFullCalendarConfig;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.components.SingleButtonPanel;

import java.util.Collection;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * 
 */
public class PollEventEditPage extends AbstractSecuredPage
{
  private static final long serialVersionUID = 2988767055605267801L;

  // private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PollEventEditPage.class);

  private MyFullCalendarConfig config;

  private MyFullCalendar calendar;

  private RepeatingView eventEntries;

  private PollEventEventsProvider eventProvider;

  private WebMarkupContainer entryContainer;

  private final NewPollFrontendModel model;

  public PollEventEditPage(final PageParameters parameters)
  {
    super(parameters);
    NewPollPage.redirectToNewPollPage(parameters);
    this.model = null;
  }

  public PollEventEditPage(final PageParameters parameters, final NewPollFrontendModel model)
  {
    super(parameters);
    this.model = model;
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    final Form<Void> form = new Form<>("form");
    body.add(form);

    form.add(new Label("title", model.getPollDo().getTitle()));
    form.add(new Label("location", model.getPollDo().getLocation()));
    eventEntries = new RepeatingView("eventEntries");
    eventEntries.setVisible(true);
    entryContainer = new WebMarkupContainer("entryContainer")
    {
      private static final long serialVersionUID = -2897780301098962428L;

      /**
       * @see org.apache.wicket.Component#onBeforeRender()
       */
      @Override
      protected void onBeforeRender()
      {
        super.onBeforeRender();
        eventEntries.removeAll();
        for (final PollEventDO pollEvent : eventProvider.getAllEvents()) {
          eventEntries.add(new PollEventEntryPanel(eventEntries.newChildId(), pollEvent)
          {
            private static final long serialVersionUID = -3844278068979559030L;

            /**
             * @see org.projectforge.plugins.poll.event.PollEventEntryPanel#onDeleteClick(org.apache.wicket.ajax.AjaxRequestTarget)
             */
            @Override
            protected void onDeleteClick(final AjaxRequestTarget target)
            {
              target.appendJavaScript("$('#"
                  + calendar.getMarkupId()
                  + "').fullCalendar('removeEvents', "
                  + eventProvider.getEventForPollEvent(pollEvent).getId()
                  + ");");
              target.add(entryContainer);
              eventProvider.removeElement(pollEvent);
            }
          });
        }
      }
    };

    final Button nextButton = new Button(SingleButtonPanel.WICKET_ID)
    {
      private static final long serialVersionUID = -7779593314951993472L;

      @Override
      public final void onSubmit()
      {
        if (!eventProvider.getAllEvents().isEmpty()) {
          onNextButtonClick(model.getPollDo(), eventProvider.getAllEvents());
        } else {
          this.error(getString("plugins.poll.event.error"));
        }
      }
    };
    nextButton.setDefaultFormProcessing(false);
    final SingleButtonPanel nextButtonPanel = new SingleButtonPanel("continueButton", nextButton, getString("next"),
        SingleButtonPanel.DEFAULT_SUBMIT);
    nextButtonPanel.setOutputMarkupId(true);
    form.add(nextButtonPanel);

    entryContainer.add(eventEntries);
    entryContainer.setOutputMarkupId(true);
    form.add(entryContainer);

    eventProvider = new PollEventEventsProvider(model.getPollDo());
    if (!model.getAllEvents().isEmpty()) {
      for (final PollEventDO event : model.getAllEvents()) {
        eventProvider.addEvent(
            new SelectedRange(new DateTime(event.getStartDate()), new DateTime(event.getEndDate()), false), null);
      }
    }
    config = new MyFullCalendarConfig(this);
    config.setSelectable(true);
    config.setEditable(true);
    config.setSelectHelper(true);
    config.setDefaultView("agendaWeek");
    config.getHeader().setRight("");
    config.setEnableContextMenu(false);
    config.setLoading("function(bool) { if (bool) $(\"#loading\").show(); else $(\"#loading\").hide(); }");
    calendar = new MyFullCalendar("cal", config)
    {
      private static final long serialVersionUID = -6819899072933690316L;

      /**
       * @see net.ftlines.wicket.fullcalendar.FullCalendar#onDateRangeSelected(net.ftlines.wicket.fullcalendar.callback.SelectedRange,
       *      net.ftlines.wicket.fullcalendar.CalendarResponse)
       */
      @Override
      protected void onDateRangeSelected(final SelectedRange range, final CalendarResponse response)
      {
        eventProvider.addEvent(range, response);
        response.getTarget().add(entryContainer);
      }

      /**
       * @see net.ftlines.wicket.fullcalendar.FullCalendar#onEventResized(net.ftlines.wicket.fullcalendar.callback.ResizedEvent,
       *      net.ftlines.wicket.fullcalendar.CalendarResponse)
       */
      @Override
      protected boolean onEventResized(final ResizedEvent event, final CalendarResponse response)
      {
        response.getTarget().add(entryContainer);
        return eventProvider.resizeEvent(event, response);
      }

      /**
       * @see net.ftlines.wicket.fullcalendar.FullCalendar#onEventDropped(net.ftlines.wicket.fullcalendar.callback.DroppedEvent,
       *      net.ftlines.wicket.fullcalendar.CalendarResponse)
       */
      @Override
      protected boolean onEventDropped(final DroppedEvent event, final CalendarResponse response)
      {
        response.getTarget().add(entryContainer);
        return eventProvider.dropEvent(event, response);
      }

      /**
       * @see net.ftlines.wicket.fullcalendar.FullCalendar#onEventClicked(net.ftlines.wicket.fullcalendar.callback.ClickedEvent,
       *      net.ftlines.wicket.fullcalendar.CalendarResponse)
       */
      @Override
      protected void onEventClicked(final ClickedEvent event, final CalendarResponse response)
      {
        response.getTarget().add(entryContainer);
        eventProvider.eventClicked(event, response);
      }
    };
    calendar.setMarkupId("calendar");
    final EventSource eventSource = new EventSource();
    eventSource.setEventsProvider(eventProvider);
    config.add(eventSource);
    form.add(calendar);

  }

  /**
   * @param allEvents
   */
  protected void onNextButtonClick(final PollDO pollDo, final Collection<PollEventDO> allEvents)
  {
    model.getAllEvents().clear();
    model.getAllEvents().addAll(allEvents);
    setResponsePage(new PollAttendeePage(getPageParameters(), model));
  }

  /**
   * @see org.projectforge.web.wicket.AbstractUnsecureBasePage#getTitle()
   */
  @Override
  protected String getTitle()
  {
    return getString("plugins.poll.event");
  }

}
