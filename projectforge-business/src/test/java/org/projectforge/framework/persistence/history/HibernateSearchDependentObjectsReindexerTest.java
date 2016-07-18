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

package org.projectforge.framework.persistence.history;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;

import org.projectforge.business.task.TaskDO;
import org.projectforge.framework.persistence.history.HibernateSearchDependentObjectsReindexer;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.UserPrefDO;
import org.testng.annotations.Test;

public class HibernateSearchDependentObjectsReindexerTest
{
  @Test
  public void register()
  {
    final HibernateSearchDependentObjectsReindexer reindexer = new HibernateSearchDependentObjectsReindexer();
    reindexer.map.clear(); // Only if the Registry isn't empty from any previous test run.
    reindexer.register(TaskDO.class);
    final List<HibernateSearchDependentObjectsReindexer.Entry> list = reindexer.map.get(PFUserDO.class);
    assertEquals(1, reindexer.map.size());
    assertEquals(1, list.size());
    assertEntry(list.get(0), TaskDO.class, "responsibleUser");
    reindexer.register(GroupDO.class);
    assertEquals(1, reindexer.map.size());
    assertEquals(2, list.size());
    assertEntry(list.get(1), GroupDO.class, "assignedUsers");
    reindexer.register(UserPrefDO.class);
    assertEquals(1, reindexer.map.size());
    assertEquals(3, list.size());
    assertEntry(list.get(2), UserPrefDO.class, "user");
  }

  private void assertEntry(final HibernateSearchDependentObjectsReindexer.Entry entry, final Class<?> clazz,
      final String fieldName)
  {
    assertEquals(fieldName, entry.fieldName);
    assertEquals(clazz, entry.clazz);
  }
}
