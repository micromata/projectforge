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

package org.projectforge.excel;

import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;

public class ExportCell
{
  private final Cell poiCell;

  private final int row;

  private final int col;

  private CellFormat cellFormat;

  private ContentProvider styleProvider;

  public ExportCell(final ContentProvider styleProvider, final Cell poiCell, final int row, final int col)
  {
    this.styleProvider = styleProvider;
    this.poiCell = poiCell;
    this.row = row;
    this.col = col;
  }

  /**
   * @param value
   * @return this for chaining.
   */
  public ExportCell setValue(final Object value)
  {
    return setValue(value, null);
  }

  /**
   * @param value
   * @param property
   * @return this for chaining.
   */
  public ExportCell setValue(final Object value, final String property)
  {
    styleProvider.setValue(this, value, property);
    return this;
  }

  public double getNumericCellValue()
  {
    if (poiCell == null) {
      return 0.0;
    }
    return poiCell.getNumericCellValue();
  }

  public boolean getBooleanCellValue()
  {
    if (poiCell == null) {
      return false;
    }
    return poiCell.getBooleanCellValue();
  }

  public String getStringCellValue()
  {
    final Object obj = getCellValue();
    if (obj == null) {
      return "";
    } else if (obj instanceof String) {
      return (String) obj;
    }
    return String.valueOf(obj);
  }

  public Object getCellValue()
  {
    if (poiCell == null) {
      return null;
    }
    switch (poiCell.getCellType()) {
      case Cell.CELL_TYPE_STRING:
        return poiCell.getRichStringCellValue().getString();
      case Cell.CELL_TYPE_NUMERIC:
        if (DateUtil.isCellDateFormatted(poiCell)) {
          return poiCell.getDateCellValue();
        }
        return poiCell.getNumericCellValue();
      case Cell.CELL_TYPE_BOOLEAN:
        return poiCell.getBooleanCellValue();
      case Cell.CELL_TYPE_FORMULA:
        return poiCell.getCellFormula();
      default:
        return null;
    }
  }

  public boolean isNumericCellType()
  {
    if (poiCell == null) {
      return false;
    }
    return poiCell.getCellType() == Cell.CELL_TYPE_NUMERIC;
  }

  public Date getDateCellValue()
  {
    if (poiCell == null) {
      return null;
    }
    return poiCell.getDateCellValue();
  }

  public Cell getPoiCell()
  {
    return poiCell;
  }

  public void setStyleProvider(final ContentProvider styleProvider)
  {
    this.styleProvider = styleProvider;
  }

  public int getRow()
  {
    return row;
  }

  public int getCol()
  {
    return col;
  }

  /**
   * @param cellFormat
   * @return this for chaining.
   */
  public ExportCell setCellFormat(final String cellFormat)
  {
    this.cellFormat = new CellFormat(cellFormat);
    return this;
  }

  /**
   * @param cellFormat
   * @return this for chaining.
   */
  public ExportCell setCellFormat(final CellFormat cellFormat)
  {
    this.cellFormat = cellFormat;
    return this;
  }

  public CellFormat ensureAndGetCellFormat()
  {
    if (cellFormat == null) {
      cellFormat = new CellFormat();
    }
    return cellFormat;
  }

  public CellFormat getCellFormat()
  {
    return cellFormat;
  }

  public CellStyle getCellStyle()
  {
    return this.poiCell.getCellStyle();
  }

  /**
   * Should only be called directly before the export. Please note: Excel does support only a limited number of different cell styles, so
   * re-use cell styles with same format.
   * @param cellStyle
   * @return this for chaining.
   */
  public ExportCell setCellStyle(final CellStyle cellStyle)
  {
    this.poiCell.setCellStyle(cellStyle);
    return this;
  }

  public CellStyle ensureAndGetCellStyle()
  {
    CellStyle cellStyle = this.poiCell.getCellStyle();
    if (cellStyle == null) {
      cellStyle = styleProvider.getWorkbook().createCellStyle();
    }
    return cellStyle;
  }

  /**
   * Excel shares the cell formats and the number of cell formats is limited. This method uses a new cell style!<br/>
   * Don't forget to call #setCellStyle(CellStyle) if you want to apply this cloned one.
   * @return
   */
  public CellStyle cloneCellStyle() {
    final CellStyle cellStyle = styleProvider.getWorkbook().createCellStyle();
    final CellStyle origCellStyle = this.poiCell.getCellStyle();
    if (origCellStyle != null) {
      cellStyle.cloneStyleFrom(origCellStyle);
    }
    return cellStyle;
  }

  /**
   * Sets the data format of the poi cell. Use this method only if you modify existing cells of an existing workbook (loaded from file).
   * @param dataFormat
   * @return this for chaining.
   */
  public ExportCell setDataFormat(final String dataFormat)
  {
    final CellStyle cellStyle = ensureAndGetCellStyle();
    final short df = styleProvider.getWorkbook().getDataFormat(dataFormat);
    cellStyle.setDataFormat(df);
    return this;
  }
}
