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

package org.projectforge.business.fibu;

import org.apache.commons.collections.CollectionUtils;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class MonthlyEmployeeReportDao {
  @Autowired
  private TimesheetDao timesheetDao;

  @Autowired
  private EmployeeService employeeService;

  @Autowired
  private VacationService vacationService;

  public MonthlyEmployeeReport getReport(int year, int month, PFUserDO user) {
    if (user == null || year <= 0) {
      return null;
    }
    MonthlyEmployeeReport report = new MonthlyEmployeeReport(employeeService, vacationService, user, year, month);
    report.init();
    TimesheetFilter filter = new TimesheetFilter();
    filter.setDeleted(false);
    filter.setStartTime(report.getFromDate());
    filter.setStopTime(report.getToDate());
    filter.setUserId(user.getId());
    List<TimesheetDO> list = timesheetDao.internalGetList(filter, false); // Attention: No access checking!!!!
    PFUserDO loggedInUser = ThreadLocalUserContext.getUser();
    if (CollectionUtils.isNotEmpty(list)) {
      for (TimesheetDO sheet : list) {
        report.addTimesheet(sheet, timesheetDao.hasUserSelectAccess(loggedInUser, sheet, false));
      }
    }
    report.calculate();
    return report;
  }

}
