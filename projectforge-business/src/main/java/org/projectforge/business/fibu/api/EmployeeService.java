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

package org.projectforge.business.fibu.api;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeStatus;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.projectforge.business.fibu.MonthlyEmployeeReport;
import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.IPersistenceService;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * Access to employee.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public interface EmployeeService extends IPersistenceService<EmployeeDO>, IDao<EmployeeDO>
{
  void setPfUser(EmployeeDO employee, Integer userId);

  void setKost1(final EmployeeDO employee, final Integer kost1Id);

  EmployeeTimedDO addNewTimeAttributeRow(final EmployeeDO employee, final String groupName);

  EmployeeDO getEmployeeByUserId(final Integer userId);

  ModificationStatus updateAttribute(Integer userId, Object attribute, String attributeName);

  boolean isEmployeeActive(EmployeeDO employee);

  BigDecimal getMonthlySalary(EmployeeDO employee, Calendar selectedDate);

  Collection<EmployeeDO> findAllActive(boolean checkAccess);

  EmployeeDO getEmployeeByStaffnumber(String staffnumber);

  List<EmployeeDO> getAll(boolean checkAccess);

  EmployeeStatus getEmployeeStatus(EmployeeDO employee);

  String getStudentVacationCountPerDay(EmployeeDO currentEmployee);

  MonthlyEmployeeReport getReportOfMonth(int year, int month, PFUserDO user);

  /**
   * Checks if the employee was full time some day at the beginning of the month or within the month.
   *
   * @param employee     The employee.
   * @param selectedDate The first day of the month to check.
   * @return The result.
   */
  boolean isFulltimeEmployee(EmployeeDO employee, Calendar selectedDate);
}
