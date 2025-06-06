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

import org.projectforge.common.i18n.Priority;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.projectforge.common.i18n.Priority.*;

public class PriorityTest
{
  @Test
  public void testKeyAndI18NKey()
  {

    String highest = "highest";
    Assertions.assertEquals(HIGHEST.getKey(), highest);
    Assertions.assertEquals(HIGHEST.getI18nKey(), "priority." + highest);

    String high = "high";
    Assertions.assertEquals(HIGH.getKey(), high);
    Assertions.assertEquals(HIGH.getI18nKey(), "priority." + high);

    String middle = "middle";
    Assertions.assertEquals(MIDDLE.getKey(), middle);
    Assertions.assertEquals(MIDDLE.getI18nKey(), "priority." + middle);

    String low = "low";
    Assertions.assertEquals(LOW.getKey(), low);
    Assertions.assertEquals(LOW.getI18nKey(), "priority." + low);

    String least = "least";
    Assertions.assertEquals(LEAST.getKey(), least);
    Assertions.assertEquals(LEAST.getI18nKey(), "priority." + least);
  }

  @Test
  public void testGetPriority()
  {
    Assertions.assertNull(Priority.getPriority(""));
    Assertions.assertEquals(Priority.getPriority("LEAST"), LEAST);
    Assertions.assertEquals(Priority.getPriority("LOW"), LOW);
    Assertions.assertEquals(Priority.getPriority("MIDDLE"), MIDDLE);
    Assertions.assertEquals(Priority.getPriority("HIGH"), HIGH);
    Assertions.assertEquals(Priority.getPriority("HIGHEST"), HIGHEST);
    try {
      Priority.getPriority("Extraordinary High");
    } catch (UnsupportedOperationException e) {
      Assertions.assertNotNull(e);
    }
  }

}
