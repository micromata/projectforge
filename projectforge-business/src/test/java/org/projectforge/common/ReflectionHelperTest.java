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
import org.projectforge.framework.utils.ReflectionHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReflectionHelperTest
{
  @Test
  public void checkInstantiation()
  {
    TestBean obj = (TestBean) ReflectionHelper.newInstance(TestBean.class, int.class, 42);
    assertEquals(42, obj.arg0);
    obj = (TestBean) ReflectionHelper.newInstance(TestBean.class, Integer.class, String.class, 42, "Hurzel");
    assertEquals(42, obj.arg0);
    assertEquals("Hurzel", obj.arg1);
  }
}
