/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu.impl;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeSalaryDO;
import org.projectforge.business.fibu.EmployeeSalaryDao;
import org.projectforge.business.fibu.api.EmployeeSalaryService;
import org.projectforge.framework.time.PFDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeSalaryServiceImpl implements EmployeeSalaryService
{

  @Autowired
  private EmployeeSalaryDao employeeSalaryDao;

  @Override
  public EmployeeSalaryDO getLatestSalaryForEmployee(EmployeeDO employee)
  {
    List<EmployeeSalaryDO> findByEmployee = employeeSalaryDao.findByEmployee(employee);
    return findByEmployee
        .stream()
        .sorted((sal1, sal2) -> {
          PFDateTime c1 = PFDateTime.withDate(sal1.getYear(), sal1.getMonth(), 1);
          PFDateTime c2 = PFDateTime.withDate(sal2.getYear(), sal2.getMonth(), 1);
          return c2.getCalendar().compareTo(c1.getCalendar());
        })
        .findFirst()
        .orElse(null);
  }

  @Override
  public EmployeeSalaryDO getEmployeeSalaryByDate(EmployeeDO employee, PFDateTime selectedDate)
  {
    List<EmployeeSalaryDO> findByEmployee = employeeSalaryDao.findByEmployee(employee);
    for (EmployeeSalaryDO sal : findByEmployee) {
      if (sal.getYear().equals(selectedDate.getYear()) && sal.getMonth().equals(selectedDate.getYear())) {
        return sal;
      }
    }
    return null;
  }

  @Override
  public void saveOrUpdate(EmployeeSalaryDO employeeSalaryDO)
  {
    employeeSalaryDao.saveOrUpdateInTrans(employeeSalaryDO);
  }

  @Override
  public EmployeeSalaryDO selectByPk(Long id)
  {
    return employeeSalaryDao.getById(id);
  }

}
