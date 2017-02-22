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

package org.projectforge.plugins.ffp.wicket;

import org.apache.log4j.Logger;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.wicket.AbstractListForm;

public class FFPDebtListForm extends AbstractListForm<FFPDebtFilter, FFPDebtListPage>
{
  private static final Logger log = Logger.getLogger(FFPDebtListForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  @SpringBean
  private EmployeeService employeeService;

  public FFPDebtListForm(final FFPDebtListPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  protected FFPDebtFilter newSearchFilterInstance()
  {
    EmployeeDO employee = employeeService.getEmployeeByUserId(ThreadLocalUserContext.getUserId());
    if (employee == null) {
      throw new AccessException("access.exception.noEmployeeToUser");
    }
    return new FFPDebtFilter(employee.getPk());
  }
}
