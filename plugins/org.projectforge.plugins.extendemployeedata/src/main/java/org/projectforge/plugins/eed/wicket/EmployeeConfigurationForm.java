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

import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaService;
import org.projectforge.plugins.eed.model.EmployeeConfigurationDO;
import org.projectforge.plugins.eed.model.EmployeeConfigurationTimedDO;
import org.projectforge.plugins.eed.service.EmployeeConfigurationService;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.slf4j.Logger;

import java.util.function.Function;

public class EmployeeConfigurationForm extends AbstractEditForm<EmployeeConfigurationDO, EmployeeConfigurationPage>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeConfigurationForm.class);

  @SpringBean
  private GuiAttrSchemaService attrSchemaService;

  @SpringBean
  private EmployeeConfigurationService employeeConfigurationService;

  public EmployeeConfigurationForm(EmployeeConfigurationPage parentPage, EmployeeConfigurationDO data)
  {
    super(parentPage, data);
  }

  @Override
  protected void init()
  {
    super.init();
    // AttrPanels
    gridBuilder.newSplitPanel(GridSize.COL100, true); // set hasSubSplitPanel to true to remove borders from this split panel
    final DivPanel divPanel = gridBuilder.getPanel();
    final Function<AttrGroup, EmployeeConfigurationTimedDO> addNewEntryFunction = group -> employeeConfigurationService
        .addNewTimeAttributeRow(data, group.getName());
    attrSchemaService.createAttrPanels(divPanel, data, parentPage, addNewEntryFunction);
  }

  @Override
  protected void updateButtonVisibility()
  {
    createButtonPanel.setVisible(false);
    deleteButtonPanel.setVisible(false);
    markAsDeletedButtonPanel.setVisible(false);
    undeleteButtonPanel.setVisible(false);
    updateAndNextButtonPanel.setVisible(false);
    try {
      checkAccess();
      updateButton.setVisible(true);
    } catch (AccessException ex) {
      log.info("No right for data update. " + ex.getMessage());
      updateButton.setVisible(false);
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  private void checkAccess()
  {
    parentPage.getAccessChecker().checkLoggedInUserRight(UserRightId.HR_EMPLOYEE_SALARY, UserRightValue.READWRITE);
    parentPage.getAccessChecker().checkRestrictedOrDemoUser();
  }
}
