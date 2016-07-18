package org.projectforge.plugins.ihkexport;

import java.util.Date;
import java.util.List;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.projectforge.business.timesheet.OrderDirection;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractStandardFormPage;
import org.projectforge.web.wicket.DownloadUtils;

public class IhkExportPage extends AbstractStandardFormPage implements ISelectCallerPage
{
  private static final long serialVersionUID = -7157440416517271655L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(IhkExportPage.class);

  @SpringBean
  private transient TimesheetDao timesheetDao;

  private final IhkExportForm form;

  public IhkExportPage(final PageParameters parameters)
  {
    super(parameters);
    form = new IhkExportForm(this);
    body.add(form);
    form.init();
  }

  @Override
  protected String getTitle()
  {
    return getString("plugins.ihkexport.title");
  }

  @Override
  public void select(String property, Object selectedValue)
  {
    if (property.startsWith("quickSelect.") == true) {
      final Date date = (Date) selectedValue;

      form.getTimePeriod().setFromDate(date);
      final DateHolder dateHolder = new DateHolder(date);
      if (property.endsWith(".week") == true) {
        dateHolder.setEndOfWeek();
      }
      form.getTimePeriod().setToDate(dateHolder.getDate());
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
      form.addError("plugins.ihkexport.noitemsfound");
      return;
    }

    final String filename = "Wochenbericht_"
        + DateHelper.getDateAsFilenameSuffix(form.getStartDate().getConvertedInput())
        + ".xls";
    byte[] xls = IhkExporter.getExcel(timeSheetList);
    if (xls == null || xls.length == 0) {
      log.error("Oups, xls has zero size. Filename: " + filename);
      return;
    }
    DownloadUtils.setDownloadTarget(xls, filename);
  }

  private List<TimesheetDO> findTimesheets()
  {
    final Date fromDate = form.getTimePeriod().getFromDate();
    final DateTime startDate = new DateTime(fromDate).withDayOfWeek(DateTimeConstants.MONDAY);
    final TimesheetFilter tf = new TimesheetFilter();
    //ASC = Montag bis Sonntag
    tf.setOrderType(OrderDirection.ASC);
    tf.setStartTime(startDate.toDate());
    tf.setUserId(this.getUserId());

    //stopDate auf Sonntag 23:59:59.999 setzten um alle Eintragungen aus der Woche zu bekommen
    DateTime stopDate = startDate.withDayOfWeek(DateTimeConstants.SUNDAY);
    stopDate = stopDate.plusHours(23);
    stopDate = stopDate.plusMinutes(59);
    stopDate = stopDate.plusSeconds(59);
    stopDate = stopDate.plusMillis(999);
    tf.setStopTime(stopDate.toDate());
    tf.setRecursive(true);

    return timesheetDao.getList(tf);
  }
}
