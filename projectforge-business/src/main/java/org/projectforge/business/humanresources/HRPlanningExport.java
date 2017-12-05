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

package org.projectforge.business.humanresources;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.hssf.util.HSSFColor;
import org.projectforge.business.excel.CellFormat;
import org.projectforge.business.excel.ContentProvider;
import org.projectforge.business.excel.ExportCell;
import org.projectforge.business.excel.ExportColumn;
import org.projectforge.business.excel.ExportRow;
import org.projectforge.business.excel.ExportSheet;
import org.projectforge.business.excel.ExportWorkbook;
import org.projectforge.business.excel.I18nExportColumn;
import org.projectforge.business.excel.PropertyMapping;
import org.projectforge.export.MyXlsContentProvider;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateTimeFormatter;
import org.springframework.stereotype.Service;

/**
 * For excel exports.
 *
 * @author Mario Groß (m.gross@micromata.de)
 */
@Service
public class HRPlanningExport
{
  private class MyContentProvider extends MyXlsContentProvider
  {
    public MyContentProvider(final ExportWorkbook workbook)
    {
      super(workbook);
    }

    @Override
    public MyContentProvider updateRowStyle(final ExportRow row)
    {
      for (final ExportCell cell : row.getCells()) {
        final CellFormat format = cell.ensureAndGetCellFormat();
        format.setFillForegroundColor(HSSFColor.WHITE.index);
        switch (row.getRowNum()) {
          case 0:
            format.setFont(FONT_HEADER);
            break;
          case 1:
            format.setFont(FONT_NORMAL_BOLD);
            // alignment = CellStyle.ALIGN_CENTER;
            break;
          default:
            format.setFont(FONT_NORMAL);
            if (row.getRowNum() % 2 == 0) {
              format.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
            }
            break;
        }
      }
      return this;
    }

