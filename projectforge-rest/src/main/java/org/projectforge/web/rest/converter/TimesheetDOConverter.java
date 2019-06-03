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

package org.projectforge.web.rest.converter;

import org.projectforge.business.converter.DOConverter;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.model.rest.Cost2Object;
import org.projectforge.model.rest.TaskObject;
import org.projectforge.model.rest.TimesheetObject;
import org.projectforge.model.rest.UserObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * For conversion of TimesheetDO to time sheet object.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class TimesheetDOConverter
{
  @Autowired
  private Kost2DOConverter kost2DOConverter;

  @Autowired
  private TaskDOConverter taskDOConverter;

  public TimesheetObject getTimesheetObject(final TimesheetDO timesheetDO)
  {
    if (timesheetDO == null) {
      return null;
    }
    final TimesheetObject timesheet = new TimesheetObject();
    DOConverter.copyFields(timesheet, timesheetDO);
    timesheet.setDescription(timesheetDO.getDescription());
    timesheet.setLocation(timesheetDO.getLocation());
    timesheet.setStartTime(timesheetDO.getStartTime());
    timesheet.setStopTime(timesheetDO.getStopTime());
    final TaskObject task = taskDOConverter.getTaskObject(timesheetDO.getTask());
    timesheet.setTask(task);
    final UserObject user = PFUserDOConverter.getUserObject(timesheetDO.getUser());
    timesheet.setUser(user);
    final Cost2Object cost2 = kost2DOConverter.getCost2Object(timesheetDO.getKost2());
    timesheet.setCost2(cost2);
    return timesheet;
  }
}
