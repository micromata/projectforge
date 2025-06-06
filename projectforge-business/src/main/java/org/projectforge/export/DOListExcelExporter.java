/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.excel.ContentProvider;
import org.projectforge.business.excel.ExportColumn;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.time.DateHelper;

import java.lang.reflect.Field;
import java.util.Date;

/**
 * MyExcelExporter with minor optimizations e. g. for AbstractListPage.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class DOListExcelExporter extends MyExcelExporter
{
  /**
   * @param filename
   */
  public DOListExcelExporter(final String filenameIdentifier)
  {
    super("ProjectForge-"
        + (filenameIdentifier != null ? filenameIdentifier : "export")
        + "_"
        + DateHelper.getDateAsFilenameSuffix(new Date())
        + ".xls");
  }

  /**
   * @see org.projectforge.export.MyExcelExporter#putFieldFormat(ContentProvider, java.lang.reflect.Field,
   * org.projectforge.common.anots.PropertyInfo, ExportColumn)
   */
  @Override
  public void putFieldFormat(final ContentProvider sheetProvider, final Field field, final PropertyInfo propInfo,
      final ExportColumn exportColumn)
  {
    super.putFieldFormat(sheetProvider, field, propInfo, exportColumn);
    if ("deleted".equals(field.getName())) {
      exportColumn.setWidth(8);
    }
  }

  /**
   * If true then the whole first row will be declared with Excel auto-filter.
   *
   * @return true at default.
   */
  public boolean isExcelAutoFilter()
  {
    return true;
  }

  /**
   * You may add here data or sheets to the Excel file before the download starts. Does nothing at default.
   *
   * @param exporter
   */
  public void onBeforeDownload()
  {
  }
}
