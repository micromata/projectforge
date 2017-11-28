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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * Convert a given Excel-Sheet into an object-Array.
 *
 * @param <T> baseclass for each row
 * @author Wolfgang Jung (w.jung@micromata.de)
 */
public class ExcelImport<T>
{
  /**
   * The logger
   */
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ExcelImport.class);

  /**
   * the workbook containing the values.
   */
  private final HSSFWorkbook work;

  /**
   * a optional map for mapping column-names to property-names.
   */
  private Map<String, String> columnToPropertyMap;

  /**
   * the worksheet from which the values should be read.
   */
  private int activeSheet = 0;

  /**
   * the zero-based index with the column-names
   */
  private int columnNameRow = 0;

  /**
   * the zero-based index of the first row containing values
   */
  private int startAtRow = 1;

  /**
   * the classfactory for creating the objects.
   */
  private ClassFactory<T> clazzFactory;

  /**
   * Opens a given Excel-document. The stream is always closed.
   *
   * @param xlsStream the stream of the Excel-document.
   * @throws IOException if the document is not readable
   */
  public ExcelImport(final InputStream xlsStream) throws IOException
  {
    // Closed by HSSFWorkbook
    work = new HSSFWorkbook(xlsStream);
  }

  /**
   * get a reference to the workbook for special features.
   *
   * @return the previously loaded document
   */
  public HSSFWorkbook getWorkbook()
  {
    return work;
  }

  /**
   * Set the optional Map from column-names to property-names.
   *
   * @param columnToPropertyMap a Map from column-names (String) to property-names (String)
   */
  public void setColumnMapping(final Map<String, String> columnToPropertyMap)
  {
    this.columnToPropertyMap = columnToPropertyMap;
  }

  /**
   * set the sheet, from which the objects should be created.
   *
   * @param sheet the zero-based index
   */
  public void setActiveSheet(final int sheet)
  {
    activeSheet = sheet;
  }

  /**
   * set the sheet, from which the objects should be created.
   *
   * @param sheetName the name of the sheet
   */
  public void setActiveSheet(final String sheetName)
  {
    activeSheet = work.getSheetIndex(sheetName);
  }

  /**
   * set the row-index of the row containing the column-names.
   *
   * @param columnNameRow the zero-based row index
   */
  public void setNameRowIndex(final int columnNameRow)
  {
    this.columnNameRow = columnNameRow;
  }

  /**
   * set the row-index of the row containing the first object values.
   *
   * @param startAtRow the zero-based row index
   */
  public void setStartingRowIndex(final int startAtRow)
  {
    this.startAtRow = startAtRow;
  }

  /**
   * set the class which should be used for storing the values.
   *
   * @param clazz the class of the target-objects for the values of the rows.
   */
  public void setRowClass(final Class<T> clazz)
  {
    this.clazzFactory = new SimpleClassFactory<T>(clazz);
  }

  /**
   * set the classFactory for dynamically creation of classes dependent of the row-contents.
   *
   * @param clazzFactory the factory
   */
  public void setRowClassFactory(final ClassFactory<T> clazzFactory)
  {
    this.clazzFactory = clazzFactory;
  }

  /**
   * returns the columnnames found
   *
   * @return list of columnnames
   */
  public List<String> getColumnNames()
  {
    final HSSFSheet sheet = work.getSheetAt(activeSheet);
    final HSSFRow columnNames = sheet.getRow(columnNameRow);
    final List<String> list = new ArrayList<String>();
    for (int column = 0; column < columnNames.getPhysicalNumberOfCells(); column++) {
      if (columnNames.getCell(column) == null) {
        continue;
      }
      final String columnName = columnNames.getCell(column).getStringCellValue();
      if (columnName != null) {
        list.add(columnName.trim());
      }
    }
    return list;
  }

  /**
   * convert the contents of the table into an array.
   *
   * @param clazz the target class
   * @return an array with the object values.
   */
  @SuppressWarnings("unchecked")
  public T[] convertToRows(final Class<T> clazz)
  {
    if (clazzFactory == null) {
      setRowClass(clazz);
    }
    final HSSFSheet sheet = work.getSheetAt(activeSheet);
    final int numberOfRows = sheet.getLastRowNum();
    final List<T> list = new ArrayList<T>(numberOfRows);
    final HSSFRow columnNames = sheet.getRow(columnNameRow);
    for (int i = startAtRow; i <= numberOfRows; i++) {
      try {
        T line;
        line = convertToBean(sheet.getRow(i), columnNames, i + 1);
        if (line == null) {
          continue;
        }
        if (clazz.isInstance(line) == false) {
          throw new IllegalStateException("returned type "
              + line.getClass()
              + " is not assignable to "
              + clazz
              + " in sheet='"
              + sheet.getSheetName()
              + "', row="
              + i);
        }
        list.add(line);
      } catch (final InstantiationException ex) {
        throw new IllegalArgumentException(
            "Can't create bean " + ex.toString() + " in sheet='" + sheet.getSheetName() + "', row=" + i);
      } catch (final IllegalAccessException ex) {
        throw new IllegalArgumentException(
            "Getter is not visible " + ex.toString() + " in sheet='" + sheet.getSheetName() + "', row=" + i);
      } catch (final InvocationTargetException ex) {
        log.error(ex.getMessage(), ex);
        throw new IllegalArgumentException("Getter threw an exception "
            + ex.toString()
            + " in sheet='"
            + sheet.getSheetName()
            + "', row="
            + i);
      } catch (final NoSuchMethodException ex) {
        throw new IllegalArgumentException(
            "Getter is not existant " + ex.toString() + " in sheet='" + sheet.getSheetName() + "', row=" + i);
      }
    }
    return list.toArray((T[]) Array.newInstance(clazz, 0));
  }

  /**
   * convert a single row to an object.
   *
   * @param row         the row containing the values.
   * @param columnNames the row containing the column-names.
   * @param rowNum      the current rownum
   * @return a new created object populated with the values.
   * @throws InstantiationException    if the object creation fails.
   * @throws IllegalAccessException    if the object creation fails or the invoked setter is not public.
   * @throws InvocationTargetException if the object creation fails with an exception or the setter threw an exception.
   * @throws NoSuchMethodException     if the setter for the property name is not existant.
   */
  private T convertToBean(final HSSFRow row, final HSSFRow columnNames, final int rowNum) throws InstantiationException,
      IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    if (row == null) {
      log.debug("created no bean for row#" + rowNum);
      return null;
    }
    final T o = clazzFactory.newInstance(row);
    if (columnNames == null) {
      return null;
    }
    for (int column = 0; column < columnNames.getPhysicalNumberOfCells(); column++) {
      if (columnNames.getCell(column) == null) {
        continue;
      }
      String columnName = columnNames.getCell(column).getStringCellValue();
      if (columnName != null) {
        columnName = columnName.trim();
      }
      String propName = columnName;
      if (columnToPropertyMap != null) {
        final String mapName = columnToPropertyMap.get(columnName);
        if (mapName != null) {
          propName = mapName.trim();
        }
      }
      try {
        final Class<?> destClazz = PropertyUtils.getPropertyType(o, propName);
        if (propName == null || destClazz == null) {
          log.debug("Skipping column " + columnName);
          continue;
        }
        final Object value = toNativeType(row.getCell(column), destClazz);
        log.debug(
            "Setting property=" + propName + " to " + value + " class=" + ClassUtils.getShortClassName(value, "null"));
        PropertyUtils.setProperty(o, propName, value);
      } catch (final ConversionException e) {
        log.warn(e);
        throw new ExcelImportException("Falscher Datentyp beim Excelimport", new Integer(row.getRowNum()), columnName);
      } catch (final Exception e) {
        log.warn(e);
        throw new ExcelImportException("Falscher Datentyp beim Excelimport", new Integer(row.getRowNum()), columnName);
      }
    }
    if (log.isDebugEnabled() == true) {
      log.debug("created bean " + o + " for row#" + rowNum);
    }
    return o;
  }

  /**
   * convert the cell-value to the type in the bean.
   *
   * @param cell      the cell containing an arbitrary value
   * @param destClazz the target class
   * @return a String, Boolean, Date or BigDecimal
   */
  private Object toNativeType(final HSSFCell cell, final Class<?> destClazz)
  {
    if (cell == null) {
      return null;
    }
    switch (cell.getCellType()) {
      case HSSFCell.CELL_TYPE_NUMERIC:
        log.debug("using numeric");
        if (Date.class.isAssignableFrom(destClazz)) {
          return cell.getDateCellValue();
        }
        String strVal = String.valueOf(cell.getNumericCellValue());
        strVal = strVal.replaceAll("\\.0*$", "");
        return ConvertUtils.convert(strVal, destClazz);
      case HSSFCell.CELL_TYPE_BOOLEAN:
        log.debug("using boolean");
        return Boolean.valueOf(cell.getBooleanCellValue());
      case HSSFCell.CELL_TYPE_STRING:
        log.debug("using string");
        strVal = StringUtils.trimToNull(cell.getStringCellValue());
        return ConvertUtils.convert(strVal, destClazz);
      case HSSFCell.CELL_TYPE_BLANK:
        return null;
      case HSSFCell.CELL_TYPE_FORMULA:
        return new Formula(cell.getCellFormula());
      default:
        return StringUtils.trimToNull(cell.getStringCellValue());
    }
  }

}
