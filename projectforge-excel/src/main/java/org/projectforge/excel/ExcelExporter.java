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

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import org.projectforge.common.BeanHelper;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.common.props.PropUtils;
import org.projectforge.common.props.PropertyType;

public class ExcelExporter
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ExportWorkbook.class);

  private final ExportWorkbook workBook;

  private int defaultColWidth = 20;

  public ExcelExporter(final String filename)
  {
    this.workBook = new ExportWorkbook();
    this.workBook.setFilename(filename);
  }

  public String getFilename()
  {
    return this.workBook.getFilename();
  }

  public ExportSheet addSheet(final ContentProvider sheetProvider, final String sheetTitle)
  {
    final ExportSheet sheet = workBook.addSheet(sheetTitle);
    // create a default Date format and currency column
    sheet.setContentProvider(sheetProvider);
    return sheet;
  }

  public <T> ExportSheet addList(final ExportSheet sheet, final List<T> list)
  {
    if (list == null || list.size() == 0) {
      // Nothing to export.
      log.info("Nothing to export for sheet '" + sheet.getName() + "'.");
      return sheet;
    }
    final ContentProvider sheetProvider = sheet.getContentProvider();
    sheet.createFreezePane(0, 1);

    final Class<?> classType = list.get(0).getClass();
    final Field[] fields = PropUtils.getPropertyInfoFields(classType);
    List<ExportColumn> cols = new LinkedList<ExportColumn>();
    for (final Field field : fields) {
      final PropertyInfo propInfo = field.getAnnotation(PropertyInfo.class);
      if (propInfo == null) {
        // Shouldn't occur.
        continue;
      }
      final ExportColumn exportColumn = new I18nExportColumn(field.getName(), propInfo.i18nKey(), defaultColWidth);
      cols.add(exportColumn);
      putFieldFormat(sheetProvider, field, propInfo, exportColumn);
    }
    cols = onBeforeSettingColumns(sheetProvider, cols);
    // column property names
    sheet.setColumns(cols);
    final PropertyMapping mapping = new PropertyMapping();
    for (final Object entry : list) {
      for (final Field field : fields) {
        final PropertyInfo propInfo = field.getAnnotation(PropertyInfo.class);
        if (propInfo == null) {
          // Shouldn't occur.
          continue;
        }
        field.setAccessible(true);
        addMapping(mapping, entry, field);
      }
      addMappings(mapping, entry);
      sheet.addRow(mapping.getMapping(), 0);
    }

    return sheet;
  }

  /**
   * You may manipulate the order or content of the columns here. Called by {@link #addList(ExportSheet, List)}.
   * 
   * @param columns Build of the PropertyInfo annotations.
   * @return the given columns.
   */
  protected List<ExportColumn> onBeforeSettingColumns(final ContentProvider sheetProvider,
      final List<ExportColumn> columns)
  {
    return columns;
  }

  /**
   * Re-orders the columns by the given names and all other columns (not specified by names) will be appended in the
   * order of origin list.
   * 
   * @param columns
   * @param names
   * @return A new list with sorted columns.
   */
  protected List<ExportColumn> reorderColumns(final List<ExportColumn> columns, final String... names)
  {
    if (names == null || names.length == 0) {
      return columns;
    }
    final List<ExportColumn> sortedList = new LinkedList<ExportColumn>();
    for (final String name : names) {
      for (final ExportColumn column : columns) {
        if (name.equals(column.getName()) == true) {
          sortedList.add(column);
        }
      }
    }
    for (final ExportColumn column : columns) {
      boolean found = false;
      for (final ExportColumn el : sortedList) {
        if (el == column) {
          found = true;
          break;
        }
      }
      if (found == false) {
        sortedList.add(column);
      }
    }
    return sortedList;
  }

  /**
   * Remove the columns by the given names.
   * 
   * @param columns
   * @param names
   */
  protected List<ExportColumn> removeColumns(final List<ExportColumn> columns, final String... names)
  {
    if (names == null || names.length == 0) {
      return columns;
    }
    for (final String name : names) {
      for (final ExportColumn column : columns) {
        if (name.equals(column.getName()) == true) {
          columns.remove(column);
          break;
        }
      }
    }
    return columns;
  }

  /**
   * Override this for modifying the object field values. Called by {@link #addList(ExportSheet, List)}.
   * 
   * @param mapping
   * @param entry The current entry of the list to add to the Excel sheet.
   * @param field The field of the entry to add.
   */
  public void addMapping(final PropertyMapping mapping, final Object entry, final Field field)
  {
    mapping.add(field.getName(), BeanHelper.getFieldValue(entry, field));
  }

  /**
   * Override this for adding additional mappings. Called by {@link #addList(ExportSheet, List)}.
   * 
   * @param mapping
   * @param entry The current entry of the list to add to the Excel sheet.
   * @param field The field of the entry to add.
   */
  protected void addMappings(final PropertyMapping mapping, final Object entry)
  {
  }

  /**
   * @return the xls
   */
  public ExportWorkbook getWorkbook()
  {
    return workBook;
  }

  /**
   * @param defaultColWidth the defaultColWidth to set
   * @return this for chaining.
   */
  public ExcelExporter setDefaultColWidth(final int defaultColWidth)
  {
    this.defaultColWidth = defaultColWidth;
    return this;
  }

  public void putFieldFormat(final ExportSheet sheet, final Field field, final PropertyInfo propInfo,
      final ExportColumn exportColumn)
  {
    putFieldFormat(sheet.getContentProvider(), field, propInfo, exportColumn);
  }

  /**
   * Adds customized formats. Put here your customized formats to your ExportSheet.
   * 
   * @param field
   * @param propInfo may-be null.
   * @param column
   * @return true, if format is handled by this method, otherwise false.
   */
  public void putFieldFormat(final ContentProvider sheetProvider, final Field field, final PropertyInfo propInfo,
      final ExportColumn exportColumn)
  {
    final PropertyType type = propInfo.type();
    if (type == PropertyType.CURRENCY) {
      putCurrencyFormat(sheetProvider, exportColumn);
    } else if (type == PropertyType.DATE) {
      sheetProvider.putFormat(exportColumn, "MM/dd/yyyy");
    } else if (type == PropertyType.DATE_TIME) {
      sheetProvider.putFormat(exportColumn, "MM/dd/yyyy HH:mm");
    } else if (type == PropertyType.DATE_TIME_SECONDS) {
      sheetProvider.putFormat(exportColumn, "MM/dd/yyyy HH:mm:ss");
    } else if (type == PropertyType.DATE_TIME_MILLIS) {
      sheetProvider.putFormat(exportColumn, "MM/dd/yyyy HH:mm:ss.fff");
    } else if (type == PropertyType.UNSPECIFIED) {
      if (java.sql.Date.class.isAssignableFrom(field.getType()) == true) {
        sheetProvider.putFormat(exportColumn, "MM/dd/yyyy");
      } else if (java.util.Date.class.isAssignableFrom(field.getType()) == true) {
        sheetProvider.putFormat(exportColumn, "MM/dd/yyyy HH:mm");
      } else if (java.lang.Integer.class.isAssignableFrom(field.getType()) == true) {
        exportColumn.setWidth(10);
      } else if (java.lang.Boolean.class.isAssignableFrom(field.getType()) == true) {
        exportColumn.setWidth(10);
      }
    }
  }

  public void putCurrencyFormat(final ContentProvider sheetProvider, final ExportColumn exportColumn)
  {
    sheetProvider.putFormat(exportColumn, "#,##0.00;[Red]-#,##0.00");
    exportColumn.setWidth(12);
  }
}
