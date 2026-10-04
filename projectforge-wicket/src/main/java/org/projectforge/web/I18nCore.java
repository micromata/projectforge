/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web;

import java.util.Locale;

/**
 * Main class for administration ProjectForge's localization. If you want to add new translations, this class should be referred first.
 *
 * @author Kai Reinhard
 */
public class I18nCore
{

  /**
   * The datepicker locale is used for localization of the DatePicker. If you add a new language please add also the datepicker translation
   * file jquery.ui.datepicker-de.js (replace de by your language short cut).
   *
   * @param locale
   * @return "de" for german otherwise null (default).
   */
  public static String getDatePickerLocale(final Locale locale)
  {
    if (locale == null) {
      return null;
    }
    if (locale.toString().startsWith("de") == true) {
      return "de";
    }
    return null;
  }

  /**
   * @param locale
   * @return null for default locale otherwise translation file of date-picker.
   */
  public static String getDatePickerLocalizationFile(final Locale locale)
  {
    final String loc = getDatePickerLocale(locale);
    if (loc == null) {
      // No translation file needed, default is used:
      return null;
    }
    return "scripts/jqueryui/jquery.ui.datepicker-" + loc + ".js";
  }
}
