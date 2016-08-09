package org.projectforge.plugins.eed.wicket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.fibu.EmployeeFilter;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

import de.micromata.genome.db.jpa.tabattr.api.TimeableService;

public class EmployeeListEditForm extends AbstractListForm<EmployeeFilter, EmployeeListEditPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeListEditForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  @SpringBean
  private TimeableService<Integer, EmployeeTimedDO> timeableService;

  @SpringBean
  private EmployeeDao employeeDao;

  private static final List<Integer> MONTH_INTEGERS = Arrays
      .asList(new Integer[] { new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5),
          new Integer(6), new Integer(7), new Integer(8), new Integer(9), new Integer(10), new Integer(11),
          new Integer(12) });

  private Set<Integer> availableYears;

  private Integer selectedMonth;

  private Integer selectedYear;

  private String selectedOption;

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
      private static final long serialVersionUID = -2985054827068348809L;

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
        .newFieldset(I18nHelper.getLocalizedString(ThreadLocalUserContext.getLocale(), "plugins.eed.yearmonth"));
    //Get actual Month as preselected
    selectedMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
    //Month DropDown
    DropDownChoicePanel<Integer> ddcMonth = new DropDownChoicePanel<Integer>(fsMonthYear.newChildId(),
        new DropDownChoice<Integer>(DropDownChoicePanel.WICKET_ID, new PropertyModel<Integer>(this, "selectedMonth"),
            MONTH_INTEGERS));
    fsMonthYear.add(ddcMonth);
    //Get actual year for pre select
    selectedYear = Calendar.getInstance().get(Calendar.YEAR);
    //Year DropDown
    DropDownChoicePanel<Integer> ddcYear = new DropDownChoicePanel<Integer>(fsMonthYear.newChildId(),
        new DropDownChoice<Integer>(DropDownChoicePanel.WICKET_ID, new PropertyModel<Integer>(this, "selectedYear"),
            getDropDownYears()));
    fsMonthYear.add(ddcYear);

    //Map for option DropDown
    Map<String, String> keyValueMap = Stream.of(SelectOption.values())
        .filter(so -> so.equals(SelectOption.NONE) == false && so.equals(SelectOption.NOT_FOUND) == false)
        .collect(Collectors.toMap(
            SelectOption::getAttrXMLKey,
            so -> I18nHelper.getLocalizedString(ThreadLocalUserContext.getLocale(), so.getI18nKey())));

    // For Java 8 newbies
    //    for (SelectOption so : SelectOption.values()) {
    //      if (so.equals(SelectOption.NONE)) {
    //        continue;
    //      }
    //      String i18nValue = I18nHelper.getLocalizedString(ThreadLocalUserContext.getLocale(), so.getI18nKey());
    //      keyValueMap.put(so.getAttrXMLKey(), i18nValue);
    //    }

    //Fieldset for option DropDown
    final FieldsetPanel fsOption = gridBuilder
        .newFieldset(I18nHelper.getLocalizedString(ThreadLocalUserContext.getLocale(), "plugins.eed.option"));
    //Option DropDown
    DropDownChoicePanel<String> ddcOption = new DropDownChoicePanel<String>(gridBuilder.getPanel().newChildId(),
        new DropDownChoice<String>(DropDownChoicePanel.WICKET_ID, new PropertyModel<String>(this, "selectedOption"),
            new ArrayList<>(keyValueMap.keySet()), new IChoiceRenderer<String>()
            {
              private static final long serialVersionUID = 8866606967292296625L;

              @Override
              public Object getDisplayValue(String object)
              {
                return keyValueMap.get(object);
              }

              @Override
              public String getIdValue(String object, int index)
              {
                return object;
              }
            }));

    fsOption.add(ddcOption);
  }

  public enum SelectOption
  {

    MOBILECONTRACT("plugins.eed.optionDropDown.costmobilecontract", "mobilecontract"), //
    MOBILECHECK("plugins.eed.optionDropDown.costmobiledevice", "mobilecheck"), //
    COSTTRAVEL("plugins.eed.optionDropDown.costtravel", "costtravel"), //
    EXPENSES("plugins.eed.optionDropDown.expenses", "expenses"), //
    OVERTIME("plugins.eed.optionDropDown.overtime", "overtime"), //
    BONUS("plugins.eed.optionDropDown.bonus", "bonus"), //
    SPECIALPAYMENT("plugins.eed.optionDropDown.specialpayment", "specialpayment"), //
    TARGETAGREEMENTS("plugins.eed.optionDropDown.targetagreements", "targetagreements"), //
    COSTSHOP("plugins.eed.optionDropDown.costshop", "costshop"), //
    WEEKENDWORK("plugins.eed.optionDropDown.weekendwork", "weekendwork"), //
    OTHERS("plugins.eed.optionDropDown.others", "others"), //
    NONE("", ""), NOT_FOUND("", "");

    private String i18nKey;

    private String attrXMLKey;

    SelectOption(String i18nKey, String attrXMLKey)
    {
      this.i18nKey = i18nKey;
      this.attrXMLKey = attrXMLKey;
    }

    public String getI18nKey()
    {
      return i18nKey;
    }

    public String getAttrXMLKey()
    {
      return attrXMLKey;
    }

    public static SelectOption findByAttrXMLKey(String attrXMLKey)
    {
      if (attrXMLKey == null) {
        return NONE;
      }
      for (SelectOption so : SelectOption.values()) {
        if (so.getAttrXMLKey().equals(attrXMLKey)) {
          return so;
        }
      }
      return NOT_FOUND;
    }

  }

  private List<Integer> getDropDownYears()
  {
    if (this.availableYears == null) {
      this.availableYears = timeableService.getAvailableStartTimeYears(employeeDao.internalLoadAll());
      Integer actualYear = new GregorianCalendar().get(Calendar.YEAR);
      if (this.availableYears.contains(actualYear) == false) {
        this.availableYears.add(actualYear);
      }
      if (this.availableYears.contains(actualYear + 1) == false) {
        this.availableYears.add(actualYear + 1);
      }
    }
    return new ArrayList<>(this.availableYears);
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

  public Integer getSelectedMonth()
  {
    return selectedMonth;
  }

  public Integer getSelectedYear()
  {
    return selectedYear;
  }

  public String getSelectedOption()
  {
    return selectedOption;
  }

}
