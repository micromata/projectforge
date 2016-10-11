package org.projectforge.plugins.eed.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.jfree.util.Log;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeStatus;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.excel.ExportRow;
import org.projectforge.excel.ExportSheet;
import org.projectforge.excel.ExportWorkbook;
import org.projectforge.framework.persistence.attr.impl.InternalAttrSchemaConstants;
import org.projectforge.plugins.eed.model.EmployeeConfigurationDO;
import org.projectforge.plugins.eed.model.EmployeeConfigurationTimedDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import de.micromata.genome.db.jpa.tabattr.api.AttrSchemaService;
import de.micromata.genome.db.jpa.tabattr.api.TimeableService;

/**
 * @author blumenstein
 */
@Service
public class LBExporterService
{
  private static final int START_ROW_NR_FULLTIME = 3;

  private static final BigDecimal FOOD_VOUCHER_DAYS_PER_MONTH = new BigDecimal(15);

  @Autowired
  private EmployeeService employeeService;

  @Autowired
  private TimeableService timeableService;

  @Autowired
  private AttrSchemaService attrSchemaService;

  @Autowired
  private EmployeeConfigurationService employeeConfigurationService;

  public byte[] getExcel(final List<EmployeeDO> employeeList, Calendar selectedDate)
  {
    if (employeeList.size() < 1) {
      return new byte[0];
    }
    ExportWorkbook workbook;
    ClassPathResource classPathResource = new ClassPathResource("LBExportTemplate.xls");
    try {
      workbook = new ExportWorkbook(classPathResource.getInputStream());
    } catch (IOException e) {
      Log.error("Something went wrong when loading xls template", e);
      return new byte[0];
    }

    ExportSheet sheetFulltimeEmployee = workbook.getSheet(0);
    int copyRowNrFulltime = START_ROW_NR_FULLTIME;
    ExportRow copyRowFulltime = sheetFulltimeEmployee.getRow(copyRowNrFulltime);
    final EmployeeConfigurationDO singleEmployeeConfigurationDO = employeeConfigurationService
        .getSingleEmployeeConfigurationDO();

    for (EmployeeDO employee : employeeList) {
      if (employeeService.isEmployeeActive(employee) == true) {
        if (isFulltimeEmployee(employee, selectedDate) == true) {
          sheetFulltimeEmployee.copyRow(copyRowFulltime);
          copyRowNrFulltime++;
          final ExportRow currentRow = sheetFulltimeEmployee.getRow(copyRowNrFulltime - 1);
          //0 -> Name
          currentRow.getCell(0).setValue(employee.getUser().getFullname());
          //1 -> Arbeitsstunden
          currentRow.getCell(1).setValue(employee.getWeeklyWorkingHours());
          //2 -> Personalnummer
          currentRow.getCell(2).setValue(employee.getStaffNumber());
          //3 -> Gehalt
          currentRow.getCell(3).setValue(round(employeeService.getMonthlySalary(employee, selectedDate)));
          //13 / 14 Essen
          final boolean hasFood = getAttrValueForMonthAsBoolean(employee, "food", "food", selectedDate);
          if (hasFood) {
            BigDecimal oneDayValue = getAttrValueForMonthAsBigDecimal(singleEmployeeConfigurationDO, "food",
                "referencevalue", selectedDate);
            if (oneDayValue != null) {
              final BigDecimal fullValue = oneDayValue.multiply(FOOD_VOUCHER_DAYS_PER_MONTH);
              currentRow.getCell(13).setValue(round(fullValue));
            }

            oneDayValue = getAttrValueForMonthAsBigDecimal(singleEmployeeConfigurationDO, "food", "contribution",
                selectedDate);
            if (oneDayValue != null) {
              final BigDecimal fullValue = oneDayValue.multiply(FOOD_VOUCHER_DAYS_PER_MONTH);
              currentRow.getCell(14).setValue(round(fullValue));
            }
          }
          //15 Tankgutschein
          final boolean hasFuelVoucher = getAttrValueForMonthAsBoolean(employee, "fuelvoucher", "fuelvoucher",
              selectedDate);
          if (hasFuelVoucher) {
            currentRow.getCell(15).setValue(round(
                getAttrValueForMonthAsBigDecimal(singleEmployeeConfigurationDO, "refuel", "voucher", selectedDate)));
          }
          //21 -> Kita
          currentRow.getCell(21).setValue(round(getAttrValueForMonthAsBigDecimal(employee, "daycarecenter", "daycarecenter", selectedDate)));
          //22 -> eBike
          currentRow.getCell(22).setValue(round(getAttrValueForMonthAsBigDecimal(employee, "ebikeleasing", "ebikeleasing", selectedDate)));
          //23 -> RK
          currentRow.getCell(23).setValue(round(getAttrValueForMonthAsBigDecimal(employee, "costtravel", "costtravel", selectedDate)));
          //24 -> Auslagen
          currentRow.getCell(24).setValue(round(getAttrValueForMonthAsBigDecimal(employee, "expenses", "expenses", selectedDate)));
          //25 Überstunden
          currentRow.getCell(25).setValue(getAttrValueForMonthAsBigDecimal(employee, "overtime", "overtime", selectedDate));
          //26 Prämie
          currentRow.getCell(26).setValue(round(getAttrValueForMonthAsBigDecimal(employee, "bonus", "bonus", selectedDate)));
          //27 Sonderzahlung
          currentRow.getCell(27).setValue(round(getAttrValueForMonthAsBigDecimal(employee, "specialpayment", "specialpayment", selectedDate)));
          //28 Zielvereinbarung
          currentRow.getCell(28).setValue(round(getAttrValueForMonthAsBigDecimal(employee, "targetagreements", "targetagreements", selectedDate)));
          //29 Shop
          currentRow.getCell(29).setValue(round(getAttrValueForMonthAsBigDecimal(employee, "costshop", "costshop", selectedDate)));
          //31 Samstagsarbeit TODO: convert hours to money and don't forget to call round()
          currentRow.getCell(31).setValue(getAttrValueForMonthAsBigDecimal(employee, "weekendwork", "workinghourssaturday", selectedDate));
          //32 Sonntagarbeit TODO: convert hours to money and don't forget to call round()
          currentRow.getCell(32).setValue(getAttrValueForMonthAsBigDecimal(employee, "weekendwork", "workinghourssunday", selectedDate));
          //33 Feiertagarbeit TODO: convert hours to money and don't forget to call round()
          currentRow.getCell(33).setValue(getAttrValueForMonthAsBigDecimal(employee, "weekendwork", "workinghoursholiday", selectedDate));
          //34 Bemerkung
          currentRow.getCell(34).setValue(getAttrValueForMonthAsString(employee, "others", "others", selectedDate));
          copyRowFulltime = sheetFulltimeEmployee.getRow(copyRowNrFulltime);
        }
      }
    }
    return workbook.getAsByteArray();
  }

