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

package org.projectforge.web.timesheet;

import org.apache.wicket.model.IModel;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.calendar.AbstractICSExportDialog;
import org.projectforge.web.calendar.CalendarFeedService;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TimesheetsICSExportDialog extends AbstractICSExportDialog
{
  private static final long serialVersionUID = 8054215266883084988L;

  private Integer timesheetUserId;

  CalendarFeedService calendarFeedService;

  /**
   * @param id
   * @param titleModel
   */
  public TimesheetsICSExportDialog(CalendarFeedService calendarFeedService, final String id,
      final IModel<String> titleModel)
  {
    super(id, titleModel);
    this.calendarFeedService = calendarFeedService;
  }

  public void init(final Integer timesheetUserId)
  {
    super.init();
    this.timesheetUserId = timesheetUserId;
  }

  /**
   * @see org.projectforge.web.calendar.AbstractICSExportDialog#getUrl()
   */
  @Override
  protected String getUrl()
  {
    return calendarFeedService
        .getUrl4Timesheets(timesheetUserId != null ? timesheetUserId : ThreadLocalUserContext.getUserId());
  }

}
