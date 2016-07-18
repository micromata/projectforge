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

package org.projectforge.web.statistics;

import java.text.NumberFormat;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.jfree.chart.JFreeChart;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.statistics.TimesheetDisciplineChartBuilder;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.JFreeChartImage;

public class PersonalStatisticsPage extends AbstractSecuredPage
{
  private static final long serialVersionUID = 5957430109012640203L;

  private static final short LAST_N_DAYS = 45;

  private static final int IMAGE_WIDTH = 500;

  private static final int IMAGE_HEIGHT = 400;

  @SpringBean
  private EmployeeDao employeeDao;

  @SpringBean
  private TimesheetDao timesheetDao;

  public PersonalStatisticsPage(final PageParameters parameters)
  {
    super(parameters);
    final Label timesheetDisciplineChartTitle = new Label("timesheetDisciplineChartTitle",
        getString("personal.statistics.timesheetDisciplineChart.title"));
    body.add(timesheetDisciplineChartTitle);
    final EmployeeDO employee = employeeDao.findByUserId(ThreadLocalUserContext.getUserId());
    double workingHoursPerDay = 8;
    if (employee != null && NumberHelper.isGreaterZero(employee.getWeeklyWorkingHours()) == true) {
      workingHoursPerDay = employee.getWeeklyWorkingHours().doubleValue() / 5;
    }
    final TimesheetDisciplineChartBuilder chartBuilder = new TimesheetDisciplineChartBuilder();
    final JFreeChart chart1 = chartBuilder.create(timesheetDao, getUser().getId(), workingHoursPerDay, LAST_N_DAYS, true);
    JFreeChartImage image = new JFreeChartImage("timesheetStatisticsImage1", chart1, IMAGE_WIDTH, IMAGE_HEIGHT);
    image.add(AttributeModifier.replace("width", String.valueOf(IMAGE_WIDTH)));
    image.add(AttributeModifier.replace("height", String.valueOf(IMAGE_HEIGHT)));
    body.add(image);
    final NumberFormat format = NumberFormat.getNumberInstance(ThreadLocalUserContext.getLocale());
    final String planHours = "<span style=\"color: #DE1821; font-weight: bold;\">"
        + format.format(chartBuilder.getPlanWorkingHours())
        + "</span>";
    final String actualHours = "<span style=\"color: #40A93B; font-weight: bold;\">"
        + format.format(chartBuilder.getActualWorkingHours())
        + "</span>";
    final String numberOfDays = String.valueOf(LAST_N_DAYS);
    final Label timesheetDisciplineChart1Legend = new Label("timesheetDisciplineChart1Legend", getLocalizedMessage(
        "personal.statistics.timesheetDisciplineChart1.legend", numberOfDays, planHours, actualHours));
    timesheetDisciplineChart1Legend.setEscapeModelStrings(false);
    body.add(timesheetDisciplineChart1Legend);

    final JFreeChart chart2 = chartBuilder.create(timesheetDao, getUser().getId(), LAST_N_DAYS, true);
    image = new JFreeChartImage("timesheetStatisticsImage2", chart2, IMAGE_WIDTH, IMAGE_HEIGHT);
    image.add(AttributeModifier.replace("width", String.valueOf(IMAGE_WIDTH)));
    image.add(AttributeModifier.replace("height", String.valueOf(IMAGE_HEIGHT)));
    body.add(image);
    final String averageDifference = "<span style=\"color: #DE1821; font-weight: bold;\">"
        + format.format(chartBuilder.getAverageDifferenceBetweenTimesheetAndBooking())
        + "</span>";
    final String plannedDifference = "<span style=\"color: #40A93B; font-weight: bold;\">"
        + format.format(chartBuilder.getPlannedAverageDifferenceBetweenTimesheetAndBooking())
        + "</span>";
    final Label timesheetDisciplineChart2Legend = new Label("timesheetDisciplineChart2Legend", getLocalizedMessage(
        "personal.statistics.timesheetDisciplineChart2.legend", numberOfDays, plannedDifference, averageDifference));
    timesheetDisciplineChart2Legend.setEscapeModelStrings(false);
    body.add(timesheetDisciplineChart2Legend);
  }

  @Override
  protected String getTitle()
  {
    return getString("personal.statistics.title");
  }

}