  private String getAttrValueForMonthAsString(EmployeeDO employee, String attrGroup, String attrProperty,
      Calendar selectedDate)
  {
    final EmployeeTimedDO attribute = timeableService.getAttrRowForSameMonth(employee, attrGroup, selectedDate.getTime());
    return attribute != null ? attribute.getStringAttribute(attrProperty) : null;
  }

  private boolean getAttrValueForMonthAsBoolean(EmployeeDO employee, String attrGroup, String attrProperty,
      Calendar selectedDate)
  {
    final EmployeeTimedDO attribute = timeableService.getAttrRowForSameMonth(employee, attrGroup, selectedDate.getTime());

    if (attribute == null) {
      return false;
    }

    final Boolean value = attribute.getAttribute(attrProperty, Boolean.class);
    return Boolean.TRUE.equals(value);
  }

  private BigDecimal getAttrValueForMonthAsBigDecimal(EmployeeDO employee, String attrGroupString, String attrProperty,
      Calendar selectedDate)
  {
    final EmployeeTimedDO attribute = timeableService.getAttrRowForSameMonth(employee, attrGroupString, selectedDate.getTime());
    return attribute != null ? attribute.getAttribute(attrProperty, BigDecimal.class) : null;
  }

  private BigDecimal getAttrValueForMonthAsBigDecimal(EmployeeConfigurationDO configuration, String attrGroup, String attrProperty, Calendar selectedDate)
  {
    EmployeeConfigurationTimedDO attribute = timeableService.getAttrRowForSameMonth(configuration, attrGroup, selectedDate.getTime());
    return attribute != null ? attribute.getAttribute(attrProperty, BigDecimal.class) : null;
  }

  private BigDecimal round(final BigDecimal value)
  {
    if (value == null) {
      return null;
    }

    return value.setScale(2, BigDecimal.ROUND_HALF_UP); // round to two decimal places
  }

  /**
   * Checks if the employee was full time some day at the beginning of the month or within the month.
   *
   * @param employee     The employee.
   * @param selectedDate The first day of the month to check.
   * @return The result.
   */
  private boolean isFulltimeEmployee(final EmployeeDO employee, final Calendar selectedDate)
  {
    final Calendar date = (Calendar) selectedDate.clone(); // create a clone to avoid changing the original object
    final Date startOfMonth = date.getTime();
    date.add(Calendar.MONTH, 1);
    date.add(Calendar.DATE, -1);
    final Date endOfMonth = date.getTime();

    final List<EmployeeTimedDO> attrRows = timeableService
        .getAttrRowsWithinDateRange(employee, InternalAttrSchemaConstants.EMPLOYEE_STATUS_GROUP_NAME, startOfMonth, endOfMonth);

    final EmployeeTimedDO rowValidAtBeginOfMonth = timeableService
        .getAttrRowValidAtDate(employee, InternalAttrSchemaConstants.EMPLOYEE_STATUS_GROUP_NAME, selectedDate.getTime());

    if (rowValidAtBeginOfMonth != null) {
      attrRows.add(rowValidAtBeginOfMonth);
    }

    return attrRows
        .stream()
        .map(row -> row.getStringAttribute(InternalAttrSchemaConstants.EMPLOYEE_STATUS_DESC_NAME))
        .filter(Objects::nonNull)
        .anyMatch(s -> EmployeeStatus.FEST_ANGESTELLTER.getI18nKey().equals(s) || EmployeeStatus.BEFRISTET_ANGESTELLTER.getI18nKey().equals(s));
  }
}
