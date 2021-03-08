/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.time.PFDateTime;

import java.io.StringWriter;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSVWriterTest
{
  @Test
  public void testWriteCSV() {
    Date date = createDate(1970, Month.NOVEMBER, 21, 13, 17, 57, 742);
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

  private Date createDate(int year, Month month, int day, int hour, int minute, int second, int millisecond)
  {
    return PFDateTime.now(ZoneId.of("UTC"), Locale.GERMAN).withYear(year).withMonth(month)
        .withDayOfMonth(day).withHour(hour).withMinute(minute).withSecond(second).withMilliSecond(millisecond).getUtilDate();
  }
}
