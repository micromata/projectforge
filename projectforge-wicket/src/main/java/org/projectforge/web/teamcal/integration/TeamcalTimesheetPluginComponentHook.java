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

import java.io.Serializable;

import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.web.teamcal.event.TeamEventEditPage;
import org.projectforge.web.timesheet.TimesheetEditPage;
import org.projectforge.web.timesheet.TimesheetPluginComponentHook;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

/**
 * Adds the switch to team event page button to the {@link TimesheetEditPage}
 *
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 */
public class TeamcalTimesheetPluginComponentHook implements TimesheetPluginComponentHook, Serializable
{
  private static final long serialVersionUID = -8986533310341503141L;

  /**
   * @see org.projectforge.web.timesheet.TimesheetPluginComponentHook#renderComponentsToTimesheetEditForm(org.projectforge.web.timesheet.TimesheetEditForm,
   * org.projectforge.business.timesheet.TimesheetDO)
   */
  @Override
  public void renderComponentsToTimesheetEditForm(final TimesheetEditPage page, final TimesheetDO timesheet)
  {
    if (timesheet == null || timesheet.getId() != null) {
      // Show button only for new timesheets.
      return;
    }
    if (page.getReturnToPage() instanceof TeamCalCalendarPage == false) {
      // May be the add button of time sheet list page was used.
      return;
    }
    @SuppressWarnings("serial")
    final ContentMenuEntryPanel menu = new ContentMenuEntryPanel(page.getNewContentMenuChildId(),
        new SubmitLink(ContentMenuEntryPanel.LINK_ID, page.getForm())
        {
          @Override
          public void onSubmit()
          {
            final TeamEventDO event = new TeamEventDO();
            event.setOwnership(true);
            if (timesheet != null) {
              event.setStartDate(timesheet.getStartTime());
              event.setEndDate(timesheet.getStopTime());
              event.setLocation(timesheet.getLocation());
              event.setNote(timesheet.getDescription());
            }
            setResponsePage(new TeamEventEditPage(new PageParameters(), event).setReturnToPage(page.getReturnToPage()));
          }
        }.setDefaultFormProcessing(false), page.getString("plugins.teamcal.switchToTeamEventButton"));
    page.addContentMenuEntry(menu);
  }
}
