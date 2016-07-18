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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.projectforge.common.DateFormatType;

public class XlsContentProvider implements ContentProvider
{
  static final int LENGHT_UNIT = 256;

  public static final String FORMAT_CURRENCY = "#,##0.00;[Red]-#,##0.00";

  public static final int LENGTH_BOOLEAN = 5;

  public static final int LENGTH_COMMENT = 30;

  public static final int LENGTH_CURRENCY = 11;

  public static final int LENGTH_DATE = 10;

  public static final int LENGTH_DATETIME = 15;

  public static final int LENGTH_DURATION = 10;

  public static final int LENGTH_EMAIL = 30;

  public static final int LENGTH_EXTRA_LONG = 80;

  public static final int LENGTH_ID = 8;

  public static final int LENGTH_PHONENUMBER = 20;

  public static final int LENGTH_STD = 30;

  public static final int LENGTH_TIMESTAMP = 16;

  public static final int LENGTH_ZIPCODE = 7;

  public static Font FONT_HEADER;

  public static short FONT_HEADER_SIZE = 10;

  public static Font FONT_NORMAL;

  public static Font FONT_NORMAL_BOLD;

  public static Font FONT_WHITE_BOLD;

  static protected Font FONT_RED;

  static protected Font FONT_RED_BOLD;

  protected Map<CellFormat, CellStyle> reusableCellFormats = new HashMap<CellFormat, CellStyle>();

  protected ExportWorkbook workbook;

  private final Map<Object, CellFormat> formatMap = new HashMap<Object, CellFormat>();

  protected final Map<Object, CellFormat> defaultFormatMap = new HashMap<Object, CellFormat>();

  private final Map<Integer, Integer> colWidthMap = new HashMap<Integer, Integer>();

  private boolean autoFormatCells = true;

  private final ExportContext exportContext;

  private List<ExportColumn> columns;

  public XlsContentProvider(final ExportWorkbook workbook)
  {
    this(ExportConfig.getInstance().getDefaultExportContext(), workbook);
  }

  public XlsContentProvider(final ExportContext exportContext, final ExportWorkbook workbook)
  {
    this.exportContext = exportContext;
    this.workbook = workbook;
    createFonts();
    defaultFormatMap.put(Integer.class, new CellFormat("#,##0", CellStyle.ALIGN_RIGHT));
    defaultFormatMap.put(Number.class, new CellFormat("#,###.######", CellStyle.ALIGN_RIGHT));
    defaultFormatMap
    .put(Date.class, new CellFormat(ExcelDateFormats.getExcelFormatString(exportContext, DateFormatType.DATE_TIME_MINUTES)));
    defaultFormatMap.put(java.sql.Date.class, new CellFormat(ExcelDateFormats.getExcelFormatString(exportContext, DateFormatType.DATE)));
    defaultFormatMap.put(java.sql.Timestamp.class,
        new CellFormat(ExcelDateFormats.getExcelFormatString(exportContext, DateFormatType.DATE_TIME_MILLIS)));

  }

  @Override
  public XlsContentProvider updateSheetStyle(final ExportSheet sheet)
  {
    for (final Map.Entry<Integer, Integer> entry : colWidthMap.entrySet()) {
      sheet.setColumnWidth(entry.getKey(), entry.getValue());
    }
    return this;
  }

  /**
   * If true then first row and even/odd rows will be formatted with bordered cells.
   * @param autoFormatCells
   */
  public void setAutoFormatCells(final boolean autoFormatCells)
  {
    this.autoFormatCells = autoFormatCells;
  }

