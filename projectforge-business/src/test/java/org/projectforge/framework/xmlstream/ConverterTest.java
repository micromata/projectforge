/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.xmlstream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.time.PFDay;
import org.projectforge.framework.xmlstream.converter.*;
import org.projectforge.test.TestSetup;

import java.time.LocalDate;
import java.time.Month;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ConverterTest {
  private static PFUserDO contextUser;

  @BeforeAll
  static void setup() {
    contextUser = TestSetup.init();
  }

  @Test
  public void testIsoDateConverter() {
    final DateConverter dateConverter = new DateConverter();
    final ISODateConverter isoDateConverter = new ISODateConverter();
    contextUser.setTimeZone(DateHelper.EUROPE_BERLIN);
    PFDateTime dt = PFDateTime.withDate(2010, Month.AUGUST, 29, 23, 8, 17, 123);
    assertEquals("1283116097123", dateConverter.toString(dt.getUtilDate()));
    assertEquals("2010-08-29 21:08:17.123", dt.getIsoStringMilli(), "2 hours back in UTC time zone.");
    assertEquals("2010-08-29 23:08:17.123", isoDateConverter.toString(dt.getUtilDate()));
    dt = dt.withMilliSecond(0);
    assertEquals("2010-08-29 23:08:17", isoDateConverter.toString(dt.getUtilDate()));
    dt = dt.withSecond(0);
    assertEquals("2010-08-29 23:08", isoDateConverter.toString(dt.getUtilDate()));
    dt = dt.withMinute(0);
    assertEquals("2010-08-29 23:00", isoDateConverter.toString(dt.getUtilDate()));
    dt = dt.withHour(0);
    assertEquals("2010-08-29", isoDateConverter.toString(dt.getUtilDate()));
    contextUser.setTimeZone(DateHelper.UTC);
    dt = PFDateTime.withDate(2010, Month.AUGUST, 29, 23, 8, 17, 123);
    assertEquals("2010-08-29 23:08:17.123", isoDateConverter.toString(dt.getUtilDate()));
    assertEquals("2010-08-29 23:08:17.123", dt.getIsoStringMilli(), "UTC time zone.");
  }

  @Test
  public void testIsoLocalDateConverter() {
    final ISOLocalDateConverter isoDateConverter = new ISOLocalDateConverter();
    LocalDate date = isoDateConverter.fromString("2020-02-07");
    assertEquals("2020-02-07", PFDay.from(date).toString());
    assertEquals("2020-02-07", isoDateConverter.toString(date));
  }

  @Test
  public void testTimeZone() {
    writeReadAndAssert((TimeZone) null, null);
    writeReadAndAssert(DateHelper.UTC, "UTC");
    writeReadAndAssert(DateHelper.EUROPE_BERLIN, "Europe/Berlin");
  }

  private void writeReadAndAssert(final TimeZone timeZone, final String id) {
    final TimeZoneConverter converter = new TimeZoneConverter();
    final String str = converter.toString(timeZone);
    assertEquals(id, str);
    final TimeZone tz = converter.fromString(str);
    if (id == null) {
      assertNull(tz);
    } else {
      assertEquals(id, tz.getID());
    }
  }

  @Test
  public void testLocale() {
    writeReadAndAssert((Locale) null, null);
    writeReadAndAssert(new Locale("DE"), "de");
    writeReadAndAssert(new Locale("DE_de"), "de_de");
  }

  private void writeReadAndAssert(final Locale locale, final String id) {
    final LocaleConverter converter = new LocaleConverter();
    final String str = converter.toString(locale);
    assertEquals(id, str);
    final Locale l = converter.fromString(str);
    if (id == null) {
      assertNull(l);
    } else {
      assertEquals(id, l.toString());
    }
  }

}
