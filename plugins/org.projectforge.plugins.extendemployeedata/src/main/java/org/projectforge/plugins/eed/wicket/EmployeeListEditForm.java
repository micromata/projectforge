package org.projectforge.plugins.eed.wicket;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.projectforge.business.fibu.EmployeeFilter;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;

public class EmployeeListEditForm extends AbstractListForm<EmployeeFilter, EmployeeListEditPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeListEditForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  private static final List<Integer> MONTH_INTEGERS = Arrays
      .asList(new Integer[] { new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5),
          new Integer(6), new Integer(7), new Integer(8), new Integer(9), new Integer(10), new Integer(11),
          new Integer(12) });

  @Override
  protected void init()
  {
    super.init();

    //Top Buttons
    final Button searchButton = new Button(SingleButtonPanel.WICKET_ID, new Model<>("search"))
    {
      @Override
      public final void onSubmit()
      {

      }
    };
    WicketUtils.addTooltip(searchButton, getString("search"));
    final SingleButtonPanel searchButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), searchButton,
        getString("search"), SingleButtonPanel.DEFAULT_SUBMIT);
    actionButtons.add(searchButtonPanel);

    setDefaultButton(searchButton);

    resetButtonPanel.setVisible(false);
    searchButtonPanel.setVisible(false);

    // Customized Filter
    remove(gridBuilder.getMainContainer());
    gridBuilder = newGridBuilder(this, "filter");
    //Filter
    DropDownChoicePanel<Integer> ddcMonth = new DropDownChoicePanel<Integer>(gridBuilder.getPanel().newChildId(),
        new DropDownChoice<Integer>(DropDownChoicePanel.WICKET_ID, MONTH_INTEGERS));
    gridBuilder.getPanel().add(ddcMonth);
    DropDownChoicePanel<Integer> ddcYear = new DropDownChoicePanel<Integer>(gridBuilder.getPanel().newChildId(),
        new DropDownChoice<Integer>(DropDownChoicePanel.WICKET_ID,
            getDropDownYears()));
    gridBuilder.getPanel().add(ddcYear);
    DropDownChoicePanel<String> ddcOption = new DropDownChoicePanel<String>(gridBuilder.getPanel().newChildId(),
        new DropDownChoice<String>(DropDownChoicePanel.WICKET_ID,
            getDropDownOptions()));
    gridBuilder.getPanel().add(ddcOption);
  }

  private List<String> getDropDownOptions()
  {
    return Arrays
        .asList(new String[] { "[TODO] Abzug Mobilfunk", "[TODO] Abzug Mobilgerät", "[TODO] Abzug Reisekosten",
            "[TODO] Auslagen", "[TODO] Überstunden", "[TODO] Prämie/Bonus", "[TODO] Sonderzahlungen",
            "[TODO] Zielvereinbarungen", "[TODO] Abzug Shop", "[TODO] Wochenendarbeit", "[TODO] Bemerkung/Sonstiges" });
  }

  private List<? extends Integer> getDropDownYears()
  {
    return Arrays
        .asList(new Integer[] { new Integer(2016) });
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

  public Panel getSaveButtonPanel(String id)
  {
    //Bottom Buttons
    final Button saveButton = new Button(SingleButtonPanel.WICKET_ID, new Model<>("save"))
    {
      @Override
      public final void onSubmit()
      {

      }
    };
    WicketUtils.addTooltip(saveButton, getString("save"));
    final SingleButtonPanel saveButtonPanel = new SingleButtonPanel(id, saveButton,
        getString("save"), SingleButtonPanel.DEFAULT_SUBMIT);
    return saveButtonPanel;
  }
}
