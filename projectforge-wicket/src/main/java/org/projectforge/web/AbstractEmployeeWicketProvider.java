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

package org.projectforge.web;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.framework.utils.NumberHelper;
import org.wicketstuff.select2.ChoiceProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by blumenstein on 13.12.16.
 */
public abstract class AbstractEmployeeWicketProvider extends ChoiceProvider<EmployeeDO>
{
  protected Collection<EmployeeDO> sortedEmployees;

  protected int pageSize = 20;

  /**
   * @param pageSize the pageSize to set
   * @return this for chaining.
   */
  public AbstractEmployeeWicketProvider setPageSize(final int pageSize)
  {
    this.pageSize = pageSize;
    return this;
  }

  public Collection<EmployeeDO> getSortedEmployees()
  {
    return sortedEmployees;
  }

  @Override
  public String getDisplayValue(final EmployeeDO choice)
  {
    return choice.getUser().getFullname();
  }

  @Override
  public String getIdValue(final EmployeeDO choice)
  {
    return String.valueOf(choice.getId());
  }

  @Override
  public Collection<EmployeeDO> toChoices(final Collection<String> ids)
  {
    final List<EmployeeDO> list = new ArrayList<>();
    if (ids == null) {
      return list;
    }
    for (final String str : ids) {
      final Integer employeeId = NumberHelper.parseInteger(str);
      if (employeeId == null) {
        continue;
      }
      EmployeeDO employee = WicketSupport.get(EmployeeDao.class).find(employeeId);
      if (employee != null) {
        list.add(employee);
      }
    }
    return list;
  }

}
