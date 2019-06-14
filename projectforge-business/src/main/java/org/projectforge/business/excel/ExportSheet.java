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

package org.projectforge.business.excel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

public class ExportSheet
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExportWorkbook.class);

  /**
   * Sheet names are limited to this length
   */
  public final static int MAX_XLS_SHEETNAME_LENGTH = 31;

  /**
   * Constant for an empty cell
   */
  public static final String EMPTY = "LEAVE_CELL_EMPTY";

  private final Sheet poiSheet;

  private final List<ExportRow> rows;

  private final String name;

  private String[] propertyNames;

  private int rowCounter = 0;

  private ContentProvider contentProvider;

  private boolean imported;

  private CellStyle cellStyle;

  public ExportSheet(final ContentProvider contentProvider, final String name, final Sheet poiSheet)
  {
    this.contentProvider = contentProvider;
    this.name = name;
    this.poiSheet = poiSheet;
    this.rows = new ArrayList<ExportRow>();
    initRowList();
    final PrintSetup printSetup = getPrintSetup();
    printSetup.setPaperSize(ExportConfig.getInstance().getDefaultPaperSizeId());
  }

  private void initRowList()
  {
    this.rows.clear();
    this.rowCounter = 0;
    final int lastRowNum = poiSheet.getLastRowNum();
    if (lastRowNum > 0) {
      // poiSheet does already exists.
      for (int i = poiSheet.getFirstRowNum(); i <= poiSheet.getLastRowNum(); i++) {
        Row poiRow = poiSheet.getRow(i);
        if (poiRow == null) {
          poiRow = poiSheet.createRow(i);
        }
        final ExportRow row = new ExportRow(contentProvider, this, poiRow, i);
        rows.add(row);
        this.rowCounter++;
      }
    }
  }

  /**
   * Convenient method: Adds all column names, titles, width and adds a head row.
   *
   * @param columns
   */
  public void setColumns(final List<ExportColumn> columns)
  {
    if (columns == null) {
      return;
    }
    // build all column names, title, widths from fixed and variable columns
    final String[] colNames = new String[columns.size()];
    final ExportRow headRow = addRow();
    int idx = 0;
    for (final ExportColumn col : columns) {
      addHeadRowCell(headRow, col, colNames, idx++);
    }
    setPropertyNames(colNames);
  }

  /**
   * Convenient method: Adds all column names, titles, width and adds a head row.
   *
   * @param columns
   */
  public void setColumns(final ExportColumn... columns)
  {
    if (columns == null) {
      return;
    }
    // build all column names, title, widths from fixed and variable columns
    final String[] colNames = new String[columns.length];
    final ExportRow headRow = addRow();
    int idx = 0;
    for (final ExportColumn col : columns) {
      addHeadRowCell(headRow, col, colNames, idx++);
    }
    setPropertyNames(colNames);
  }

  private void addHeadRowCell(final ExportRow headRow, final ExportColumn col, final String[] colNames, final int idx)
  {
    headRow.addCell(idx, col.getTitle());
    colNames[idx] = col.getName();
    contentProvider.putColWidth(idx, col.getWidth());
  }

  public PrintSetup getPrintSetup()
  {
    return poiSheet.getPrintSetup();
  }

  public ExportRow copyRow(ExportRow targetRow)
  {
    final Row poiRow = copyRow(targetRow.getSheet().getPoiSheet(), targetRow.getRowNum());
    initRowList();
    return rows.get(poiRow.getRowNum());
  }

  public ExportRow addRow()
  {
    final Row poiRow = poiSheet.createRow(rowCounter);
    final ExportRow row = new ExportRow(contentProvider, this, poiRow, rowCounter++);
    this.rows.add(row);
    return row;
  }

  public ExportRow addRow(final Object... values)
  {
    final ExportRow row = addRow();
    row.setValues(values);
    return row;
  }

  public ExportRow addRow(final Object rowBean)
  {
    return addRow(rowBean, 0);
  }

  public ExportRow addRow(final Object rowBean, final int startCol)
  {
    final ExportRow row = addRow();
    row.fillBean(rowBean, propertyNames, 0);
    return row;
  }

  public void addRows(final Object[] rowBeans)
  {
    addRows(rowBeans, 0);
  }

  public void addRows(final Object[] rowBeans, final int startCol)
  {
    for (final Object rowBean : rowBeans) {
      addRow(rowBean, startCol);
    }
  }

  public void addRows(final Collection<?> rowBeans)
  {
    addRows(rowBeans, 0);
  }

  public void addRows(final Collection<?> rowBeans, final int startCol)
  {
    for (final Object rowBean : rowBeans) {
      addRow(rowBean, startCol);
    }
  }

  public String getName()
  {
    return name;
  }

  public ExportRow getRow(final int row)
  {
    return this.rows.get(row);
  }

  /**
   * @return the rowCounter
   */
  public int getRowCounter()
  {
    return rowCounter;
  }

  public List<ExportRow> getRows()
  {
    return rows;
  }

  /**
   * For filling the table via beans.
   *
   * @param propertyNames
   */
  public void setPropertyNames(final String[] propertyNames)
  {
    this.propertyNames = propertyNames;
  }

  /**
   * @return the propertyNames
   */
  public String[] getPropertyNames()
  {
    return propertyNames;
  }

  public void updateStyles()
  {
    if (contentProvider != null) {
      contentProvider.updateSheetStyle(this);
      for (final ExportRow row : rows) {
        row.updateStyles(contentProvider);
      }
    }
  }

  public ContentProvider getContentProvider()
  {
    return contentProvider;
  }

  public void setContentProvider(final ContentProvider contentProvider)
  {
    this.contentProvider = contentProvider;
  }

  public void setColumnWidth(final int col, final int width)
  {
    poiSheet.setColumnWidth(col, width);
  }

  /**
   * Freezes the first toCol columns and the first toRow lines.
   *
   * @param toCol
   * @param toRow
   * @see Sheet#createFreezePane(int, int)
   */
  public void createFreezePane(final int toCol, final int toRow)
  {
    poiSheet.createFreezePane(toCol, toRow);
  }

  /**
   * @param x
   * @param y
   * @see Sheet#setZoom(int, int)
   */
  public void setZoom(final int x, final int y)
  {
    poiSheet.setZoom(x, y);
  }

  /**
   * Merges cells and sets the value.
   *
   * @param firstRow
   * @param lastRow
   * @param firstCol
   * @param lastCol
   * @param value
   */
  public ExportCell setMergedRegion(final int firstRow, final int lastRow, final int firstCol, final int lastCol,
      final Object value)
  {
    final CellRangeAddress region = new CellRangeAddress(firstRow, lastRow, firstCol, lastCol);
    poiSheet.addMergedRegion(region);
    final ExportRow row = getRow(firstRow);
    final ExportCell cell = row.addCell(firstCol, value);
    return cell;
  }

  public Sheet getPoiSheet()
  {
    return poiSheet;
  }

  /**
   * Set auto-filter for the whole first row. Must be called after adding the first row with all heading cells.
   *
   * @return this for chaining.
   */
  public ExportSheet setAutoFilter()
  {
    final int headingRow = 0;
    final ExportRow row = getRow(headingRow);
    final int lastCol = row.getMaxCol();
    final CellRangeAddress range = new CellRangeAddress(headingRow, headingRow, 0, lastCol);
    getPoiSheet().setAutoFilter(range);
    return this;
  }

  /**
   * @return true if this sheet was imported by a file.
   */
  public boolean isImported()
  {
    return imported;
  }

  public void setImported(final boolean imported)
  {
    this.imported = imported;
  }

  private Row copyRow(Sheet worksheet, int rowNum)
  {
    Row sourceRow = worksheet.getRow(rowNum);

    //Save the text of any formula before they are altered by row shifting
    String[] formulasArray = new String[sourceRow.getLastCellNum()];
    for (int i = 0; i < sourceRow.getLastCellNum(); i++) {
      if (sourceRow.getCell(i) != null && sourceRow.getCell(i).getCellType() == Cell.CELL_TYPE_FORMULA)
        formulasArray[i] = sourceRow.getCell(i).getCellFormula();
    }

    worksheet.shiftRows(rowNum, worksheet.getLastRowNum(), 1);
    Row newRow = sourceRow; //Now sourceRow is the empty line, so let's rename it
    sourceRow = worksheet.getRow(rowNum + 1); //Now the source row is at rowNum+1

    // Copy style from old cell and apply to new cell
    CellStyle newCellStyle = createOrGetCellStyle(worksheet);

    // Loop through source columns to add to new row
    for (int i = 0; i < sourceRow.getLastCellNum(); i++) {
      // Grab a copy of the old/new cell
      Cell oldCell = sourceRow.getCell(i);
      Cell newCell;

      // If the old cell is null jump to next cell
      if (oldCell == null) {
        continue;
      } else {
        newCell = newRow.createCell(i);
      }

      newCellStyle.cloneStyleFrom(oldCell.getCellStyle());
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
          break;
        case Cell.CELL_TYPE_BOOLEAN:
          newCell.setCellValue(oldCell.getBooleanCellValue());
          break;
        case Cell.CELL_TYPE_ERROR:
          newCell.setCellErrorValue(oldCell.getErrorCellValue());
          break;
        case Cell.CELL_TYPE_FORMULA:
          newCell.setCellFormula(formulasArray[i]);
          break;
        case Cell.CELL_TYPE_NUMERIC:
          newCell.setCellValue(oldCell.getNumericCellValue());
          break;
        case Cell.CELL_TYPE_STRING:
          newCell.setCellValue(oldCell.getRichStringCellValue());
          break;
        default:
          break;
      }
    }

    // If there are any merged regions in the source row, copy to new row
    for (int i = 0; i < worksheet.getNumMergedRegions(); i++) {
      CellRangeAddress cellRangeAddress = worksheet.getMergedRegion(i);
      if (cellRangeAddress.getFirstRow() == sourceRow.getRowNum()) {
        CellRangeAddress newCellRangeAddress = new CellRangeAddress(newRow.getRowNum(),
            (newRow.getRowNum() +
                (cellRangeAddress.getLastRow() - cellRangeAddress.getFirstRow())),
            cellRangeAddress.getFirstColumn(),
            cellRangeAddress.getLastColumn());
        worksheet.addMergedRegion(newCellRangeAddress);
      }
    }
    return newRow;
  }

  private CellStyle createOrGetCellStyle(Sheet worksheet) {
    if(cellStyle == null) {
      cellStyle = worksheet.getWorkbook().createCellStyle();
    }
    return cellStyle;
  }
}
