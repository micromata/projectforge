/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu.datev;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.apache.poi.hssf.util.HSSFColor;
import org.projectforge.business.excel.*;
import org.projectforge.business.fibu.*;
import org.projectforge.business.fibu.MonthlyEmployeeReport.Kost2Row;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.export.MyXlsContentProvider;
import org.projectforge.framework.calendar.MonthHolder;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.framework.utils.CurrencyHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * For excel export of employee salaries for import in Datev.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class EmployeeSalaryExportDao {
  public static final int KONTO = 6000;

  public static final int GEGENKONTO = 3791;
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeSalaryExportDao.class);
  @Autowired
  private MonthlyEmployeeReportDao monthlyEmployeeReportDao;
  @Autowired
  private EmployeeDao employeeDao;

  /**
   * Exports the filtered list as table with almost all fields.
   */
  public byte[] export(final List<EmployeeSalaryDO> list) {
    log.info("Exporting employee salary list.");
    Validate.notEmpty(list);
    list.sort(new Comparator<EmployeeSalaryDO>() {
      @Override
      public int compare(final EmployeeSalaryDO o1, final EmployeeSalaryDO o2) {
        return (o1.getEmployee().getUser().getFullname()).compareTo(o2.getEmployee().getUser().getFullname());
      }
    });
    final EmployeeFilter filter = new EmployeeFilter();
    filter.setShowOnlyActiveEntries(true);
    filter.setDeleted(false);
    final List<EmployeeDO> employees = employeeDao.getList(filter);
    final List<EmployeeDO> missedEmployees = new ArrayList<>();
    for (final EmployeeDO employee : employees) {
      boolean found = false;
      for (final EmployeeSalaryDO salary : list) {
        if (salary.getEmployeeId().equals(employee.getId())) {
          found = true;
          break;
        }
      }
      if (!found) {
        missedEmployees.add(employee);
      }
    }
    if (CollectionUtils.isNotEmpty(missedEmployees)) {
      missedEmployees.sort(Comparator.comparing(o -> (o.getUser().getFullname())));
    }
    final ExportWorkbook xls = new ExportWorkbook();
    final ContentProvider contentProvider = new MyContentProvider(xls);
    // create a default Date format and currency column
    xls.setContentProvider(contentProvider);

    final EmployeeSalaryDO first = list.get(0);
    final int year = first.getYear();
    final int month = first.getMonth();
    final DayHolder buchungsdatum = new DayHolder();
    buchungsdatum.setDate(year, month, 1);
    final MonthHolder monthHolder = new MonthHolder(buchungsdatum.getDate());
    final BigDecimal numberOfWorkingDays = monthHolder.getNumberOfWorkingDays();
    buchungsdatum.setEndOfMonth();

    final String sheetTitle = DateHelper.formatMonth(year, month);
    final ExportSheet sheet = xls.addSheet(sheetTitle);
    sheet.createFreezePane(0, 1);

    final ExportSheet employeeSheet = xls.addSheet(ThreadLocalUserContext.getLocalizedString("fibu.employee"));
    employeeSheet.setColumnWidth(0, MyXlsContentProvider.LENGTH_USER * 256);
    employeeSheet.setColumnWidth(1, 14 * 256);
    employeeSheet.setColumnWidth(2, 12 * 256);
    employeeSheet.setColumnWidth(3, 12 * 256);
    employeeSheet.setColumnWidth(4, 12 * 256);
    employeeSheet.setColumnWidth(5, 26 * 256);
    employeeSheet.setColumnWidth(6, 20 * 256);
    final ContentProvider provider = employeeSheet.getContentProvider();
    provider.putFormat("STUNDEN", "0.00;[Red]-0.00");
    final ExportRow employeeRow = employeeSheet.addRow();
    employeeRow.addCell(0, ThreadLocalUserContext.getLocalizedString("fibu.employee"));
    employeeRow.addCell(1, ThreadLocalUserContext.getLocalizedString("fibu.employee.wochenstunden"));
    employeeRow.addCell(2, ThreadLocalUserContext.getLocalizedString("fibu.employee.sollstunden"));
    employeeRow.addCell(3, ThreadLocalUserContext.getLocalizedString("fibu.employee.iststunden"));
    employeeRow.addCell(4, ThreadLocalUserContext.getLocalizedString("fibu.common.difference"));
    employeeRow.addCell(5, ThreadLocalUserContext.getLocalizedString("fibu.monthlyEmployeeReport.daysCountWithoutTimesheets"));
    employeeRow.addCell(6, ThreadLocalUserContext.getLocalizedString("fibu.monthlyEmployeeReport.daysWithoutTimesheets"));

    // build all column names, title, widths from fixed and variable columns
    final int numCols = ExcelColumn.values().length;

    final String[] colNames = new String[numCols];
    final String[] colTitles = new String[numCols];
    final int[] colWidths = new int[numCols];

    int idx = 0;
    for (final ExcelColumn col : EnumSet.range(ExcelColumn.START, ExcelColumn.END)) {
      colNames[idx] = col.name();
      colTitles[idx] = ThreadLocalUserContext.getLocalizedString(col.theTitle);
      colWidths[idx] = col.width;
      ++idx;
    }

    // column property names
    sheet.setPropertyNames(colNames);

    final ContentProvider sheetProvider = sheet.getContentProvider();
    sheetProvider.putFormat("STUNDEN", "0.00");
    sheetProvider.putFormat("BRUTTO_MIT_AG", "#,##0.00;[Red]-#,##0.00");
    sheetProvider.putFormat("KORREKTUR", "#,##0.00;[Red]-#,##0.00");
    sheetProvider.putFormat("SUMME", "#,##0.00;[Red]-#,##0.00");
    sheetProvider.putFormat("KOST1", "#");
    sheetProvider.putFormat("KOST2", "#");
    sheetProvider.putFormat("KONTO", "#");
    sheetProvider.putFormat("GEGENKONTO", "#");
    sheetProvider.putFormat("DATUM", "dd.MM.yyyy");
    // inform provider of column widths
    for (int ci = 0; ci < colWidths.length; ++ci) {
      sheetProvider.putColWidth(ci, colWidths[ci]);
    }

    final ExportRow headRow = sheet.addRow();
    int i = 0;
    for (final String title : colTitles) {
      headRow.addCell(i++, title);
    }

    for (final EmployeeSalaryDO salary : list) {
      final PropertyMapping mapping = new PropertyMapping();
      final PFUserDO user = getUserGroupCache().getUser(salary.getEmployee().getUserId());
      Validate.isTrue(year == salary.getYear());
      Validate.isTrue(month == salary.getMonth());
      final MonthlyEmployeeReport report = monthlyEmployeeReportDao.getReport(year, month, user);
      mapping.add(ExcelColumn.MITARBEITER, user.getFullname());
      final Kost1DO kost1 = salary.getEmployee().getKost1();
      final BigDecimal bruttoMitAGAnteil = salary.getBruttoMitAgAnteil();
      final BigDecimal netDuration = new BigDecimal(report.getTotalNetDuration());
      final Map<String, Kost2Row> rows = report.getKost2Rows();
      BigDecimal sum = BigDecimal.ZERO;
      int j = rows.size();
      for (final Kost2Row row : rows.values()) {
        final Kost2DO kost2 = row.getKost2();
        final MonthlyEmployeeReportEntry entry = report.getKost2Durations().get(kost2.getId());
        mapping.add(ExcelColumn.KOST1, kost1.getNummer());
        mapping.add(ExcelColumn.MITARBEITER, user.getFullname());
        mapping.add(ExcelColumn.KOST2, kost2.getNummer());
        final BigDecimal duration = new BigDecimal(entry.getMillis() / 1000); // Seconds
        // duration = duration.divide(new BigDecimal(60 * 60 * 24), 8, RoundingMode.HALF_UP); // Fraction of day (24 hours)
        // mapping.add(ExcelColumn.STUNDEN, duration);
        mapping.add(ExcelColumn.STUNDEN, duration.divide(new BigDecimal(3600), 2, RoundingMode.HALF_UP));
        mapping.add(ExcelColumn.BEZEICHNUNG, kost2.getToolTip());
        final BigDecimal betrag;
        if (NumberHelper.isNotZero(netDuration)) {
          betrag = CurrencyHelper.multiply(bruttoMitAGAnteil,
                  new BigDecimal(entry.getMillis()).divide(netDuration, 8, RoundingMode.HALF_UP));
        } else {
          betrag = BigDecimal.ZERO;
        }
        sum = sum.add(betrag);
        if (--j == 0) {
          final BigDecimal korrektur = bruttoMitAGAnteil.subtract(sum);
          mapping.add(ExcelColumn.BRUTTO_MIT_AG, betrag.add(korrektur));
          mapping.add(ExcelColumn.KORREKTUR, korrektur);
          if (NumberHelper.isEqual(sum.add(korrektur), bruttoMitAGAnteil)) {
            mapping.add(ExcelColumn.SUMME, bruttoMitAGAnteil);
          } else {
            mapping.add(ExcelColumn.SUMME, "*** " + sum + " != " + bruttoMitAGAnteil);
          }
        } else {
          mapping.add(ExcelColumn.BRUTTO_MIT_AG, betrag);
          mapping.add(ExcelColumn.KORREKTUR, "");
          mapping.add(ExcelColumn.SUMME, "");
        }
        mapping.add(ExcelColumn.DATUM, buchungsdatum.getCalendar()); // Last day of month
        mapping.add(ExcelColumn.KONTO, KONTO); // constant.
        mapping.add(ExcelColumn.GEGENKONTO, GEGENKONTO); // constant.
        sheet.addRow(mapping.getMapping(), 0);
      }
      addEmployeeRow(employeeSheet, salary.getEmployee(), numberOfWorkingDays, netDuration, report);
    }
    for (final EmployeeDO employee : missedEmployees) {
      final PFUserDO user = getUserGroupCache().getUser(employee.getUserId());
      final PropertyMapping mapping = new PropertyMapping();
      mapping.add(ExcelColumn.MITARBEITER, user.getFullname());
      mapping.add(ExcelColumn.SUMME, "***");
      mapping.add(ExcelColumn.BEZEICHNUNG, "*** FEHLT! ***");
      sheet.addRow(mapping.getMapping(), 0);
      final MonthlyEmployeeReport report = monthlyEmployeeReportDao.getReport(year, month, user);
      final BigDecimal netDuration = new BigDecimal(report.getTotalNetDuration());
      addEmployeeRow(employeeSheet, employee, numberOfWorkingDays, netDuration, report);
    }
    // sheet.setZoom(3, 4); // 75%

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      xls.write(baos);
    } catch (final IOException ex) {
      log.error("Exception encountered " + ex, ex);
      throw new RuntimeException(ex);
    }
    return baos.toByteArray();
  }

  private void addEmployeeRow(final ExportSheet sheet, final EmployeeDO employee, final BigDecimal numberOfWorkingDays, final BigDecimal totalDuration,
                              final MonthlyEmployeeReport report) {
    final PFUserDO user = getUserGroupCache().getUser(employee.getUserId());
    final ExportRow row = sheet.addRow();
    row.addCell(0, user.getFullname());
    // Wochenstunden
    row.addCell(1, employee.getWeeklyWorkingHours(), "STUNDEN");
    // Sollstunden: Wochenstunden * Arbeitstage / 5 Arbeitstage pro Woche
    BigDecimal wochenstunden = employee.getWeeklyWorkingHours();
    if (wochenstunden == null) {
      wochenstunden = BigDecimal.ZERO;
    }
    final BigDecimal soll = wochenstunden.multiply(numberOfWorkingDays).divide(new BigDecimal(5), 2,
            RoundingMode.HALF_UP);
    row.addCell(2, soll, "STUNDEN");
    // Iststunden
    final BigDecimal total = totalDuration.divide(new BigDecimal(3600000), 2, RoundingMode.HALF_UP);
    row.addCell(3, total, "STUNDEN");
    // Differenz
    final BigDecimal differenz = total.subtract(soll);
    row.addCell(4, differenz, "STUNDEN");
    row.addCell(5, report.getUnbookedDays().size());
    row.addCell(6, report.getFormattedUnbookedDays());
  }

  public TenantRegistry getTenantRegistry() {
    return TenantRegistryMap.getInstance().getTenantRegistry();
  }

  public UserGroupCache getUserGroupCache() {
    return getTenantRegistry().getUserGroupCache();
  }

  private enum ExcelColumn {
    KOST1("fibu.kost1", MyXlsContentProvider.LENGTH_KOSTENTRAEGER), MITARBEITER("fibu.employee",
            MyXlsContentProvider.LENGTH_USER), STUNDEN(
            "hours", MyXlsContentProvider.LENGTH_DURATION), KOST2("fibu.kost2",
            MyXlsContentProvider.LENGTH_KOSTENTRAEGER), BRUTTO_MIT_AG(
            "fibu.employee.salary.bruttoMitAgAnteil",
            MyXlsContentProvider.LENGTH_CURRENCY), KORREKTUR("fibu.common.korrekturWert",
            MyXlsContentProvider.LENGTH_CURRENCY), SUMME("sum",
            MyXlsContentProvider.LENGTH_CURRENCY), BEZEICHNUNG("description",
            MyXlsContentProvider.LENGTH_EXTRA_LONG), DATUM("date",
            MyXlsContentProvider.LENGTH_DATE), KONTO("fibu.buchungssatz.konto", 14), GEGENKONTO(
            "fibu.buchungssatz.gegenKonto", 14);

    final static ExcelColumn START = KOST1;
    final static ExcelColumn END = GEGENKONTO;
    final String theTitle;
    final int width;

    ExcelColumn(final String theTitle, final int width) {
      this.theTitle = theTitle;
      this.width = (short) width;
    }
  }

  private class MyContentProvider extends MyXlsContentProvider {
    public MyContentProvider(final ExportWorkbook workbook) {
      super(workbook);
    }

    @Override
    public MyContentProvider updateRowStyle(final ExportRow row) {
      for (final ExportCell cell : row.getCells()) {
        final CellFormat format = cell.ensureAndGetCellFormat();
        format.setFillForegroundColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
        switch (row.getRowNum()) {
          case 0:
            format.setFont(FONT_NORMAL_BOLD);
            // alignment = CellStyle.ALIGN_CENTER;
            break;
          default:
            format.setFont(FONT_NORMAL);
            if (row.getRowNum() % 2 == 0) {
              format.setFillForegroundColor(HSSFColor.HSSFColorPredefined.GREY_25_PERCENT.getIndex());
            }
            break;
        }
      }
      return this;
    }

    @Override
    public ContentProvider newInstance() {
      return new MyContentProvider(this.workbook);
    }
  }
}
