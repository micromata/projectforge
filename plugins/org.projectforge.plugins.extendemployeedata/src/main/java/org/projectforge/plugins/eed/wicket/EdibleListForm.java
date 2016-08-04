package org.projectforge.plugins.eed.wicket;

import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.SingleButtonPanel;

public class EdibleListForm extends AbstractStandardForm<Object, EdibleListPage>
{
  private static final long serialVersionUID = -5295529979362746832L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EdibleListForm.class);

  private EdibleListPanel elPanel;

  @SpringBean
  private EmployeeDao employeeDao;

  public EdibleListForm(EdibleListPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected void init()
  {
    super.init();

    //DataTable
    List<EmployeeDO> employeeList = employeeDao.internalLoadAll();
    elPanel = new EdibleListPanel(newChildId(), parentPage, employeeList);
    add(elPanel);

    //Buttons
    final Button saveButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("save"))
    {
      private static final long serialVersionUID = -2962196557114790574L;

      @Override
      public final void onSubmit()
      {

      }
    };
    WicketUtils.addTooltip(saveButton, getString("save"));
    final SingleButtonPanel sendButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), saveButton,
        getString("save"),
        SingleButtonPanel.DEFAULT_SUBMIT);
    actionButtons.add(sendButtonPanel);
    setDefaultButton(saveButton);
  }

  private String newChildId()
  {
    return gridBuilder.getPanel().newChildId();
  }

  private void add(WebMarkupContainer con)
  {
    gridBuilder.getPanel().add(con);
  }

}
