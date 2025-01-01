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

package org.projectforge.framework.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClassHelperTest
{
  @Test
  public void testGetDefaultType()
  {
    assertEquals(false, ClassHelper.getDefaultType(Boolean.TYPE));
    assertNull(ClassHelper.getDefaultType(Boolean.class));
    assertEquals(0, ClassHelper.getDefaultType(Integer.TYPE));
    assertNull(ClassHelper.getDefaultType(Integer.class));
  }

  @Test
  public void testIsDefaultType()
  {
    assertTrue(ClassHelper.isDefaultType(Boolean.TYPE, null));
    assertTrue(ClassHelper.isDefaultType(Boolean.TYPE, false));
    assertTrue(ClassHelper.isDefaultType(Boolean.TYPE, Boolean.FALSE));
    assertFalse(ClassHelper.isDefaultType(Boolean.TYPE, true));
  }
}
