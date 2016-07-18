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

package org.projectforge.business.fibu;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class MonthlyEmployeeReportDao
{
  @Autowired
  private TimesheetDao timesheetDao;

  @Autowired
  private EmployeeDao employeeDao;

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public MonthlyEmployeeReport getReport(int year, int month, PFUserDO user)
  {
    if (user == null || year <= 0) {
      return null;
    }
    MonthlyEmployeeReport report = new MonthlyEmployeeReport(year, month);
    EmployeeDO employee = employeeDao.findByUserId(user.getId());
    if (employee != null) {
      report.setEmployee(employee);
    } else {
      report.setUser(user);
    }
    report.init();
    TimesheetFilter filter = new TimesheetFilter();
    filter.setDeleted(false);
    filter.setStartTime(report.getFromDate());
    filter.setStopTime(report.getToDate());
    filter.setUserId(user.getId());
    List<TimesheetDO> list = timesheetDao.getList(filter);
    if (CollectionUtils.isNotEmpty(list) == true) {
      for (TimesheetDO sheet : list) {
        report.addTimesheet(sheet);
      }
    }
    report.calculate();
    return report;
  }

}
