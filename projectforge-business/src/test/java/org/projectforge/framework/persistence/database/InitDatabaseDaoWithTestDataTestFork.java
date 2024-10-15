/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.book.BookDO;
import org.projectforge.business.book.BookDao;
import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.fibu.AuftragDao;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.user.UserAuthenticationsService;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserTokenType;
import org.projectforge.framework.access.AccessDao;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.GroupTaskAccessDO;
import org.projectforge.framework.persistence.history.HistoryEntryDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InitDatabaseDaoWithTestDataTestFork extends AbstractTestBase {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
          .getLogger(InitDatabaseDaoWithTestDataTestFork.class);
  // old format.

  @Autowired
  private DatabaseService databaseService;

  @Autowired
  private AccessDao accessDao;

  @Autowired
  private AddressDao addressDao;

  @Autowired
  private AuftragDao auftragDao;

  @Autowired
  private BookDao bookDao;

  @Autowired
  private UserAuthenticationsService userAuthenticationsService;

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private UserGroupCache userGroupCache;

  @Override
  protected void initDb() {
    init(false);
  }

  @Test
  public void initializeEmptyDatabase() {
    final char[] testPassword = "demo123".toCharArray();
    userGroupCache.setExpired(); // Force reload (because it's may be expired due to previous tests).
    assertFalse(databaseService.databaseTablesWithEntriesExist());
    PFUserDO admin = new PFUserDO();
    admin.setUsername("myadmin");
    userService.encryptAndSavePassword(admin, testPassword);
    //pfJpaXmlDumpService.createTestDatabase();
    admin = databaseService.updateAdminUser(admin, null);
    databaseService.afterCreatedTestDb(true);
    final PFUserDO initialAdminUser = userService.authenticateUser("myadmin", testPassword);
    assertNotNull(initialAdminUser);
    assertEquals("myadmin", initialAdminUser.getUsername());
    final Collection<Long> col = userGroupCache.getUserGroups(initialAdminUser);
    assertEquals(6, col.size());
    assertTrue(userGroupCache.isUserMemberOfAdminGroup(initialAdminUser.getId()));
    assertTrue(userGroupCache.isUserMemberOfFinanceGroup(initialAdminUser.getId()));

    final List<PFUserDO> userList = userService.loadAll(false);
    assertTrue(userList.size() > 0);
    for (final PFUserDO user : userList) {
      assertNull("For security reasons the stay-logged-in-key should be null.", userAuthenticationsService.getToken(user.getId(), UserTokenType.STAY_LOGGED_IN_KEY));
    }

    final List<GroupTaskAccessDO> accessList = accessDao.loadAll(false);
    assertTrue(accessList.size() > 0);
    for (final GroupTaskAccessDO access : accessList) {
      assertNotNull(access.getAccessEntries(), "Access entries should be serialized.");
      assertTrue(access.getAccessEntries().size() > 0, "Access entries should be serialized.");
    }

    final List<AddressDO> addressList = addressDao.loadAll(false);
    assertTrue(addressList.size() > 0);

    final List<BookDO> bookList = bookDao.loadAll(false);
    assertTrue(bookList.size() > 2);

    final List<TaskDO> taskList = taskDao.loadAll(false);
    assertTrue(taskList.size() > 10);

    final List<AuftragDO> orderList = auftragDao.loadAll(false);
    AuftragDO order = null;
    for (final AuftragDO ord : orderList) {
      if (ord.getNummer() == 1) {
        order = ord;
        break;
      }
    }
    assertNotNull(order, "Order #1 not found.");
    assertEquals(3, order.getPositionenIncludingDeleted().size(), "Order #1 must have 3 order positions.");

    final List<HistoryEntryDO> list = persistenceService.executeQuery(
            "select t from " + HistoryEntryDO.class.getName() + " t where t.id = :id", HistoryEntryDO.class);
    // assertTrue("At least 10 history entries expected: " + list.size(), list.size() >= 10);

    log.error("****> Next exception and error message are OK (part of the test).");
    boolean exception = false;
    admin.setUsername(DatabaseService.DEFAULT_ADMIN_USER);
    try {
      //pfJpaXmlDumpService.createTestDatabase();
      databaseService.updateAdminUser(admin, null);
      databaseService.afterCreatedTestDb(false);
      fail("AccessException expected.");
    } catch (final AccessException ex) {//|| ConstraintPersistenceException ex) {
      exception = true;
      // Everything fine.
    }
    log.error("Last exception and error messages were OK (part of the test). <****");
    assertTrue(exception);

    log.error("****> Next exception and error message are OK (part of the test).");
    exception = false;
    try {
      //pfJpaXmlDumpService.createTestDatabase();
      databaseService.updateAdminUser(admin, null);
      databaseService.afterCreatedTestDb(true);
      fail("AccessException expected.");
    } catch (AccessException ex) {//|| ConstraintPersistenceException ex) {
      exception = true;
      // Everything fine.
    }
    log.error("Last exception and error messages were OK (part of the test). <****");
    assertTrue(exception);
  }
}
