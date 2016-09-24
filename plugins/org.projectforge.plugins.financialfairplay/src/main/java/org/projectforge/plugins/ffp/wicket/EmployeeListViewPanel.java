package org.projectforge.plugins.ffp.wicket;

import java.util.List;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.projectforge.plugins.ffp.repository.FFPEventService;
import org.projectforge.web.employee.EmployeeWicketProvider;
import org.projectforge.web.wicket.flowlayout.Select2SingleChoicePanel;

import com.vaynberg.wicket.select2.Select2Choice;

public class EmployeeListViewPanel extends Panel
{
  private static final long serialVersionUID = 8483010398548170498L;

  public EmployeeListViewPanel(final String id, EmployeeService employeeService, FFPEventService ffpEventService,
      FFPEventDO event)
  {
    super(id);
    List<EmployeeDO> list = event.getAttendeeList();
    ListView<EmployeeDO> listview = new ListView<EmployeeDO>("listview", list)
    {
      private static final long serialVersionUID = 8768998191127603941L;

      protected void populateItem(ListItem<EmployeeDO> item)
      {
        final Select2Choice<EmployeeDO> managerSelect = new Select2Choice<>(
            Select2SingleChoicePanel.WICKET_ID,
            new PropertyModel<>(item, "defaultModelObject"),
            new EmployeeWicketProvider(employeeService));
        item.add(new Select2SingleChoicePanel<EmployeeDO>("employeeSelect", managerSelect));
      }
    };
    add(listview);
  }
}
