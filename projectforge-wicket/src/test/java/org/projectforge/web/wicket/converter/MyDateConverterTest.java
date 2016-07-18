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

package org.projectforge.web.wicket.converter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.fail;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.wicket.util.convert.ConversionException;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.test.AbstractTestBase;
import org.testng.annotations.Test;

public class MyDateConverterTest extends AbstractTestBase
{
  @Test
  public void preProcessInput()
  {
    final int year = Calendar.getInstance(Locale.GERMAN).get(Calendar.YEAR);
    final MyDateConverter conv = new MyDateConverter();
    assertEquals("21.11.1970", conv.preProcessInput("21.11.1970", Locale.GERMAN));
    assertEquals("21.11.1970", conv.preProcessInput(" 21 .   11 . 1970   ", Locale.GERMAN));
    assertEquals("21.11." + year, conv.preProcessInput("21 .11.", Locale.GERMAN));
    assertEquals("21.11." + year, conv.preProcessInput("21.11", Locale.GERMAN));
    assertEquals("21.November 1970", conv.preProcessInput("21. November 1970", Locale.GERMAN));
    assertEquals("21.November 1970", conv.preProcessInput("21  .November  1970", Locale.GERMAN));
    assertEquals("21.November." + year, conv.preProcessInput("21  .November ", Locale.GERMAN)); // Parsing will fail, OK, won't fix.
    assertEquals("11/21/1970", conv.preProcessInput("11 / 21 / 1970", Locale.ENGLISH));
    assertEquals("11/21/" + year, conv.preProcessInput("11/21/", Locale.ENGLISH));
    assertEquals("11/21/" + year, conv.preProcessInput("11/21", Locale.ENGLISH));
  }

  @Test
  public void convertToObject()
  {
    convertToObjectGerman(DateHelper.EUROPE_BERLIN);
    convertToObjectGerman(DateHelper.UTC);
    convertToObjectEnglish(DateHelper.EUROPE_BERLIN);
    convertToObjectEnglish(DateHelper.UTC);
  }

  @Test
  public void convertToObjetErrors()
  {
    final MyDateConverter conv = new MyDateConverter();
    try {
      conv.convertToObject("31.11.09", Locale.GERMAN);
      fail("ConversionException exprected.");
    } catch (final ConversionException ex) {
      // OK
    }
    try {
      conv.convertToObject("31.11.", Locale.GERMAN);
      fail("ConversionException exprected.");
    } catch (final ConversionException ex) {
      // OK
    }
    try {
      conv.convertToObject("11/31/1970", Locale.ENGLISH);
      fail("ConversionException exprected.");
    } catch (final ConversionException ex) {
      // OK
    }
    try {
      conv.convertToObject("11/31/70", Locale.ENGLISH);
      fail("ConversionException exprected.");
    } catch (final ConversionException ex) {
      // OK
    }
    try {
      conv.convertToObject("11/31", Locale.ENGLISH);
      fail("ConversionException exprected.");
    } catch (final ConversionException ex) {
      // OK
    }
  }

  @Test
  public void convertToString()
  {
    final MyDateConverter conv = new MyDateConverter();
    PFUserDO user = new PFUserDO();
    user.setTimeZone(DateHelper.EUROPE_BERLIN);
    user.setLocale(Locale.GERMAN);
    user.setDateFormat("dd.MM.yyyy");
    ThreadLocalUserContext.setUser(userCache, user);
    user = ThreadLocalUserContext.getUser(); // ThreadLocalUserContext made a copy!
    Date testDate = createDate(1970, 10, 21, 0, 0, 0, 0, DateHelper.EUROPE_BERLIN);
    assertEquals("21.11.1970", conv.convertToString(testDate, Locale.GERMAN));
    user.setLocale(Locale.ENGLISH);
    user.setDateFormat("MM/dd/yyyy");
    assertEquals("11/21/1970", conv.convertToString(testDate, Locale.GERMAN));

    user.setLocale(Locale.GERMAN);
    user.setDateFormat("dd.MM.yyyy");
    testDate = createDate(2009, 1, 1, 0, 0, 0, 0, DateHelper.EUROPE_BERLIN);
    assertEquals("01.02.2009", conv.convertToString(testDate, Locale.GERMAN));
    user.setLocale(Locale.ENGLISH);
    user.setDateFormat("MM/dd/yyyy");
    assertEquals("02/01/2009", conv.convertToString(testDate, Locale.GERMAN));
  }

