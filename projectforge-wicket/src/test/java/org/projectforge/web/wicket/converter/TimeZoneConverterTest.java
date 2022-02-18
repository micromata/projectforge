/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimeZoneConverterTest {
  @Test
  public void convertToObject() {
    final TimeZoneConverter con = new TimeZoneConverter();
    final Locale locale = Locale.ENGLISH;
    assertNull(con.convertToObject(null, locale));
    assertNull(con.convertToObject("", locale));
    assertEquals("Europe/Berlin", ((TimeZone) con.convertToObject("Europe/Berlin", locale)).getID());
    assertEquals("CET", ((TimeZone) con.convertToObject("CET", locale)).getID());
  }

  @Test
  public void convertToString() {
    final TimeZoneConverter con = new TimeZoneConverter();
    final Locale locale = Locale.ENGLISH;
    assertNull(con.convertToString(null, locale));
    // Result might be "Europe/Berlin (Central European Time)" (Java 1.8) or "<Europe/Berlin (Central European Standard Time)" OpenJDK 11.
    assertTrue(con.convertToString(TimeZone.getTimeZone("Europe/Berlin"), locale).startsWith("Europe/Berlin (Central European"));
    assertEquals("CET (Central European Time)", con.convertToString(TimeZone.getTimeZone("CET"), locale));
  }
}
