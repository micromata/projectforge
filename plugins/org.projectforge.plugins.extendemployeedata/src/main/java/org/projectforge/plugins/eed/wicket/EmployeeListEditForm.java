package org.projectforge.plugins.eed.wicket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.fibu.EmployeeFilter;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class EmployeeListEditForm extends AbstractListForm<EmployeeFilter, EmployeeListEditPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeListEditForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  private static final List<Integer> MONTH_INTEGERS = Arrays
      .asList(new Integer[] { new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5),
          new Integer(6), new Integer(7), new Integer(8), new Integer(9), new Integer(10), new Integer(11),
          new Integer(12) });

  private Integer selectedMonth;

  private Integer selectedYear;

  private String selectedOption;

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
    //Fieldset for Date DropDown
    final FieldsetPanel fsMonthYear = gridBuilder.newFieldset("TODO Monat/Jahr");
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

    //Map for option DropDown <i18nKey, Map<attrXMLKey, displayValue>
    Map<String, Map<String, String>> keyValueMap = new HashMap<>();
    for (String i18nKey : getI18nDropDownOptions().keySet()) {
      String i18nValue = I18nHelper.getLocalizedString(ThreadLocalUserContext.getLocale(), i18nKey);
      Map<String, String> innerKeyValueMap = new HashMap<>();
      innerKeyValueMap.put(getI18nDropDownOptions().get(i18nKey), i18nValue);
      keyValueMap.put(i18nKey, innerKeyValueMap);
    }
    List<String> keyList = new ArrayList<>(getI18nDropDownOptions().keySet());
    //Fieldset for option DropDown
    final FieldsetPanel fsOption = gridBuilder.newFieldset("TODO Option");
    //Option DropDown
    DropDownChoicePanel<String> ddcOption = new DropDownChoicePanel<String>(gridBuilder.getPanel().newChildId(),
        new DropDownChoice<String>(DropDownChoicePanel.WICKET_ID, new PropertyModel<String>(this, "selectedOption"),
            keyList, new IChoiceRenderer<String>()
            {
              private static final long serialVersionUID = 8866606967292296625L;

              @Override
              public Object getDisplayValue(String object)
              {
                Map<String, String> diplayValueMap = keyValueMap.get(object);
                return diplayValueMap.values().iterator().next();
              }

              @Override
              public String getIdValue(String object, int index)
              {
                Map<String, String> diplayValueMap = keyValueMap.get(object);
                return diplayValueMap.keySet().iterator().next();
              }
            }));

    fsOption.add(ddcOption);
  }

  private Map<String, String> getI18nDropDownOptions()
  {
    Map<String, String> i18nAttrXMLKeyMap = new HashMap<>();
    i18nAttrXMLKeyMap.put("plugins.eed.optionDropDown.costmobilecontract", "mobilecontract");
    i18nAttrXMLKeyMap.put("plugins.eed.optionDropDown.costmobiledevice", "mobilecheck");
    i18nAttrXMLKeyMap.put("plugins.eed.optionDropDown.costtravel", "costtravel");
    i18nAttrXMLKeyMap.put("plugins.eed.optionDropDown.expenses", "expenses");
    i18nAttrXMLKeyMap.put("plugins.eed.optionDropDown.overtime", "overtime");
    i18nAttrXMLKeyMap.put("plugins.eed.optionDropDown.bonus", "bonus");
    i18nAttrXMLKeyMap.put("plugins.eed.optionDropDown.specialpayment", "specialpayment");
    i18nAttrXMLKeyMap.put("plugins.eed.optionDropDown.targetagreements", "targetagreements");
    i18nAttrXMLKeyMap.put("plugins.eed.optionDropDown.costshop", "costshop");
    i18nAttrXMLKeyMap.put("plugins.eed.optionDropDown.weekendwork", "weekendwork");
    i18nAttrXMLKeyMap.put("plugins.eed.optionDropDown.others", "others");
    return i18nAttrXMLKeyMap;
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

  public Integer getSelectedMonth()
  {
    return selectedMonth;
  }

  public Integer getSelectedYear()
  {
    return selectedYear;
  }

}
