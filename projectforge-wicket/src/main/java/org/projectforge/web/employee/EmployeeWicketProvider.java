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

package org.projectforge.web.employee;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.utils.NumberHelper;

import com.vaynberg.wicket.select2.Response;
import com.vaynberg.wicket.select2.TextChoiceProvider;

public class EmployeeWicketProvider extends TextChoiceProvider<EmployeeDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeWicketProvider.class);

  private static final long serialVersionUID = 6228672123966093257L;

  private transient EmployeeService employeeService;

  private int pageSize = 20;

  public EmployeeWicketProvider(EmployeeService employeeService)
  {
    this.employeeService = employeeService;
  }

  /**
   * @param pageSize the pageSize to set
   * @return this for chaining.
   */
  public EmployeeWicketProvider setPageSize(final int pageSize)
  {
    this.pageSize = pageSize;
    return this;
  }

  /**
   * @see com.vaynberg.wicket.select2.TextChoiceProvider#getDisplayText(java.lang.Object)
   */
  @Override
  protected String getDisplayText(final EmployeeDO choice)
  {
    return choice.getUser().getFullname();
  }

  /**
   * @see com.vaynberg.wicket.select2.TextChoiceProvider#getId(java.lang.Object)
   */
  @Override
  protected Object getId(final EmployeeDO choice)
  {
    return choice.getId();
  }

  /**
   * @see com.vaynberg.wicket.select2.ChoiceProvider#query(java.lang.String, int, com.vaynberg.wicket.select2.Response)
   */
  @Override
  public void query(String term, final int page, final Response<EmployeeDO> response)
  {
    boolean hasMore = false;
    Collection<EmployeeDO> result = new ArrayList<>();
    List<EmployeeDO> employeesWithoutLoginedUser = employeeService.findAllActive(false).stream()
        .filter(emp -> emp.getUser().getPk().equals(ThreadLocalUserContext.getUserId()) == false)
        .collect(Collectors.toList());
    for (EmployeeDO emp : employeesWithoutLoginedUser) {
      if (StringUtils.isBlank(term) == false) {
        if (emp.getUser().getFullname().toLowerCase().contains(term.toLowerCase())) {
          result.add(emp);
        }
      } else {
        result.add(emp);
      }
      if (result.size() == pageSize) {
        hasMore = true;
        break;
      }
    }
    response.addAll(result);
    response.setHasMore(hasMore);
  }

  /**
   * @see com.vaynberg.wicket.select2.ChoiceProvider#toChoices(java.util.Collection)
   */
  @Override
  public Collection<EmployeeDO> toChoices(final Collection<String> ids)
  {
    final List<EmployeeDO> list = new ArrayList<>();
    if (ids == null) {
      return list;
    }
    for (final String str : ids) {
      final Integer employeedId = NumberHelper.parseInteger(str);
      if (employeedId == null) {
        continue;
      }
      EmployeeDO employee = employeeService.selectByPkDetached(employeedId);
      if (employee != null) {
        list.add(employee);
      }
    }
    return list;
  }

}