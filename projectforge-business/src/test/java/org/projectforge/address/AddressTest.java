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

package org.projectforge.address;

import org.junit.jupiter.api.Test;
import org.projectforge.business.address.*;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AddressTest extends AbstractTestBase {
  private final static Logger log = LoggerFactory.getLogger(AddressTest.class);

  @Autowired
  private AddressDao addressDao;

  @Autowired
  private AddressbookDao addressbookDao;

  @Test
  public void testSaveAndUpdate() {
    logon(AbstractTestBase.ADMIN);
    AddressDO a1 = new AddressDO();
    a1.setName("Kai Reinhard");
    addressDao.save(a1);
    log.debug(a1.toString());

    a1.setName("Hurzel");
    addressDao.update(a1);
    assertEquals("Hurzel", a1.getName());

    AddressDO a2 = addressDao.getById(a1.getId());
    assertEquals("Hurzel", a2.getName());
    a2.setName("Micromata GmbH");
    addressDao.update(a2);
    log.debug(a2.toString());

    AddressDO a3 = addressDao.getById(a1.getId());
    assertEquals("Micromata GmbH", a3.getName());
    log.debug(a3.toString());
  }

  @Test
  public void testDeleteAndUndelete() {
    logon(AbstractTestBase.ADMIN);
    AddressDO a1 = new AddressDO();
    a1.setName("Test");
    addressDao.save(a1);

    Integer id = a1.getId();
    a1 = addressDao.getById(id);
    addressDao.markAsDeleted(a1);
    a1 = addressDao.getById(id);
    assertEquals(true, a1.isDeleted(), "Should be marked as deleted.");

    addressDao.undelete(a1);
    a1 = addressDao.getById(id);
    assertEquals(false, a1.isDeleted(), "Should be undeleted.");
  }

  public void testDelete() {
    assertThrows(RuntimeException.class,
            () -> {
              AddressDO a1 = new AddressDO();
              a1.setName("Not deletable");
              addressDao.save(a1);
              Integer id = a1.getId();
              a1 = addressDao.getById(id);
              addressDao.delete(a1);
            });
  }

  @Test
  public void checkStandardAccess() {
    AddressbookDO testAddressbook = new AddressbookDO();
    testAddressbook.setTitle("testAddressbook");
    addressbookDao.internalSave(testAddressbook);
    Set<AddressbookDO> addressbookSet = new HashSet<>();
    addressbookSet.add(testAddressbook);

    AddressDO a1 = new AddressDO();
    a1.setName("testa1");
    addressDao.internalSave(a1);
    AddressDO a2 = new AddressDO();
    a2.setName("testa2");
    addressDao.internalSave(a2);
    AddressDO a3 = new AddressDO();
    a3.setName("testa3");
    addressDao.internalSave(a3);
    AddressDO a4 = new AddressDO();
    a4.setName("testa4");
    a4.setAddressbookList(addressbookSet);
    addressDao.internalSave(a4);
    logon(AbstractTestBase.TEST_USER);

    // Select
    try {
      addressDao.getById(a4.getId());
      fail("User has no access to select");
    } catch (AccessException ex) {
      assertEquals("access.exception.userHasNotRight", ex.getI18nKey());
      assertEquals(UserRightId.MISC_ADDRESSBOOK.getId(), ex.getParams()[0].toString());
      assertEquals("select", ex.getParams()[1].toString());
    }
    AddressDO address = addressDao.getById(a3.getId());
    assertEquals("testa3", address.getName());

    // Select filter
    BaseSearchFilter searchFilter = new BaseSearchFilter();
    searchFilter.setSearchString("testa*");
    QueryFilter filter = new QueryFilter(searchFilter);
    filter.addOrder(SortProperty.asc("name"));
    List<AddressDO> result = addressDao.getList(filter);
    assertEquals(3, result.size(), "Should found 3 address'.");
    HashSet<String> set = new HashSet<>();
    set.add("testa1");
    set.add("testa2");
    set.add("testa3");
    assertTrue(set.remove(result.get(0).getName()), "Hit first entry");
    assertTrue(set.remove(result.get(1).getName()), "Hit second entry");
    assertTrue(set.remove(result.get(2).getName()), "Hit third entry");
    // test_a4 should not be included in result list (no select access)

    // Insert
    address = new AddressDO();
    address.setName("test");
    address.setAddressbookList(addressbookSet);
    try {
      addressDao.save(address);
      fail("User has no access to insert");
    } catch (AccessException ex) {
      assertEquals("access.exception.userHasNotRight", ex.getI18nKey());
      assertEquals(UserRightId.MISC_ADDRESSBOOK.getId(), ex.getParams()[0].toString());
      assertEquals("insert", ex.getParams()[1].toString());
    }
    address.setAddressbookList(null);
    addressDao.save(address);
    assertEquals("test", address.getName());

    // Update
    a4.setName("test_a4test");
    try {
      addressDao.update(a4);
      fail("User has no access to update");
    } catch (AccessException ex) {
      assertEquals("access.exception.userHasNotRight", ex.getI18nKey());
      assertEquals(UserRightId.MISC_ADDRESSBOOK.getId(), ex.getParams()[0].toString());
      assertEquals("update", ex.getParams()[1].toString());
    }
    a2.setName("testa2test");
    addressDao.update(a2);
    address = addressDao.getById(a2.getId());
    assertEquals("testa2test", address.getName());

    // Delete
    try {
      addressDao.delete(a1);
      fail("Address is historizable and should not be allowed to delete.");
    } catch (RuntimeException ex) {
      assertEquals(true, ex.getMessage().startsWith(AddressDao.EXCEPTION_HISTORIZABLE_NOTDELETABLE));
    }
    try {
      addressDao.markAsDeleted(a4);
      fail("User has no access to delete");
    } catch (AccessException ex) {
      assertEquals("access.exception.userHasNotRight", ex.getI18nKey());
      assertEquals(UserRightId.MISC_ADDRESSBOOK.getId(), ex.getParams()[0].toString());
      assertEquals("delete", ex.getParams()[1].toString());
    }
  }

  /**
   * The user shouldn't be able to remove address books from addresses he has no access to.
   */
  @Test
  void preserveAddressbooksTest() {
    logon(TEST_ADMIN_USER);
    PFUserDO testUser = getUser(TEST_USER);
    AddressbookDO addressbookWithUserAccess = new AddressbookDO();
    addressbookWithUserAccess.setTitle("address book with user access");
    addressbookWithUserAccess.setFullAccessUserIds("" + testUser.getId());
    addressbookDao.save(addressbookWithUserAccess);

    AddressbookDO addressbookWithoutUserAccess = new AddressbookDO();
    addressbookWithoutUserAccess.setTitle("address book without user access");
    addressbookDao.save(addressbookWithoutUserAccess);

    Set<AddressbookDO> addressbookSet = new HashSet<>();
    addressbookSet.add(addressbookWithUserAccess);
    addressbookSet.add(addressbookWithoutUserAccess);

    AddressDO address = new AddressDO();
    address.setName("Kai Reinhard");
    address.setAddressbookList(addressbookSet);
    Integer id = addressDao.save(address);

    address = addressDao.getById(id);
    assertEquals(2, address.getAddressbookList().size());

    logon(testUser);
    address = addressDao.getById(id);
    assertEquals(2, address.getAddressbookList().size());
    address.getAddressbookList().clear();
    address.getAddressbookList().add(addressbookWithUserAccess);
    addressDao.update(address);

    address = addressDao.getById(id);
    assertEquals(2, address.getAddressbookList().size(), "Address book without user access should be preserved.");
  }

  @Test
  public void testInstantMessagingField() throws Exception {
    AddressDO address = new AddressDO();
    assertNull(address.getInstantMessaging4DB());
    address.setInstantMessaging(InstantMessagingType.SKYPE, "skype-name");
    assertEquals("SKYPE=skype-name", address.getInstantMessaging4DB());
    address.setInstantMessaging(InstantMessagingType.AIM, "aim-id");
    assertEquals("SKYPE=skype-name\nAIM=aim-id", address.getInstantMessaging4DB());
    address.setInstantMessaging(InstantMessagingType.YAHOO, "yahoo-name");
    assertEquals("SKYPE=skype-name\nAIM=aim-id\nYAHOO=yahoo-name", address.getInstantMessaging4DB());
    address.setInstantMessaging(InstantMessagingType.YAHOO, "");
    assertEquals("SKYPE=skype-name\nAIM=aim-id", address.getInstantMessaging4DB());
    address.setInstantMessaging(InstantMessagingType.SKYPE, "");
    assertEquals("AIM=aim-id", address.getInstantMessaging4DB());
    address.setInstantMessaging(InstantMessagingType.AIM, "");
    assertNull(address.getInstantMessaging4DB());
  }

  // TODO HISTORY
  //  @Test
  //  public void testHistory()
  //  {
  //    PFUserDO user = getUser(AbstractTestBase.ADMIN);
  //    logon(user.getUsername());
  //    AddressDO a1 = new AddressDO();
  //    a1.setName("History test");
  //    a1.setTask(getTask("1.1"));
  //    addressDao.save(a1);
  //    Integer id = a1.getId();
  //    a1.setName("History 2");
  //    addressDao.update(a1);
  //    HistoryEntry[] historyEntries = addressDao.getHistoryEntries(a1);
  //    assertEquals(2, historyEntries.length);
  //    HistoryEntry entry = historyEntries[0];
  //    log.debug(entry);
  //    assertHistoryEntry(entry, id, user, HistoryEntryType.UPDATE, "name", String.class, "History test", "History 2");
  //    entry = historyEntries[1];
  //    log.debug(entry);
  //    assertHistoryEntry(entry, id, user, HistoryEntryType.INSERT, null, null, null, null);
  //
  //    a1.setTask(getTask("1.2"));
  //    addressDao.update(a1);
  //    historyEntries = addressDao.getHistoryEntries(a1);
  //    assertEquals(3, historyEntries.length);
  //    entry = historyEntries[0];
  //    log.debug(entry);
  //    assertHistoryEntry(entry, id, user, HistoryEntryType.UPDATE, "task", TaskDO.class, getTask("1.1").getId(),
  //        getTask("1.2").getId());
  //
  //    a1.setTask(getTask("1.1"));
  //    a1.setName("History test");
  //    addressDao.update(a1);
  //    historyEntries = addressDao.getHistoryEntries(a1);
  //    assertEquals(4, historyEntries.length);
  //    entry = historyEntries[0];
  //    log.debug(entry);
  //    assertHistoryEntry(entry, id, user, HistoryEntryType.UPDATE, null, null, null, null);
  //    List<PropertyDelta> delta = entry.getDelta();
  //    assertEquals(2, delta.size());
  //    for (int i = 0; i < 2; i++) {
  //      PropertyDelta prop = delta.get(0);
  //      if ("name".equals(prop.getPropertyName()) == true) {
  //        assertPropertyDelta(prop, "name", String.class, "History 2", "History test");
  //      } else {
  //        assertPropertyDelta(prop, "task", TaskDO.class, getTask("1.2").getId(), getTask("1.1").getId());
  //      }
  //    }
  //
  //    List<SimpleHistoryEntry> list = addressDao.getSimpleHistoryEntries(a1);
  //    assertEquals(5, list.size());
  //    for (int i = 0; i < 2; i++) {
  //      SimpleHistoryEntry se = list.get(i);
  //      if ("name".equals(se.getPropertyName()) == true) {
  //        assertSimpleHistoryEntry(se, user, HistoryEntryType.UPDATE, "name", String.class, "History 2", "History test");
  //      } else {
  //        assertSimpleHistoryEntry(se, user, HistoryEntryType.UPDATE, "task", TaskDO.class, getTask("1.2").getId(),
  //            getTask("1.1").getId());
  //      }
  //    }
  //    SimpleHistoryEntry se = list.get(2);
  //    assertSimpleHistoryEntry(se, user, HistoryEntryType.UPDATE, "task", TaskDO.class, getTask("1.1").getId(),
  //        getTask("1.2").getId());
  //    se = list.get(3);
  //    assertSimpleHistoryEntry(se, user, HistoryEntryType.UPDATE, "name", String.class, "History test", "History 2");
  //    se = list.get(4);
  //    assertSimpleHistoryEntry(se, user, HistoryEntryType.INSERT, null, null, null, null);
  //
  //    a1 = addressDao.getById(a1.getId());
  //    Date date = a1.getLastUpdate();
  //    String oldName = a1.getName();
  //    a1.setName("Micromata GmbH");
  //    a1.setName(oldName);
  //    addressDao.update(a1);
  //    a1 = addressDao.getById(a1.getId());
  //    list = addressDao.getSimpleHistoryEntries(a1);
  //    assertEquals(5, list.size());
  //    assertEquals(date, a1.getLastUpdate()); // Fails: Fix AbstractBaseDO.copyDeclaredFields: Objects.equals(Boolean, boolean) etc.
  //  }
}