    @Override
    public ContentProvider newInstance()
    {
      return new MyContentProvider(this.workbook);
    }
  }

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HRPlanningExport.class);

  final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.instance();

  private enum Col
  {
    USER, PROJEKT, WEEK_OF_YEAR, PRIORITY, PROBABILITY, UNASSIGNEDHOURS, MONDAYHOURS, TUESDAYHOURS, WEDNESDAYHOURS, THURSDAYHOURS, FRIDAYHOURS, WEEKENDHOURS, DESCRIPTION, TOTAL_DURATION, WORKDAYS
  }

  /**
   * Exports the filtered list as table with almost all fields. sheet 1 all fields sheet 2 Week to Users sheet 3 Week to
   * Projects sheet 4 Projects to Users
   */
  public byte[] export(final List<HRPlanningDO> list, final Locale locale)
  {
    log.info("Exporting resourceplanning list.");
    ExportWorkbook xls = new ExportWorkbook();
    xls = exportCompleteList(list, xls, locale);
    xls = exportKWUsers(list, xls, locale);
    xls = exportKWProjects(list, xls, locale);
    xls = exportProjectUserView(list, xls);

    return xls.getAsByteArray();
  }

  public ExportWorkbook exportCompleteList(final List<HRPlanningDO> list, final ExportWorkbook xls, final Locale locale)
  {

    final ContentProvider contentProvider = new MyContentProvider(xls);

    // create a default Date format and currency column
    xls.setContentProvider(contentProvider);

    final String sheetTitle = ThreadLocalUserContext.getLocalizedString("hr.plannings");
    final ExportSheet sheet = xls.addSheet(sheetTitle);
    sheet.createFreezePane(8, 1);

    final ExportColumn[] cols = new ExportColumn[] { //
        new I18nExportColumn(Col.USER, "timesheet.user", MyXlsContentProvider.LENGTH_USER),
        new I18nExportColumn(Col.PROJEKT, "fibu.projekt", MyXlsContentProvider.LENGTH_STD),
        new I18nExportColumn(Col.WEEK_OF_YEAR, "calendar.weekOfYearShortLabel", 4),
        new I18nExportColumn(Col.PRIORITY, "resourceplanning.priority", 8),
        new I18nExportColumn(Col.PROBABILITY, "resourceplanning.probability", 16),
        new I18nExportColumn(Col.UNASSIGNEDHOURS, "resourceplanning.unassignedHours", MyXlsContentProvider.LENGTH_STD),
        new I18nExportColumn(Col.MONDAYHOURS, "calendar.shortday.monday", 4),
        new I18nExportColumn(Col.TUESDAYHOURS, "calendar.shortday.tuesday", 4),
        new I18nExportColumn(Col.WEDNESDAYHOURS, "calendar.shortday.wednesday", 4),
        new I18nExportColumn(Col.THURSDAYHOURS, "calendar.shortday.thursday", 4),
        new I18nExportColumn(Col.FRIDAYHOURS, "calendar.shortday.friday", 4),
        new I18nExportColumn(Col.WEEKENDHOURS, "resourceplanning.weekend", MyXlsContentProvider.LENGTH_STD),
        new I18nExportColumn(Col.DESCRIPTION, "timesheet.description", MyXlsContentProvider.LENGTH_EXTRA_LONG) };

    // column property names
    sheet.setColumns(cols);

    final ContentProvider sheetProvider = sheet.getContentProvider();
    // Columnformats
    sheetProvider.putFormat(Col.UNASSIGNEDHOURS, "0.00");
    sheetProvider.putFormat(Col.MONDAYHOURS, "0.00");
    sheetProvider.putFormat(Col.TUESDAYHOURS, "0.00");
    sheetProvider.putFormat(Col.WEDNESDAYHOURS, "0.00");
    sheetProvider.putFormat(Col.THURSDAYHOURS, "0.00");
    sheetProvider.putFormat(Col.FRIDAYHOURS, "0.00");
    sheetProvider.putFormat(Col.WEEKENDHOURS, "0.00");

    final PropertyMapping mapping = new PropertyMapping();
    for (final HRPlanningDO planningSheet : list) {
      // final ProjektDO projekt = projektDao.getById(planningSheet.getProjektId());
      // final PFUserDO user = userGroupCache.getUser(planningSheet.getUserId());
      // mapping.add(Col.USER, user.getFullname());
      // final String projektName = projekt != null ? projekt.getName() : "";
      // mapping.add(Col.PROJEKT, projektName);
      // mapping.add(Col.WEEK_OF_YEAR, DateTimeFormatter.instance().getFormattedWeekOfYear(planningSheet.getStartTime()));
      // mapping.add(Col.PRIORITY, planningSheet.getPriority());
      // mapping.add(Col.PROBABILITY, planningSheet.getProbability());
      // mapping.add(Col.UNASSIGNEDHOURS, planningSheet.getUnassignedHours());
      // mapping.add(Col.MONDAYHOURS, planningSheet.getMondayHours());
      // mapping.add(Col.TUESDAYHOURS, planningSheet.getTuesdayHours());
      // mapping.add(Col.WEDNESDAYHOURS, planningSheet.getWednesdayHours());
      // mapping.add(Col.THURSDAYHOURS, planningSheet.getThursdayHours());
      // mapping.add(Col.FRIDAYHOURS, planningSheet.getFridayHours());
      // mapping.add(Col.WEEKENDHOURS, planningSheet.getWeekendHours());
      // mapping.add(Col.DESCRIPTION, planningSheet.getDescription());

      sheet.addRow(mapping.getMapping(), 0);
    }
    sheet.setZoom(3, 4); // 75%

    return xls;
  }

  /**
   * Exports a Calendarweek Overview to Excel
   *
   * @param list
   * @return
   */
  public ExportWorkbook exportKWProjects(final List<HRPlanningDO> list, final ExportWorkbook xls, final Locale locale)
  {
    log.info("Exporting resourceplanning list to Calendar View.");
    final ContentProvider contentProvider = new MyContentProvider(xls);
    // create a default Date format and currency column
    xls.setContentProvider(contentProvider);

    final String sheetTitle = ThreadLocalUserContext.getLocalizedString("exportKWProjects");
    final ExportSheet sheet = xls.addSheet(sheetTitle);
    sheet.createFreezePane(8, 1);

    final ExportColumn[] cols = new ExportColumn[] {
        new I18nExportColumn(Col.WEEK_OF_YEAR, "calendar.weekOfYearShortLabel", 12),
        new I18nExportColumn(Col.PROJEKT, "fibu.projekt", MyXlsContentProvider.LENGTH_STD),
        new I18nExportColumn(Col.TOTAL_DURATION, "timesheet.totalDuration", MyXlsContentProvider.LENGTH_STD),
        new I18nExportColumn(Col.WORKDAYS, "resourceplanning.workdays", MyXlsContentProvider.LENGTH_STD) };

    // column property names
    sheet.setColumns(cols);

    final ContentProvider sheetProvider = sheet.getContentProvider();
    // Columnformats
    sheetProvider.putFormat(Col.TOTAL_DURATION, "0.00");
    sheetProvider.putFormat(Col.WORKDAYS, "0.00");

    final PropertyMapping mapping = new PropertyMapping();

    // Ermittele Anzahl unterschiedlicher Projekte
    final List<String> projectNames = new ArrayList<String>();
    for (final HRPlanningDO planningSheet : list) {
      // final String projectName = planningSheet.getProjekt().getName();
      // boolean exists = false;
      // for (int i = 0; i < projectNames.size(); i++) {
      // if (projectName.equals(projectNames.get(i))) {
      // exists = true;
      // }
      // }
      // if (exists == false) {
      // projectNames.add(projectName);
      // }
    }

    // Get StartYear and EndYear from List
    // Date year = list.get(0).getStartTime();
    // Date startYear = year;
    // Date endYear = year;
    //
    // for (int i = 0; i < list.size(); i++) {
    // if (list.get(i).getStartTime().compareTo(startYear) < 0) {
    // startYear = list.get(i).getStartTime();
    // }
    // if (list.get(i).getStartTime().compareTo(endYear) > 0) {
    // endYear = list.get(i).getStartTime();
    // }
    // }

    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");

    // int syear = Integer.valueOf(simpleDateFormat.format(startYear));
    // int eyear = Integer.valueOf(simpleDateFormat.format(endYear));

    // for (int actualYear = syear; actualYear <= eyear; actualYear++) {
    // // Add actualYear row
    // // ExportRow r = sheet.addRow();
    // // r.setValues(String.valueOf(actualYear));
    //
    // // get maxKW for actual Year
    // Calendar cal = Calendar.getInstance(locale);
    // cal.set(actualYear, 11, 31);
    // int maxKW = Integer.valueOf(dateTimeFormatter.getFormattedDate(cal.getTime(), DateTimeFormatter.I18N_KEY_WEEK_OF_YEAR_FORMAT));
    //
    // // Durchlaufe KalenderWochen
    // for (int kw = 1; kw <= maxKW; kw++) {
    //
    // List<ExportObject> exportObjects = new LinkedList<ExportObject>();
    //
    // for (HRPlanningDO planningSheet : list) {
    // ExportObject exportObj = new ExportObject();
    //
    // // If kw=kw && year = actualyear
    // if ((Integer.valueOf(dateTimeFormatter.getFormattedDate(planningSheet.getStartTime(),
    // DateTimeFormatter.I18N_KEY_WEEK_OF_YEAR_FORMAT)) == kw)
    // && (getStartYearfromDO(planningSheet)) == actualYear) {
    // exportObj.setProjectName(planningSheet.getProjekt().getName());
    // exportObj.setUserName(planningSheet.getUser().getFullname());
    // exportObj.setTotalDuration(planningSheet.getTotalDuration());
    // exportObjects.add(exportObj);
    // }
    //
    // }
    // boolean rowAdded = false;
    // for (String projectName : projectNames) {
    // BigDecimal totalDur = new BigDecimal(0);
    // for (ExportObject obj : exportObjects) {
    // if (obj.getProjectName().equals(projectName)) {
    // totalDur = totalDur.add(obj.getTotalDuration());
    // }
    // }
    // mapping.add(Col.WEEK_OF_YEAR, String.valueOf(actualYear) + " - " + String.valueOf(kw));
    // mapping.add(Col.PROJEKT, projectName);
    // mapping.add(Col.TOTAL_DURATION, totalDur);
    // // Workdays = totalDur / 8
    // mapping.add(Col.WORKDAYS, totalDur.divide(new BigDecimal(8)));
    // if (totalDur.equals(new BigDecimal(0)) == false) {
    // sheet.addRow(mapping.getMapping(), 0);
    // rowAdded = true;
    // }
    // }
    //
    // if (rowAdded == false) {
    // mapping.add(Col.WEEK_OF_YEAR, String.valueOf(actualYear) + " - " + String.valueOf(kw));
    // mapping.add(Col.PROJEKT, " - ");
    // mapping.add(Col.TOTAL_DURATION, " - ");
    // mapping.add(Col.WORKDAYS, " - ");
    // sheet.addRow(mapping.getMapping(), 0);
    // }
    // }
    // }
    sheet.setZoom(3, 4); // 75%

    return xls;
  }

  public ExportWorkbook exportKWUsers(final List<HRPlanningDO> list, final ExportWorkbook xls, final Locale locale)
  {
    log.info("Exporting resourceplanning list to Calendar View.");

    final ContentProvider contentProvider = new MyContentProvider(xls);
    // create a default Date format and currency column
    xls.setContentProvider(contentProvider);

    final String sheetTitle = ThreadLocalUserContext.getLocalizedString("exportKWUsers");
    final ExportSheet sheet = xls.addSheet(sheetTitle);
    sheet.createFreezePane(8, 1);

    final ExportColumn[] cols = new ExportColumn[] {
        new I18nExportColumn(Col.WEEK_OF_YEAR, "calendar.weekOfYearShortLabel", 12),
        new I18nExportColumn(Col.USER, "timesheet.user", MyXlsContentProvider.LENGTH_USER),
        new I18nExportColumn(Col.TOTAL_DURATION, "timesheet.totalDuration", MyXlsContentProvider.LENGTH_STD),
        new I18nExportColumn(Col.WORKDAYS, "resourceplanning.workdays", MyXlsContentProvider.LENGTH_STD) };

    // column property names
    sheet.setColumns(cols);

    final ContentProvider sheetProvider = sheet.getContentProvider();
    // Columnformats
    sheetProvider.putFormat(Col.TOTAL_DURATION, "0.00");
    sheetProvider.putFormat(Col.WORKDAYS, "0.00");

    final PropertyMapping mapping = new PropertyMapping();

    // Ermittele Anzahl unterschiedlicher User
    final List<String> userNames = new ArrayList<String>();
    for (final HRPlanningDO planningSheet : list) {
      final String userName = planningSheet.getUser().getFullname();
      boolean exists = false;
      for (int i = 0; i < userNames.size(); i++) {
        if (userName.equals(userNames.get(i))) {
          exists = true;
        }
      }
      if (exists == false) {
        userNames.add(userName);
      }
    }

    // Get StartYear and EndYear
    // Date year = list.get(0).getStartTime();
    // Date startYear = year;
    // Date endYear = year;
    //
    // for (int i = 0; i < list.size(); i++) {
    // if (list.get(i).getStartTime().compareTo(startYear) < 0) {
    // startYear = list.get(i).getStartTime();
    // }
    // if (list.get(i).getStartTime().compareTo(endYear) > 0) {
    // endYear = list.get(i).getStartTime();
    // }
    // }

    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");

    // int syear = Integer.valueOf(simpleDateFormat.format(startYear));
    // int eyear = Integer.valueOf(simpleDateFormat.format(endYear));
    //
    // for (int actualYear = syear; actualYear <= eyear; actualYear++) {
    // // Add actualYear row
    // // ExportRow r = sheet.addRow();
    // // r.setValues(String.valueOf(actualYear));
    //
    // // get maxKW for actual Year
    // Calendar cal = Calendar.getInstance(locale);
    // cal.set(actualYear, 11, 31);
    // int maxKW = Integer.valueOf(dateTimeFormatter.getFormattedDate(cal.getTime(), DateTimeFormatter.I18N_KEY_WEEK_OF_YEAR_FORMAT));
    //
    // // Durchlaufe KalenderWochen
    // for (int kw = 1; kw <= maxKW; kw++) {
    //
    // List<ExportObject> exportObjects = new LinkedList<ExportObject>();
    //
    // for (HRPlanningDO planningSheet : list) {
    // ExportObject exportObj = new ExportObject();
    //
    // // If kw=kw && year = actualyear
    // if ((Integer.valueOf(dateTimeFormatter.getFormattedDate(planningSheet.getStartTime(),
    // DateTimeFormatter.I18N_KEY_WEEK_OF_YEAR_FORMAT)) == kw)
    // && (getStartYearfromDO(planningSheet)) == actualYear) {
    // exportObj.setProjectName(planningSheet.getProjekt().getName());
    // exportObj.setUserName(planningSheet.getUser().getFullname());
    // exportObj.setTotalDuration(planningSheet.getTotalDuration());
    // exportObjects.add(exportObj);
    // }

    // }

    // boolean rowAdded = false;
    // for (String userName : userNames) {
    // BigDecimal totalDur = new BigDecimal(0);
    // for (ExportObject obj : exportObjects) {
    // if (obj.getUserName().equals(userName)) {
    // totalDur = totalDur.add(obj.getTotalDuration());
    // }
    // }
    // mapping.add(Col.WEEK_OF_YEAR, String.valueOf(actualYear) + " - " + String.valueOf(kw));
    // mapping.add(Col.USER, userName);
    // mapping.add(Col.TOTAL_DURATION, totalDur);
    // // Workdays = totalDur / 8
    // mapping.add(Col.WORKDAYS, totalDur.divide(new BigDecimal(8)));
    // if (totalDur.equals(new BigDecimal(0)) == false) {
    // sheet.addRow(mapping.getMapping(), 0);
    // rowAdded = true;
    // }
    //
    // }
    // if (rowAdded == false) {
    // mapping.add(Col.WEEK_OF_YEAR, String.valueOf(actualYear) + " - " + String.valueOf(kw));
    // mapping.add(Col.USER, " - ");
    // mapping.add(Col.TOTAL_DURATION, " - ");
    // // Workdays = totalDur / 8
    // mapping.add(Col.WORKDAYS, " - ");
    // sheet.addRow(mapping.getMapping(), 0);
    // }
    //
    // }
    // }
    // sheet.setZoom(3, 4); // 75%

    return xls;
  }

  /**
   * Exports a Project to User View to excel
   *
   * @param list
   * @return
   */
  public ExportWorkbook exportProjectUserView(final List<HRPlanningDO> list, final ExportWorkbook xls)
  {
    log.info("Exporting resourceplanning list to Calendar View.");
    final ContentProvider contentProvider = new MyContentProvider(xls);
    // create a default Date format and currency column
    xls.setContentProvider(contentProvider);

    final String sheetTitle = ThreadLocalUserContext.getLocalizedString("exportProjectsUsers");
    final ExportSheet sheet = xls.addSheet(sheetTitle);
    sheet.createFreezePane(8, 1);

    // Ermittele Anzahl unterschiedlicher Projekte
    final List<String> projectNames = new ArrayList<String>();
    // for (HRPlanningDO planningSheet : list) {
    // String projectName = planningSheet.getProjekt().getName();
    // boolean exists = false;
    // for (int i = 0; i < projectNames.size(); i++) {
    // if (projectName.equals(projectNames.get(i))) {
    // exists = true;
    // }
    // }
    // if (exists == false) {
    // projectNames.add(projectName);
    // }
    // }
    //
    // // Ermittele Anzahl unterschiedlicher User
    // List<String> userNames = new ArrayList<String>();
    // for (HRPlanningDO planningSheet : list) {
    // String userName = planningSheet.getUser().getFullname();
    // boolean exists = false;
    // for (int i = 0; i < userNames.size(); i++) {
    // if (userName.equals(userNames.get(i))) {
    // exists = true;
    // }
    // }
    // if (exists == false) {
    // userNames.add(userName);
    // }
    // }
    //
    // // Erzeuge Columns
    // int sizeOfTable = 1 + projectNames.size();
    // ExportColumn[] cols = new ExportColumn[sizeOfTable];
    // // Erste Spalte Usernames
    // cols[0] = new I18nExportColumn(Col.USER, "timesheet.user", XlsContentProvider.LENGTH_USER);
    // // Restliche Spalten Projektnamen
    // for (int i = 1; i < sizeOfTable; i++) {
    // cols[i] = new ExportColumn(projectNames.get(i - 1), projectNames.get(i - 1), XlsContentProvider.LENGTH_STD);
    // }
    //
    // // column property names
    // sheet.setColumns(cols);
    //
    // final ContentProvider sheetProvider = sheet.getContentProvider();
    //
    // // Columnformats
    // for (String prjName : projectNames) {
    // sheetProvider.putFormat(prjName, "0.00");
    // }
    //
    // for (String userName : userNames) {
    //
    // PropertyMapping mapping = new PropertyMapping();
    //
    // // Map zum Speichern der Gesamtdauer der einzelnen Projekte des Users
    // Map<String, BigDecimal> projectsDurations = new HashMap<String, BigDecimal>();
    // // initialisiere Map
    // for (String prjName : projectNames) {
    // projectsDurations.put(prjName, new BigDecimal(0));
    // }
    //
    // for (HRPlanningDO planningSheet : list) {
    // if (planningSheet.getUser().getFullname().equals(userName)) {
    // for (String prjName : projectNames) {
    // if (planningSheet.getProjekt().getName().equals(prjName)) {
    // // addiere Duration auf Project
    // BigDecimal duration = projectsDurations.get(prjName);
    // duration = duration.add(planningSheet.getTotalDuration());
    // projectsDurations.put(prjName, duration);
    // }
    // }
    // }
    // }
    //
    // // Fülle Row
    // mapping.add(Col.USER, userName);
    // for (String prjName : projectNames) {
    // mapping.add(prjName, projectsDurations.get(prjName).toString());
    // }
    //
    // sheet.addRow(mapping.getMapping(), 0);
    // }

    sheet.setZoom(3, 4); // 75%

    return xls;
  }

  public int getStartYearfromDO(final HRPlanningDO sheet)
  {
    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");
    final Integer year = Integer.valueOf(simpleDateFormat.format(sheet.getWeek()));

    return year;
  }
}
