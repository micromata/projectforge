/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.eed.wicket;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.plugins.eed.model.EmployeeConfigurationDO;
import org.projectforge.plugins.eed.service.EmployeeConfigurationService;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.slf4j.Logger;

import java.util.List;

public class EmployeeConfigurationPage
    extends AbstractEditPage<EmployeeConfigurationDO, EmployeeConfigurationForm, EmployeeConfigurationService>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeConfigurationPage.class);

  @SpringBean
  private EmployeeConfigurationService employeeConfigurationService;

  private List<EmployeeConfigurationDO> dataList;

  public EmployeeConfigurationPage(PageParameters parameters)
  {
    super(parameters, "plugins.eed.config");
    if (parameters != null && parameters.get(PARAMETER_KEY_ID).isEmpty()) {
      parameters.add(PARAMETER_KEY_ID, employeeConfigurationService.getSingleEmployeeConfigurationDOId());
    }
    init();
  }

  @Override
  protected EmployeeConfigurationForm newEditForm(AbstractEditPage<?, ?, ?> parentPage, EmployeeConfigurationDO data)
  {
    return new EmployeeConfigurationForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  public EmployeeConfigurationService getBaseDao()
  {
    return employeeConfigurationService;
  }

  @Override
  public AbstractSecuredBasePage afterSaveOrUpdate()
  {
    return new EmployeeConfigurationPage(getPageParameters());
  }

}