  private void convertToObjectGerman(final TimeZone timeZone)
  {
    final MyDateConverter conv = new MyDateConverter();
    assertNull(conv.convertToObject("", Locale.GERMAN));

    final PFUserDO user = new PFUserDO();
    user.setTimeZone(timeZone);
    user.setDateFormat("dd.MM.yyyy");
    ThreadLocalUserContext.setUser(userCache, user);
    Date testDate = createDate(1970, 9, 21, 0, 0, 0, 0, timeZone);
    Date date = conv.convertToObject("21.10.1970", Locale.GERMAN);
    assertDates(testDate, date);
    try {
      date = conv.convertToObject("21/10/1970", Locale.GERMAN);
      fail("ConversionException exprected.");
    } catch (final ConversionException ex) {
      // OK
    }
    assertDates(testDate, date);
    date = conv.convertToObject("21.10.70", Locale.GERMAN);
    assertDates(testDate, date);

    // date = (Date) conv.convertToObject("21. Okt 1970", Locale.GERMAN);
    // assertDates(testDate, date);
    // date = (Date) conv.convertToObject("21. Oktober 1970", Locale.GERMAN);
    // assertDates(testDate, date);

    date = conv.convertToObject("1970-10-21", Locale.GERMAN);
    assertDates(testDate, date);
    date = conv.convertToObject("1970 - 10 - 21", Locale.GERMAN);
    assertDates(testDate, date);
    try {
      date = conv.convertToObject("1970.10.21", Locale.GERMAN);
      fail("ConversionException exprected.");
    } catch (final ConversionException ex) {
      // OK
    }

    try {
      date = conv.convertToObject(String.valueOf(testDate.getTime()), Locale.GERMAN); // millis not supported.
      fail("ConversionException exprected.");
    } catch (final ConversionException ex) {
      // OK
    }

    testDate = createDate(1970, 9, 1, 0, 0, 0, 0, timeZone);
    date = conv.convertToObject("1.10.1970", Locale.GERMAN);
    assertDates(testDate, date);
    date = conv.convertToObject("01.10.70", Locale.GERMAN);
    assertDates(testDate, date);

    final Calendar cal = Calendar.getInstance(timeZone);
    final int year = cal.get(Calendar.YEAR);

    testDate = createDate(year, 9, 21, 0, 0, 0, 0, timeZone);
    date = conv.convertToObject("21.10.", Locale.GERMAN);
    assertDates(testDate, date);
    date = conv.convertToObject("21.10", Locale.GERMAN);
    assertDates(testDate, date);
    // date = (Date) conv.convertToObject("21. Okt " + year, Locale.GERMAN);
    // assertDates(testDate, date);
    // date = (Date) conv.convertToObject("21. Oktober " + year, Locale.GERMAN);
    // assertDates(testDate, date);
  }

  private void convertToObjectEnglish(final TimeZone timeZone)
  {
    final MyDateConverter conv = new MyDateConverter();
    final PFUserDO user = new PFUserDO();
    user.setTimeZone(timeZone);
    ThreadLocalUserContext.setUser(userCache, user);
    Date testDate = createDate(1970, 9, 21, 0, 0, 0, 0, timeZone);
    Date date = conv.convertToObject("10/21/1970", Locale.ENGLISH);
    assertDates(testDate, date);
    date = conv.convertToObject("10/21/70", Locale.ENGLISH);
    assertDates(testDate, date);
    date = conv.convertToObject("1970-10-21", Locale.ENGLISH);
    assertDates(testDate, date);
    try {
      date = conv.convertToObject(String.valueOf(testDate.getTime()), Locale.ENGLISH); // millis not supported.
      fail("ConversionException exprected.");
    } catch (final ConversionException ex) {
      // OK
    }

    final Calendar cal = Calendar.getInstance(timeZone);
    final int year = cal.get(Calendar.YEAR);

    testDate = createDate(year, 9, 21, 0, 0, 0, 0, timeZone);
    date = conv.convertToObject("10/21", Locale.ENGLISH);
    assertDates(testDate, date);
  }

  private void assertDates(final Date exptected, final Date actual)
  {
    assertEquals(DateHelper.formatAsUTC(exptected), DateHelper.formatAsUTC(actual));
    assertEquals(exptected.getTime(), actual.getTime());
  }

  private Date createDate(final int year, final int month, final int day, final int hour, final int minute,
      final int second,
      final int millisecond, final TimeZone timeZone)
  {
    final Calendar cal = Calendar.getInstance(timeZone);
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, month);
    cal.set(Calendar.DAY_OF_MONTH, day);
    cal.set(Calendar.HOUR_OF_DAY, hour);
    cal.set(Calendar.MINUTE, minute);
    cal.set(Calendar.SECOND, second);
    cal.set(Calendar.MILLISECOND, millisecond);
    return cal.getTime();
  }
}
