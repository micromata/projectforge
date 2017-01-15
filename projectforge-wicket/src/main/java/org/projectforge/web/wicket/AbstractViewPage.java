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

package org.projectforge.web.wicket;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.wicket.bootstrap.GridBuilder;

public abstract class AbstractViewPage extends AbstractSecuredPage
{
  private static final long serialVersionUID = 6317381238021216284L;

  @SpringBean
  protected EmployeeService employeeService;

  protected GridBuilder gridBuilder;

  protected PFUserDO currentUser;

  protected EmployeeDO currentEmployee;

  protected WebMarkupContainer container;

  public AbstractViewPage(final PageParameters parameters)
  {
    super(parameters);
  }

  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    currentUser = ThreadLocalUserContext.getUser();
    currentEmployee = employeeService.getEmployeeByUserId(currentUser.getPk());
  }

  @Override
  protected void onConfigure()
  {
    super.onConfigure();
    container = new WebMarkupContainer("container");
    body.addOrReplace(container);
    gridBuilder = new GridBuilder(container, "flowform", true);
  }

}
