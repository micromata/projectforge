/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.ihk;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.timesheet.OrderDirection;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.common.logging.LogSubscription;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.rest.admin.LogViewerPageRest;
import org.projectforge.rest.core.PagesResolver;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.timesheet.TimesheetEditPage;
import org.projectforge.web.wicket.AbstractStandardFormPage;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by mnuhn on 05.12.2019
 * Updated by mweishaar, jhpeters and mopreusser on 28.05.2020
 * Updated by mweishaar on 08.07.2021
 */
public class IHKPage extends AbstractStandardFormPage implements ISelectCallerPage
{
  private static final long serialVersionUID = -7157440416517271655L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IHKPage.class);

  @SpringBean
  private transient TimesheetDao timesheetDao;

  private final IHKForm form;

  public IHKPage(final PageParameters parameters)
  {
    super(parameters);
    final LogSubscription logSubscription = IHKPlugin.ensureUserLogSubscription();
    form = new IHKForm(this);
    body.add(form);
    form.init();
    final ExternalLink logViewerLink = new ExternalLink(ContentMenuEntryPanel.LINK_ID,
        PagesResolver.getDynamicPageUrl(LogViewerPageRest.class, null, logSubscription.getId(), true));
    addContentMenuEntry(new ContentMenuEntryPanel(getNewContentMenuChildId(), logViewerLink,
        getString("system.admin.logViewer.title")));
  }

  @Override
  protected String getTitle()
  {
    return getString("plugins.ihk.title");
  }

  @Override
  public void select(String property, Object selectedValue)
  {
    if (property.startsWith("quickSelect.")) {
      final LocalDate date = (LocalDate) selectedValue;

      PFDateTime dateTime = PFDateTime.fromOrNow(date).getBeginOfDay();
      form.getTimePeriod().setFromDate(dateTime.getUtilDate());
      form.getTimePeriod().setToDate(dateTime.getEndOfWeek().getUtilDate());
      form.startDate.markModelAsChanged();
      form.stopDate.markModelAsChanged();
    }
  }

  @Override
  public void unselect(String property)
  {

  }

  @Override
  public void cancelSelection(String property)
  {

  }

  void export(boolean ignoreMissingDescription)
  {
    List<TimesheetDO> timeSheetList = findTimesheets();
    if (timeSheetList.size() <= 0) {
      form.addError("plugins.ihk.noitemsfound");
      return;
    }

    if (!ignoreMissingDescription) {
      List<TimesheetDO> missingDescriptionList = new LinkedList<>();
      for (TimesheetDO ts : timeSheetList) {
        if (ts.getDescription() == null) {
          missingDescriptionList.add(ts);
        }
      }
      if (missingDescriptionList.size() > 0) {
        form.addError("plugins.ihk.nodescriptionfound");
        form.showMissingDescriptionList(missingDescriptionList);
        return;
      }
    }

    IHKExporter ihkExporter = new IHKExporter();
    byte[] xlsx = ihkExporter
        .getExcel(timeSheetList, form.getAusbildungsbeginn(), form.getTeamname(), form.getAusbildungsjahr(),
            ThreadLocalUserContext.getTimeZone());

    final String filename = "WB-Nr_" + ihkExporter.getDocNr() + "_"
        + DateHelper.getDateAsFilenameSuffix(form.getStartDate().getConvertedInput())
        + ".xlsx";

    if (xlsx == null || xlsx.length == 0) {
      log.error("Oups, xlsx has zero size. Filename: " + filename);
      return;
    }
    DownloadUtils.setDownloadTarget(xlsx, filename);
  }

  void edit(TimesheetDO ts)
  {
    final TimesheetEditPage timesheetEditPage = new TimesheetEditPage(ts);
    timesheetEditPage.error(getString("plugins.ihk.nodescriptionfound"));
    timesheetEditPage.setReturnToPage(this);
    setResponsePage(timesheetEditPage);
  }

  private List<TimesheetDO> findTimesheets()
  {
    final TimeZone usersTimeZone = ThreadLocalUserContext.getTimeZone();
    final Date fromDate = form.getTimePeriod().getFromDate();
    final PFDateTime startDate = PFDateTime.fromOrNow(fromDate, usersTimeZone)
        .withDayOfWeek(DayOfWeek.MONDAY.getValue());
    final TimesheetFilter tf = new TimesheetFilter();
    //ASC = Montag bis Sonntag
    tf.setOrderType(OrderDirection.ASC);
    tf.setStartTime(startDate.getUtilDate());
    tf.setUserId(this.getUserId());

    //stopDate auf Sonntag 23:59:59.999 setzten um alle Eintragungen aus der Woche zu bekommen
    PFDateTime stopDate = startDate.withDayOfWeek(DayOfWeek.SUNDAY.getValue());
    stopDate = stopDate.plus(23, ChronoUnit.HOURS);
    stopDate = stopDate.plus(59, ChronoUnit.MINUTES);
    stopDate = stopDate.plus(59, ChronoUnit.SECONDS);
    stopDate = stopDate.plus(999, ChronoUnit.MILLIS);
    tf.setStopTime(stopDate.getUtilDate());
    tf.setRecursive(true);

    return timesheetDao.getList(tf);
  }
}
