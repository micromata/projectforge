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

package org.projectforge.business.excel;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.projectforge.common.BeanHelper;

public class ExportRow
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ExportRow.class);

  private final ExportSheet sheet;

  private final Row poiRow;

  private final Map<Integer, ExportCell> cellMap;

  private ContentProvider contentProvider;

  private int rowNum = 0;

  private int maxCol = 0;

  private ExportCell[] cells;

  public ExportRow(final ContentProvider contentProvider, final ExportSheet sheet, final Row poiRow, final int rowNum)
  {
    this.contentProvider = contentProvider;
    this.sheet = sheet;
    this.poiRow = poiRow;
    this.rowNum = rowNum;
    cellMap = new HashMap<Integer, ExportCell>();
    if (poiRow.getLastCellNum() > 0) {
      // poiRow does already exists.
      for (int i = poiRow.getFirstCellNum(); i < poiRow.getLastCellNum(); i++) {
        final Cell poiCell = poiRow.getCell(i);
        if (poiCell != null) {
          addPoiCell(i, poiCell);
        }
      }
    }
  }

  public ExportCell addCell(final int col)
  {
    return addCell(col, null, null);
  }

  public ExportCell addCell(final int col, final Object value)
  {
    return addCell(col, value, null);
  }

  public ExportCell addCell(final int col, final Object value, final String property)
  {
    final Cell poiCell = poiRow.createCell(col);
    final ExportCell cell = addPoiCell(col, poiCell);
    cell.setValue(value, property);
    return cell;
  }

  public ExportCell addPoiCell(final int col, final Cell poiCell)
  {
    if (poiCell == null) {
      throw new UnsupportedOperationException("poiCell should not be null.");
    }
    final ExportCell cell = new ExportCell(contentProvider, poiCell, rowNum, col);
    cellMap.put(col, cell);
    if (col > maxCol) {
      maxCol = col;
      cells = null;
    }
    return cell;
  }

  /**
   * Gets only added cells, if the requested cell does not exist, null will be returned.
   *
   * @param col
   */
  public ExportCell getCell(final int col)
  {
    return getCells()[col];
  }

  public void fillBean(final Object bean, final String[] propertyNames, final int startCol)
  {
    int col = startCol;
    for (final String property : propertyNames) {
      Object value;
      if (bean instanceof Map<?, ?>) {
        value = property == null ? bean : ((Map<?, ?>) bean).get(property);
      } else if (StringUtils.equals(property, ExportSheet.EMPTY)) {
        value = "";
      } else {
        try {
          if (property != null) {
            value = BeanHelper.getNestedProperty(bean, property);
          } else {
            value = null;
          }
        } catch (final RuntimeException ex) {
          log.info("Can't load property " + property + " from bean " + bean + " (" + ex.getMessage() + ")");
          value = "N/A";
        }
      }
      // fill the value
      addCell(col++, value, property);
    }
  }

  /**
   * If the sheet has not its own StyleProvider then the given StyleProvider will be used (if not null).
   *
   * @param contentProvider Can be null.
   */
  public void updateStyles(final ContentProvider contentProvider)
  {
    ContentProvider cp = this.contentProvider;
    if (cp == null) {
      cp = contentProvider;
    }
    if (cp == null) {
      return;
    }
    cp.updateRowStyle(this);
    for (final ExportCell cell : getCells()) {
      if (cell != null) {
        cp.updateCellStyle(cell);
      }
    }
  }

  public ExportCell[] getCells()
  {
    if (cells == null) {
      cells = new ExportCell[maxCol + 1];
      for (int i = 0; i <= maxCol; i++) {
        ExportCell cell = cellMap.get(i);
        if (cell == null) {
          cell = addCell(i);
          cellMap.put(i, cell);
        }
        cells[i] = cell;
      }
    }
    return cells;
  }

  /**
   * @return the maxCol
   */
  public int getMaxCol()
  {
    return maxCol;
  }

  public int getRowNum()
  {
    return rowNum;
  }

  public void setStyleProvider(final ContentProvider contentProvider)
  {
    this.contentProvider = contentProvider;
  }

  public ContentProvider getContentProvider()
  {
    return contentProvider;
  }

  public ExportSheet getSheet()
  {
    return sheet;
  }

  public void setValues(final Object... values)
  {
    setValuesFrom(0, values);
  }

  public void setValuesFrom(int fromCol, final Object... values)
  {
    for (final Object value : values) {
      addCell(fromCol++, value);
    }
  }

  /**
   * Sets the columns of this row beginning with col no 0 with capitalized values.
   *
   * @param values
   * @see #setCapitalizedValuesFrom(int, String...)
   */
  public void setCapitalizedValues(final String... values)
  {
    setCapitalizedValuesFrom(0, values);
  }

  /**
   * Sets the columns of this row beginning with the given col no. Each value will be capitalized first. <br/>
   * Example: setCapitalizedValues("name", "street", "business phone") results in setValues("Name", "Street", "Business
   * phone")
   *
   * @param fromCol
   * @param values
   * @see StringUtils#capitalize(String)
   */
  public void setCapitalizedValuesFrom(int fromCol, final String... values)
  {
    for (final String value : values) {
      addCell(fromCol++, StringUtils.capitalize(value));
    }
  }

  /**
   * Merges cells and sets the value.
   *
   * @param numberOfRows
   * @param colFrom
   * @param colTo
   * @param value
   */
  public ExportCell setMergedRegion(final int numberOfRows, final int firstCol, final int lastCol, final Object value)
  {
    final CellRangeAddress region = new CellRangeAddress(rowNum, rowNum + numberOfRows - 1, firstCol, lastCol);
    sheet.getPoiSheet().addMergedRegion(region);
    final ExportCell cell = addCell(firstCol, value);
    return cell;
  }

  public Row getPoiRow()
  {
    return poiRow;
  }
}
