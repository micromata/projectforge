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


public interface ContentProvider
{
  /**
   * @param sheet
   * @return this for chaining.
   */
  public ContentProvider updateSheetStyle(ExportSheet sheet);

  /**
   * @param row
   * @return this for chaining.
   */
  public ContentProvider updateRowStyle(ExportRow row);

  /**
   * @param cell
   * @return this for chaining.
   */
  public ContentProvider updateCellStyle(ExportCell cell);

  /**
   * @param cell
   * @param value
   * @return this for chaining.
   */
  public ContentProvider setValue(ExportCell cell, Object value);

  /**
   * @param cell
   * @param value
   * @param property
   * @return this for chaining.
   */
  public ContentProvider setValue(ExportCell cell, Object value, String property);

  /**
   * @param obj property name or class of the matching cells.
   * @param format The cell format to use for all matching cells.
   * @return this for chaining.
   */
  public ContentProvider putFormat(Object obj, CellFormat cellFormat);

  /**
   * @param col
   * @param cellFormat
   * @return this for chaining.
   */
  public ContentProvider putFormat(Enum< ? > col, CellFormat cellFormat);

  /**
   * Equivalent to: putFormat(obj, new CellFormat(format))
   * @return this for chaining.
   * @see CellFormat#CellFormat(String)
   */
  public ContentProvider putFormat(Object obj, String dataFormat);

  /**
   * @param col
   * @param dataFormat
   * @return this for chaining.
   */
  public ContentProvider putFormat(Enum< ? > col, String dataFormat);

  /**
   * @param col
   * @param dataFormat
   * @return this for chaining.
   */
  public ContentProvider putFormat(ExportColumn col, String dataFormat);

  /**
   * @param dataFormat
   * @param cols
   * @return this for chaining.
   */
  public ContentProvider putFormat(String dataFormat, Enum< ? >... cols);

  /**
   * @param colIdx
   * @param charLength
   * @return this for chaining.
   */
  public ContentProvider putColWidth(int colIdx, int charLength);

  /**
   * @param charLengths
   * @return this for chaining.
   */
  public ContentProvider setColWidths(int... charLengths);

  /**
   * Creates a new instance. This is usefull because every sheet of the workbook should have its own content provider (regarding col widths,
   * property formats etc.) if not set explicit.
   * @return
   */
  public ContentProvider newInstance();

  public ExportWorkbook getWorkbook();
}
