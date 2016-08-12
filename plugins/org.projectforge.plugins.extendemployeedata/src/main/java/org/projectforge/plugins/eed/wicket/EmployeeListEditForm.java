package org.projectforge.plugins.eed.wicket;

import java.util.Arrays;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeFilter;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.plugins.eed.EEDHelper;
import org.projectforge.plugins.eed.ExtendedEmployeeDataEnum;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class EmployeeListEditForm extends AbstractListForm<EmployeeFilter, EmployeeListEditPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeListEditForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  @SpringBean
  private EEDHelper eedHelper;

  private Integer selectedMonth;

  private Integer selectedYear;

  private ExtendedEmployeeDataEnum selectedOption;

  @Override
  protected void init()
  {
    super.init();

    //Top Buttons
    //Disable default buttons
    resetButtonPanel.setVisible(false);
    searchButtonPanel.setVisible(false);
    //Create custom search button
    final Button searchButton = new Button(SingleButtonPanel.WICKET_ID, new Model<>("search"))
    {
      @Override
      public final void onSubmit()
      {
        parentPage.refreshDataTable();
      }
    };
    WicketUtils.addTooltip(searchButton, getString("search"));
    final SingleButtonPanel customizedSearchButtonPanel = new SingleButtonPanel(actionButtons.newChildId(),
        searchButton,
        getString("search"), SingleButtonPanel.DEFAULT_SUBMIT);
    actionButtons.add(customizedSearchButtonPanel);

    //    setDefaultButton(searchButton);

    // Customized Filter
    remove(gridBuilder.getMainContainer());
    gridBuilder = newGridBuilder(this, "filter");
    //Filter
    //Fieldset for Date DropDown
    final FieldsetPanel fsMonthYear = gridBuilder
        .newFieldset(
            I18nHelper.getLocalizedString(ThreadLocalUserContext.getLocale(), "plugins.eed.listcare.yearmonth"));
    //Get actual Month as preselected
    selectedMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
    //Month DropDown
    DropDownChoicePanel<Integer> ddcMonth = new DropDownChoicePanel<>(fsMonthYear.newChildId(),
        new DropDownChoice<>(DropDownChoicePanel.WICKET_ID, new PropertyModel<>(this, "selectedMonth"),
            EEDHelper.MONTH_INTEGERS));
    fsMonthYear.add(ddcMonth);
    //Get actual year for pre select
    selectedYear = Calendar.getInstance().get(Calendar.YEAR);
    //Year DropDown
    DropDownChoicePanel<Integer> ddcYear = new DropDownChoicePanel<>(fsMonthYear.newChildId(),
        new DropDownChoice<>(DropDownChoicePanel.WICKET_ID, new PropertyModel<>(this, "selectedYear"),
            eedHelper.getDropDownYears()));
    fsMonthYear.add(ddcYear);

    //Fieldset for option DropDown
    final FieldsetPanel fsOption = gridBuilder
        .newFieldset(I18nHelper.getLocalizedString(ThreadLocalUserContext.getLocale(), "plugins.eed.listcare.option"));
    //Option DropDown
    final DropDownChoicePanel<ExtendedEmployeeDataEnum> ddcOption = new DropDownChoicePanel<>(gridBuilder.getPanel().newChildId(),
        new DropDownChoice<>(DropDownChoicePanel.WICKET_ID, new PropertyModel<>(this, "selectedOption"),
            Arrays.asList(ExtendedEmployeeDataEnum.values()), new IChoiceRenderer<ExtendedEmployeeDataEnum>()
            {
              @Override
              public Object getDisplayValue(ExtendedEmployeeDataEnum eede)
              {
                return I18nHelper.getLocalizedString(eede.getI18nKeyDropDown());
              }

              @Override
              public String getIdValue(ExtendedEmployeeDataEnum eede, int index)
              {
                return String.valueOf(index);
              }
            }
        )
    );

    fsOption.add(ddcOption);
  }

  public EmployeeListEditForm(final EmployeeListEditPage parentPage)
  {
    super(parentPage);
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
        parentPage.saveList();
      }
    };
    WicketUtils.addTooltip(saveButton, getString("save"));
    return new SingleButtonPanel(id, saveButton, getString("save"), SingleButtonPanel.DEFAULT_SUBMIT);
  }

  public Integer getSelectedMonth()
  {
    return selectedMonth;
  }

  public Integer getSelectedYear()
  {
    return selectedYear;
  }

  public ExtendedEmployeeDataEnum getSelectedOption()
  {
    return selectedOption;
  }

  @Override
  protected EmployeeFilter newSearchFilterInstance()
  {
    return new EmployeeFilter();
  }

}

