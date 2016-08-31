package org.projectforge.plugins.eed.wicket;

import java.util.function.Function;

import org.apache.log4j.Logger;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaService;
import org.projectforge.plugins.eed.model.EmployeeConfigurationDO;
import org.projectforge.plugins.eed.model.EmployeeConfigurationTimedDO;
import org.projectforge.plugins.eed.service.EmployeeConfigurationServiceImpl;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.DivPanel;

import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;

public class EmployeeConfigurationForm extends AbstractEditForm<EmployeeConfigurationDO, EmployeeConfigurationPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeConfigurationForm.class);

  @SpringBean
  private GuiAttrSchemaService attrSchemaService;

  @SpringBean
  private EmployeeConfigurationServiceImpl employeeConfigurationService;

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
    attrSchemaService.createTimedAttrPanels(divPanel, data, parentPage, addNewEntryFunction);
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

  protected void checkAccess()
  {
    parentPage.getAccessChecker().checkLoggedInUserRight(UserRightId.HR_EMPLOYEE, UserRightValue.READWRITE);
    parentPage.getAccessChecker().checkRestrictedOrDemoUser();
  }
}
