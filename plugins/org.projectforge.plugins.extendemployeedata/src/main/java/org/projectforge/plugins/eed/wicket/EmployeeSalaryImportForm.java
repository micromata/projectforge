package org.projectforge.plugins.eed.wicket;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.plugins.eed.service.EEDHelper;
import org.projectforge.web.core.importstorage.AbstractImportForm;
import org.projectforge.web.core.importstorage.ImportFilter;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FileUploadPanel;

public class EmployeeSalaryImportForm extends AbstractImportForm<ImportFilter, EmployeeSalaryImportPage, EmployeeSalaryImportStoragePanel>
{
  @SpringBean
  private EEDHelper eedHelper;

  FileUploadField fileUploadField;

  private Integer selectedMonth;

  private Integer selectedYear = Calendar.getInstance(ThreadLocalUserContext.getTimeZone()).get(Calendar.YEAR);

  private DropDownChoicePanel<Integer> dropDownMonth;

  private DropDownChoicePanel<Integer> dropDownYear;

  public EmployeeSalaryImportForm(final EmployeeSalaryImportPage parentPage)
  {
    super(parentPage);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();

    gridBuilder.newGridPanel();

    // Date DropDowns
    final FieldsetPanel fsMonthYear = gridBuilder.newFieldset(I18nHelper.getLocalizedString("plugins.eed.listcare.yearmonth"));

    dropDownMonth = new DropDownChoicePanel<>(fsMonthYear.newChildId(),
        new DropDownChoice<>(DropDownChoicePanel.WICKET_ID, new PropertyModel<>(this, "selectedMonth"), EEDHelper.MONTH_INTEGERS)
    );
    dropDownMonth.setRequired(true);
    fsMonthYear.add(dropDownMonth);

    dropDownYear = new DropDownChoicePanel<>(fsMonthYear.newChildId(),
        new DropDownChoice<>(DropDownChoicePanel.WICKET_ID, new PropertyModel<>(this, "selectedYear"), eedHelper.getDropDownYears()));
    dropDownYear.setRequired(true);
    fsMonthYear.add(dropDownYear);

    // upload buttons
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("file"), "*.xls");
      fileUploadField = new FileUploadField(FileUploadPanel.WICKET_ID);
      fs.add(new FileUploadPanel(fs.newChildId(), fileUploadField));
      fs.add(new SingleButtonPanel(fs.newChildId(), new Button(SingleButtonPanel.WICKET_ID)
      {
        @Override
        public final void onSubmit()
        {
          final Date dateToSelectAttrRow = new GregorianCalendar(selectedYear, selectedMonth - 1, 1, 0, 0).getTime();
          storagePanel.setDateToSelectAttrRow(dateToSelectAttrRow);
          final boolean success = parentPage.doImport(dateToSelectAttrRow);
          if (success) {
            setDateDropDownsEnabled(false);
          }
        }
      }, getString("upload"), SingleButtonPanel.NORMAL).setTooltip(getString("common.import.upload.tooltip")));
      addClearButton(fs);
    }

    addImportFilterRadio(gridBuilder);

    // preview of the imported data
    gridBuilder.newGridPanel();
    final DivPanel panel = gridBuilder.getPanel();
    storagePanel = new EmployeeSalaryImportStoragePanel(panel.newChildId(), parentPage, importFilter);
    final Date dateToSelectAttrRow = new GregorianCalendar(selectedYear,
        (selectedMonth != null ? selectedMonth : Calendar.getInstance(ThreadLocalUserContext.getTimeZone()).get(Calendar.MONTH)) - 1, 1, 0, 0).getTime();
    storagePanel.setDateToSelectAttrRow(dateToSelectAttrRow);
    panel.add(storagePanel);
  }

  void setDateDropDownsEnabled(boolean enabled)
  {
    dropDownMonth.setEnabled(enabled);
    dropDownYear.setEnabled(enabled);
  }
}
