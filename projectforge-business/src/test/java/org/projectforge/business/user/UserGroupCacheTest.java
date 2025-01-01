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

package org.projectforge.business.user;

import org.junit.jupiter.api.Test;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.business.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserGroupCacheTest extends AbstractTestBase {
    @Autowired
    private GroupDao groupDao;

    @Autowired
    private UserGroupCache userGroupCache;

    @Test
    public void testUserMemberOfAtLeastOneGroup() {
        logon(AbstractTestBase.TEST_ADMIN_USER);
        GroupDO group1 = new GroupDO();
        group1.setName("testusergroupcache1");
        Set<PFUserDO> assignedUsers = new HashSet<>();
        group1.setAssignedUsers(assignedUsers);
        assignedUsers.add(getUser(AbstractTestBase.TEST_USER));
        Serializable id = groupDao.insert(group1);
        group1 = groupDao.find(id);

        GroupDO group2 = new GroupDO();
        group2.setName("testusergroupcache2");
        assignedUsers = new HashSet<>();
        group2.setAssignedUsers(assignedUsers);
        assignedUsers.add(getUser(AbstractTestBase.TEST_ADMIN_USER));
        id = groupDao.insert(group2);
        group2 = groupDao.find(id);

        assertFalse(userGroupCache.isUserMemberOfAtLeastOneGroup(getUser(AbstractTestBase.TEST_ADMIN_USER).getId()));
        assertFalse(userGroupCache.isUserMemberOfAtLeastOneGroup(getUser(AbstractTestBase.TEST_ADMIN_USER).getId(), group1.getId()));
        assertTrue(userGroupCache.isUserMemberOfAtLeastOneGroup(getUser(AbstractTestBase.TEST_ADMIN_USER).getId(), group2.getId()));
        assertTrue(
                userGroupCache.isUserMemberOfAtLeastOneGroup(getUser(AbstractTestBase.TEST_ADMIN_USER).getId(), group1.getId(), group2.getId()));
        assertTrue(userGroupCache.isUserMemberOfAtLeastOneGroup(getUser(AbstractTestBase.TEST_ADMIN_USER).getId(), null, group1.getId(),
                group2.getId()));
        assertTrue(userGroupCache.isUserMemberOfAtLeastOneGroup(getUser(AbstractTestBase.TEST_ADMIN_USER).getId(), null, group1.getId(),
                null, group2.getId(), null));
        assertTrue(
                userGroupCache.isUserMemberOfAtLeastOneGroup(getUser(AbstractTestBase.TEST_ADMIN_USER).getId(), group2.getId(), group1.getId()));
    }
}
