package org.projectforge.plugins.eed.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jfree.util.Log;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeStatus;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.excel.ExportRow;
import org.projectforge.excel.ExportSheet;
import org.projectforge.excel.ExportWorkbook;
import org.projectforge.plugins.eed.model.EmployeeConfigurationDO;
import org.projectforge.plugins.eed.model.EmployeeConfigurationTimedDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;
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
  private TimeableService<Integer, EmployeeTimedDO> timeableEmployeeService;

  @Autowired
  private TimeableService<Integer, EmployeeConfigurationTimedDO> timeableEmployeeConfigurationService;

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
    final EmployeeConfigurationDO singleEmployeeConfigurationDO = employeeConfigurationService.getSingleEmployeeConfigurationDO();

    for (EmployeeDO employee : employeeList) {
      if (employeeService.isEmployeeActive(employee) == true) {
        if (isFulltimeEmployee(employee) == true) {
          sheetFulltimeEmployee.copyRow(copyRowFulltime);
          copyRowNrFulltime++;
          final ExportRow actualRow = sheetFulltimeEmployee.getRow(copyRowNrFulltime - 1);
          //0 -> Name
          actualRow.getCell(0).setValue(employee.getUser().getFullname());
          //1 -> Arbeitsstunden
          actualRow.getCell(1).setValue(employee.getWeeklyWorkingHours());
          //2 -> Personalnummer
          actualRow.getCell(2).setValue(employee.getStaffNumber());
          //3 -> Gehalt
          actualRow.getCell(3).setValue(employeeService.getMonthlySalary(employee, selectedDate));
          //13 / 14 Essen
          final String hasFood = getActualAttrValue(employee, "food", "food", selectedDate);
          if (StringUtils.isNotEmpty(hasFood) && Boolean.parseBoolean(hasFood)) {
            BigDecimal oneDayValue = getAttrValueForMonthAsBigDecimal(singleEmployeeConfigurationDO, "food", "referencevalue", selectedDate);
            if (oneDayValue != null) {
              final BigDecimal fullValue = oneDayValue.multiply(FOOD_VOUCHER_DAYS_PER_MONTH);
              actualRow.getCell(13).setValue(fullValue);
            }

            oneDayValue = getAttrValueForMonthAsBigDecimal(singleEmployeeConfigurationDO, "food", "contribution", selectedDate);
            if (oneDayValue != null) {
              final BigDecimal fullValue = oneDayValue.multiply(FOOD_VOUCHER_DAYS_PER_MONTH);
              actualRow.getCell(14).setValue(fullValue);
            }
          }
          //15 Tankgutschein
          final String hasFuelVoucher = getActualAttrValue(employee, "fuelvoucher", "fuelvoucher", selectedDate);
          if (StringUtils.isNotEmpty(hasFuelVoucher) && Boolean.parseBoolean(hasFuelVoucher)) {
            actualRow.getCell(15).setValue(getAttrValueForMonthAsBigDecimal(singleEmployeeConfigurationDO, "refuel", "voucher", selectedDate));
          }
          //21 -> Kita
          actualRow.getCell(21)
              .setValue(getActualAttrValue(employee, "daycarecenter", "daycarecenter", selectedDate));
          //22 -> eBike
          actualRow.getCell(22).setValue(getAttrValueForMonthAsString(employee, "ebikeleasing", "ebikeleasing", selectedDate));
          //23 -> RK
          actualRow.getCell(23).setValue(getAttrValueForMonthAsString(employee, "costtravel", "costtravel", selectedDate));
          //24 -> Auslagen
          actualRow.getCell(24).setValue(getAttrValueForMonthAsString(employee, "expenses", "expenses", selectedDate));
          //25 Überstunden
          actualRow.getCell(25).setValue(getAttrValueForMonthAsString(employee, "overtime", "overtime", selectedDate));
          //26 Prämie
          actualRow.getCell(26).setValue(getAttrValueForMonthAsString(employee, "bonus", "bonus", selectedDate));
          //27 Sonderzahlung
          actualRow.getCell(27)
              .setValue(getAttrValueForMonthAsString(employee, "specialpayment", "specialpayment", selectedDate));
          //28 Zielvereinbarung
          actualRow.getCell(28)
              .setValue(getAttrValueForMonthAsString(employee, "targetagreements", "targetagreements", selectedDate));
          //29 Shop
          actualRow.getCell(29).setValue(getAttrValueForMonthAsString(employee, "costshop", "costshop", selectedDate));
          //31 Samstagsarbeit
          actualRow.getCell(31)
              .setValue(getAttrValueForMonthAsString(employee, "weekendwork", "workinghourssaturday", selectedDate));
          //32 Sonntagarbeit
          actualRow.getCell(32)
              .setValue(getAttrValueForMonthAsString(employee, "weekendwork", "workinghourssunday", selectedDate));
          //33 Feiertagarbeit
          actualRow.getCell(33)
              .setValue(getAttrValueForMonthAsString(employee, "weekendwork", "workinghoursholiday", selectedDate));
          //34 Bemerkung
          actualRow.getCell(34).setValue(getAttrValueForMonthAsString(employee, "others", "others", selectedDate));
          copyRowFulltime = sheetFulltimeEmployee.getRow(copyRowNrFulltime);
        }
      }
    }
    return workbook.getAsByteArray();
  }

  private String getActualAttrValue(EmployeeDO employee, String attrGroupString, String attrProperty,
      Calendar selectedDate)
  {
    AttrGroup attrGroup = attrSchemaService.getAttrGroup(employee, attrGroupString);
    EmployeeTimedDO attribute = timeableEmployeeService.getAttrRowForDate(
        timeableEmployeeService.getTimeableAttrRowsForGroup(employee, attrGroup), attrGroup, selectedDate.getTime());
    return attribute != null ? attribute.getStringAttribute(attrProperty) : null;
  }

  private String getAttrValueForMonthAsString(EmployeeDO employee, String attrGroup, String attrProperty, Calendar selectedDate)
  {
    EmployeeTimedDO attribute = timeableEmployeeService.getAttrRowForSameMonth(employee, attrGroup,
        selectedDate.getTime());
    return attribute != null ? attribute.getStringAttribute(attrProperty) : null;
  }

  private String getAttrValueForMonthAsString(EmployeeConfigurationDO configuration, String attrGroup, String attrProperty,
      Calendar selectedDate)
  {
    EmployeeConfigurationTimedDO attribute = timeableEmployeeConfigurationService.getAttrRowForSameMonth(configuration,
        attrGroup,
        selectedDate.getTime());
    return attribute != null ? attribute.getStringAttribute(attrProperty) : null;
  }

  private BigDecimal getAttrValueForMonthAsBigDecimal(EmployeeConfigurationDO configuration, String attrGroup, String attrProperty,
      Calendar selectedDate)
  {
    EmployeeConfigurationTimedDO attribute = timeableEmployeeConfigurationService.getAttrRowForSameMonth(configuration,
        attrGroup,
        selectedDate.getTime());
    return attribute != null ? attribute.getAttribute(attrProperty, BigDecimal.class) : null;
  }

  private boolean isFulltimeEmployee(EmployeeDO employee)
  {
    return EmployeeStatus.FEST_ANGESTELLTER.equals(employee.getStatus())
        || EmployeeStatus.BEFRISTET_ANGESTELLTER.equals(employee.getStatus());
  }

}
