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

package org.projectforge.plugins.ihkexport;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.projectforge.business.excel.ExportRow;
import org.projectforge.business.excel.ExportSheet;
import org.projectforge.business.excel.ExportWorkbook;
import org.projectforge.business.timesheet.TimesheetDO;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.getUser;

/**
 * Created by jsiebert on 18.05.16.
 */
class IhkExporter
{
  private static final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

  private static final int FIRST_DATA_ROW_NUM = 2;

  static byte[] getExcel(final List<TimesheetDO> timesheets)
  {
    if (timesheets.size() < 1) {
      return new byte[] {};
    }
    ExportWorkbook workbook;

    DateTime mondayDate = new DateTime(timesheets.get(0).getStartTime()).withDayOfWeek(DateTimeConstants.MONDAY);
    DateTime sundayDate = mondayDate.withDayOfWeek(DateTimeConstants.SUNDAY);

    ClassPathResource classPathResource = new ClassPathResource("IHK-Template.xls");

    try {
      workbook = new ExportWorkbook(classPathResource.getInputStream());
    } catch (IOException e) {
      return new byte[] {};
    }

    ExportSheet sheet = workbook.getSheet(0);
    Row row = sheet.getRow(0).getPoiRow();
    Cell header = row.getCell(0);
    Cell sumHours = sheet.getRow(3).getCell(4).getPoiCell();

    String string = header.getStringCellValue();
    string = string.replace("%fullName%", getUser().getFullname());
    string = string.replace("%startDate%", sdf.format(mondayDate.toDate()));
    string = string.replace("%endDate%", sdf.format(sundayDate.toDate()));
    header.setCellValue(string);

    // first data row
    double hourCounter = 0;
    ExportRow firstDataRow = sheet.getRow(FIRST_DATA_ROW_NUM);
    hourCounter = fillRow(hourCounter, firstDataRow.getPoiRow(), timesheets.get(0));

    // other data rows
    for (int i = 1; i < timesheets.size(); i++) {
      final Row newRow = copyRow(firstDataRow, FIRST_DATA_ROW_NUM + i);
      final TimesheetDO timesheet = timesheets.get(i);
      hourCounter = fillRow(hourCounter, newRow, timesheet);

      CellStyle style = workbook.createCellStyle();
      style.setBorderBottom((short) 1);
      style.setShrinkToFit(true);
      style.setWrapText(true);
      newRow.setRowStyle(style);
    }

    sumHours.setCellValue(String.valueOf(hourCounter));
    return workbook.getAsByteArray();
  }

  private static double fillRow(double hourCounter, final Row newRow, final TimesheetDO timesheet)
  {
    final double durationInHours = timesheet.getDuration() / (1000.0 * 60.0 * 60.0);
    hourCounter += durationInHours;

    newRow.getCell(0).setCellValue(sdf.format(timesheet.getStartTime()));
    newRow.getCell(1).setCellValue(timesheet.getDescription());
    newRow.getCell(3).setCellValue(String.valueOf(durationInHours));
    newRow.getCell(4).setCellValue(String.valueOf(hourCounter));

    return hourCounter;
  }

  /**
   * Copies a Row
   *
   * @param destinationRowNum
   * @return copied PoiRow
   */
  static Row copyRow(ExportRow source, int destinationRowNum)
  {
    // Get the source / new row
    HSSFSheet worksheet = (HSSFSheet) source.getPoiRow().getSheet();
    HSSFRow newRow = worksheet.getRow(destinationRowNum);
    HSSFRow sourceRow = worksheet.getRow(source.getRowNum());

    // If the row exist in destination, push down all rows by 1 else create a new row
    if (newRow != null) {
      worksheet.shiftRows(destinationRowNum, worksheet.getLastRowNum(), 1);
    } else {
      newRow = worksheet.createRow(destinationRowNum);
    }

    // Loop through source columns to add to new row
    for (int i = 0; i < sourceRow.getLastCellNum(); i++) {
      // Grab a copy of the old/new cell
      HSSFCell oldCell = sourceRow.getCell(i);
      HSSFCell newCell = newRow.createCell(i);

      // If the old cell is null jump to next cell
      if (oldCell == null) {
        newCell = null;
        continue;
      }

      // Copy style from old cell and apply to new cell
      HSSFCellStyle newCellStyle = worksheet.getWorkbook().createCellStyle();
      newCellStyle.cloneStyleFrom(oldCell.getCellStyle());
      ;
      newCell.setCellStyle(newCellStyle);

      // If there is a cell comment, copy
      if (oldCell.getCellComment() != null) {
        newCell.setCellComment(oldCell.getCellComment());
      }

      // If there is a cell hyperlink, copy
      if (oldCell.getHyperlink() != null) {
        newCell.setHyperlink(oldCell.getHyperlink());
      }

      // Set the cell data type
      newCell.setCellType(oldCell.getCellType());

      // Set the cell data value
      switch (oldCell.getCellType()) {
        case Cell.CELL_TYPE_BLANK:
          newCell.setCellValue(oldCell.getStringCellValue());
          break;
        case Cell.CELL_TYPE_BOOLEAN:
          newCell.setCellValue(oldCell.getBooleanCellValue());
          break;
        case Cell.CELL_TYPE_ERROR:
          newCell.setCellErrorValue(oldCell.getErrorCellValue());
          break;
        case Cell.CELL_TYPE_FORMULA:
          newCell.setCellFormula(oldCell.getCellFormula());
          break;
        case Cell.CELL_TYPE_NUMERIC:
          newCell.setCellValue(oldCell.getNumericCellValue());
          break;
        case Cell.CELL_TYPE_STRING:
          newCell.setCellValue(oldCell.getRichStringCellValue());
          break;
      }
    }

    // If there are are any merged regions in the source row, copy to new row
    for (int i = 0; i < worksheet.getNumMergedRegions(); i++) {
      CellRangeAddress cellRangeAddress = worksheet.getMergedRegion(i);
      if (cellRangeAddress.getFirstRow() == sourceRow.getRowNum()) {
        CellRangeAddress newCellRangeAddress = new CellRangeAddress(newRow.getRowNum(),
            (newRow.getRowNum() +
                (cellRangeAddress.getLastRow() - cellRangeAddress.getFirstRow()
                )),
            cellRangeAddress.getFirstColumn(),
            cellRangeAddress.getLastColumn());
        worksheet.addMergedRegion(newCellRangeAddress);
      }
    }
    return newRow;
  }

}
