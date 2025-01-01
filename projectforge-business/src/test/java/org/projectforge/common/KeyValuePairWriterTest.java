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

package org.projectforge.common;

import org.junit.jupiter.api.Test;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.utils.KeyValuePairWriter;
import org.projectforge.business.test.AbstractTestBase;

import java.io.StringWriter;
import java.time.Month;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KeyValuePairWriterTest extends AbstractTestBase {

  private static transient final org.slf4j.Logger log = org.slf4j.LoggerFactory
          .getLogger(KeyValuePairWriterTest.class);

  @Test
  public void testWritekeyValuePairs() throws Exception {
    final Date date = createDate(1970, Month.NOVEMBER, 21, 13, 17, 57, 742);
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
    assertEquals("a1=\"Hallo\",a2=\"Hal\"\"lo\",a3=,a4=,a5=\"1970-11-21 12:17:57.742\",a6=42", stringWriter.toString(), "Time zone Europe/Berlin expected. UTC-Date is 12:17.");
  }

  private Date createDate(int year, Month month, int day, int hour, int minute, int second, int millisecond) {
    return PFDateTime.withDate(year, month, day, hour, minute, second, millisecond).getUtilDate();
  }
}
