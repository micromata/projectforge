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

import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.utils.FileHelper;
import org.projectforge.test.AbstractTestNGBase;
import org.testng.annotations.Test;

public class FileHelperTest extends AbstractTestNGBase
{
  @Test
  public void createSafeFilename()
  {
    assertEquals("http_www.micromata.de", FileHelper.createSafeFilename("http://www.micromata.de", 100));
    assertEquals("http_www", FileHelper.createSafeFilename("http://www.micromata.de", 8));
    assertEquals("Schroedinger", FileHelper.createSafeFilename("Schrödinger", 100));
    assertEquals("Micromata_is_a_great_software_company.",
        FileHelper.createSafeFilename("Micromata is a great software company.", 100));
    assertEquals("AeOeUeaeoeuess", FileHelper.createSafeFilename("ÄÖÜäöüß", 100));
    assertEquals("AeOeU", FileHelper.createSafeFilename("ÄÖÜäöüß", 5));
    assertEquals("Ae", FileHelper.createSafeFilename("ÄÖÜäöüß", 2));
    assertEquals("AeOe", FileHelper.createSafeFilename("ÄÖÜäöüß", 4));
    assertEquals("Ha", FileHelper.createSafeFilename("Hä", 2));

    final DateHolder dh = new DateHolder();
    assertEquals("basename_"
        + dh.getYear()
        + "-"
        + StringHelper.format2DigitNumber(dh.getMonth() + 1)
        + "-"
        + StringHelper.format2DigitNumber(dh.getDayOfMonth())
        + ".pdf", FileHelper.createSafeFilename("basename", ".pdf", 8, true));
  }
}
