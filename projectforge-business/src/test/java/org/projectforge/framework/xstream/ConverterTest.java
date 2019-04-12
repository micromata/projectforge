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

package org.projectforge.framework.xstream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.configuration.ConfigXmlTest;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.xstream.converter.DateConverter;
import org.projectforge.framework.xstream.converter.ISODateConverter;
import org.projectforge.framework.xstream.converter.LocaleConverter;
import org.projectforge.framework.xstream.converter.TimeZoneConverter;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ConverterTest
{

  @BeforeAll
  static void setup() {
    ConfigXmlTest.createTestConfiguration();
  }

  @Test
  public void testIsoDateConverter()
  {
    final DateConverter dateConverter = new DateConverter();
    final ISODateConverter isoDateConverter = new ISODateConverter();
    final PFUserDO cetUser = new PFUserDO();
    cetUser.setTimeZone(DateHelper.EUROPE_BERLIN);
    ThreadLocalUserContext.setUser(null, cetUser); // login CET user.
    DateHolder dh = new DateHolder();
    dh.setDate(2010, Calendar.AUGUST, 29, 23, 8, 17, 123);
    assertEquals("1283116097123", dateConverter.toString(dh.getDate()));
    assertEquals("2010-08-29 23:08:17.123", isoDateConverter.toString(dh.getDate()));
    assertEquals("2010-08-29 23:08:17", isoDateConverter.toString(dh.setMilliSecond(0).getDate()));
    assertEquals("2010-08-29 23:08", isoDateConverter.toString(dh.setSecond(0).getDate()));
    assertEquals("2010-08-29 23:00", isoDateConverter.toString(dh.setMinute(0).getDate()));
    assertEquals("2010-08-29", isoDateConverter.toString(dh.setHourOfDay(0).getDate()));
    final PFUserDO utcUser = new PFUserDO();
    utcUser.setTimeZone(DateHelper.UTC);
    ThreadLocalUserContext.setUser(null, utcUser); // login UTC user.
    dh = new DateHolder(DateHelper.UTC);
    dh.setDate(2010, Calendar.AUGUST, 29, 23, 8, 17, 123);
    assertEquals("2010-08-29 23:08:17.123", isoDateConverter.toString(dh.getDate()));
  }

  @Test
  public void testTimeZone()
  {
    writeReadAndAssert((TimeZone) null, null);
    writeReadAndAssert(DateHelper.UTC, "UTC");
    writeReadAndAssert(DateHelper.EUROPE_BERLIN, "Europe/Berlin");
  }

  private void writeReadAndAssert(final TimeZone timeZone, final String id)
  {
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
  public void testLocale()
  {
    writeReadAndAssert((Locale) null, null);
    writeReadAndAssert(new Locale("DE"), "de");
    writeReadAndAssert(new Locale("DE_de"), "de_de");
  }

  private void writeReadAndAssert(final Locale locale, final String id)
  {
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
