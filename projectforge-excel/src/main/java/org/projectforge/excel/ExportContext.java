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

public interface ExportContext
{
  /**
   * The locale to use for translations.
   * @return the locale.
   */
  public Locale getLocale();

  /**
   * You may set the locale manually.
   * @param locale
   */
  public void setLocale(Locale locale);

  /**
   * Implement this method with your i18n mechanism.
   * @param i18nKey
   * @return The translated string with the locale.
   */
  public String getLocalizedString(String i18nKey);

  /**
   * The Excel date format to use for exporting dates and time-stamps.
   * @return
   */
  public String getExcelDateFormat();
}
