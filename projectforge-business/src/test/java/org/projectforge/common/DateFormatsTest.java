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
import org.projectforge.framework.time.DateFormats;

import static org.junit.jupiter.api.Assertions.*;

public class DateFormatsTest
{
  @Test
  public void isFormatMonthFirst()
  {
    assertTrue(DateFormats.isFormatMonthFirst(null));
    assertTrue(DateFormats.isFormatMonthFirst("odfas"));
    assertTrue(DateFormats.isFormatMonthFirst("MM/dd/yyyy mm:hh"));
    assertFalse(DateFormats.isFormatMonthFirst("dd/MM/yyyy mm:hh"));
    assertFalse(DateFormats.isFormatMonthFirst("dd.MM.yyyy mm:hh"));
  }

  @Test
  public void getDateSeparatorChar()
  {
    assertEquals('/', DateFormats.getDateSeparatorChar(null));
    assertEquals('/', DateFormats.getDateSeparatorChar("dskjfs"));
    assertEquals('/', DateFormats.getDateSeparatorChar("MM/dd/yyyy mm:hh"));
    assertEquals('/', DateFormats.getDateSeparatorChar("dd/MM/yyyy mm:hh"));
    assertEquals('.', DateFormats.getDateSeparatorChar("dd.MM.yyyy mm:hh"));
  }

  @Test
  public void isIsoFormat()
  {
    assertFalse(DateFormats.isIsoFormat(null));
    assertFalse(DateFormats.isIsoFormat("dfsfds"));
    assertFalse(DateFormats.isIsoFormat("MM/dd/yyyy"));
    assertTrue(DateFormats.isIsoFormat("yyyy-MM-dd"));
    assertTrue(DateFormats.isIsoFormat("yyyy-MM-dd mm:hh"));
  }
}
