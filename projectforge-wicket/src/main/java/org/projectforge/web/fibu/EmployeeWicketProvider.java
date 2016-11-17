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

package org.projectforge.web.fibu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.orga.VisitorbookDO;
import org.projectforge.framework.utils.NumberHelper;

import com.vaynberg.wicket.select2.Response;
import com.vaynberg.wicket.select2.TextChoiceProvider;

public class EmployeeWicketProvider extends TextChoiceProvider<EmployeeDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeWicketProvider.class);

  private static final long serialVersionUID = 6228672635966093257L;

  final private VisitorbookDO visitorbook;

  private List<EmployeeDO> sortedEmployees;

  private transient EmployeeService employeeService;

  private int pageSize = 20;

  public EmployeeWicketProvider(VisitorbookDO visitorbook, EmployeeService employeeService)
  {
    this.visitorbook = visitorbook;
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

  public void initSortedEmployees()
  {
    if (sortedEmployees == null) {
      sortedEmployees = employeeService.getAll(false);
      Set<EmployeeDO> assignedEmployees = visitorbook.getContactPersons();
      List<EmployeeDO> removeEmployeeList = new ArrayList<>();
      if (assignedEmployees != null) {
        for (EmployeeDO contactPerson : sortedEmployees) {
          for (EmployeeDO alreadyAssignedEmployee : assignedEmployees) {
            if (contactPerson.equals(alreadyAssignedEmployee)) {
              removeEmployeeList.add(contactPerson);
            }
          }
        }
        sortedEmployees.removeAll(removeEmployeeList);
      }
    }
  }

  public List<EmployeeDO> getSortedEmployees()
  {
    return sortedEmployees;
  }

  /**
   * @see TextChoiceProvider#getDisplayText(Object)
   */
  @Override
  protected String getDisplayText(final EmployeeDO choice)
  {
    return choice.getUser().getFullname();
  }

  /**
   * @see TextChoiceProvider#getId(Object)
   */
  @Override
  protected Object getId(final EmployeeDO choice)
  {
    return choice.getPk();
  }

  /**
   * @see com.vaynberg.wicket.select2.ChoiceProvider#query(String, int, Response)
   */
  @Override
  public void query(String term, final int page, final Response<EmployeeDO> response)
  {
    initSortedEmployees();
    final List<EmployeeDO> result = new ArrayList<>();
    term = term.toLowerCase();
    String[] splitTerm = term.split(" ");

    final int offset = page * pageSize;

    int matched = 0;
    boolean hasMore = false;
    for (final EmployeeDO employee : sortedEmployees) {
      if (result.size() == pageSize) {
        hasMore = true;
        break;
      }
      if (Stream.of(splitTerm)
          .allMatch(streamTerm -> employee.getUser().getFullname().toLowerCase().contains(streamTerm))) {
        matched++;
        if (matched > offset) {
          result.add(employee);
        }
      }
    }
    response.addAll(result);
    response.setHasMore(hasMore);
  }

  /**
   * @see com.vaynberg.wicket.select2.ChoiceProvider#toChoices(Collection)
   */
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
      EmployeeDO employee = employeeService.selectByPkDetached(employeeId);
      if (employee != null) {
        list.add(employee);
      }
    }
    return list;
  }

}