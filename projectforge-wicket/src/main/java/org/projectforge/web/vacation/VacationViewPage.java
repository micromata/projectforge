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
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.web.vacation.helper.VacationViewHelper;
import org.projectforge.web.wicket.AbstractViewPage;

public class VacationViewPage extends AbstractViewPage
{
  private static final long serialVersionUID = 6317381238012316284L;

  @SpringBean
  private VacationViewHelper vacationViewHelper;

  @SpringBean
  private VacationService vacationService;

  public VacationViewPage(final PageParameters parameters)
  {
    super(parameters);
  }

  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    vacationService.couldUserUseVacationService(currentUser, true); // throw runtime exception if not allowed
  }

  @Override
  protected void onConfigure()
  {
    super.onConfigure();
    vacationViewHelper.createVacationView(gridBuilder, currentEmployee, true, this);
  }

  @Override
  protected String getTitle()
  {
    return getString("vacation.leaveaccount.title");
  }

}
