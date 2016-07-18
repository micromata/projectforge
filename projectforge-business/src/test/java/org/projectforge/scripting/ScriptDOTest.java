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

package org.projectforge.scripting;

import static org.testng.AssertJUnit.assertEquals;

import org.projectforge.business.scripting.ScriptDO;
import org.testng.annotations.Test;

public class ScriptDOTest
{
  @Test
  public void getParameterNames()
  {
    final ScriptDO script = new ScriptDO();
    script.setParameter1Name("p1");
    script.setParameter3Name("test");
    script.setParameter4Name("Test");
    assertEquals("P1, Test, Test", script.getParameterNames(true));
    assertEquals("p1, test, Test", script.getParameterNames(false));
    script.setParameter1Name(null);
    assertEquals("test, Test", script.getParameterNames(false));
  }
}
