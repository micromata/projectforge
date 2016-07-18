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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.lang.Bytes;
import org.projectforge.business.fibu.kost.reporting.Report;
import org.projectforge.business.fibu.kost.reporting.ReportStorage;
import org.projectforge.business.scripting.GroovyResult;
import org.projectforge.business.scripting.ScriptDO;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.web.fibu.ReportScriptingStorage;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.components.AceEditorPanel;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FileUploadPanel;
import org.projectforge.web.wicket.flowlayout.Heading1Panel;
import org.springframework.util.CollectionUtils;

public class ScriptingForm extends AbstractStandardForm<ScriptDO, ScriptingPage>
{
  private static final long serialVersionUID = 1868796548657011785L;

  protected FileUploadField fileUploadField;

  private String reportPathHeading;

  private DivPanel reportPathPanel;

  public ScriptingForm(final ScriptingPage parentPage)
  {
    super(parentPage);
    initUpload(Bytes.megabytes(1));
  }

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    super.init();
    gridBuilder.newGridPanel();
    reportPathPanel = gridBuilder.getPanel();
    reportPathPanel.add(new Heading1Panel(reportPathPanel.newChildId(), new Model<String>() {
      /**
       * @see org.apache.wicket.model.Model#getObject()
       */
      @Override
      public String getObject()
      {
        return reportPathHeading;
      }
    }));
    gridBuilder.newGridPanel();
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("file"), "*.xsl, *.jrxml");
      fileUploadField = new FileUploadField(FileUploadPanel.WICKET_ID);
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject()
        {
          final ReportScriptingStorage storage = getReportScriptingStorage();
          return storage != null ? storage.getLastAddedFilename() : "";
        }
      }));
      fs.add(new FileUploadPanel(fs.newChildId(), fileUploadField));
      final Button uploadButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("upload")) {
        @Override
        public final void onSubmit()
        {
          parentPage.upload();
        }
      };
      fs.add(new SingleButtonPanel(fs.newChildId(), uploadButton, getString("upload"), SingleButtonPanel.NORMAL));
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("label.groovyScript"));
      final AceEditorPanel textArea = new AceEditorPanel(fs.newChildId(), new PropertyModel<String>(this, "groovyScript"));
      fs.add(textArea);
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("label.groovy.result")).suppressLabelForWarning();
      final DivTextPanel groovyResultPanel = new DivTextPanel(fs.newChildId(), new Model<String>() {
        /**
         * @see org.apache.wicket.model.Model#getObject()
         */
        @Override
        public String getObject()
        {
          final GroovyResult groovyResult = parentPage.groovyResult;
          final StringBuffer buf = new StringBuffer();
          buf.append(groovyResult.getResultAsHtmlString());
          if (groovyResult.getResult() != null && StringUtils.isNotEmpty(groovyResult.getOutput()) == true) {
            buf.append("<br/>\n");
            buf.append(HtmlHelper.escapeXml(groovyResult.getOutput()));
          }
          return buf.toString();
        }
      }) {
        /**
         * @see org.apache.wicket.Component#isVisible()
         */
        @Override
        public boolean isVisible()
        {
          final GroovyResult groovyResult = parentPage.groovyResult;
          return (groovyResult != null && groovyResult.hasResult() == true);
        }
      };
      groovyResultPanel.getLabel().setEscapeModelStrings(false);
      fs.add(groovyResultPanel);
    }
    {
      final Button executeButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("execute")) {
        @Override
        public final void onSubmit()
        {
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
  public void onBeforeRender()
  {
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

  private String getReportPath(final Report report)
  {
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

  public String getGroovyScript()
  {
    return getReportScriptingStorage().getGroovyScript();
  }

  public void setGroovyScript(final String groovyScript)
  {
    getReportScriptingStorage().setGroovyScript(groovyScript);
  }

  private ReportScriptingStorage getReportScriptingStorage()
  {
    return parentPage.getReportScriptingStorage();
  }
}
