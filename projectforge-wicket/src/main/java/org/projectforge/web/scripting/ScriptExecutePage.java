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

import java.io.ByteArrayOutputStream;
import java.util.Date;

import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.excel.ExportWorkbook;
import org.projectforge.business.scripting.ScriptDO;
import org.projectforge.business.scripting.ScriptDao;
import org.projectforge.business.scripting.ScriptParameter;
import org.projectforge.business.scripting.xstream.RecentScriptCalls;
import org.projectforge.business.scripting.xstream.ScriptCallData;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.user.UserDao;
import org.projectforge.export.ExportJFreeChart;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.TimePeriod;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.export.ExportJson;
import org.projectforge.web.export.ExportZipArchive;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class ScriptExecutePage extends AbstractScriptingPage implements ISelectCallerPage
{
  private static final long serialVersionUID = -183858142939207911L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ScriptExecutePage.class);

  @SpringBean
  private ScriptDao scriptDao;

  @SpringBean
  private TaskDao taskDao;

  @SpringBean
  private UserDao userDao;

  private ScriptExecuteForm form;

  private Integer id;

  protected FieldsetPanel scriptResultFieldsetPanel;

  private GridBuilder resultGridBuilder;

  @SuppressWarnings("serial")
  public ScriptExecutePage(final PageParameters parameters)
  {
    super(parameters);
    id = WicketUtils.getAsInteger(parameters, AbstractEditPage.PARAMETER_KEY_ID);
    final ContentMenuEntryPanel editMenuEntryPanel = new ContentMenuEntryPanel(getNewContentMenuChildId(),
        new Link<Object>("link")
        {
          @Override
          public void onClick()
          {
            storeRecentScriptCalls();
            final PageParameters params = new PageParameters();
            params.add(AbstractEditPage.PARAMETER_KEY_ID, String.valueOf(id));
            final ScriptEditPage editPage = new ScriptEditPage(params);
            editPage.setReturnToPage(ScriptExecutePage.this);
            form.refresh = true; // Force reload of parameter settings.
            setResponsePage(editPage);
          }

        }, getString("edit"));
    addContentMenuEntry(editMenuEntryPanel);
    form = new ScriptExecuteForm(this, loadScript());
    body.add(form);
    form.init();
    resultGridBuilder = form.newGridBuilder(body, "results");
    resultGridBuilder.newGridPanel();
    {
      scriptResultFieldsetPanel = new FieldsetPanel(resultGridBuilder.getPanel(), getString("scripting.script.result"))
      {
        /**
         * @see org.apache.wicket.Component#isVisible()
         */
        @Override
        public boolean isVisible()
        {
          return groovyResult != null;
        }
      }.suppressLabelForWarning();
      final DivTextPanel resultPanel = new DivTextPanel(scriptResultFieldsetPanel.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          return groovyResult != null ? groovyResult.getResultAsHtmlString() : "";
        }
      });
      resultPanel.getLabel().setEscapeModelStrings(false);
      scriptResultFieldsetPanel.add(resultPanel);
    }
  }

  protected ScriptDO loadScript()
  {
    final ScriptDO script = scriptDao.getById(id);
    if (script == null) {
      log.error("Script with id '" + id + "' not found");
      throw new UserException("scripting.script.error.notFound");
    }
    return script;
  }

  @Override
  protected String getTitle()
  {
    return getString("scripting.script.execute");
  }

  protected void cancel()
  {
    final PageParameters params = new PageParameters();
    params.add(AbstractListPage.PARAMETER_HIGHLIGHTED_ROW, id);
    setResponsePage(ScriptListPage.class, params);
  }

  protected void execute()
  {
    final StringBuffer buf = new StringBuffer();
    buf.append("Execute script '").append(getScript().getName()).append("': ");
    if (form.scriptParameters != null) {
      boolean first = true;
      for (final ScriptParameter parameter : form.scriptParameters) {
        if (first == true) {
          first = false;
        } else {
          buf.append(',');
        }
        buf.append(parameter.getAsString());
      }
    }
    log.info(buf.toString());
    storeRecentScriptCalls();
    groovyResult = scriptDao.execute(getScript(), form.scriptParameters);
    if (groovyResult.hasException() == true) {
      form.error(getLocalizedMessage("exception.groovyError", String.valueOf(groovyResult.getException())));
      return;
    }
    if (groovyResult.hasResult() == true) {
      // TODO maybe a good point to generalize to AbstractScriptingPage?
      final Object obj = groovyResult.getResult();
      if (obj instanceof ExportWorkbook == true) {
        exportExcel((ExportWorkbook) obj);
      } else if (obj instanceof ExportJFreeChart == true) {
        exportJFreeChart((ExportJFreeChart) obj);
      } else if (obj instanceof ExportZipArchive == true) {
        exportZipArchive((ExportZipArchive) obj);
      } else if (obj instanceof ExportJson) {
        jsonExport();
      }
    }
  }

  private void exportExcel(final ExportWorkbook workbook)
  {
    final StringBuffer buf = new StringBuffer();
    if (workbook.getFilename() != null) {
      buf.append(workbook.getFilename()).append("_");
    } else {
      buf.append("pf_scriptresult_");
    }
    buf.append(DateHelper.getTimestampAsFilenameSuffix(new Date())).append(".xls");
    final String filename = buf.toString();
    final byte[] xls = workbook.getAsByteArray();
    if (xls == null || xls.length == 0) {
      log.error("Oups, xls has zero size. Filename: " + filename);
      return;
    }
    DownloadUtils.setDownloadTarget(xls, filename);
  }

  private void exportJFreeChart(final ExportJFreeChart exportJFreeChart)
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("pf_chart_");
    sb.append(DateHelper.getTimestampAsFilenameSuffix(new Date()));
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final String extension = exportJFreeChart.write(out);
    sb.append('.').append(extension);
    DownloadUtils.setDownloadTarget(out.toByteArray(), sb.toString());
  }

  private void exportZipArchive(final ExportZipArchive exportZipArchive)
  {
    try {
      final StringBuilder sb = new StringBuilder();
      sb.append(exportZipArchive.getFilename()).append("_");
      sb.append(DateHelper.getTimestampAsFilenameSuffix(new Date())).append(".zip");
      final String filename = sb.toString();
      DownloadUtils.setDownloadTarget(filename, exportZipArchive.createResourceStreamWriter());
    } catch (final Exception ex) {
      error(getLocalizedMessage("error", ex.getMessage()));
      log.error(ex.getMessage(), ex);
    }
  }

  protected void storeRecentScriptCalls()
  {
    final RecentScriptCalls recents = getRecentScriptCalls();
    final ScriptCallData scriptCallData = new ScriptCallData(getScript().getName(), form.scriptParameters);
    recents.append(scriptCallData);
  }

  protected RecentScriptCalls getRecentScriptCalls()
  {
    RecentScriptCalls recentScriptCalls = (RecentScriptCalls) getUserPrefEntry(ScriptExecutePage.class.getName());
    if (recentScriptCalls == null) {
      recentScriptCalls = new RecentScriptCalls();
      putUserPrefEntry(ScriptExecutePage.class.getName(), recentScriptCalls, true);
    }
    return recentScriptCalls;
  }

  protected ScriptDO getScript()
  {
    return form.data;
  }

  @Override
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    if (property == null) {
      log.error("Oups, null property not supported for selection.");
      return;
    }
    final int colonPos = property.indexOf(':'); // For date:idx e. g. date:3 for 3rd parameter.
    final int dotPos = property.lastIndexOf('.'); // For quick select e. g. quickSelect:3.month
    String indexString = null;
    if (dotPos > 0) {
      indexString = property.substring(colonPos + 1, dotPos);
    } else {
      indexString = colonPos > 0 ? property.substring(colonPos + 1) : null;
    }
    final Integer idx = NumberHelper.parseInteger(indexString);
    if (property.startsWith("quickSelect:") == true) {
      final Date date = (Date) selectedValue;
      TimePeriod timePeriod = form.scriptParameters.get(idx).getTimePeriodValue();
      if (timePeriod == null) {
        timePeriod = new TimePeriod();
      }
      timePeriod.setFromDate(date);
      final DateHolder dateHolder = new DateHolder(date);
      if (property.endsWith(".month") == true) {
        dateHolder.setEndOfMonth();
      } else if (property.endsWith(".week") == true) {
        dateHolder.setEndOfWeek();
      } else {
        log.error("Property '" + property + "' not supported for selection.");
      }
      timePeriod.setToDate(dateHolder.getDate());
      form.scriptParameters.get(idx).setTimePeriodValue(timePeriod);
      form.datePanel1[idx].markModelAsChanged();
      form.datePanel2[idx].markModelAsChanged();
    } else if (property.startsWith("taskId:") == true) {
      final TaskDO task = taskDao.getById((Integer) selectedValue);
      form.scriptParameters.get(idx).setTask(task);
    } else if (property.startsWith("userId:") == true) {
      final PFUserDO user = userDao.getById((Integer) selectedValue);
      form.scriptParameters.get(idx).setUser(user);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(final String property)
  {
    // Do nothing.
  }
}
