package org.projectforge.plugins.eed;

import java.io.IOException;
import java.util.List;

import org.jfree.util.Log;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.excel.ExportRow;
import org.projectforge.excel.ExportSheet;
import org.projectforge.excel.ExportWorkbook;
import org.springframework.core.io.ClassPathResource;

/**
 * @author blumenstein
 */
public class LBExporter
{
  private static final int START_ROW_NR = 10;

  public static byte[] getExcel(final List<EmployeeDO> employeeList)
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
    int copyRowNr = START_ROW_NR;
    ExportRow copyRow = sheetFulltimeEmployee.getRow(copyRowNr);
    for (EmployeeDO employee : employeeList) {
      sheetFulltimeEmployee.copyRow(copyRow);
      copyRowNr++;
      ExportRow actualRow = sheetFulltimeEmployee.getRow(copyRowNr - 1);
      actualRow.getCell(0).setValue(employee.getUser().getFullname());
      copyRow = sheetFulltimeEmployee.getRow(copyRowNr);
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

}
