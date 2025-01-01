/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.kost.reporting.Report;
import org.projectforge.business.fibu.kost.reporting.ReportDao;
import org.projectforge.business.fibu.kost.reporting.ReportStorage;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.framework.time.PFDay;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.AbstractStandardFormPage;

import java.io.InputStream;

public class ReportObjectivesPage extends AbstractStandardFormPage
{
  private static final long serialVersionUID = 5880523229854750164L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReportObjectivesPage.class);

  public static final String KEY_REPORT_STORAGE = "ReportObjectivesPage:storage";

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
        final Report report = WicketSupport.get(ReportDao.class).createReport(is);

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
    final PFDay day = PFDay.from(filter.getFromDate()); // not null
    report.setFrom(day.getYear(), day.getMonthValue());
    PFDay untilDay;
    if (filter.getToDate() != null) {
      untilDay = PFDay.from(filter.getToDate()); // not null
    } else {
      untilDay = day.getEndOfMonth();
    }
    report.setTo(untilDay.getYear(), untilDay.getMonthValue());
    WicketSupport.get(ReportDao.class).loadReport(report);
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
   * @return Any existing user storage or null if not exist (whether in class nor in user's session).
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
    getAccessChecker().isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP);
    getAccessChecker().checkRestrictedOrDemoUser();
  }

  @Override
  protected String getTitle()
  {
    return getString("fibu.kost.reporting");
  }
}
