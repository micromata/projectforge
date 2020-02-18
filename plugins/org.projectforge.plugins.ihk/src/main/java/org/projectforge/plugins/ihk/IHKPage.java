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

package org.projectforge.plugins.ihk;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.timesheet.OrderDirection;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractStandardFormPage;
import org.projectforge.web.wicket.DownloadUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by mnuhn on 05.12.2019
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
    form = new IHKForm(this);
    body.add(form);
    form.init();
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

  void export()
  {
    List<TimesheetDO> timeSheetList = findTimesheets();
    if (timeSheetList.size() <= 0) {
      form.addError("plugins.ihk.noitemsfound");
      return;
    }

    final String filename = "Wochenbericht_"
        + DateHelper.getDateAsFilenameSuffix(form.getStartDate().getConvertedInput())
        + ".xls";
    byte[] xls = IHKExporter.getExcel(timeSheetList);
    if (xls == null || xls.length == 0) {
      log.error("Oups, xls has zero size. Filename: " + filename);
      return;
    }
    DownloadUtils.setDownloadTarget(xls, filename);
  }

  private List<TimesheetDO> findTimesheets()
  {
    final TimeZone usersTimeZone = ThreadLocalUserContext.getTimeZone();
    final Date fromDate = form.getTimePeriod().getFromDate();
    final PFDateTime startDate = PFDateTime.fromOrNow(fromDate, usersTimeZone).withDayOfWeek(DayOfWeek.MONDAY.getValue());
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