  /**
   * Highlights even and odd rows and sets first column bold if even and odd rows are configured.
   * @see org.projectforge.excel.ContentProvider#updateRowStyle(org.projectforge.excel.ExportRow)
   */
  @Override
  public XlsContentProvider updateRowStyle(final ExportRow row)
  {
    if (autoFormatCells == true) {
      for (final ExportCell cell : row.getCells()) {
        final CellFormat format = cell.ensureAndGetCellFormat();
        format.setFillForegroundColor(HSSFColor.WHITE.index);
        switch (row.getRowNum()) {
          /*
           * case 0: font = FONT_HEADER; break;
           */
          case 0:
            format.setFont(FONT_NORMAL_BOLD);
            // alignment = CellStyle.ALIGN_CENTER;
            break;
          default:
            format.setFont(FONT_NORMAL);
            if (row.getRowNum() % 2 == 0) {
              format.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
            }
            break;
        }
      }
    }
    return this;
  }

  @Override
  public XlsContentProvider updateCellStyle(final ExportCell cell)
  {
    final CellFormat format = cell.ensureAndGetCellFormat();
    CellStyle cellStyle = reusableCellFormats.get(format);
    if (cellStyle == null) {
      cellStyle = workbook.createCellStyle();
      reusableCellFormats.put(format, cellStyle);
      format.copyToCellStyle(cellStyle);
      if (format.getFillForegroundColor() != null) {
        cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
      }
      cellStyle.setBorderBottom((short) 1);
      cellStyle.setBorderLeft((short) 1);
      cellStyle.setBorderRight((short) 1);
      cellStyle.setBorderTop((short) 1);
      cellStyle.setWrapText(true);
      final String dataFormat = format.getDataFormat();
      if (dataFormat != null) {
        final short df = workbook.getDataFormat(format.getDataFormat());
        cellStyle.setDataFormat(df);
      }
    }
    cell.setCellStyle(cellStyle);
    return this;
  }

  @Override
  public XlsContentProvider setValue(final ExportCell cell, final Object value)
  {
    setValue(cell, value, null);
    return this;
  }

  /**
   * Override this method if you need to convert complex data types, e. g. value is a DateHolder object you may return value.getDate().
   * Please note, that only some object types are supported by {@link Cell} and by this implementation.
   * @param value
   * @return null at default.
   */
  public Object getCustomizedValue(final Object value)
  {
    return null;
  }

  /**
   * 
   */
  @Override
  public XlsContentProvider setValue(final ExportCell cell, final Object value, final String property)
  {
    final Cell poiCell = cell.getPoiCell();
    final Object customizedValue = getCustomizedValue(value);
    if (customizedValue != null) {
      if (customizedValue instanceof Calendar) {
        poiCell.setCellValue((Calendar) customizedValue);
      } else if (customizedValue instanceof Date) {
        poiCell.setCellValue((Date) customizedValue);
      } else if (customizedValue instanceof String) {
        poiCell.setCellValue((String) customizedValue);
      } else {
        poiCell.setCellValue(String.valueOf(customizedValue));
      }
    } else if (value instanceof Date) { // Attention: Time zone is not given!
      poiCell.setCellValue((Date) value);
    } else if (value instanceof Calendar) {
      poiCell.setCellValue((Calendar) value);
    } else if (value instanceof Boolean) {
      poiCell.setCellValue(((Boolean) value).booleanValue());
    } else if (value instanceof Number) {
      poiCell.setCellValue(((Number) value).doubleValue());
    } else if (value instanceof Formula) {
      poiCell.setCellFormula(((Formula) value).getExpr());
    } else {
      poiCell.setCellValue(ConvertUtils.convert(value));
    }
    CellFormat cellFormat = getCellFormat(cell, value, property, formatMap);
    if (cellFormat == null) {
      cellFormat = getCellFormat(cell, value, property, defaultFormatMap);
    }
    if (cellFormat == null) {
      cellFormat = new CellFormat();
      cellFormat.setAlignment(CellStyle.ALIGN_LEFT);
      cellFormat.setDataFormat("@");
      cellFormat.setWrapText(true);
    }
    cell.setCellFormat(cellFormat);
    return this;
  }

