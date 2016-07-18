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

package org.projectforge.web;

import java.util.Locale;

import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateFormatType;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.TimeNotation;
import org.projectforge.web.calendar.MyFullCalendarConfig;

/**
 * Main class for administration ProjectForge's localization. If you want to add new translations, this class should be referred first.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class I18nCore
{

  /**
   * The datepicker locale is used for localization of the DatePicker. If you add a new language please add also the datepicker translation
   * file jquery.ui.datepicker-de.js (replace de by your language short cut).
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

  /**
   * Sets the date and time formats of the FullCalendar (jquery plugin). It's easier to understand this method if you run ProjectForge and
   * check the calendar page during analyzing this method.
   * @param config
   */
  public static void setFullCalendarDateFormats(final PFUserDO user, final MyFullCalendarConfig config)
  {
    if (TimeNotation.H12.equals(user.getTimeNotation()) == true) {
      config.setAxisFormat("h(:mm)tt");
      config.setTimeFormat("h:mmt{ - h:mmt}");
    } else {
      config.setAxisFormat("HH:mm");
      config.setTimeFormat("HH:mm { - HH:mm}");
    }
    final String usersDateFormat = DateFormats.getFormatString(DateFormatType.DATE);
    final boolean formatMonthFirst = DateFormats.isFormatMonthFirst(usersDateFormat);
    final char dateSeparatorChar = DateFormats.getDateSeparatorChar(usersDateFormat);
    if (DateFormats.isIsoFormat(usersDateFormat) == true) {
      // ISO format: yyyy-MM-dd HH:mm
      config.setTitleFormatDay("dddd, yyyy-MM-dd");
      config.setTitleFormatMonth("MMMM yyyy");
      config.setTitleFormatWeek("yyyy-MM-dd { '&#8212;' yyyy-MM-dd}");
      config.setColumnFormatDay("dddd, MM-dd");
      config.setColumnFormatMonth("ddd");
      config.setColumnFormatWeek("ddd, MM-dd");
    } else if (dateSeparatorChar == '.') {
      // German format: dd.MM.yyyy
      config.setTitleFormatDay("dddd, d. MMMM yyyy");
      config.setTitleFormatMonth("MMMM yyyy");
      config.setTitleFormatWeek("d.[ MMMM] [ yyyy] { '&#8212;' d. MMMM yyyy}");
      config.setColumnFormatDay("dddd, dd.MM.");
      config.setColumnFormatMonth("ddd");
      config.setColumnFormatWeek("ddd, dd.MM.");
    } else if (formatMonthFirst == true) {
      // American format: MM/dd/yyyy
      config.setTitleFormatDay("dddd, MMM d, yyyy");
      config.setTitleFormatMonth("MMMM yyyy");
      config.setTitleFormatWeek("MMM d[ yyyy]{ '&#8212;'[ MMM] d yyyy}");
      config.setColumnFormatDay("dddd M/d");
      config.setColumnFormatMonth("ddd");
      config.setColumnFormatWeek("ddd M/d");
    } else {
      // British format: dd/MM/yyyy
      config.setTitleFormatDay("dddd, d MMM yyyy");
      config.setTitleFormatMonth("MMMM yyyy");
      config.setTitleFormatWeek("d[ MMM][ yyyy]{ '&#8212;' d MMM yyyy}");
      config.setColumnFormatDay("dddd d/M");
      config.setColumnFormatMonth("ddd");
      config.setColumnFormatWeek("ddd d/M");
    }
  }
}
