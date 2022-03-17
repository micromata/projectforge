/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeStatus;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.AbstractEmployeeWicketProvider;
import org.wicketstuff.select2.Response;

import java.util.*;

public class DefaultEmployeeWicketProvider extends AbstractEmployeeWicketProvider {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultEmployeeWicketProvider.class);

  private static final long serialVersionUID = 6228672123966093257L;

  private boolean withMyself;

  private List<EmployeeStatus> employeeStatusFilter;

  public DefaultEmployeeWicketProvider(EmployeeService employeeService, boolean withMyself, EmployeeStatus... employeeStatusFilter) {
    super(employeeService);
    this.withMyself = withMyself;
    this.employeeStatusFilter = Arrays.asList(employeeStatusFilter);
  }

  @Override
  public void query(String term, final int page, final Response<EmployeeDO> response) {
    boolean hasMore = false;
    Collection<EmployeeDO> result = new ArrayList<>();
    Collection<EmployeeDO> employeesWithoutLoggedInUser = employeeService.findAllActive(false);
    if (CollectionUtils.isEmpty(employeesWithoutLoggedInUser)) {
      employeesWithoutLoggedInUser = new ArrayList<>();
    } else {
      final Integer loggedInUserId = ThreadLocalUserContext.getUserId();
      for (EmployeeDO emp : employeesWithoutLoggedInUser) {
        if (!withMyself && Objects.equals(emp.getUserId(), loggedInUserId)) {
          // Don't add myself as employee.
          continue;
        }
        if (CollectionUtils.isNotEmpty(employeeStatusFilter) && !employeeStatusFilter.contains(employeeService.getEmployeeStatus(emp))) {
          continue;
        }
        if (StringUtils.isNotBlank(term)) {
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
    }
    response.addAll(result);
    response.setHasMore(hasMore);
  }

}
