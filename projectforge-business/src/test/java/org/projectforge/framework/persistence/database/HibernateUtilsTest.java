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

package org.projectforge.framework.persistence.database;

import org.junit.jupiter.api.Test;
import org.projectforge.business.task.TaskDO;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.test.AbstractTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class HibernateUtilsTest extends AbstractTestBase
{
  @Test
  public void propertyLengthTest()
  {
    Integer length = HibernateUtils.getPropertyLength(TaskDO.class.getName(), "title");
    assertEquals(new Integer(40), length);
    length = HibernateUtils.getPropertyLength(TaskDO.class.getName(), "shortDescription");
    assertEquals(new Integer(255), length);
    length = HibernateUtils.getPropertyLength(TaskDO.class, "shortDescription");
    assertEquals(new Integer(255), length);
    HibernateUtils.enterTestMode();
    length = HibernateUtils.getPropertyLength(TaskDO.class.getName(), "unknown");
    HibernateUtils.exitTestMode();
    assertNull(length);
    length = HibernateUtils.getPropertyLength("unknown", "name");
    assertNull(length);
  }

}
