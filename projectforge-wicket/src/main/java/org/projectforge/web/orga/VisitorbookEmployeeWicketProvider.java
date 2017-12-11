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

package org.projectforge.web.orga;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.orga.VisitorbookDO;
import org.projectforge.web.AbstractEmployeeWicketProvider;
import org.wicketstuff.select2.Response;

public class VisitorbookEmployeeWicketProvider extends AbstractEmployeeWicketProvider
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VisitorbookEmployeeWicketProvider.class);

  private static final long serialVersionUID = 6228672635966093257L;

  final private VisitorbookDO visitorbook;

  public VisitorbookEmployeeWicketProvider(VisitorbookDO visitorbook, EmployeeService employeeService)
  {
    super(employeeService);
    this.visitorbook = visitorbook;
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

  @Override
  public void query(String term, final int page, final Response<EmployeeDO> response)
  {
    initSortedEmployees();
    final List<EmployeeDO> result = new ArrayList<>();
    term = term != null ? term.toLowerCase() : "";
    String[] splitTerm = term.split(" ");

    final int offset = page * pageSize;

    int matched = 0;
    boolean hasMore = false;
    for (final EmployeeDO employee : sortedEmployees) {
      if (result.size() == pageSize) {
        hasMore = true;
        break;
      }
      if (Stream.of(splitTerm).allMatch(streamTerm -> employee.getUser().getFullname().toLowerCase().contains(streamTerm))) {
        matched++;
        if (matched > offset) {
          result.add(employee);
        }
      }
    }
    response.addAll(result);
    response.setHasMore(hasMore);
  }

}