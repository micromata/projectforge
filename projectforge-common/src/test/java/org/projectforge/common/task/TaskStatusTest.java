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

package org.projectforge.common.task;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.projectforge.common.task.TaskStatus.*;

public class TaskStatusTest
{
  @Test
  public void testGetTaskStatus()
  {

    Assertions.assertEquals(getTaskStatus("N"), N);
    Assertions.assertEquals(getTaskStatus("O"), O);
    Assertions.assertEquals(getTaskStatus("C"), C);
    try {
      getTaskStatus("");
    } catch (UnsupportedOperationException e) {
      Assertions.assertNotNull(e);
    }
  }

  @Test
  public void testIsIn()
  {
    Assertions.assertTrue(N.isIn(O, C, N));
    Assertions.assertFalse(O.isIn(C, N));
    Assertions.assertFalse(C.isIn(new TaskStatus[] {}));
    try {
      Assertions.assertFalse(C.isIn(null));
    } catch (NullPointerException e) {
      Assertions.assertNotNull(e);
    }
  }

  @Test
  public void testGetKeyAndI18nKey()
  {
    String notOpened = "notOpened";
    Assertions.assertEquals(N.getKey(), notOpened);
    String opened = "opened";
    Assertions.assertEquals(O.getKey(), opened);
    String closed = "closed";
    Assertions.assertEquals(C.getKey(), closed);

    String pre = "task.status.";
    Assertions.assertEquals(N.getI18nKey(), pre + notOpened);
    Assertions.assertEquals(O.getI18nKey(), pre + opened);
    Assertions.assertEquals(C.getI18nKey(), pre + closed);
  }
}
