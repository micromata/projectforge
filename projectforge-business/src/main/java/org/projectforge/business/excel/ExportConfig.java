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

package org.projectforge.business.excel;

import org.apache.poi.ss.usermodel.PrintSetup;
import org.projectforge.business.configuration.ConfigurationServiceAccessor;

public class ExportConfig
{
  private transient short excelDefaultPaperSizeValue = -42;

  private ExportContext defaultExportContext = new DefaultExportContext();

  private static ExportConfig instance = new ExportConfig();

  public static ExportConfig getInstance()
  {
    return instance;
  }

  public static void setInstance(ExportConfig exportConfig)
  {
    instance = exportConfig;
  }

  /**
   * Override this method for own {@link XlsContentProvider}.
   *
   * @param workbook
   * @return
   */
  protected ContentProvider createNewContentProvider(ExportWorkbook workbook)
  {
    return new XlsContentProvider(getDefaultExportContext(), workbook);
  }

  /**
   * This context is used e. g. by I18nExportColumns to do internationalizations...
   *
   * @return default export context
   */
  public ExportContext getDefaultExportContext()
  {
    return defaultExportContext;
  }

  /**
   * For using own ExportContext, otherwise {@link DefaultExportContext} is used. This context is used e. g. by I18nExportColumns to do
   * internationalizations...
   *
   * @param exportContext
   * @return this for chaining.
   */
  public ExportConfig setDefaultExportContext(ExportContext exportContext)
  {
    this.defaultExportContext = exportContext;
    return this;
  }

  /**
   * Supported values "LETTER", default is "DINA4".
   *
   * @return PrintSetup short value. Default is
   * @see PrintSetup#A4_PAPERSIZE.
   */
  public short getDefaultPaperSizeId()
  {
    if (excelDefaultPaperSizeValue != -42) {
      return excelDefaultPaperSizeValue;
    }
    String excelDefaultPaperSize = ConfigurationServiceAccessor.get().getExcelPaperSize();
    if ("LETTER".equals(excelDefaultPaperSize)) {
      excelDefaultPaperSizeValue = PrintSetup.LETTER_PAPERSIZE;
    } else {
      excelDefaultPaperSizeValue = PrintSetup.A4_PAPERSIZE;
    }
    return excelDefaultPaperSizeValue;
  }

}
