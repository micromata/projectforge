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

import org.joda.time.DateMidnight;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.test.AbstractTestBase;
import org.testng.annotations.Test;

public class JodaDateMidnightConverterTest extends AbstractTestBase
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
    ThreadLocalUserContext.setUser(getUserGroupCache(), user);
    final JodaDateMidnightConverter converter = new JodaDateMidnightConverter();
    final DateMidnight dateMidnight = (DateMidnight) converter.parse("1970-11-21");
    assertEquals(1970, dateMidnight.getYear());
    assertEquals(11, dateMidnight.getMonthOfYear());
    assertEquals(21, dateMidnight.getDayOfMonth());
    assertEquals("1970-11-21", converter.toString(dateMidnight));
  }
}
