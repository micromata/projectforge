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

package org.projectforge.web.scripting;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.lang.Bytes;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.scripting.ScriptDO;
import org.projectforge.business.scripting.ScriptParameterType;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.web.dialog.ModalDialog;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.AceEditorPanel;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FileUploadPanel;

public class ScriptEditForm extends AbstractEditForm<ScriptDO, ScriptEditPage>
{
  private static final long serialVersionUID = 9088102999434892079L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ScriptEditForm.class);

  @SpringBean
  private AccessChecker accessChecker;

  protected ModalDialog showBackupScriptDialog;

  protected FileUploadPanel fileUploadPanel;

  public ScriptEditForm(final ScriptEditPage parentPage, final ScriptDO data)
  {
    super(parentPage, data);
    initUpload(Bytes.megabytes(1));
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newSplitPanel(GridSize.COL33);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("scripting.script.name"));
      fs.add(new MaxLengthTextField(fs.getTextFieldId(), new PropertyModel<String>(data, "name")));
    }
    gridBuilder.newSplitPanel(GridSize.COL66);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("file"));
      fs.addHelpIcon(getString("scripting.script.editForm.file.tooltip"));
      fileUploadPanel = new FileUploadPanel(fs.newChildId(), fs, this, true, new PropertyModel<String>(data, "filename"),
          new PropertyModel<byte[]>(data, "file")) {
        /**
         * @see org.projectforge.web.wicket.flowlayout.FileUploadPanel#upload()
         */
        @Override
        protected void upload(final FileUpload fileUpload)
        {
          accessChecker.checkIsLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP);
          accessChecker.checkRestrictedOrDemoUser();
          super.upload(fileUpload);
        }
      };
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    addParameterSettings(1);
    gridBuilder.newSplitPanel(GridSize.COL50);
    addParameterSettings(2);
    gridBuilder.newSplitPanel(GridSize.COL50);
    addParameterSettings(3);
    gridBuilder.newSplitPanel(GridSize.COL50);
    addParameterSettings(4);
    gridBuilder.newSplitPanel(GridSize.COL50);
    addParameterSettings(5);
    gridBuilder.newSplitPanel(GridSize.COL50);
    addParameterSettings(6);
    gridBuilder.newGridPanel();
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("description"));
      fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<String>(data, "description"))).setAutogrow();
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("scripting.script"));
      final AceEditorPanel script = new AceEditorPanel(fs.newChildId(), new PropertyModel<String>(data, "scriptAsString"));
      fs.add(script);
      fs.addHelpIcon(getString("fieldNotHistorizable"));
    }
    addShowBackupScriptDialog();
  }

  private void addParameterSettings(final int idx)
  {
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("scripting.script.parameterName") + " " + idx);

    final String parameterType = "parameter" + idx + "Type";
    final String parameterName = "parameter" + idx + "Name";
    final MaxLengthTextField name = new MaxLengthTextField(fs.getTextFieldId(), new PropertyModel<String>(data, parameterName));
    WicketUtils.setSize(name, 20);
    fs.add(name);
    // DropDownChoice type
    final LabelValueChoiceRenderer<ScriptParameterType> typeChoiceRenderer = new LabelValueChoiceRenderer<ScriptParameterType>(this,
        ScriptParameterType.values());
    final DropDownChoice<ScriptParameterType> typeChoice = new DropDownChoice<ScriptParameterType>(fs.getDropDownChoiceId(),
        new PropertyModel<ScriptParameterType>(data, parameterType), typeChoiceRenderer.getValues(), typeChoiceRenderer);
    typeChoice.setNullValid(true);
    typeChoice.setRequired(false);
    fs.add(typeChoice);
  }

  @SuppressWarnings("serial")
  protected void addShowBackupScriptDialog()
  {
    showBackupScriptDialog = new ModalDialog(parentPage.newModalDialogId()) {
      @Override
      public void init()
      {
        setTitle(getString("scripting.scriptBackup"));
        init(new Form<String>(getFormId()));
        {
          final FieldsetPanel fs = gridBuilder.newFieldset(getString("scripting.scriptBackup")).setLabelSide(false);
          final AceEditorPanel scriptBackup = new AceEditorPanel(fs.newChildId(), new PropertyModel<String>(data, "scriptBackupAsString"));
          fs.add(scriptBackup);
        }
      }
    };
    showBackupScriptDialog.setBigWindow().setOutputMarkupId(true);
    parentPage.add(showBackupScriptDialog);
    showBackupScriptDialog.init();

  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
