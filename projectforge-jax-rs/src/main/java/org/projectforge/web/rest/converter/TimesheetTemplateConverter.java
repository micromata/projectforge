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

import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.user.UserPrefDao;
import org.projectforge.framework.persistence.user.entities.UserPrefDO;
import org.projectforge.model.rest.Cost2Object;
import org.projectforge.model.rest.TaskObject;
import org.projectforge.model.rest.TimesheetTemplateObject;
import org.projectforge.model.rest.UserObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class TimesheetTemplateConverter
{
  @Autowired
  private UserPrefDao userPrefDao;

  @Autowired
  private Kost2DOConverter kost2DOConverter;

  @Autowired
  private TaskDOConverter taskDOConverter;

  public TimesheetTemplateObject getTimesheetTemplateObject(final UserPrefDO userPrefDO)
  {
    if (userPrefDO == null) {
      return null;
    }
    final TimesheetTemplateObject template = new TimesheetTemplateObject();
    final TimesheetDO timesheet = new TimesheetDO();
    userPrefDao.fillFromUserPrefParameters(userPrefDO, timesheet);
    template.setName(userPrefDO.getName());
    template.setDescription(timesheet.getDescription());
    template.setLocation(timesheet.getLocation());
    final UserObject user = PFUserDOConverter.getUserObject(timesheet.getUser());
    if (user != null) {
      template.setUser(user);
    }
    final TaskObject task = taskDOConverter.getTaskObject(timesheet.getTask());
    if (task != null) {
      template.setTask(task);
    }
    final Cost2Object cost2 = kost2DOConverter.getCost2Object(timesheet.getKost2());
    if (cost2 != null) {
      template.setCost2(cost2);
    }
    return template;
  }
}
