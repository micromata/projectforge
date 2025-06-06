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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class MimeTypeTest
{
  @Test
  public void extensionTest()
  {
    assertNull(MimeType.getMimeType(null));
    assertNull(MimeType.getMimeType(""));
    assertNull(MimeType.getMimeType("file"));
    assertNull(MimeType.getMimeType("pdf"));
    assertNull(MimeType.getMimeType("pdf."));
    assertEquals(MimeType.PDF, MimeType.getMimeType(".pdf"));
    assertEquals(MimeType.PDF, MimeType.getMimeType("file.pdf"));
    assertEquals(MimeType.JPG, MimeType.getMimeType("picture.jpg"));
    assertEquals(MimeType.JPG, MimeType.getMimeType("picture.jpeg"));
  }
}
