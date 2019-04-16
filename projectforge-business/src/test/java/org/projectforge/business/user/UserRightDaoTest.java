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

package org.projectforge.business.user;

import org.junit.jupiter.api.Test;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class UserRightDaoTest extends AbstractTestBase {
  @Autowired
  private GroupDao groupDao;

  @Autowired
  private UserRightDao userRightDao;

  @Test
  public void testUpdateUserRights() {
    logon(AbstractTestBase.ADMIN);
    final PFUserDO user = initTestDB.addUser("testUserRightDaoTest");
    final Set<GroupDO> groupsToAssign = new HashSet<GroupDO>();
    groupsToAssign.add(getGroup(AbstractTestBase.FINANCE_GROUP));
    groupDao.assignGroups(user, groupsToAssign, null);
    List<UserRightVO> list = userRightDao.getUserRights(user);
    UserRightVO right1 = null;
    UserRightVO right2 = null;
    for (final UserRightVO item : list) {
      if (item.getRight().getId() == UserRightId.FIBU_AUSGANGSRECHNUNGEN) {
        right1 = item;
      } else if (item.getRight().getId() == UserRightId.FIBU_EINGANGSRECHNUNGEN) {
        right2 = item;
      }
    }
    assertNotNull(right1, "right not found!");
    assertNotNull(right2, "right not found!");
    assertNull(right1.getValue());
    assertNull(right2.getValue());
    right1.setValue(UserRightValue.READWRITE);
    right2.setValue(UserRightValue.READONLY);
    userRightDao.updateUserRights(user, list);
    list = userRightDao.getUserRights(user);
    right1 = right2 = null;
    for (final UserRightVO item : list) {
      if (item.getRight().getId() == UserRightId.FIBU_AUSGANGSRECHNUNGEN) {
        right1 = item;
      } else if (item.getRight().getId() == UserRightId.FIBU_EINGANGSRECHNUNGEN) {
        right2 = item;
      }
    }
    assertNotNull(right1, "right not found!");
    assertNotNull(right2, "right not found!");
    assertEquals(UserRightValue.READWRITE, right1.getValue());
    assertEquals(UserRightValue.READONLY, right2.getValue());
    right1.setValue(UserRightValue.READONLY);
    right2.setValue(null);
    userRightDao.updateUserRights(user, list);
    list = userRightDao.getUserRights(user);
    right1 = right2 = null;
    for (final UserRightVO item : list) {
      if (item.getRight().getId() == UserRightId.FIBU_AUSGANGSRECHNUNGEN) {
        right1 = item;
      } else if (item.getRight().getId() == UserRightId.FIBU_EINGANGSRECHNUNGEN) {
        right2 = item;
      }
    }
    assertNotNull(right1, "right not found!");
    assertNotNull(right2, "right not found!");
    assertEquals(UserRightValue.READONLY, right1.getValue());
    assertNull(right2.getValue());
  }
}
