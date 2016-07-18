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

package org.projectforge.framework.calendar;

import java.util.Calendar;

public enum HolidayDefinition
{
  /**
   * New year: 01/01
   */
  NEW_YEAR("calendar.holiday.newYear", Calendar.JANUARY, 1, false),

  /**
   * Xmas eve: 12/24
   */
  XMAS_EVE("calendar.holiday.xmasEve", Calendar.DECEMBER, 24, true),

  /**
   * First Xmas day: 12/25
   */
  FIRST_XMAS_DAY("calendar.holiday.firstXmasDay", Calendar.DECEMBER, 25, false),

  /**
   * Second Xmas day: 12/26
   */
  SECOND_XMAS_DAY("calendar.holiday.secondXmasDay", Calendar.DECEMBER, 26, false),

  /**
   * Sylvester: 12/31.
   */
  SYLVESTER("calendar.holiday.sylvester", Calendar.DECEMBER, 31, true),

  /**
   * Shrove Monday, 48 days before Easter (Rosenmontag)
   */
  SHROVE_MONDAY("calendar.holiday.shroveMonday", -48, true),

  /**
   * Shrove Tuesday, aka Mardi Gras, 47 days before Easter (Fastnachstdienstag)
   */
  SHROVE_TUESDAY("calendar.holiday.shroveTuesday", -47, true),

  /**
   * Ash Wednesday, start of Lent, 46 days before Easter (Aschermittwoch)
   */
  ASH_WEDNESDAY("calendar.holiday.ashWednesday", -46, true),

  /**
   * Palm Sunday, 7 days before Easter
   */
  PALM_SUNDAY("calendar.holiday.palmSunday", -7, false),

  /**
   * Maundy Thursday, 3 days before Easter (Gruendonnerstag)
   */
  MAUNDY_THURSDAY("calendar.holiday.maundyThursday", -3, true),

  /**
   * Good Friday, 2 days before Easter (Karfreitag)
   */
  GOOD_FRIDAY("calendar.holiday.goodFriday", -2, false),

  /**
   * Easter Sunday (Ostersonntag)
   */
  EASTER_SUNDAY("calendar.holiday.easterSunday", 0, false),

  /**
   * Easter Monday, 1 day after Easter (Ostermontag)
   */
  EASTER_MONDAY("calendar.holiday.easterMonday", +1, false),

  /**
   * Ascension, 39 days after Easter (Christi Himmelfahrt)
   */
  ASCENSION("calendar.holiday.ascension", +39, false),

  /**
   * Pentecost (aka Whit Sunday), 49 days after Easter (Pfingsten)
   */
  WHIT_SUNDAY("calendar.holiday.whitSunday", +49, false),

  /**
   * Whit Monday, 50 days after Easter (Pfingstmontag)
   */
  WHIT_MONDAY("calendar.holiday.whitMonday", +50, false),

  /**
   * Corpus Christi, 60 days after Easter (Fronleichnahm)
   */
  CORPUS_CHRISTI("calendar.holiday.corpusChristi", 60, false);

  private String i18nKey;

  private Integer dayOfMonth;

  private Integer month;

  private Integer easterOffset;

  private boolean workingDay;

  /**
   * @param month Calendar.MONTH
   * @param dayOfMonth
   * @param i18nKey
   * @param workingDay
   */
  HolidayDefinition(final String i18nKey, final int month, final int dayOfMonth, final boolean workingDay)
  {
    this.i18nKey = i18nKey;
    this.month = month;
    this.dayOfMonth = dayOfMonth;
    this.workingDay = workingDay;
  }

  /**
   * @param easterOffset
   * @param i18nKey
   * @param workingDay
   */
  HolidayDefinition(final String i18nKey, final int easterOffset, final boolean workingDay)
  {
    this.i18nKey = i18nKey;
    this.workingDay = workingDay;
    this.easterOffset = easterOffset;
  }

  public String getI18nKey()
  {
    return i18nKey;
  }

  /**
   * @return null, if holiday is easter sunday based.
   */
  public Integer getMonth()
  {
    return month;
  }

  /**
   * @return null, if holiday is easter sunday based.
   */
  public Integer getDayOfMonth()
  {
    return dayOfMonth;
  }

  /**
   * @return null, if holiday is not easter sunday based.
   */
  public Integer getEasterOffset()
  {
    return easterOffset;
  }

  public boolean isWorkingDay()
  {
    return workingDay;
  }
}
