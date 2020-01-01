/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.xstream.converter.ISODateConverter;
import org.projectforge.test.TestSetup;

import java.time.Month;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class XmlRegistryTest
{

  @BeforeAll
  static void beforeAll()
  {
    TestSetup.init();
  }

  @Test
  public void testWrite()
  {
    final XmlObjectWriter writer = new XmlObjectWriter();
    TestObject obj = new TestObject();
    PFDateTime dt = PFDateTime.now(ZoneId.of("UTC")).withDate(2010, Month.AUGUST, 29, 23, 8, 17, 123);
    obj.date = dt.getUtilDate();
    assertEquals("<test d1=\"0.0\" i1=\"0\" date=\"1283116097123\"/>", writer.writeToXml(obj));
    final XmlRegistry reg = new XmlRegistry();
    reg.registerConverter(Date.class, new ISODateConverter());
    writer.setXmlRegistry(reg);
    obj.date = dt.getUtilDate();
    assertEquals("<test d1=\"0.0\" i1=\"0\" date=\"2010-08-29 23:08:17.123\"/>", writer.writeToXml(obj));
    dt = dt.withMilliSecond(0);
    obj.date = dt.getUtilDate();
    assertEquals("<test d1=\"0.0\" i1=\"0\" date=\"2010-08-29 23:08:17\"/>", writer.writeToXml(obj));
    dt = dt.withSecond(0);
    obj.date = dt.getUtilDate();
    assertEquals("<test d1=\"0.0\" i1=\"0\" date=\"2010-08-29 23:08\"/>", writer.writeToXml(obj));
    dt = dt.withMinute(0);
    obj.date = dt.getUtilDate();
    assertEquals("<test d1=\"0.0\" i1=\"0\" date=\"2010-08-29 23:00\"/>", writer.writeToXml(obj));
    dt = dt.withHour(0);
    obj.date = dt.getUtilDate();
    assertEquals("<test d1=\"0.0\" i1=\"0\" date=\"2010-08-29\"/>", writer.writeToXml(obj));
  }
}
