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

package org.projectforge.framework.xstream.converter;

import static org.testng.AssertJUnit.assertEquals;

import java.util.TimeZone;

import org.joda.time.DateTime;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.test.AbstractTestBase;
import org.testng.annotations.Test;

public class JodaDateTimeConverterTest extends AbstractTestBase
{
  @Test
  public void testConverter()
  {
    test(DateHelper.EUROPE_BERLIN);
    test(DateHelper.UTC);
  }

  private void test(final TimeZone timeZone)
  {
    final PFUserDO user = new PFUserDO();
    user.setTimeZone(timeZone);
    ThreadLocalUserContext.setUser(userCache, user);
    final JodaDateTimeConverter converter = new JodaDateTimeConverter();
    final DateTime dateTime = (DateTime) converter.parse("1970-11-21 16:00:00");
    assertEquals(1970, dateTime.getYear());
    assertEquals(11, dateTime.getMonthOfYear());
    assertEquals(21, dateTime.getDayOfMonth());
    assertEquals("1970-11-21 16:00:00", converter.toString(dateTime));
  }
}
