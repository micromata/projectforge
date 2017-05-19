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

package org.projectforge.web.admin;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.lang.Bytes;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.wicket.AbstractForm;
import org.projectforge.web.wicket.CsrfTokenHandler;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FileUploadPanel;
import org.wicketstuff.html5.fileapi.FileFieldSizeCheckBehavior;
import org.wicketstuff.html5.fileapi.FileList;

public class SetupImportForm extends AbstractForm<SetupImportForm, SetupPage>
{
  private static final long serialVersionUID = -277853572580468505L;

  protected FileUploadField fileUploadField;

  protected String filename;

  @SpringBean
  private ConfigurationService configurationService;

  /**
   * Cross site request forgery token.
   */
  private final CsrfTokenHandler csrfTokenHandler;

  public SetupImportForm(final SetupPage parentPage)
  {
    super(parentPage, "importform");
    csrfTokenHandler = new CsrfTokenHandler(this);
  }

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    FeedbackPanel feedbackPanel = createFeedbackPanel();
    feedbackPanel.setOutputMarkupId(true);
    add(feedbackPanel);

    this.setOutputMarkupId(true);

    // set max size
    Bytes maxSize = Bytes.valueOf(configurationService.getMaxFileSizeXmlDumpImport());
    this.setMaxSize(maxSize);
    this.setMultiPart(true);

    final GridBuilder gridBuilder = newGridBuilder(this, "flowform");
    gridBuilder.newFormHeading(getString("import"));
    {
      // Upload dump file
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("administration.setup.dumpFile"));
      fileUploadField = new FileUploadField(FileUploadPanel.WICKET_ID);
      fs.add(new FileUploadPanel(fs.newChildId(), fileUploadField));
    }
    final RepeatingView actionButtons = new RepeatingView("buttons");
    add(actionButtons);
    {
      final Button importButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("import"))
      {
        @Override
        public final void onSubmit()
        {
          csrfTokenHandler.onSubmit();
          parentPage.upload();
        }
      };
      final SingleButtonPanel importButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), importButton,
          getString("import"),
          SingleButtonPanel.DEFAULT_SUBMIT);
      actionButtons.add(importButtonPanel);
    }

    this.fileUploadField.add(new FileFieldSizeCheckBehavior()
    {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final FileList fileList)
      {
        if (fileList.getNumOfFiles() == 1) {
          if (false == (fileList.get(0).getName().endsWith(".xml.gz") || fileList.get(0).getName().endsWith(".xml")))
            SetupImportForm.this.addError("common.uploadpanel.filewrongtype", ".xml; .xml.gz");
        }

        // return form to remove errors
        target.add(feedbackPanel);
      }

      @Override
      protected void addErrorMsg(AjaxRequestTarget target, FileList fileList)
      {
        SetupImportForm.this.addError("common.uploadpanel.filetolarge", NumberHelper.formatBytes(maxSize.bytes()));
      }

      @Override
      protected void onError(final AjaxRequestTarget target, final FileList fileList)
      {
        // clear input field to avoid uploading the image
        SetupImportForm.this.fileUploadField.clearInput();

        // return form
        target.add(SetupImportForm.this);
      }
    });
  }
}
