package org.projectforge.plugins.eed.wicket;

import java.util.function.Function;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaService;
import org.projectforge.plugins.eed.EmployeeConfigurationDO;
import org.projectforge.plugins.eed.EmployeeConfigurationTimedDO;
import org.projectforge.plugins.eed.service.EmployeeConfigurationService;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;

import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;

public class EmployeeConfigurationForm extends AbstractEditForm<EmployeeConfigurationDO, EmployeeConfigurationPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeConfigurationForm.class);

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

    showUpdateAndStayButton();

    // AttrPanels
    gridBuilder.newSplitPanel(GridSize.COL100, true); // set hasSubSplitPanel to true to remove borders from this split panel
    final DivPanel divPanel = gridBuilder.getPanel();
    final Function<AttrGroup, EmployeeConfigurationTimedDO> addNewEntryFunction =
        group -> employeeConfigurationService.addNewTimeAttributeRow(data, group.getName());
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
    updateButton.setVisible(false);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
