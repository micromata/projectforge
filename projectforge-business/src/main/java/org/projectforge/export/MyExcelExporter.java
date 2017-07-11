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

package org.projectforge.export;

import java.lang.reflect.Field;

import org.projectforge.business.excel.CellFormat;
import org.projectforge.business.excel.ContentProvider;
import org.projectforge.business.excel.ExcelExporter;
import org.projectforge.business.excel.ExportColumn;
import org.projectforge.business.excel.ExportSheet;
import org.projectforge.common.DateFormatType;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.common.props.PropertyType;
import org.projectforge.framework.time.DateFormats;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MyExcelExporter extends ExcelExporter
{
  /**
   * @param filename
   */
  public MyExcelExporter(final String filename)
  {
    super(filename);
  }

  public ExportSheet addSheet(final String sheetTitle)
  {
    final ContentProvider contentProvider = new MyXlsContentProvider(getWorkbook())
    {
      /**
       * @see org.projectforge.export.MyXlsContentProvider#getCustomizedCellFormat(CellFormat, java.lang.Object)
       */
      @Override
      protected CellFormat getCustomizedCellFormat(final CellFormat format, final Object value)
      {
        return null;
      }
    };
    return addSheet(contentProvider, sheetTitle);
  }

  /**
   * Adds customized formats. Put here your customized formats to your ExportSheet.
   *
   * @param field
   * @param propInfo may-be null.
   * @param column
   * @return true, if format is handled by this method, otherwise false.
   */
  @Override
  public void putFieldFormat(final ContentProvider sheetProvider, final Field field, final PropertyInfo propInfo,
      final ExportColumn exportColumn)
  {
    final PropertyType type = propInfo.type();
    if (type == PropertyType.DATE) {
      sheetProvider.putFormat(exportColumn, DateFormats.getExcelFormatString(DateFormatType.DATE));
      exportColumn.setWidth(10);
    } else if (type == PropertyType.DATE_TIME) {
      sheetProvider.putFormat(exportColumn, DateFormats.getExcelFormatString(DateFormatType.DATE_TIME_MINUTES));
      exportColumn.setWidth(10);
    } else if (type == PropertyType.DATE_TIME_SECONDS) {
      sheetProvider.putFormat(exportColumn, DateFormats.getExcelFormatString(DateFormatType.DATE_TIME_SECONDS));
      exportColumn.setWidth(16);
    } else if (type == PropertyType.DATE_TIME_MILLIS) {
      sheetProvider.putFormat(exportColumn, DateFormats.getExcelFormatString(DateFormatType.DATE_TIME_MILLIS));
      exportColumn.setWidth(18);
    } else if (type == PropertyType.UNSPECIFIED) {
      if (java.sql.Date.class.isAssignableFrom(field.getType()) == true) {
        sheetProvider.putFormat(exportColumn, DateFormats.getExcelFormatString(DateFormatType.DATE));
        exportColumn.setWidth(10);
      } else if (java.util.Date.class.isAssignableFrom(field.getType()) == true) {
        sheetProvider.putFormat(exportColumn, DateFormats.getExcelFormatString(DateFormatType.DATE_TIME_MINUTES));
        exportColumn.setWidth(16);
      } else {
        super.putFieldFormat(sheetProvider, field, propInfo, exportColumn);
      }
    } else {
      super.putFieldFormat(sheetProvider, field, propInfo, exportColumn);
    }
  }
}
