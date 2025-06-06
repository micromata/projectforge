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

package org.projectforge.business.group.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.GroupsComparator;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GroupsProviderTest
{

  @Test
  public void convertGroupIds()
  {
    final GroupService groupService = new GroupService();
    groupService.setUserGroupCache(Mockito.mock(UserGroupCache.class));
    groupService.setGroupDao(Mockito.mock(GroupDao.class));
    Mockito.when(groupService.getGroup(1L)).thenReturn(cg("1", 1));
    Mockito.when(groupService.getGroup(2L)).thenReturn(cg("2", 2));
    Mockito.when(groupService.getGroup(3L)).thenReturn(cg("3", 3));
    Mockito.when(groupService.getGroup(4L)).thenReturn(cg("4", 4));

    assertNull(groupService.getGroupIds(createGroupsCol()));
    assertEquals("1", groupService.getGroupIds(createGroupsCol(1)));
    assertEquals("1,2", groupService.getGroupIds(createGroupsCol(1, 2)));
    assertEquals("1,2,3", groupService.getGroupIds(createGroupsCol(3, 1, 2)));

    assertGroupSet(groupService.getSortedGroups(""));
    assertGroupSet(groupService.getSortedGroups(" ,, ,"));
    assertGroupSet(groupService.getSortedGroups("1"), 1);
    assertGroupSet(groupService.getSortedGroups("3,1"), 1, 3);
    assertGroupSet(groupService.getSortedGroups("3,1,2,4"), 1, 2, 3, 4);
  }

  /**
   * Creates a group with the given name and id.
   *
   * @param name
   * @param id
   */
  private GroupDO cg(final String name, final long id)
  {
    final GroupDO group = new GroupDO();
    group.setName(name);
    group.setId(id);
    return group;
  }

  private Collection<GroupDO> createGroupsCol(final int... groupIds)
  {
    final Collection<GroupDO> col = new TreeSet<>(new GroupsComparator());
    for (final int id : groupIds) {
      col.add(cg(String.valueOf(id), id));
    }
    return col;
  }

  private void assertGroupSet(final Collection<GroupDO> actualGroupSet, final long... expectedIds)
  {
    if (expectedIds == null || expectedIds.length == 0) {
      assertTrue(CollectionUtils.isEmpty(actualGroupSet));
      return;
    }
    assertEquals(expectedIds.length, actualGroupSet.size());
    final Set<Long> actualIdSet = new HashSet<>();
    for (final GroupDO actualGroup : actualGroupSet) {
      actualIdSet.add(actualGroup.getId());
    }
    for (final long expectedId : expectedIds) {
      assertTrue(actualIdSet.contains(expectedId));
    }
  }
}
