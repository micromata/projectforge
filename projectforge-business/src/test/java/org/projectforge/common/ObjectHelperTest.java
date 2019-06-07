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

package org.projectforge.common;

import org.junit.jupiter.api.Test;
import org.projectforge.framework.utils.ObjectHelper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ObjectHelperTest
{
  @Test
  public void testIsEmpty()
  {
    assertTrue(ObjectHelper.isEmpty((Object[]) null));
    assertTrue(ObjectHelper.isEmpty("", "  ", null, BigDecimal.ZERO));
    assertFalse(ObjectHelper.isEmpty("", " . ", null, BigDecimal.ZERO));
    assertFalse(ObjectHelper.isEmpty("", "  ", null, BigDecimal.ONE));
  }
}
