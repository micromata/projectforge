/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.lang.Bytes;
import org.projectforge.business.fibu.kost.reporting.Report;
import org.projectforge.business.fibu.kost.reporting.ReportStorage;
import org.projectforge.business.scripting.ScriptDO;
import org.projectforge.business.scripting.ScriptExecutionResult;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.web.fibu.ReportScriptingStorage;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.AceEditorPanel;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.*;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class ScriptingForm extends AbstractStandardForm<ScriptDO, ScriptingPage> {
  private static final long serialVersionUID = 1868796548657011785L;

  protected FileUploadField fileUploadField;

  private String reportPathHeading;

  private DivPanel reportPathPanel;

  public ScriptingForm(final ScriptingPage parentPage) {
    super(parentPage);
    initUpload(Bytes.megabytes(1));
  }

  @Override
  @SuppressWarnings("serial")
  protected void init() {
    super.init();
    gridBuilder.newGridPanel();
    reportPathPanel = gridBuilder.getPanel();
    reportPathPanel.add(new Heading1Panel(reportPathPanel.newChildId(), new Model<String>() {
      /**
       * @see org.apache.wicket.model.Model#getObject()
       */
      @Override
      public String getObject() {
        return reportPathHeading;
      }
    }));
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("file"), "*.xsl, *.jrxml");
      fileUploadField = new FileUploadField(FileUploadPanel.WICKET_ID);
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject() {
          final ReportScriptingStorage storage = getReportScriptingStorage();
          return storage != null ? storage.getLastAddedFilename() : "";
        }
      }));
      fs.add(new FileUploadPanel(fs.newChildId(), fileUploadField));
      final Button uploadButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("upload")) {
        @Override
        public final void onSubmit() {
          parentPage.upload();
        }
      };
      fs.add(new SingleButtonPanel(fs.newChildId(), uploadButton, getString("upload"), SingleButtonPanel.NORMAL));
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("scripting.script.type"));
      // DropDownChoice type
      final LabelValueChoiceRenderer<ScriptDO.ScriptType> typeChoiceRenderer = new LabelValueChoiceRenderer<ScriptDO.ScriptType>();
      typeChoiceRenderer.addValue(ScriptDO.ScriptType.GROOVY, "Groovy");
      typeChoiceRenderer.addValue(ScriptDO.ScriptType.KOTLIN, "Kotlin");
      final DropDownChoice<ScriptDO.ScriptType> typeChoice = new DropDownChoice<ScriptDO.ScriptType>(fs.getDropDownChoiceId(),
              new PropertyModel<ScriptDO.ScriptType>(getReportScriptingStorage(), "type"), typeChoiceRenderer.getValues(), typeChoiceRenderer);
      typeChoice.setNullValid(true);
      typeChoice.setRequired(false);
      fs.add(typeChoice);
    }
    gridBuilder.newGridPanel();
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("label.script"));
      final AceEditorPanel textArea = new AceEditorPanel(fs.newChildId(), new PropertyModel<String>(getReportScriptingStorage(), "script"));
      fs.add(textArea);
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("label.script.result")).suppressLabelForWarning();
      final DivTextPanel scriptResultPanel = new DivTextPanel(fs.newChildId(), new Model<String>() {
        /**
         * @see org.apache.wicket.model.Model#getObject()
         */
        @Override
        public String getObject() {
          final ScriptExecutionResult scriptingExecutionResult = parentPage.scriptExecutionResult;
          final StringBuffer buf = new StringBuffer();
          if (scriptingExecutionResult.hasException()) {
            buf.append(scriptingExecutionResult.getException().getMessage()).append("\n");
          }
          buf.append(scriptingExecutionResult.getResultAsHtmlString());
          if (scriptingExecutionResult.getResult() != null && StringUtils.isNotEmpty(scriptingExecutionResult.getOutput()) == true) {
            buf.append("<br/>\n");
            buf.append(scriptingExecutionResult.getOutput());
          }
          return "<pre>" + HtmlHelper.escapeHtml(buf.toString(), true) + "</pre>";
        }
      }) {
        /**
         * @see org.apache.wicket.Component#isVisible()
         */
        @Override
        public boolean isVisible() {
          final ScriptExecutionResult scriptExecutionResult = parentPage.scriptExecutionResult;
          return scriptExecutionResult != null;
        }
      };
      scriptResultPanel.getLabel().setEscapeModelStrings(false);
      fs.add(scriptResultPanel);
    }
    {
      final Button executeButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("execute")) {
        @Override
        public final void onSubmit() {
          parentPage.execute();
        }
      };
      final SingleButtonPanel executeButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), executeButton, getString("execute"),
              SingleButtonPanel.DEFAULT_SUBMIT);
      actionButtons.add(executeButtonPanel);
      setDefaultButton(executeButton);
    }
  }

  @Override
  public void onBeforeRender() {
    final ReportStorage reportStorage = parentPage.getReportStorage();
    final Report currentReport = reportStorage != null ? reportStorage.getCurrentReport() : null;
    final String reportPathHeading = getReportPath(currentReport);
    if (reportPathHeading != null) {
      reportPathPanel.setVisible(true);
    } else {
      reportPathPanel.setVisible(false);
    }
    super.onBeforeRender();
  }

  private String getReportPath(final Report report) {
    if (report == null) {
      return null;
    }
    final List<Report> ancestorList = report.getPath();
    if (CollectionUtils.isEmpty(ancestorList) == true) {
      return null;
    }
    final StringBuffer buf = new StringBuffer();
    for (final Report ancestor : ancestorList) {
      buf.append(ancestor.getId()).append(" -> ");
    }
    buf.append(report.getId());
    return buf.toString();
  }

  private ReportScriptingStorage getReportScriptingStorage() {
    return parentPage.getReportScriptingStorage();
  }
}
