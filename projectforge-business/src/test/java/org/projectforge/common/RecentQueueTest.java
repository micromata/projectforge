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
import static org.testng.AssertJUnit.assertNull;

import org.projectforge.framework.utils.RecentQueue;
import org.testng.annotations.Test;

public class RecentQueueTest
{
  @Test
  public void test()
  {
    RecentQueue<Integer> queue = new RecentQueue<Integer>(4);
    assertEquals("Length of queue", 0, queue.size());
    assertNull(queue.get(null));
    assertNull(queue.get(5));
    assertNull(queue.get(-1));
    checkQueue(queue);
    queue.append(4);
    checkQueue(queue, 4);
    queue.append(4);
    checkQueue(queue, 4);
    queue.append(3).append(2);
    checkQueue(queue, 2, 3, 4);
    queue.append(4);
    checkQueue(queue, 4, 2, 3);
    queue.append(3).append(2);
    checkQueue(queue, 2, 3, 4);
    queue.append(1);
    checkQueue(queue, 1, 2, 3, 4);
    queue.append(0);
    checkQueue(queue, 0, 1, 2, 3);
    queue.append(3);
    checkQueue(queue, 3, 0, 1, 2);
    assertEquals(new Integer(3), queue.get(null));
    assertNull(queue.get(5));
    assertNull(queue.get(-1));
  }

  private void checkQueue(RecentQueue<Integer> queue, Integer... values)
  {
    int pos = 0;
    assertEquals("Length of queue", values.length, queue.size());
    for (Integer val : values) {
      assertEquals("Value at pos " + pos, val, queue.get(pos++));
    }
  }

}
