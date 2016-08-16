package org.projectforge.plugins.eed.service;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.jfree.util.Log;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeStatus;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.projectforge.business.fibu.api.EmployeeSalaryService;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.excel.ExportRow;
import org.projectforge.excel.ExportSheet;
import org.projectforge.excel.ExportWorkbook;
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

  @Autowired
  private EmployeeService employeeService;

  @Autowired
  private TimeableService<Integer, EmployeeTimedDO> timeableService;

  @Autowired
  private EmployeeSalaryService employeeSalaryService;

  @Autowired
  private AttrSchemaService attrSchemaService;

  public byte[] getExcel(final List<EmployeeDO> employeeList, Calendar selectedDate)
  {
    if (employeeList.size() < 1) {
      return new byte[] {};
    }
    ExportWorkbook workbook;
    ClassPathResource classPathResource = new ClassPathResource("LBExportTemplate.xls");
    try {
      workbook = new ExportWorkbook(classPathResource.getInputStream());
    } catch (IOException e) {
      Log.error("Something went wrong when loading xls template", e);
      return new byte[] {};
    }

    ExportSheet sheetFulltimeEmployee = workbook.getSheet(0);
    int copyRowNrFulltime = START_ROW_NR_FULLTIME;
    ExportRow copyRowFulltime = sheetFulltimeEmployee.getRow(copyRowNrFulltime);

    for (EmployeeDO employee : employeeList) {
      if (employeeService.isEmployeeActive(employee) == true) {
        if (isFulltimeEmployee(employee) == true) {
          sheetFulltimeEmployee.copyRow(copyRowFulltime);
          copyRowNrFulltime++;
          ExportRow actualRow = sheetFulltimeEmployee.getRow(copyRowNrFulltime - 1);
          //0 -> Name
          actualRow.getCell(0).setValue(employee.getUser().getFullname());
          //1 -> Arbeitsstunden
          actualRow.getCell(1).setValue(employee.getWeeklyWorkingHours());
          //2 -> Personalnummer
          actualRow.getCell(2).setValue(employee.getStaffNumber());
          //3 -> Gehalt
          //TODO: HR -> Gehalt ist nicht das richtige
          //          BigDecimal bruttoMitAgAnteil = employeeSalaryService.getLatestSalaryForEmployee(employee) != null
          //              ? employeeSalaryService.getLatestSalaryForEmployee(employee).getBruttoMitAgAnteil() : null;
          //          actualRow.getCell(3).setValue(bruttoMitAgAnteil);
          //20 -> Kita
          actualRow.getCell(20)
              .setValue(getActualAttrValue(employee, "daycarecenter", "daycarecenter", selectedDate));
          //21 -> eBike
          actualRow.getCell(21).setValue(getAttrValueForMonth(employee, "ebikeleasing", "ebikeleasing", selectedDate));
          //22 -> RK
          actualRow.getCell(21).setValue(getAttrValueForMonth(employee, "costtravel", "costtravel", selectedDate));
          //23 -> Auslagen
          actualRow.getCell(23).setValue(getAttrValueForMonth(employee, "expenses", "expenses", selectedDate));
          //24 Überstunden
          actualRow.getCell(24).setValue(getAttrValueForMonth(employee, "overtime", "overtime", selectedDate));
          //25 Prämie
          actualRow.getCell(25).setValue(getAttrValueForMonth(employee, "bonus", "bonus", selectedDate));
          //26 Sonderzahlung
          actualRow.getCell(26)
              .setValue(getAttrValueForMonth(employee, "specialpayment", "specialpayment", selectedDate));
          //27 Zielvereinbarung
          actualRow.getCell(27)
              .setValue(getAttrValueForMonth(employee, "targetagreements", "targetagreements", selectedDate));
          //28 Shop
          actualRow.getCell(28).setValue(getAttrValueForMonth(employee, "costshop", "costshop", selectedDate));
          //30 Samstagsarbeit
          actualRow.getCell(30)
              .setValue(getAttrValueForMonth(employee, "weekendwork", "workinghourssaturday", selectedDate));
          //31 Sonntagarbeit
          actualRow.getCell(31)
              .setValue(getAttrValueForMonth(employee, "weekendwork", "workinghourssunday", selectedDate));
          //32 Feiertagarbeit
          actualRow.getCell(32)
              .setValue(getAttrValueForMonth(employee, "weekendwork", "workinghoursholiday", selectedDate));
          //33 Bemerkung
          actualRow.getCell(33).setValue(getAttrValueForMonth(employee, "others", "others", selectedDate));
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
    EmployeeTimedDO attribute = timeableService.getAttrRowForDate(
        timeableService.getTimeableAttrRowsForGroup(employee, attrGroup), attrGroup, selectedDate.getTime());
    return attribute != null ? attribute.getStringAttribute(attrProperty) : null;
  }

  private String getAttrValueForMonth(EmployeeDO employee, String attrGroup, String attrProperty, Calendar selectedDate)
  {
    EmployeeTimedDO attribute = timeableService.getAttrRowForSameMonth(employee, attrGroup, selectedDate.getTime());
    return attribute != null ? attribute.getStringAttribute(attrProperty) : null;
  }

  private boolean isFulltimeEmployee(EmployeeDO employee)
  {
    return EmployeeStatus.FEST_ANGESTELLTER.equals(employee.getStatus())
        || EmployeeStatus.BEFRISTET_ANGESTELLTER.equals(employee.getStatus());
  }

}
