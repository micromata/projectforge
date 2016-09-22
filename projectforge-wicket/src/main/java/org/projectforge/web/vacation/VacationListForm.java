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

import org.apache.log4j.Logger;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.vacation.VacationFilter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.wicket.AbstractListForm;

public class VacationListForm extends AbstractListForm<VacationFilter, VacationListPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(VacationListForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  @SpringBean
  private EmployeeService employeeService;

  public VacationListForm(final VacationListPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected VacationFilter newSearchFilterInstance()
  {
    return new VacationFilter(employeeService.getEmployeeByUserId(ThreadLocalUserContext.getUserId()));
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

}
