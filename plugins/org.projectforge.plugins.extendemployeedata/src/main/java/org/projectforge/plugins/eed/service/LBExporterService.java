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

package org.projectforge.plugins.eed.service;

import de.micromata.genome.db.jpa.tabattr.api.AttrSchemaService;
import de.micromata.genome.db.jpa.tabattr.api.TimeableService;
import org.jfree.util.Log;
import org.projectforge.business.excel.ExportRow;
import org.projectforge.business.excel.ExportSheet;
import org.projectforge.business.excel.ExportWorkbook;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.plugins.eed.model.EmployeeConfigurationDO;
import org.projectforge.plugins.eed.model.EmployeeConfigurationTimedDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

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
      if (employeeService.isEmployeeActive(employee)) {
        if (employeeService.isFulltimeEmployee(employee, selectedDate)) {
          sheetFulltimeEmployee.copyRow(copyRowFulltime);
          copyRowNrFulltime++;
          final ExportRow currentRow = sheetFulltimeEmployee.getRow(copyRowNrFulltime - 1);
          //0 -> Lastname
          currentRow.getCell(0).setValue(employee.getUser().getLastname());
          //1 -> Firstname
          currentRow.getCell(1).setValue(employee.getUser().getFirstname());
          //2 -> Arbeitsstunden
          currentRow.getCell(2).setValue(employee.getWeeklyWorkingHours());
          //3 -> Personalnummer
          currentRow.getCell(3).setValue(employee.getStaffNumber());
          //4 -> Gehalt
          currentRow.getCell(4).setValue(round(employeeService.getMonthlySalary(employee, selectedDate)));
          //14 / 15 Essen
          final boolean hasFood = getAttrValueForMonthAsBoolean(employee, "food", "food", selectedDate);
          if (hasFood) {
            BigDecimal oneDayValue = getAttrValueForMonthAsBigDecimal(singleEmployeeConfigurationDO, "food",
                "referencevalue", selectedDate);
            if (oneDayValue != null) {
              final BigDecimal fullValue = oneDayValue.multiply(FOOD_VOUCHER_DAYS_PER_MONTH);
              currentRow.getCell(14).setValue(round(fullValue));
            }

            oneDayValue = getAttrValueForMonthAsBigDecimal(singleEmployeeConfigurationDO, "food", "contribution",
                selectedDate);
            if (oneDayValue != null) {
              final BigDecimal fullValue = oneDayValue.multiply(FOOD_VOUCHER_DAYS_PER_MONTH);
              currentRow.getCell(15).setValue(round(fullValue));
            }
          }
          //16 Tankgutschein
          final boolean hasFuelVoucher = getAttrValueForMonthAsBoolean(employee, "fuelvoucher", "fuelvoucher",
              selectedDate);
          if (hasFuelVoucher) {
            currentRow.getCell(16).setValue(round(
                getAttrValueForMonthAsBigDecimal(singleEmployeeConfigurationDO, "refuel", "voucher", selectedDate)));
          }
          //22 -> Kita
          currentRow.getCell(22).setValue(round(getAttrValueForMonthAsBigDecimal(employee, "daycarecenter", "daycarecenter", selectedDate)));
          //23 -> eBike
          currentRow.getCell(23).setValue(round(getAttrValueForMonthAsBigDecimal(employee, "ebikeleasing", "ebikeleasing", selectedDate)));
          //24 -> RK
          currentRow.getCell(24).setValue(round(getAttrValueForMonthAsBigDecimal(employee, "costtravel", "costtravel", selectedDate)));
          //25 -> Auslagen
          currentRow.getCell(25).setValue(round(getAttrValueForMonthAsBigDecimal(employee, "expenses", "expenses", selectedDate)));
          //26 Überstunden
          currentRow.getCell(26).setValue(getAttrValueForMonthAsBigDecimal(employee, "overtime", "overtime", selectedDate));
          //27 Prämie
          currentRow.getCell(27).setValue(round(getAttrValueForMonthAsBigDecimal(employee, "bonus", "bonus", selectedDate)));
          //28 Sonderzahlung
          currentRow.getCell(28).setValue(round(getAttrValueForMonthAsBigDecimal(employee, "specialpayment", "specialpayment", selectedDate)));
          //29 Zielvereinbarung
          currentRow.getCell(29).setValue(round(getAttrValueForMonthAsBigDecimal(employee, "targetagreements", "targetagreements", selectedDate)));
          //30 Shop
          currentRow.getCell(30).setValue(round(getAttrValueForMonthAsBigDecimal(employee, "costshop", "costshop", selectedDate)));
          //32 Samstagsarbeit TODO: convert hours to money and don't forget to call round()
          currentRow.getCell(32).setValue(getAttrValueForMonthAsBigDecimal(employee, "weekendwork", "workinghourssaturday", selectedDate));
          //33 Sonntagarbeit TODO: convert hours to money and don't forget to call round()
          currentRow.getCell(33).setValue(getAttrValueForMonthAsBigDecimal(employee, "weekendwork", "workinghourssunday", selectedDate));
          //34 Feiertagarbeit TODO: convert hours to money and don't forget to call round()
          currentRow.getCell(34).setValue(getAttrValueForMonthAsBigDecimal(employee, "weekendwork", "workinghoursholiday", selectedDate));
          //35 Bemerkung
          currentRow.getCell(35).setValue(getAttrValueForMonthAsString(employee, "others", "others", selectedDate));
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
}
