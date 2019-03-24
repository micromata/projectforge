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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestNGBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class UserXmlPreferencesTestFork extends AbstractTestNGBase
{
  @Autowired
  UserXmlPreferencesCache userXmlPreferencesCache;

  @Test
  public void testUserDO()
  {

    PFUserDO user1 = getUser("user1");
    PFUserDO user2 = getUser("user2");
    logon(user1);
    userXmlPreferencesCache.putEntry(user1.getId(), "msg", "Hurzel", true);
    assertEquals("Hurzel", userXmlPreferencesCache.getEntry(user1.getId(), "msg"));
    UserXmlPreferencesMap data = userXmlPreferencesCache.ensureAndGetUserPreferencesData(user1.getId());
    assertEquals(true, data.isModified()); // Because after getting it, it may be modified.
    userXmlPreferencesCache.refresh();
    assertEquals(false, data.isModified());
    userXmlPreferencesCache.putEntry(user1.getId(), "value", 42, true);
    assertEquals(true, data.isModified());
    userXmlPreferencesCache.refresh();
    assertEquals(false, data.isModified());
    userXmlPreferencesCache.putEntry(user1.getId(), "application", "ProjectForge", false);
    assertEquals(false, data.isModified());
    assertEquals("ProjectForge", userXmlPreferencesCache.getEntry(user1.getId(), "application"));
    try {
      userXmlPreferencesCache.putEntry(user2.getId(), "msg", "Hurzel2", true);
      fail("User 1 should not have access to entry of user 2");
    } catch (AccessException ex) {
      // OK
    }
    logon(TEST_ADMIN_USER);
    userXmlPreferencesCache.putEntry(user2.getId(), "msg", "Hurzel2", true);
    assertEquals("Hurzel", userXmlPreferencesCache.getEntry(user1.getId(), "msg"));
    logon(user2);
    assertEquals("ProjectForge", userXmlPreferencesCache.getEntry(user1.getId(), "application"));
  }
}
