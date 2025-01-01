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

package org.projectforge.web.wicket.converter;

import org.apache.wicket.util.convert.ConversionException;
import org.joda.time.DateMidnight;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.business.test.TestSetup;

import java.util.Calendar;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class JodaDateConverterTest
{
  private static PFUserDO contextUser = TestSetup.init();
  private final static DateTimeZone EUROPE_BERLIN = DateTimeZone.forID("Europe/Berlin");

  @Test
  public void convertToObject()
  {
    convertToObjectGerman(EUROPE_BERLIN);
    convertToObjectGerman(DateTimeZone.UTC);
    convertToObjectEnglish(EUROPE_BERLIN);
    convertToObjectEnglish(DateTimeZone.UTC);
  }

  @Test
  public void convertToObjetErrors()
  {
    final JodaDateConverter conv = new JodaDateConverter();
    try {
      conv.convertToObject("31.11.09", Locale.GERMAN);
      fail("ConversionException expected.");
    } catch (final ConversionException ex) {
      // OK
    }
    try {
      conv.convertToObject("31.11.", Locale.GERMAN);
      fail("ConversionException expected.");
    } catch (final ConversionException ex) {
      // OK
    }
    try {
      conv.convertToObject("11/31/1970", Locale.ENGLISH);
      fail("ConversionException expected.");
    } catch (final ConversionException ex) {
      // OK
    }
    try {
      conv.convertToObject("11/31/70", Locale.ENGLISH);
      fail("ConversionException expected.");
    } catch (final ConversionException ex) {
      // OK
    }
    try {
      conv.convertToObject("11/31", Locale.ENGLISH);
      fail("ConversionException expected.");
    } catch (final ConversionException ex) {
      // OK
    }
  }

  @Test
  public void convertToString()
  {
    contextUser.setTimeZone(DateHelper.EUROPE_BERLIN);
    contextUser.setLocale(Locale.GERMAN);
    contextUser.setDateFormat("dd.MM.yyyy");
    JodaDateConverter conv = new JodaDateConverter();
    DateMidnight testDate = createDate(1970, DateTimeConstants.NOVEMBER, 21, EUROPE_BERLIN);
    assertEquals("21.11.1970", conv.convertToString(testDate, Locale.GERMAN));
    contextUser.setLocale(Locale.ENGLISH);
    contextUser.setDateFormat("MM/dd/yyyy");
    conv = new JodaDateConverter();
    assertEquals("11/21/1970", conv.convertToString(testDate, Locale.GERMAN)); // User's locale should be used instead.

    contextUser.setLocale(Locale.GERMAN);
    contextUser.setDateFormat("dd.MM.yyyy");
    conv = new JodaDateConverter();
    testDate = createDate(2009, DateTimeConstants.FEBRUARY, 1, EUROPE_BERLIN);
    assertEquals("01.02.2009", conv.convertToString(testDate, Locale.GERMAN));
    contextUser.setLocale(Locale.ENGLISH);
    contextUser.setDateFormat("MM/dd/yyyy");
    conv = new JodaDateConverter();
    assertEquals("02/01/2009", conv.convertToString(testDate, Locale.GERMAN));
  }

  private void convertToObjectGerman(final DateTimeZone timeZone)
  {
    contextUser.setTimeZoneString(timeZone.getID());
    contextUser.setDateFormat("dd.MM.yyyy");
    final JodaDateConverter conv = new JodaDateConverter();
    assertNull(conv.convertToObject("", Locale.GERMAN));
    DateMidnight testDate = createDate(1970, DateTimeConstants.OCTOBER, 21, timeZone);
    DateMidnight date = conv.convertToObject("21.10.1970", Locale.GERMAN);
    assertDates(testDate, date);
    try {
      date = conv.convertToObject("21/10/1970", Locale.GERMAN);
      fail("ConversionException expected.");
    } catch (final ConversionException ex) {
      // OK
    }
    assertDates(testDate, date);
    date = conv.convertToObject("21.10.70", Locale.GERMAN);
    assertDates(testDate, date);

    date = conv.convertToObject("1970-10-21", Locale.GERMAN);
    assertDates(testDate, date);
    try {
      date = conv.convertToObject("1970.10.21", Locale.GERMAN);
      fail("ConversionException expected.");
    } catch (final ConversionException ex) {
      // OK
    }

    try {
      date = conv.convertToObject(String.valueOf(testDate), Locale.GERMAN); // millis not supported.
      fail("ConversionException expected.");
    } catch (final ConversionException ex) {
      // OK
    }

    testDate = createDate(1970, DateTimeConstants.OCTOBER, 1, timeZone);
    date = conv.convertToObject("1.10.1970", Locale.GERMAN);
    assertDates(testDate, date);
    date = conv.convertToObject("01.10.70", Locale.GERMAN);
    assertDates(testDate, date);

    final Calendar cal = Calendar.getInstance();
    final int year = cal.get(Calendar.YEAR);

    testDate = createDate(year, DateTimeConstants.OCTOBER, 21, timeZone);
    date = conv.convertToObject("21.10.", Locale.GERMAN);
    assertDates(testDate, date);
  }

  private void convertToObjectEnglish(final DateTimeZone timeZone)
  {
    contextUser.setTimeZoneString(timeZone.getID());
    contextUser.setDateFormat("MM/dd/yyyy");
    final JodaDateConverter conv = new JodaDateConverter();
    DateMidnight testDate = createDate(1970, DateTimeConstants.OCTOBER, 21, timeZone);
    DateMidnight date = conv.convertToObject("10/21/1970", Locale.ENGLISH);
    assertDates(testDate, date);
    date = conv.convertToObject("10/21/70", Locale.ENGLISH);
    assertDates(testDate, date);
    date = conv.convertToObject("1970-10-21", Locale.ENGLISH);
    assertDates(testDate, date);
    try {
      date = conv.convertToObject(String.valueOf(testDate), Locale.ENGLISH); // millis not supported.
      fail("ConversionException expected.");
    } catch (final ConversionException ex) {
      // OK
    }

    final Calendar cal = Calendar.getInstance();
    final int year = cal.get(Calendar.YEAR);

    testDate = createDate(year, DateTimeConstants.OCTOBER, 21, timeZone);
    date = conv.convertToObject("10/21", Locale.ENGLISH);
    assertDates(testDate, date);
  }

  private void assertDates(final DateMidnight expected, final DateMidnight actual)
  {
    assertEquals(expected.getYear(), actual.getYear());
    assertEquals(expected.getMonthOfYear(), actual.getMonthOfYear());
    assertEquals(expected.getDayOfMonth(), actual.getDayOfMonth());
    assertEquals(expected.getHourOfDay(), actual.getHourOfDay());
    assertEquals(expected.getMinuteOfHour(), actual.getMinuteOfHour());
    assertEquals(expected.getSecondOfMinute(), actual.getSecondOfMinute());
    assertEquals(expected.getMillisOfSecond(), actual.getMillisOfSecond());
  }

  private DateMidnight createDate(final int year, final int month, final int day, final DateTimeZone timeZone)
  {
    return new DateMidnight(year, month, day, timeZone);
  }
}
