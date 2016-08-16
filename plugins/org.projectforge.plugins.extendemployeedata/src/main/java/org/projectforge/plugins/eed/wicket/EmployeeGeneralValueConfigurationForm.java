package org.projectforge.plugins.eed.wicket;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.SingleButtonPanel;

public class EmployeeGeneralValueConfigurationForm extends AbstractListForm<BaseSearchFilter,EmployeeGeneralValueConfigurationPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeGeneralValueConfigurationForm.class);

  public EmployeeGeneralValueConfigurationForm(EmployeeGeneralValueConfigurationPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected BaseSearchFilter newSearchFilterInstance()
  {
    return new BaseSearchFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  protected void init()
  {
    super.init();

    //Top Buttons
    //Disable default buttons
    resetButtonPanel.setVisible(false);
    searchButtonPanel.setVisible(false);

    // Customized Filter
    remove(gridBuilder.getMainContainer());
    gridBuilder = newGridBuilder(this, "filter");
  }

  public Panel getSaveButtonPanel(String id)
  {
    //Bottom Buttons
    final Button saveButton = new Button(SingleButtonPanel.WICKET_ID, new Model<>("save"))
    {
      @Override
      public final void onSubmit()
      {
        parentPage.saveList();
      }
    };
    WicketUtils.addTooltip(saveButton, getString("save"));
    final SingleButtonPanel saveButtonPanel = new SingleButtonPanel(id, saveButton,
        getString("save"), SingleButtonPanel.DEFAULT_SUBMIT);
    return saveButtonPanel;
  }
}