  /**
   * @param cell
   * @param value
   * @param property
   * @param map
   * @return A clone of a pre-defined cell format if found, otherwise null.
   */
  private CellFormat getCellFormat(final ExportCell cell, final Object value, final String property, final Map<Object, CellFormat> map)
  {
    CellFormat format = null;
    if (property != null) {
      format = map.get(property);
    }
    if (format == null && columns != null) {
      for (final ExportColumn column : columns) {
        if (column.getName().equals(property) == true) {
          format = map.get(column);
          break;
        }
      }
    }
    if (format == null) {
      Class< ? > clazz = value == null ? null : value.getClass();
      while (format == null && clazz != null) {
        format = map.get(clazz);
        clazz = clazz.getSuperclass();
      }
    }
    final CellFormat customizedCellFormat = getCustomizedCellFormat(format, value);
    if (customizedCellFormat != null) {
      format = customizedCellFormat;
    }
    if (format == null) {
      return null;
    }
    return format.clone();
  }

  /**
   * Override this method for creating own cell formats.
   * @param format May-be null if no mapping was found for the given value.
   * @param value
   * @return null at default.
   */
  protected CellFormat getCustomizedCellFormat(final CellFormat format, final Object value)
  {
    return null;
  }

  @Override
  public XlsContentProvider putFormat(final Object obj, final CellFormat cellFormat)
  {
    formatMap.put(obj, cellFormat);
    return this;
  }

  @Override
  public XlsContentProvider putFormat(final Enum< ? > col, final CellFormat cellFormat)
  {
    putFormat(col.name(), cellFormat);
    return this;
  }

  @Override
  public XlsContentProvider putFormat(final Object obj, final String dataFormat)
  {
    formatMap.put(obj, new CellFormat(dataFormat));
    return this;
  }

  @Override
  public XlsContentProvider putFormat(final Enum< ? > col, final String dataFormat)
  {
    putFormat(col.name(), dataFormat);
    return this;
  }

  @Override
  public XlsContentProvider putFormat(final ExportColumn col, final String dataFormat)
  {
    registerColumn(col);
    putFormat((Object) col, dataFormat);
    return this;
  }

  @Override
  public XlsContentProvider putFormat(final String dataFormat, final Enum< ? >... cols)
  {
    for (final Enum< ? > col : cols) {
      putFormat(col, dataFormat);
    }
    return this;
  }

  @Override
  public XlsContentProvider putColWidth(final int colIdx, final int charLength)
  {
    this.colWidthMap.put(colIdx, charLength * LENGHT_UNIT);
    return this;
  }

  @Override
  public XlsContentProvider setColWidths(final int... charLengths)
  {
    for (int colIdx = 0; colIdx < charLengths.length; colIdx++) {
      putColWidth(colIdx, charLengths[colIdx]);
    }
    return this;
  }

  /**
   * @param columns the columns to set
   * @return this for chaining.
   */
  private XlsContentProvider registerColumn(final ExportColumn column)
  {
    if (this.columns == null) {
      this.columns = new LinkedList<ExportColumn>();
    }
    this.columns.add(column);
    return this;
  }

  public ExportContext getExportContext()
  {
    return exportContext;
  }

  public ContentProvider newInstance()
  {
    return new XlsContentProvider(this.exportContext, this.workbook);
  }

  @Override
  public ExportWorkbook getWorkbook()
  {
    return workbook;
  }

  private void createFonts()
  {
    FONT_HEADER = workbook.createFont();
    FONT_HEADER.setFontHeightInPoints(FONT_HEADER_SIZE);
    FONT_HEADER.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);

    FONT_NORMAL_BOLD = workbook.createFont();
    FONT_NORMAL_BOLD.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);

    FONT_WHITE_BOLD = workbook.createFont();
    FONT_WHITE_BOLD.setColor(HSSFColor.WHITE.index);
    FONT_WHITE_BOLD.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);

    FONT_RED = workbook.createFont();
    FONT_RED.setColor(HSSFColor.RED.index);

    FONT_RED_BOLD = workbook.createFont();
    FONT_RED_BOLD.setColor(HSSFColor.RED.index);
    FONT_RED_BOLD.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);

    FONT_NORMAL = workbook.createFont();
  }
}
