package org.projectforge.plugins.eed.wicket;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.projectforge.web.core.importstorage.AbstractImportForm;
import org.projectforge.web.core.importstorage.ImportFilter;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FileUploadPanel;

public class EmployeeBillingImportForm extends AbstractImportForm<ImportFilter, EmployeeBillingImportPage, EmployeeBillingImportStoragePanel>
{
  FileUploadField fileUploadField;

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
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("file"), "*.xls");
      fileUploadField = new FileUploadField(FileUploadPanel.WICKET_ID);
      fs.add(new FileUploadPanel(fs.newChildId(), fileUploadField));
      fs.add(new SingleButtonPanel(fs.newChildId(), new Button(SingleButtonPanel.WICKET_ID)
      {
        @Override
        public final void onSubmit()
        {
          parentPage.importAccountList();
        }
      }, getString("finance.datev.uploadAccountList"), SingleButtonPanel.NORMAL).setTooltip(getString("common.import.upload.tooltip"))); // TODO CT
      addClearButton(fs);
    }
    {
      addImportFilterRadio(gridBuilder);
    }

    gridBuilder.newGridPanel();
    final DivPanel panel = gridBuilder.getPanel();
    storagePanel = new EmployeeBillingImportStoragePanel(panel.newChildId(), parentPage, importFilter);
    panel.add(storagePanel);
  }
}
