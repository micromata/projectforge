/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.fibu;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.lang.Bytes;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.fibu.kost.BusinessAssessment;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.core.importstorage.AbstractImportForm;
import org.projectforge.web.core.importstorage.ImportFilter;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FileUploadPanel;
import org.wicketstuff.html5.fileapi.FileFieldSizeCheckBehavior;
import org.wicketstuff.html5.fileapi.FileList;

public class DatevImportForm extends AbstractImportForm<ImportFilter, DatevImportPage, DatevImportStoragePanel>
{
  private static final long serialVersionUID = -4812284533159635654L;

  protected FileUploadField fileUploadField;

  @SpringBean
  private ConfigurationService configurationService;

  public DatevImportForm(final DatevImportPage parentPage)
  {
    super(parentPage);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    this.setOutputMarkupId(true);

    Bytes maxSize = Bytes.valueOf(configurationService.getMaxFileSizeDatev());
    this.setMaxSize(maxSize);

    gridBuilder.newGridPanel();

    String hint = I18nHelper.getLocalizedMessage("finance.datev.upload.hint", NumberHelper.formatBytes(maxSize.bytes()));
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("file"), hint);
    fileUploadField = new FileUploadField(FileUploadPanel.WICKET_ID);
    fs.add(new FileUploadPanel(fs.newChildId(), fileUploadField));
    fs.add(new SingleButtonPanel(fs.newChildId(), new Button(SingleButtonPanel.WICKET_ID, new Model<String>("uploadAccounts"))
    {
      @Override
      public final void onSubmit()
      {
        parentPage.importAccountList();
      }
    }, getString("finance.datev.uploadAccountList"), SingleButtonPanel.NORMAL).setTooltip(getString("common.import.upload.tooltip")));
    fs.add(new SingleButtonPanel(fs.newChildId(), new Button(SingleButtonPanel.WICKET_ID, new Model<String>("uploadRecords"))
    {
      @Override
      public final void onSubmit()
      {
        parentPage.importAccountRecords();
      }
    }, getString("finance.datev.uploadAccountingRecords"), SingleButtonPanel.NORMAL).setTooltip(getString("common.import.upload.tooltip")));
    addClearButton(fs);

    addImportFilterRadio(gridBuilder);

    // Statistics
    new BusinessAssessment4Fieldset(gridBuilder)
    {
      /**
       * @see org.projectforge.web.fibu.BusinessAssessment4Fieldset#getBusinessAssessment()
       */
      @Override
      protected BusinessAssessment getBusinessAssessment()
      {
        return storagePanel.businessAssessment;
      }

      @Override
      public boolean isVisible()
      {
        return storagePanel.businessAssessment != null;
      }
    };

    gridBuilder.newGridPanel();
    final DivPanel panel = gridBuilder.getPanel();
    storagePanel = new DatevImportStoragePanel(panel.newChildId(), parentPage, importFilter);
    panel.add(storagePanel);

    this.fileUploadField.add(new FileFieldSizeCheckBehavior()
    {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final FileList fileList)
      {
        // return form to remove errors
        target.add(DatevImportForm.this.feedbackPanel);
      }

      @Override
      protected void addErrorMsg(AjaxRequestTarget target, FileList fileList)
      {
        DatevImportForm.this.addError("common.uploadpanel.filetolarge", NumberHelper.formatBytes(maxSize.bytes()));
      }

      @Override
      protected void onError(final AjaxRequestTarget target, final FileList fileList)
      {
        // clear input field to avoid uploading the image
        DatevImportForm.this.fileUploadField.clearInput();

        // return form
        target.add(DatevImportForm.this);
      }
    });
  }

  protected void setBusinessAssessment(final BusinessAssessment businessAssessment)
  {
    storagePanel.businessAssessment = businessAssessment;
  }
}
