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

package org.projectforge.framework.persistence.database;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.util.Collection;

import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class InitDatabaseDaoTestFork extends AbstractTestBase
{
  static final String DEFAULT_ADMIN_PASSWORD = "manage";

  @Autowired
  private DatabaseUpdateService myDatabaseUpdateService;

  @Autowired
  private InitDatabaseDao initDatabaseDao;

  @Autowired
  private PfJpaXmlDumpService pfJpaXmlDumpService;

  @Override
  protected void initDb()
  {
    init(false);
  }

  @Test
  public void initializeEmptyDatabase()
  {
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    TenantRegistryMap.getInstance().setAllUserGroupCachesAsExpired(); // Force reload (because it's may be expired due to previous tests).
    getUserGroupCache().setExpired();
    assertTrue(myDatabaseUpdateService.databaseTablesWithEntriesExists());
    final PFUserDO admin = new PFUserDO();
    admin.setUsername(InitDatabaseDao.DEFAULT_ADMIN_USER);
    admin.setId(1);
    userService.createEncryptedPassword(admin, DEFAULT_ADMIN_PASSWORD);
    ThreadLocalUserContext.setUser(getUserGroupCache(), admin);
    pfJpaXmlDumpService.createTestDatabase();
    initDatabaseDao.updateAdminUser(admin, null);
    initDatabaseDao.afterCreatedTestDb(true);
    final PFUserDO user = userService.authenticateUser(InitDatabaseDao.DEFAULT_ADMIN_USER, DEFAULT_ADMIN_PASSWORD);
    assertNotNull(user);
    assertEquals(InitDatabaseDao.DEFAULT_ADMIN_USER, user.getUsername());
    final Collection<Integer> col = userGroupCache.getUserGroups(user);
    assertEquals(5, col.size());
    assertTrue(userGroupCache.isUserMemberOfAdminGroup(user.getId()));
    assertTrue(userGroupCache.isUserMemberOfFinanceGroup(user.getId()));

    boolean exception = false;
    try {
      initDatabaseDao.initializeDefaultData(admin, null);
      fail("AccessException expected.");
    } catch (final AccessException ex) {
      exception = true;
      // Everything fine.
    }
    assertTrue(exception);
    //clearDatabase();
    // don't know, what to test here
    //    exception = false;
    //    try {
    //      pfJpaXmlDumpService.createTestDatabase();
    //      initDatabaseDao.afterCreatedTestDb(admin, null);
    //      fail("AccessException expected.");
    //    } catch (final AccessException ex) {
    //      exception = true;
    //      // Everything fine.
    //    }
    //    assertTrue(exception);
  }
}
