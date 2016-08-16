package org.projectforge.plugins.eed.wicket;

import java.util.Calendar;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.plugins.eed.service.EEDHelper;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class ExportDataForm extends AbstractStandardForm<Object, ExportDataPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ExportDataForm.class);

  @SpringBean
  private EEDHelper eedHelper;

  private Integer selectedMonth;

  private Integer selectedYear;

  public ExportDataForm(ExportDataPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected void init()
  {
    super.init();

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

    final Button exportButton = new Button(SingleButtonPanel.WICKET_ID, new Model<>("export"))
    {
      private static final long serialVersionUID = -2985054827068348809L;

      @Override
      public final void onSubmit()
      {
        parentPage.exportData();
      }
    };
    WicketUtils.addTooltip(exportButton, getString("export"));
    final SingleButtonPanel exportButtonPanel = new SingleButtonPanel(actionButtons.newChildId(),
        exportButton,
        getString("export"), SingleButtonPanel.DEFAULT_SUBMIT);
    actionButtons.add(exportButtonPanel);
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
