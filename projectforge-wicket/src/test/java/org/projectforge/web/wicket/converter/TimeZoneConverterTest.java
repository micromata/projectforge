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

package org.projectforge.web.wicket.converter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import java.util.Locale;
import java.util.TimeZone;

import org.testng.annotations.Test;

public class TimeZoneConverterTest
{
  @Test
  public void convertToObject()
  {
    final TimeZoneConverter con = new TimeZoneConverter();
    final Locale locale = Locale.ENGLISH;
    assertNull(con.convertToObject(null, locale));
    assertNull(con.convertToObject("", locale));
    assertEquals("Europe/Berlin", ((TimeZone) con.convertToObject("Europe/Berlin", locale)).getID());
    assertEquals("CET", ((TimeZone) con.convertToObject("CET", locale)).getID());
  }

  @Test
  public void convertToString()
  {
    final TimeZoneConverter con = new TimeZoneConverter();
    final Locale locale = Locale.ENGLISH;
    assertNull(con.convertToString(null, locale));
    assertEquals("Europe/Berlin (Central European Time)",
        con.convertToString(TimeZone.getTimeZone("Europe/Berlin"), locale));
    assertEquals("CET (Central European Time)", con.convertToString(TimeZone.getTimeZone("CET"), locale));
  }
}
