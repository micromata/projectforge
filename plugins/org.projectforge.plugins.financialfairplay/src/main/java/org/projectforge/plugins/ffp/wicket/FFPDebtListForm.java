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

package org.projectforge.plugins.ffp.wicket;

import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFPDebtListForm extends AbstractListForm<FFPDebtFilter, FFPDebtListPage>
{
  private static final Logger log = LoggerFactory.getLogger(FFPDebtListForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  @SpringBean
  private EmployeeService employeeService;

  public FFPDebtListForm(final FFPDebtListPage parentPage)
  {
    super(parentPage);
  }
  
  @Override
  protected void onOptionsPanelCreate(FieldsetPanel optionsFieldsetPanel, DivPanel optionsCheckBoxesPanel) {
    final FFPDebtFilter ffpDebtFilter = getSearchFilter();
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<>(ffpDebtFilter, "fromMe"), getString("plugins.ffp.debt.options.fromMe")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<>(ffpDebtFilter, "toMe"), getString("plugins.ffp.debt.options.toMe")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<>(ffpDebtFilter, "iNeedToApprove"), getString("plugins.ffp.debt.options.iNeedToApprove")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<>(ffpDebtFilter, "hideBothApproved"), getString("plugins.ffp.debt.options.hideBothApprove")));
  }
  

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  protected FFPDebtFilter newSearchFilterInstance()
  {
    return new FFPDebtFilter(ThreadLocalUserContext.getUserId());
  }
}
