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

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.excel.ExportWorkbook;
import org.projectforge.business.fibu.kost.reporting.ReportGeneratorList;
import org.projectforge.business.fibu.kost.reporting.ReportStorage;
import org.projectforge.business.scripting.GroovyExecutor;
import org.projectforge.business.scripting.GroovyResult;
import org.projectforge.business.scripting.ScriptDao;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.export.ExportJFreeChart;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.utils.FileHelper;
import org.projectforge.web.export.ExportJson;
import org.projectforge.web.export.ExportZipArchive;
import org.projectforge.web.fibu.ReportObjectivesPage;
import org.projectforge.web.fibu.ReportScriptingStorage;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.JFreeChartImage;

public class ScriptingPage extends AbstractScriptingPage
{
  private static final long serialVersionUID = -1910145309628761662L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ScriptingPage.class);

  private transient ReportScriptingStorage reportScriptingStorage;

  @SpringBean
  private ScriptDao scriptDao;

  @SpringBean
  private GroovyExecutor groovyExecutor;

  private final ScriptingForm form;

  private Label availableScriptVariablesLabel;

  private WebMarkupContainer imageResultContainer;

  private transient Map<String, Object> scriptVariables;

  protected transient ReportStorage reportStorage;

  public ScriptingPage(final PageParameters parameters)
  {
    super(parameters);
    form = new ScriptingForm(this);
    body.add(form);
    form.init();
    initScriptVariables();
    body.add(imageResultContainer = (WebMarkupContainer) new WebMarkupContainer("imageResult").setVisible(false));
  }

  private void initScriptVariables()
  {
    if (scriptVariables != null) {
      // Already initialized.
      return;
    }
    scriptVariables = new HashMap<String, Object>();
    scriptVariables.put("reportStorage", null);
    scriptVariables.put("reportScriptingStorage", null);
    scriptDao.addScriptVariables(scriptVariables);
    final SortedSet<String> set = new TreeSet<String>();
    set.addAll(scriptVariables.keySet());
    final StringBuffer buf = new StringBuffer();
    buf.append("scriptResult"); // first available variable.
    for (final String key : set) {
      buf.append(", ").append(key);
    }
    if (availableScriptVariablesLabel == null) {
      body.add(availableScriptVariablesLabel = new Label("availableScriptVariables", buf.toString()));
    }
    //scriptDao.addAliasForDeprecatedScriptVariables(scriptVariables);
    // buf = new StringBuffer();
    // boolean first = true;
    // for (final BusinessAssessmentRowConfig rowConfig : AccountingConfig.getInstance().getBusinessAssessmentConfig().getRows()) {
    // if (rowConfig.getId() == null) {
    // continue;
    // }
    // if (first == true) {
    // first = false;
    // } else {
    // buf.append(", ");
    // }
    // buf.append('r').append(rowConfig.getNo()).append(", ").append(rowConfig.getId());
    // }
    // if (businessAssessmentRowsVariablesLabel == null) {
    // body.add(businessAssessmentRowsVariablesLabel = new Label("businessAssessmentRowsVariables", buf.toString()));
    // }
  }

  protected void execute()
  {
    accessChecker.checkIsLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP,
        ProjectForgeGroup.CONTROLLING_GROUP);
    accessChecker.checkRestrictedOrDemoUser();
    imageResultContainer.setVisible(false);
    ReportGeneratorList reportGeneratorList = new ReportGeneratorList();
    initScriptVariables();
    scriptVariables.put("reportStorage", getReportStorage());
    scriptVariables.put("reportScriptingStorage", getReportScriptingStorage());
    scriptVariables.put("reportList", reportGeneratorList);
    if (StringUtils.isNotBlank(getReportScriptingStorage().getGroovyScript()) == true) {
      groovyResult = groovyExecutor.execute(new GroovyResult(), getReportScriptingStorage().getGroovyScript(),
          scriptVariables);
      if (groovyResult.hasException() == true) {
        form.error(getLocalizedMessage("exception.groovyError", String.valueOf(groovyResult.getException())));
        return;
      }
      if (groovyResult.hasResult() == true) {
        // TODO maybe a good point to generalize to AbstractScriptingPage?
        final Object result = groovyResult.getResult();
        if (result instanceof ExportWorkbook == true) {
          excelExport();
        } else if (groovyResult.getResult() instanceof ReportGeneratorList == true) {
          reportGeneratorList = (ReportGeneratorList) groovyResult.getResult();
          // jasperReport(reportGeneratorList);
        } else if (result instanceof ExportZipArchive) {
          zipExport();
        } else if (result instanceof ExportJFreeChart) {
          jFreeChartExport();
        } else if (result instanceof ExportJson) {
          jsonExport();
        }
      }
      // } else if (getReportScriptingStorage().getJasperReport() != null) {
      // jasperReport();
    }
  }

  protected void upload()
  {
    accessChecker.checkIsLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP,
        ProjectForgeGroup.CONTROLLING_GROUP);
    accessChecker.checkRestrictedOrDemoUser();
    log.info("upload");
    final FileUpload fileUpload = form.fileUploadField.getFileUpload();
    if (fileUpload != null) {
      final boolean delete = false;
      try {
        final InputStream is = fileUpload.getInputStream();
        final String clientFileName = fileUpload.getClientFileName();
        if (clientFileName.endsWith(".jrxml") == true) {
          log.error("Jasper reports not supported.");
          // delete = true;
          // final JasperReport report = JasperCompileManager.compileReport(is);
          // if (report != null) {
          // getReportScriptingStorage().setJasperReport(report, clientFileName);
          // }
        } else if (clientFileName.endsWith(".xls") == true) {
          final StringBuffer buf = new StringBuffer();
          buf.append("report_")
              .append(FileHelper.createSafeFilename(ThreadLocalUserContext.getUser().getUsername(), 20)).append(".xls");
          final File file = new File(ConfigXml.getInstance().getWorkingDirectory(), buf.toString());
          fileUpload.writeTo(file);
          getReportScriptingStorage().setFilename(clientFileName, file.getAbsolutePath());
        } else {
          log.error("File extension not supported: " + clientFileName);
        }
      } catch (final Exception ex) {
        log.error(ex.getMessage(), ex);
        error("An error occurred (see log files for details): " + ex.getMessage());
      } finally {
        if (delete == true) {
          fileUpload.delete();
        }
      }
    }
  }

  /**
   * Creates the reports for the entries.
   *
   * @param reportGeneratorList
   */
  // private void jasperReport(final ReportGeneratorList reportGeneratorList)
  // {
  // if (CollectionUtils.isEmpty(reportGeneratorList.getReports()) == true) {
  // error(getString("fibu.reporting.jasper.error.reportListIsEmpty"));
  // return;
  // }
  // final ReportGenerator report = reportGeneratorList.getReports().get(0);
  // final Collection< ? > beanCollection = report.getBeanCollection();
  // final Map<String, Object> parameters = report.getParameters();
  // jasperReport(parameters, beanCollection);
  // }

  /**
   * Default report from reportStorage. Uses the current report and puts the business assessment values in parameter
   * map.
   */
  // private void jasperReport()
  // {
  // if (getReportStorage() == null || getReportStorage().getRoot() == null || getReportStorage().getRoot().isLoad() == false) {
  // error(getString("fibu.reporting.jasper.error.reportDataDoesNotExist"));
  // return;
  // }
  // final Map<String, Object> parameters = new HashMap<String, Object>();
  // final Report report = getReportStorage().getCurrentReport();
  // final Collection< ? > beanCollection = report.getBuchungssaetze();
  // BusinessAssessment.putBusinessAssessmentRows(parameters, report.getBusinessAssessment());
  // jasperReport(parameters, beanCollection);
  // }
  //
  // private void jasperReport(final Map<String, Object> parameters, final Collection< ? > beanCollection)
  // {
  // try {
  // final JasperReport jasperReport = getReportScriptingStorage().getJasperReport();
  // final JasperPrint jp = JasperFillManager.fillReport(jasperReport, parameters, new JRBeanCollectionDataSource(beanCollection));
  // final JasperPrint jasperPrint = jp;
  // final StringBuffer buf = new StringBuffer();
  // buf.append("pf_report_");
  // buf.append(DateHelper.getTimestampAsFilenameSuffix(new Date())).append(".pdf");
  // final String filename = buf.toString();
  // DownloadUtils.setDownloadTarget(JasperExportManager.exportReportToPdf(jasperPrint), filename);
  // } catch (final Exception ex) {
  // error(getLocalizedMessage("error", ex.getMessage()));
  // log.error(ex.getMessage(), ex);
  // }
  // }
  private void excelExport()
  {
    try {
      final ExportWorkbook workbook = (ExportWorkbook) groovyResult.getResult();
      final StringBuffer buf = new StringBuffer();
      if (workbook.getFilename() != null) {
        buf.append(workbook.getFilename()).append("_");
      } else {
        buf.append("pf_scriptresult_");
      }
      buf.append(DateHelper.getTimestampAsFilenameSuffix(new Date())).append(".xls");
      final String filename = buf.toString();
      DownloadUtils.setDownloadTarget(workbook.getAsByteArray(), filename);
    } catch (final Exception ex) {
      error(getLocalizedMessage("error", ex.getMessage()));
      log.error(ex.getMessage(), ex);
    }
  }

  private void jFreeChartExport()
  {
    try {
      final ExportJFreeChart exportJFreeChart = (ExportJFreeChart) groovyResult.getResult();
      final StringBuilder sb = new StringBuilder();
      sb.append("pf_chart_");
      sb.append(DateHelper.getTimestampAsFilenameSuffix(new Date()));
      final Response response = getResponse();
      final String extension = exportJFreeChart.write(response.getOutputStream());
      sb.append('.').append(extension);
      final String filename = sb.toString();
      final int width = exportJFreeChart.getWidth();
      final int height = exportJFreeChart.getHeight();
      final JFreeChartImage image = new JFreeChartImage("image", exportJFreeChart.getJFreeChart(),
          exportJFreeChart.getImageType(), width,
          height);
      image.add(AttributeModifier.replace("width", String.valueOf(width)));
      image.add(AttributeModifier.replace("height", String.valueOf(height)));
      imageResultContainer.removeAll();
      imageResultContainer.add(image).setVisible(true);
      ((WebResponse) response).setAttachmentHeader(filename);
      ((WebResponse) response).setContentType(DownloadUtils.getContentType(filename));
      log.info("Starting download for file. filename:" + filename + ", content-type:"
          + DownloadUtils.getContentType(filename));
      response.getOutputStream().flush();
    } catch (final Exception ex) {
      error(getLocalizedMessage("error", ex.getMessage()));
      log.error(ex.getMessage(), ex);
    }
  }

  private void zipExport()
  {
    try {
      final ExportZipArchive exportZipArchive = (ExportZipArchive) groovyResult.getResult();
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

  /**
   * @return Any existing user storage or null if not exist (wether in class nor in user's session).
   */
  protected ReportStorage getReportStorage()
  {
    if (reportStorage != null) {
      return reportStorage;
    }
    return (ReportStorage) getUserPrefEntry(ReportObjectivesPage.KEY_REPORT_STORAGE);
  }

  protected ReportScriptingStorage getReportScriptingStorage()
  {
    if (reportScriptingStorage != null) {
      return reportScriptingStorage;
    }
    reportScriptingStorage = (ReportScriptingStorage) getUserPrefEntry(ReportScriptingStorage.class.getName());
    if (reportScriptingStorage == null) {
      reportScriptingStorage = new ReportScriptingStorage();
      putUserPrefEntry(ReportScriptingStorage.class.getName(), reportScriptingStorage, false);
    }
    return reportScriptingStorage;
  }

  @Override
  protected String getTitle()
  {
    return getString("fibu.reporting.scripting");
  }
}
