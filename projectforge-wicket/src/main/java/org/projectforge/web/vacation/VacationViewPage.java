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

package org.projectforge.web.vacation;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.vacation.helper.VacationViewHelper;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.bootstrap.GridBuilder;

public class VacationViewPage extends AbstractSecuredPage
{
  private static final long serialVersionUID = 6317381238021216284L;

  @SpringBean
  private EmployeeService employeeService;

  @SpringBean
  private VacationViewHelper vacationViewHelper;

  private GridBuilder gridBuilder;

  public VacationViewPage(final PageParameters parameters)
  {
    this(parameters, null);
  }

  public VacationViewPage(final PageParameters parameters, final AbstractSecuredPage returnToPage)
  {
    super(parameters);
    final PFUserDO currentUser = ThreadLocalUserContext.getUser();
    final EmployeeDO currentEmployee = employeeService.getEmployeeByUserId(currentUser.getPk());
    if (currentEmployee == null) {
      throw new AccessException("access.exception.noEmployeeToUser");
    }
    if (currentEmployee.getUrlaubstage() == null) {
      throw new AccessException("access.exception.employeeHasNoVacationDays");
    }
    gridBuilder = new GridBuilder(body, "flowform", true);
    vacationViewHelper.createVacationView(gridBuilder, currentEmployee);

  }

  @Override
  protected String getTitle()
  {
    return getString("vacation.leaveaccount.title");
  }

}
