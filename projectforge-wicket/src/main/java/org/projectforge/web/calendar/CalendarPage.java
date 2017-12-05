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

package org.projectforge.web.calendar;

import java.util.Date;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.joda.time.DateMidnight;
import org.projectforge.business.teamcal.filter.ICalendarFilter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractStandardFormPage;

public class CalendarPage extends AbstractStandardFormPage implements ISelectCallerPage
{
  private static final long serialVersionUID = 8710165041912824126L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CalendarPage.class);

  private static final String USERPREF_KEY = "CalendarPage.userPrefs";

  private CalendarForm form;

  protected CalendarPanel calendarPanel;

  protected final PageParameters pageParameters;

  public CalendarPage(final PageParameters parameters)
  {
    super(parameters);
    this.pageParameters = parameters;
    initPage();
  }

  public void initPage()
  {
    form = initCalendarForm(this);
    body.add(form);
    final ICalendarFilter filter = initCalendarFilter();
    form.setFilter(filter);
    form.init();
    calendarPanel = initCalenderPanel();
    if (pageParameters != null) {
      if (pageParameters.get("showTimesheets").isNull() == false) {
        form.getFilter().setTimesheetUserId(getUserId());
      }
      if (pageParameters.get("showBirthdays").isNull() == false) {
        form.getFilter().setShowBirthdays(true);
      }
    }
  }

  /**
   * @param calendarPage
   * @return
   */
  protected CalendarForm initCalendarForm(final CalendarPage calendarPage)
  {
    return new CalendarForm(this);
  }

  /**
   * @return
   */
  protected CalendarPanel initCalenderPanel()
  {
    final CalendarPanel calendar = new CalendarPanel("cal", form.getCurrentDatePanel());
    calendar.setOutputMarkupId(true);
    body.add(calendar);
    calendar.init(getFilter());
    return calendar;
  }

  /**
   * 
   * @return
   */
  protected ICalendarFilter initCalendarFilter()
  {
    CalendarFilter filter = (CalendarFilter) getUserPrefEntry(USERPREF_KEY);
    if (filter == null) {
      filter = new CalendarFilter();
      putUserPrefEntry(USERPREF_KEY, filter, true);
    }
    return filter;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSecuredPage#onBeforeRender()
   */
  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    calendarPanel.forceReload(); // Needed if e. g. user or anything was changed.
  }

  @Override
  protected String getTitle()
  {
    return getString("calendar.title");
  }

  protected ICalendarFilter getFilter()
  {
    return form.getFilter();
  }

  /**
   * Does nothing at default, is called if the user clicks the "refresh" icon.
   */
  protected void refresh()
  {
  }

  @Override
  public void cancelSelection(final String property)
  {
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("userId".equals(property) == true) {
      getFilter().setTimesheetUserId((Integer) selectedValue);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(final String property)
  {
    if ("userId".equals(property) == true) {
      getFilter().setTimesheetUserId(null);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  /**
   * @param startDate
   * @return this for chaining.
   */
  public CalendarPage setStartDate(final Date startDate)
  {
    form.getFilter().setStartDate(new DateMidnight(startDate, ThreadLocalUserContext.getDateTimeZone()));
    return this;
  }

  /**
   * @return the form
   */
  public CalendarForm getForm()
  {
    return form;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSecuredPage#isBreadCrumbVisible()
   */
  @Override
  protected boolean isBreadCrumbVisible()
  {
    return false;
  }
}
