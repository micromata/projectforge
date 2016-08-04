package org.projectforge.plugins.eed.wicket;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.model.Model;
import org.projectforge.business.fibu.EmployeeFilter;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.SingleButtonPanel;

public class EmployeeListEditForm extends AbstractListForm<EmployeeFilter, EmployeeListEditPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeListEditForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  @Override
  protected void init()
  {
    super.init();

    //Buttons
    final Button saveButton = new Button(SingleButtonPanel.WICKET_ID, new Model<>("save"))
    {
      @Override
      public final void onSubmit()
      {

      }
    };
    WicketUtils.addTooltip(saveButton, getString("save"));
    final SingleButtonPanel sendButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), saveButton, getString("save"),
        SingleButtonPanel.DEFAULT_SUBMIT);
    actionButtons.add(sendButtonPanel);
    setDefaultButton(saveButton);

    resetButtonPanel.setVisible(false);
    searchButtonPanel.setVisible(false);

    // hide search panel
    gridBuilder.getMainContainer().setVisible(false);
  }

  public EmployeeListEditForm(final EmployeeListEditPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected EmployeeFilter newSearchFilterInstance()
  {
    return new EmployeeFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}

