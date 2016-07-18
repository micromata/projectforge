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

import javax.security.auth.login.Configuration;

import org.projectforge.common.DateFormatType;

/**
 * Date formats. All the formats base on the given defaultDateFormat. Default date formats are e. g. "dd.MM.yyyy", "dd.MM.yy", "dd/MM/yyyy",
 * "dd/MM/yy", "MM/dd/yyyy", "MM/dd/yy".
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ExcelDateFormats
{
  public static final String EXCEL_DEFAULT_DATE = "DD/MM/YYYY";

  public static final String EXCEL_ISO_DATE = "YYYY-MM-DD";

  /**
   * Gets the format string for the logged-in user. Uses the date format of the logged in user and if not given, a default format is
   * returned.
   * @param format
   * @see Configuration#getExcelDateFormats()
   * @see PFUserDO#getExcelDateFormat()
   */
  public static String getExcelFormatString(final ExportContext exportContext, final DateFormatType format)
  {
    return getExcelFormatString(exportContext.getExcelDateFormat(), format);
  }

  public static String getExcelFormatString(final String defaultExcelDateFormat, final DateFormatType format)
  {
    switch (format) {
      case DATE:
        return defaultExcelDateFormat;
      case DATE_TIME_MINUTES:
        return defaultExcelDateFormat + " hh:mm";
      case DATE_TIME_SECONDS:
        return defaultExcelDateFormat + " hh:mm:ss";
      case DATE_TIME_MILLIS:
        return defaultExcelDateFormat + " hh:mm:ss.000";
      default:
        return defaultExcelDateFormat + " hh:mm:ss";
    }
  }
}
