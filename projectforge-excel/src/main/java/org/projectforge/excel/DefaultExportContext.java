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

import java.util.Locale;

/**
 * This default context does nothing special, you may implement your own context.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class DefaultExportContext implements ExportContext
{
  private Locale locale;
  
  /**
   * @return the default locale of the system or the locale set by {@link #setLocale(Locale)}.
   * @see org.projectforge.excel.ExportContext#getLocale()
   */
  public Locale getLocale()
  {
    if (locale != null) {
      return locale;
    }
    return Locale.getDefault();
  }
  
  @Override
  public void setLocale(Locale locale)
  {
    this.locale = locale;
  }

  /**
   * @return Does not translation: returns the i18nKey itself.
   * @see org.projectforge.excel.ExportContext#getLocalizedString(java.lang.String)
   */
  public String getLocalizedString(String i18nKey)
  {
    return i18nKey;
  }
  
  /**
   * Returns {@link ExcelDateFormats#EXCEL_DEFAULT_DATE}
   * @see org.projectforge.excel.ExportContext#getExcelDateFormat()
   */
  @Override
  public String getExcelDateFormat()
  {
    return ExcelDateFormats.EXCEL_DEFAULT_DATE;
  }
}
