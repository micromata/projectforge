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

package org.projectforge.business.address

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.UserRightId
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class AddressTest : AbstractTestBase() {
    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    @Test
    fun testSaveAndUpdate() {
        logon(ADMIN)
        val a1 = AddressDO()
        a1.name = "Kai Reinhard"
        addressDao.insert(a1)
        log.debug(a1.toString())

        a1.name = "Hurzel"
        addressDao.update(a1)
        Assertions.assertEquals("Hurzel", a1.name)

        val a2 = addressDao.find(a1.id)!!
        Assertions.assertEquals("Hurzel", a2.name)
        a2.name = "Micromata GmbH"
        addressDao.update(a2)
        log.debug(a2.toString())

        val a3 = addressDao.find(a1.id)!!
        Assertions.assertEquals("Micromata GmbH", a3.name)
        log.debug(a3.toString())
    }

    @Test
    fun testDeleteAndUndelete() {
        logon(ADMIN)
        var a1 = AddressDO()
        a1.name = "Test"
        addressDao.insert(a1)

        val id = a1.id
        a1 = addressDao.find(id)!!
        addressDao.markAsDeleted(a1)
        a1 = addressDao.find(id)!!
        Assertions.assertEquals(true, a1.deleted, "Should be marked as deleted.")

        addressDao.undelete(a1)
        a1 = addressDao.find(id)!!
        Assertions.assertEquals(false, a1.deleted, "Should be undeleted.")
    }

    fun testDelete() {
        Assertions.assertThrows(
            RuntimeException::class.java
        ) {
            var a1 = AddressDO()
            a1.name = "Not deletable"
            addressDao.insert(a1)
            val id = a1.id
            a1 = addressDao.find(id)!!
            addressDao.delete(a1)
        }
    }

    @Test
    fun checkStandardAccess() {
        val testAddressbook = AddressbookDO()
        testAddressbook.title = "testAddressbook"
        addressbookDao.insert(testAddressbook, checkAccess = false)
        val addressbookSet: MutableSet<AddressbookDO> = HashSet()
        addressbookSet.add(testAddressbook)

        val globalAddressbook = addressbookDao.globalAddressbook
        globalAddressbook.fullAccessUserIds = "" + getUser(TEST_USER).id
        addressbookDao.update(globalAddressbook, checkAccess = false)

        val a1 = AddressDO()
        a1.name = "testa1"
        addressDao.insert(a1, checkAccess = false)
        val a2 = AddressDO()
        a2.name = "testa2"
        addressDao.insert(a2, checkAccess = false)
        val a3 = AddressDO()
        a3.name = "testa3"
        addressDao.insert(a3, checkAccess = false)
        val a4 = AddressDO()
        a4.name = "testa4"
        a4.addressbookList = addressbookSet
        addressDao.insert(a4, checkAccess = false)
        logon(TEST_USER)

        // Select
        try {
            suppressErrorLogs {
                addressDao.find(a4.id)
            }
            Assertions.fail { "User has no access to select" }
        } catch (ex: AccessException) {
            Assertions.assertEquals("access.exception.userHasNotRight", ex.i18nKey)
            Assertions.assertEquals(UserRightId.MISC_ADDRESSBOOK.id, ex.params!![0].toString())
            Assertions.assertEquals("select", ex.params!![1].toString())
        }
        var address = addressDao.find(a3.id)!!
        Assertions.assertEquals("testa3", address.name)

        // Select filter
        val searchFilter = BaseSearchFilter()
        searchFilter.searchString = "testa*"
        val filter = QueryFilter(searchFilter)
        filter.addOrder(asc("name"))
        val result = addressDao.select(filter)
        Assertions.assertEquals(3, result.size, "Should found 3 address'.")
        val set = HashSet<String?>()
        set.add("testa1")
        set.add("testa2")
        set.add("testa3")
        Assertions.assertTrue(set.remove(result[0].name), "Hit first entry")
        Assertions.assertTrue(set.remove(result[1].name), "Hit second entry")
        Assertions.assertTrue(set.remove(result[2].name), "Hit third entry")

        // test_a4 should not be included in result list (no select access)

        // Insert
        address = AddressDO()
        address.name = "test"
        address.addressbookList = addressbookSet
        try {
            suppressErrorLogs {
                addressDao.insert(address)
            }
            Assertions.fail { "User has no access to insert" }
        } catch (ex: AccessException) {
            Assertions.assertEquals("access.exception.userHasNotRight", ex.i18nKey)
            Assertions.assertEquals(UserRightId.MISC_ADDRESSBOOK.id, ex.params!![0].toString())
            Assertions.assertEquals("insert", ex.params!![1].toString())
        }
        address.addressbookList = null
        addressDao.insert(address)
        Assertions.assertEquals("test", address.name)

        // Update
        a4.name = "test_a4test"
        try {
            suppressErrorLogs {
                addressDao.update(a4)
            }
            Assertions.fail { "User has no access to update" }
        } catch (ex: AccessException) {
            Assertions.assertEquals("access.exception.userHasNotRight", ex.i18nKey)
            Assertions.assertEquals(UserRightId.MISC_ADDRESSBOOK.id, ex.params!![0].toString())
            Assertions.assertEquals("update", ex.params!![1].toString())
        }
        a2.name = "testa2test"
        addressDao.update(a2)
        address = addressDao.find(a2.id)!!
        Assertions.assertEquals("testa2test", address.name)

        // Delete
        try {
            suppressErrorLogs {
                addressDao.delete(a1)
            }
            Assertions.fail { "Address is historizable and should not be allowed to delete." }
        } catch (ex: RuntimeException) {
            Assertions.assertEquals(true, ex.message!!.startsWith(BaseDao.EXCEPTION_HISTORIZABLE_NOTDELETABLE))
        }
        try {
            suppressErrorLogs {
                addressDao.markAsDeleted(a4)
            }
            Assertions.fail { "User has no access to delete" }
        } catch (ex: AccessException) {
            Assertions.assertEquals("access.exception.userHasNotRight", ex.i18nKey)
            Assertions.assertEquals(UserRightId.MISC_ADDRESSBOOK.id, ex.params!![0].toString())
            Assertions.assertEquals("delete", ex.params!![1].toString())
        }
    }

    /**
     * The user shouldn't be able to remove address books from addresses he has no access to.
     */
    @Test
    fun preserveAddressbooksTest() {
        logon(TEST_ADMIN_USER)
        val testUser = getUser(TEST_USER)
        val addressbookWithUserAccess = AddressbookDO()
        addressbookWithUserAccess.title = "address book with user access"
        addressbookWithUserAccess.fullAccessUserIds = "" + testUser.id
        addressbookDao.insert(addressbookWithUserAccess)

        val addressbookWithoutUserAccess = AddressbookDO()
        addressbookWithoutUserAccess.title = "address book without user access"
        addressbookDao.insert(addressbookWithoutUserAccess)

        val addressbookSet: MutableSet<AddressbookDO> = HashSet()
        addressbookSet.add(addressbookWithUserAccess)
        addressbookSet.add(addressbookWithoutUserAccess)

        logon(testUser)
        var address = AddressDO()
        address.name = "Kai Reinhard"
        address.addressbookList = addressbookSet
        val id = addressDao.insert(address)

        address = addressDao.find(id)!!
        Assertions.assertEquals(2, address.addressbookList!!.size)

        logon(testUser)
        address = addressDao.find(id)!!
        Assertions.assertEquals(2, address.addressbookList!!.size)
        address.addressbookList!!.clear()
        address.addressbookList!!.add(addressbookWithUserAccess)
        addressDao.update(address)

        address = addressDao.find(id)!!
        Assertions.assertEquals(
            2,
            address.addressbookList!!.size,
            "Address book without user access should be preserved."
        )
    }

    @Test
    @Throws(Exception::class)
    fun testInstantMessagingField() {
        val address = AddressDO()
        Assertions.assertNull(address.instantMessaging4DB)
        address.setInstantMessaging(InstantMessagingType.SKYPE, "skype-name")
        Assertions.assertEquals("SKYPE=skype-name", address.instantMessaging4DB)
        address.setInstantMessaging(InstantMessagingType.AIM, "aim-id")
        Assertions.assertEquals("SKYPE=skype-name\nAIM=aim-id", address.instantMessaging4DB)
        address.setInstantMessaging(InstantMessagingType.YAHOO, "yahoo-name")
        Assertions.assertEquals("SKYPE=skype-name\nAIM=aim-id\nYAHOO=yahoo-name", address.instantMessaging4DB)
        address.setInstantMessaging(InstantMessagingType.YAHOO, "")
        Assertions.assertEquals("SKYPE=skype-name\nAIM=aim-id", address.instantMessaging4DB)
        address.setInstantMessaging(InstantMessagingType.SKYPE, "")
        Assertions.assertEquals("AIM=aim-id", address.instantMessaging4DB)
        address.setInstantMessaging(InstantMessagingType.AIM, "")
        Assertions.assertNull(address.instantMessaging4DB)
    } // TODO HISTORY
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

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AddressTest::class.java)
    }
}
