package org.projectforge.plugins.eed.wicket;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

import de.micromata.genome.db.jpa.tabattr.api.TimeableService;

public class ExportDataForm extends AbstractStandardForm<Object, ExportDataPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ExportDataForm.class);

  private Integer selectedMonth;

  private List<Integer> availableYears;

  private Integer selectedYear;

  @SpringBean
  private TimeableService<Integer, EmployeeTimedDO> timeableService;

  @SpringBean
  private EmployeeDao employeeDao;

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
    DropDownChoicePanel<Integer> ddcMonth = new DropDownChoicePanel<Integer>(fsMonthYear.newChildId(),
        new DropDownChoice<Integer>(DropDownChoicePanel.WICKET_ID, new PropertyModel<Integer>(this, "selectedMonth"),
            EmployeeListEditForm.MONTH_INTEGERS));
    fsMonthYear.add(ddcMonth);
    //Get actual year for pre select
    selectedYear = Calendar.getInstance().get(Calendar.YEAR);
    //Year DropDown
    DropDownChoicePanel<Integer> ddcYear = new DropDownChoicePanel<Integer>(fsMonthYear.newChildId(),
        new DropDownChoice<Integer>(DropDownChoicePanel.WICKET_ID, new PropertyModel<Integer>(this, "selectedYear"),
            getDropDownYears()));
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
    return this.availableYears;
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
