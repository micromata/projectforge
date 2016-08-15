package org.projectforge.plugins.eed.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.jfree.util.Log;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeStatus;
import org.projectforge.business.fibu.api.EmployeeSalaryService;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.excel.ExportRow;
import org.projectforge.excel.ExportSheet;
import org.projectforge.excel.ExportWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * @author blumenstein
 */
@Service
public class LBExporterService
{
  private static final int START_ROW_NR_FULLTIME = 10;

  @Autowired
  private EmployeeService employeeService;

  @Autowired
  private EmployeeSalaryService employeeSalaryService;

  public byte[] getExcel(final List<EmployeeDO> employeeList)
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
          actualRow.getCell(0).setValue(employee.getUser().getFullname());
          actualRow.getCell(1).setValue(employee.getWeeklyWorkingHours());
          actualRow.getCell(2).setValue(employee.getStaffNumber());
          BigDecimal bruttoMitAgAnteil = employeeSalaryService.getLatestSalaryForEmployee(employee) != null
              ? employeeSalaryService.getLatestSalaryForEmployee(employee).getBruttoMitAgAnteil() : null;
          actualRow.getCell(3).setValue(bruttoMitAgAnteil);
          copyRowFulltime = sheetFulltimeEmployee.getRow(copyRowNrFulltime);
        }
      }
    }
    //    Cell header = row.getCell(0);
    //    Cell sumHours = sheet.getRow(3).getCell(4).getPoiCell();
    //
    //    String string = header.getStringCellValue();
    //    string = string.replace("%fullName%", getUser().getFullname());
    //    string = string.replace("%startDate%", sdf.format(mondayDate.toDate()));
    //    string = string.replace("%endDate%", sdf.format(sundayDate.toDate()));
    //    header.setCellValue(string);
    //
    //    // first data row
    //    double hourCounter = 0;
    //    ExportRow firstDataRow = sheet.getRow(FIRST_DATA_ROW_NUM);
    //    hourCounter = fillRow(hourCounter, firstDataRow.getPoiRow(), timesheets.get(0));
    //
    //    // other data rows
    //    for (int i = 1; i < timesheets.size(); i++) {
    //      final Row newRow = copyRow(firstDataRow, FIRST_DATA_ROW_NUM + i);
    //      final TimesheetDO timesheet = timesheets.get(i);
    //      hourCounter = fillRow(hourCounter, newRow, timesheet);
    //
    //      CellStyle style = workbook.createCellStyle();
    //      style.setBorderBottom((short) 1);
    //      style.setShrinkToFit(true);
    //      style.setWrapText(true);
    //      newRow.setRowStyle(style);
    //    }
    //
    //    sumHours.setCellValue(String.valueOf(hourCounter));
    return workbook.getAsByteArray();
  }

  private void insertFullTimeRow(EmployeeDO employee)
  {
    // TODO Auto-generated method stub

  }

  private boolean isFulltimeEmployee(EmployeeDO employee)
  {
    return EmployeeStatus.FEST_ANGESTELLTER.equals(employee.getStatus())
        || EmployeeStatus.BEFRISTET_ANGESTELLTER.equals(employee.getStatus());
  }

}
