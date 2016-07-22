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

package org.projectforge.common;

import static org.testng.AssertJUnit.assertEquals;

import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;

import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.utils.KeyValuePairWriter;
import org.projectforge.test.AbstractTestBase;
import org.testng.annotations.Test;

public class KeyValuePairWriterTest extends AbstractTestBase
{

  private static transient final org.apache.log4j.Logger log = org.apache.log4j.Logger
      .getLogger(KeyValuePairWriterTest.class);

  @Test
  public void testWritekeyValuePairs() throws Exception
  {
    final Date date = createDate(1970, Calendar.NOVEMBER, 21, 14, 17, 57, 742);
    log.info("Created date: " + date.toString());
    final StringWriter stringWriter = new StringWriter();
    final KeyValuePairWriter writer = new KeyValuePairWriter(stringWriter);
    writer.write("a1", "Hallo");
    writer.write("a2", "Hal\"lo");
    writer.write("a3", (String) null);
    writer.write("a4", (Date) null);
    writer.write("a5", date);
    writer.write("a6", 42);
    writer.flush();
    assertEquals("a1=\"Hallo\",a2=\"Hal\"\"lo\",a3=,a4=,a5=\"1970-11-21 13:17:57.742\",a6=42", stringWriter.toString());
  }

  private Date createDate(final int year, final int month, final int day, final int hour, final int minute,
      final int second,
      final int millisecond)
  {
    final DateHolder date = new DateHolder();
    date.setDate(year, month, day, hour, minute, second, millisecond);
    return date.getDate();
  }
}
