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

package org.projectforge.web.teamcal.integration;

import java.util.Set;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.externalsubscription.TeamEventExternalSubscriptionCache;
import org.projectforge.business.teamcal.filter.ICalendarFilter;
import org.projectforge.business.teamcal.filter.TeamCalCalendarFilter;
import org.projectforge.business.teamcal.filter.TemplateEntry;
import org.projectforge.web.calendar.CalendarForm;
import org.projectforge.web.calendar.CalendarPage;
import org.projectforge.web.calendar.CalendarPanel;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * 
 */
public class TeamCalCalendarPage extends CalendarPage
{
  private static final long serialVersionUID = -6413028759027309796L;

  private TeamCalCalendarForm form;

  @SpringBean
  private transient TeamCalDao teamCalDao;

  @SpringBean
  private transient TeamCalCache teamCalCache;

  @SpringBean
  private transient TeamEventExternalSubscriptionCache teamEventExternalSubscriptionCache;

  public static final String USERPREF_KEY = "TeamCalendarPage.userPrefs";

  /**
   * @param parameters
   */
  public TeamCalCalendarPage(final PageParameters parameters)
  {
    super(parameters);
  }

  @Override
  protected CalendarForm initCalendarForm(final CalendarPage calendarPage)
  {
    this.form = new TeamCalCalendarForm(calendarPage);
    return this.form;
  }

  @Override
  protected CalendarPanel initCalenderPanel()
  {
    final TeamCalCalendarPanel calendar = new TeamCalCalendarPanel("cal", form.getCurrentDatePanel());
    calendar.setOutputMarkupId(true);
    body.add(calendar);
    calendar.init(getFilter());
    return calendar;
  }

  /**
   * @see org.projectforge.web.calendar.CalendarPage#initCalendarFilter()
   */
  @Override
  protected ICalendarFilter initCalendarFilter()
  {
    TeamCalCalendarFilter filter = (TeamCalCalendarFilter) getUserPrefEntry(USERPREF_KEY);
    if (filter == null) {
      filter = new TeamCalCalendarFilter();
      putUserPrefEntry(USERPREF_KEY, filter, true);
    }
    return filter;

  }

  /**
   * @see org.projectforge.web.calendar.CalendarPage#refresh()
   */
  @Override
  protected void refresh()
  {
    super.refresh();
    final TeamCalCalendarFilter filter = getFilter();
    final TemplateEntry templateEntry = filter.getActiveTemplateEntry();
    if (templateEntry == null) {
      // Nothing to do.
      return;
    }
    final Set<Integer> visibleCalendarIds = templateEntry.getVisibleCalendarIds();
    if (visibleCalendarIds == null) {
      // Nothing to do.
      return;
    }
    for (final Integer calId : visibleCalendarIds) {
      final TeamCalDO teamCalDO = teamCalCache.getCalendar(calId);
      if (teamCalDO == null || !teamCalDO.getExternalSubscription()) {
        // Nothing to do.
        continue;
      }
      teamEventExternalSubscriptionCache.updateCache(teamCalDO, true);
    }
  }

  @Override
  protected TeamCalCalendarFilter getFilter()
  {
    return form.getFilter();
  }
}
