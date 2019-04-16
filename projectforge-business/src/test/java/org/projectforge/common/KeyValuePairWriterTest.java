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

import org.junit.jupiter.api.Test;
import org.projectforge.framework.utils.KeyValuePairWriter;
import org.projectforge.test.AbstractTestBase;

import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KeyValuePairWriterTest extends AbstractTestBase
{

  private static transient final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(KeyValuePairWriterTest.class);

  @Test
  public void testWritekeyValuePairs() throws Exception
  {
    final Date date = createDate(1970, Calendar.NOVEMBER, 21, 13, 17, 57, 742);
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

  private Date createDate(int year, int month, int day, int hour, int minute, int second, int millisecond)
  {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.YEAR, year);
    c.set(Calendar.MONTH, month);
    c.set(Calendar.DAY_OF_MONTH, day);
    c.set(Calendar.HOUR_OF_DAY, hour);
    c.set(Calendar.MINUTE, minute);
    c.set(Calendar.SECOND, second);
    c.set(Calendar.MILLISECOND, millisecond);
    return c.getTime();
  }
}
