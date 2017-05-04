package org.projectforge.plugins.eed.wicket;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.plugins.eed.service.EEDHelper;
import org.projectforge.web.core.importstorage.AbstractImportForm;
import org.projectforge.web.core.importstorage.ImportFilter;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FileUploadPanel;

public class EmployeeBillingImportForm extends AbstractImportForm<ImportFilter, EmployeeBillingImportPage, EmployeeBillingImportStoragePanel>
{
  @SpringBean
  private EEDHelper eedHelper;

  FileUploadField fileUploadField;

  private int selectedMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;

  private int selectedYear = Calendar.getInstance().get(Calendar.YEAR);

  private DropDownChoicePanel<Integer> dropDownMonth;

  private DropDownChoicePanel<Integer> dropDownYear;

  public EmployeeBillingImportForm(final EmployeeBillingImportPage parentPage)
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
    final FieldsetPanel fsMonthYear = gridBuilder.newFieldset(I18nHelper.getLocalizedMessage("plugins.eed.listcare.yearmonth"));

    dropDownMonth = new DropDownChoicePanel<>(fsMonthYear.newChildId(),
        new DropDownChoice<>(DropDownChoicePanel.WICKET_ID, new PropertyModel<>(this, "selectedMonth"), EEDHelper.MONTH_INTEGERS)
    );
    fsMonthYear.add(dropDownMonth);

    dropDownYear = new DropDownChoicePanel<>(fsMonthYear.newChildId(),
        new DropDownChoice<>(DropDownChoicePanel.WICKET_ID, new PropertyModel<>(this, "selectedYear"), eedHelper.getDropDownYears()));
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
    storagePanel = new EmployeeBillingImportStoragePanel(panel.newChildId(), parentPage, importFilter);
    final Date dateToSelectAttrRow = new GregorianCalendar(selectedYear, selectedMonth - 1, 1, 0, 0).getTime();
    storagePanel.setDateToSelectAttrRow(dateToSelectAttrRow);
    panel.add(storagePanel);
  }

  void setDateDropDownsEnabled(boolean enabled)
  {
    dropDownMonth.setEnabled(enabled);
    dropDownYear.setEnabled(enabled);
  }
}
