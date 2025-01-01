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

package org.projectforge.business.gantt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GanttTaskImplTest
{
  @Test
  public void getNextId()
  {
    GanttTaskImpl root = new GanttTaskImpl();
    root.addChild(new GanttTaskImpl(5L));
    assertEquals(-1, root.getNextId());
    root.addChild(new GanttTaskImpl(-1L));
    assertEquals(-2, root.getNextId());
    root.addChild(new GanttTaskImpl(-5L));
    assertEquals(-6, root.getNextId());
    root.addChild(new GanttTaskImpl(-6L));
    assertEquals(-7, root.getNextId());
  }
}
