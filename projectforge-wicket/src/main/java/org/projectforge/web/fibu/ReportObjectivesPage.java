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

import java.io.InputStream;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.kost.reporting.Report;
import org.projectforge.business.fibu.kost.reporting.ReportDao;
import org.projectforge.business.fibu.kost.reporting.ReportStorage;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.web.wicket.AbstractStandardFormPage;

public class ReportObjectivesPage extends AbstractStandardFormPage
{
  private static final long serialVersionUID = 5880523229854750164L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReportObjectivesPage.class);

  public static final String KEY_REPORT_STORAGE = "ReportObjectivesPage:storage";

  @SpringBean
  private ReportDao reportDao;

  private final ReportObjectivesForm form;

  protected transient ReportStorage reportStorage;

  public ReportObjectivesPage(final PageParameters parameters)
  {
    super(parameters);
    form = new ReportObjectivesForm(this);
    body.add(form);
    form.init();
  }

  protected void importReportObjectivs()
  {
    checkAccess();
    log.info("import report objectives.");
    final FileUpload fileUpload = form.fileUploadField.getFileUpload();
    if (fileUpload != null) {
      try {
        final String clientFileName = fileUpload.getClientFileName();
        final InputStream is = fileUpload.getInputStream();
        final Report report = reportDao.createReport(is);

        if (report == null) {
          error("An error occurred during the import (see log files for details).");
          return;
        }

        reportStorage = new ReportStorage(report);
        reportStorage.setFileName(clientFileName);
        putUserPrefEntry(KEY_REPORT_STORAGE, reportStorage, false);
      } catch (final Exception ex) {
        log.error(ex.getMessage(), ex);
        error("An error occurred (see log files for details): " + ex.getMessage());
      }
    }
  }

  protected void createReport()
  {
    checkAccess();
    final ReportObjectivesFilter filter = form.getFilter();
    if (filter.getFromDate() == null) {
      return;
    }
    log.info("load report: " + filter);
    final ReportStorage storage = getReportStorage();
    final Report report = storage.getRoot();
    final String currentReportId = storage.getCurrentReport().getId(); // Store current report id.
    final DateHolder day = new DateHolder(filter.getFromDate());
    report.setFrom(day.getYear(), day.getMonth());
    if (filter.getToDate() != null) {
      day.setDate(filter.getToDate());
    } else {
      day.setEndOfMonth();
    }
    report.setTo(day.getYear(), day.getMonth());
    reportDao.loadReport(report);
    storage.setCurrentReport(currentReportId); // Select previous current report.
  }

  protected void clear()
  {
    checkAccess();
    log.info("clear report.");
    removeUserPrefEntry(KEY_REPORT_STORAGE);
    this.reportStorage = null;
  }

  /**
   * @return Any existing user storage or null if not exist (wether in class nor in user's session).
   */
  protected ReportStorage getReportStorage()
  {
    if (reportStorage != null) {
      return reportStorage;
    }
    return (ReportStorage) getUserPrefEntry(KEY_REPORT_STORAGE);
  }

  private void checkAccess()
  {
    accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP);
    accessChecker.checkRestrictedOrDemoUser();
  }

  @Override
  protected String getTitle()
  {
    return getString("fibu.kost.reporting");
  }
}
