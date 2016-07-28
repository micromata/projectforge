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

import org.projectforge.test.AbstractTestBase;
import org.testng.annotations.Test;

public class CSVWriterTest extends AbstractTestBase
{
  @Test
  public void testWriteCSV() throws Exception
  {
    Date date = createDate(1970, Calendar.NOVEMBER, 21, 13, 17, 57, 742);
    StringWriter writer = new StringWriter();
    CSVWriter csv = new CSVWriter(writer);
    csv.write("Hallo");
    csv.write("Hal\"lo");
    csv.write((String) null);
    csv.write((Date) null);
    csv.write(date);
    csv.write(42);
    csv.writeEndOfLine();
    csv.flush();
    assertEquals("\"Hallo\";\"Hal\"\"lo\";;;\"1970-11-21 13:17:57.742\";42\n", writer.toString());
    csv.write("\"");
    csv.writeEndOfLine();
    csv.flush();
    assertEquals("\"Hallo\";\"Hal\"\"lo\";;;\"1970-11-21 13:17:57.742\";42\n\"\"\"\"\n", writer.getBuffer().toString());
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
