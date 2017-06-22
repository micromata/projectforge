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

import java.util.Locale;

import org.projectforge.business.excel.ContentProvider;
import org.projectforge.business.excel.ExcelDateFormats;
import org.projectforge.business.excel.ExportConfig;
import org.projectforge.business.excel.ExportContext;
import org.projectforge.business.excel.ExportWorkbook;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MyXlsExportContext implements ExportContext
{
  private static boolean initialized = false;

  public MyXlsExportContext()
  {
    if (initialized == false) {
      initialized = true;
      ExportConfig.setInstance(new ExportConfig()
      {
        @Override
        protected ContentProvider createNewContentProvider(final ExportWorkbook workbook)
        {
          return new MyXlsContentProvider(workbook);
        }
      }.setDefaultExportContext(new MyXlsExportContext()));
    }
  }

  private Locale locale;

  /**
   * @return the default locale of the system or the locale set by {@link #setLocale(Locale)}.
   * @see ExportContext#getLocale()
   */
  public Locale getLocale()
  {
    if (this.locale != null) {
      locale = ThreadLocalUserContext.getLocale();
    }
    return locale;
  }

  @Override
  public void setLocale(final Locale locale)
  {
    this.locale = locale;
  }

  /**
   * @return Does not translation: returns the i18nKey itself.
   * @see ExportContext#getLocalizedString(java.lang.String)
   */
  public String getLocalizedString(final String i18nKey)
  {
    return ThreadLocalUserContext.getLocalizedString(i18nKey);
  }

  /**
   * Returns the excel format of the context user if found, otherwise: {@link ExcelDateFormats#EXCEL_DEFAULT_DATE}
   *
   * @see ExportContext#getExcelDateFormat()
   */
  @Override
  public String getExcelDateFormat()
  {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    return user != null ? user.getExcelDateFormat() : ExcelDateFormats.EXCEL_DEFAULT_DATE;
  }
}
