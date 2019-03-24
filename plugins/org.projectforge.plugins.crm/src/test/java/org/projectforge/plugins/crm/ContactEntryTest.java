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

package org.projectforge.plugins.crm;

import static org.testng.AssertJUnit.assertEquals;

import org.projectforge.test.AbstractTestBase;
import org.projectforge.test.AbstractTestNGBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ContactEntryTest extends AbstractTestNGBase
{
  private final static Logger log = LoggerFactory.getLogger(ContactEntryTest.class);

  @Autowired
  private ContactEntryDao contactEntryDao;
  @Autowired
  private ContactDao contactDao;

  public void setContactEntryDao(final ContactEntryDao contactEntryDao)
  {
    this.contactEntryDao = contactEntryDao;
  }

  public void setContactDao(final ContactDao contactDao)
  {
    this.contactDao = contactDao;
  }

  //  @Test
  public void testSaveAndUpdate()
  {
    logon(AbstractTestBase.ADMIN);

    //final ContactDao contactDao = new ContactDao();
    final ContactDO a1 = new ContactDO();
    a1.setName("Kai Reinhard");
    a1.setTask(getTask("1.1"));
    contactDao.save(a1);
    log.debug(a1.toString());

    final ContactEntryDO ae1 = new ContactEntryDO();
    ae1.setContact(a1);
    ae1.setContactType(ContactType.BUSINESS);
    ae1.setStreet("Marie-Calm-Straße 1-5");
    contactEntryDao.save(ae1);
    log.debug(ae1.toString());

    final ContactEntryDO ae2 = contactEntryDao.getById(ae1.getId());
    assertEquals("Marie-Calm-Straße 1-5", ae2.getStreet());

    ae2.setContactType(ContactType.POSTAL);
    ae2.setStreet("Teststrasse 42");

    contactEntryDao.update(ae2);
    log.debug(ae2.toString());

    final ContactEntryDO ae3 = contactEntryDao.getById(ae2.getId());
    assertEquals("Teststrasse 42", ae3.getStreet());
    assertEquals(ContactType.POSTAL, ae3.getContactType());
    log.debug(ae3.toString());
  }

  //@Test
  public void testDeleteAndUndelete()
  {
    logon(AbstractTestBase.ADMIN);
    final ContactDO a1 = new ContactDO();
    a1.setName("Test");
    a1.setTask(getTask("1.1"));
    contactDao.save(a1);

    ContactEntryDO ae1 = new ContactEntryDO();
    ae1.setContact(a1);
    ae1.setContactType(ContactType.BUSINESS);
    ae1.setStreet("Marie-Calm-Straße 1-5");
    contactEntryDao.save(ae1);
    log.debug(ae1.toString());

    final Integer id = ae1.getId();
    ae1 = contactEntryDao.getById(id);
    contactEntryDao.markAsDeleted(ae1);
    ae1 = contactEntryDao.getById(id);
    assertEquals("Should be marked as deleted.", true, ae1.isDeleted());

    contactEntryDao.undelete(ae1);
    ae1 = contactEntryDao.getById(id);
    assertEquals("Should be undeleted.", false, ae1.isDeleted());
  }

  //  @Test(expected = RuntimeException.class)
  //  public void testDelete()
  //  {
  //    ContactEntryDO a1 = new ContactEntryDO();
  //    a1.setName("Not deletable");
  //    a1.setTask(getTask("1.1"));
  //    contactDao.save(a1);
  //    final Integer id = a1.getId();
  //    a1 = contactDao.getById(id);
  //    contactDao.delete(a1);
  //  }

  //  @Test
  //  public void testHistory()
  //  {
  //    final PFUserDO user = getUser(AbstractTestBase.ADMIN);
  //    logon(user.getUsername());
  //    ContactDO a1 = new ContactDO();
  //    a1.setName("History test");
  //    a1.setTask(getTask("1.1"));
  //    contactDao.save(a1);
  //    final Integer id = a1.getId();
  //    a1.setName("History 2");
  //    contactDao.update(a1);
  //    HistoryEntry[] historyEntries = contactDao.getHistoryEntries(a1);
  //    assertEquals(2, historyEntries.length);
  //    HistoryEntry entry = historyEntries[0];
  //    log.debug(entry);
  //    assertHistoryEntry(entry, id, user, HistoryEntryType.UPDATE, "name", String.class, "History test", "History 2");
  //    entry = historyEntries[1];
  //    log.debug(entry);
  //    assertHistoryEntry(entry, id, user, HistoryEntryType.INSERT, null, null, null, null);
  //
  //    a1.setTask(getTask("1.2"));
  //    contactDao.update(a1);
  //    historyEntries = contactDao.getHistoryEntries(a1);
  //    assertEquals(3, historyEntries.length);
  //    entry = historyEntries[0];
  //    log.debug(entry);
  //    assertHistoryEntry(entry, id, user, HistoryEntryType.UPDATE, "task", TaskDO.class, getTask("1.1").getId(), getTask("1.2").getId());
  //
  //    a1.setTask(getTask("1.1"));
  //    a1.setName("History test");
  //    contactDao.update(a1);
  //    historyEntries = contactDao.getHistoryEntries(a1);
  //    assertEquals(4, historyEntries.length);
  //    entry = historyEntries[0];
  //    log.debug(entry);
  //    assertHistoryEntry(entry, id, user, HistoryEntryType.UPDATE, null, null, null, null);
  //    final List<PropertyDelta> delta = entry.getDelta();
  //    assertEquals(2, delta.size());
  //    for (int i = 0; i < 2; i++) {
  //      final PropertyDelta prop = delta.get(0);
  //      if ("name".equals(prop.getPropertyName()) == true) {
  //        assertPropertyDelta(prop, "name", String.class, "History 2", "History test");
  //      } else {
  //        assertPropertyDelta(prop, "task", TaskDO.class, getTask("1.2").getId(), getTask("1.1").getId());
  //      }
  //    }
  //
  //    List<SimpleHistoryEntry> list = contactDao.getSimpleHistoryEntries(a1);
  //    assertEquals(5, list.size());
  //    for (int i = 0; i < 2; i++) {
  //      final SimpleHistoryEntry se = list.get(i);
  //      if ("name".equals(se.getPropertyName()) == true) {
  //        assertSimpleHistoryEntry(se, user, HistoryEntryType.UPDATE, "name", String.class, "History 2", "History test");
  //      } else {
  //        assertSimpleHistoryEntry(se, user, HistoryEntryType.UPDATE, "task", TaskDO.class, getTask("1.2").getId(), getTask("1.1").getId());
  //      }
  //    }
  //    SimpleHistoryEntry se = list.get(2);
  //    assertSimpleHistoryEntry(se, user, HistoryEntryType.UPDATE, "task", TaskDO.class, getTask("1.1").getId(), getTask("1.2").getId());
  //    se = list.get(3);
  //    assertSimpleHistoryEntry(se, user, HistoryEntryType.UPDATE, "name", String.class, "History test", "History 2");
  //    se = list.get(4);
  //    assertSimpleHistoryEntry(se, user, HistoryEntryType.INSERT, null, null, null, null);
  //
  //    a1 = contactDao.getById(a1.getId());
  //    final Date date = a1.getLastUpdate();
  //    final String oldName = a1.getName();
  //    a1.setName("Micromata GmbH");
  //    a1.setName(oldName);
  //    contactDao.update(a1);
  //    a1 = contactDao.getById(a1.getId());
  //    list = contactDao.getSimpleHistoryEntries(a1);
  //    assertEquals(5, list.size());
  //    assertEquals(date, a1.getLastUpdate()); // Fails: Fix AbstractBaseDO.copyDeclaredFields: ObjectUtils.equals(Boolean, boolean) etc.
  //  }
  //
  //  @Test
  //  public void checkStandardAccess()
  //  {
  //    ContactDO a1 = new ContactDO();
  //    a1.setName("testa1");
  //    a1.setTask(getTask("ta_1_siud"));
  //    contactDao.internalSave(a1);
  //    ContactDO a2 = new ContactDO();
  //    a2.setName("testa2");
  //    a2.setTask(getTask("ta_2_siux"));
  //    contactDao.internalSave(a2);
  //    final ContactDO a3 = new ContactDO();
  //    a3.setName("testa3");
  //    a3.setTask(getTask("ta_3_sxxx"));
  //    contactDao.internalSave(a3);
  //    final ContactDO a4 = new ContactDO();
  //    a4.setName("testa4");
  //    a4.setTask(getTask("ta_4_xxxx"));
  //    contactDao.internalSave(a4);
  //    logon(AbstractTestBase.TEST_USER);
  //
  //    // Select
  //    try {
  //      contactDao.getById(a4.getId());
  //      fail("User has no access to select");
  //    } catch (final AccessException ex) {
  //      assertAccessException(ex, getTask("ta_4_xxxx").getId(), AccessType.TASKS, OperationType.SELECT);
  //    }
  //    ContactDO address = contactDao.getById(a3.getId());
  //    assertEquals("testa3", address.getName());
  //
  //    // Select filter
  //    final BaseSearchFilter searchFilter = new BaseSearchFilter();
  //    searchFilter.setSearchString("testa*");
  //    final QueryFilter filter = new QueryFilter(searchFilter);
  //    filter.addOrder(Order.asc("name"));
  //    final List<ContactDO> result = contactDao.getList(filter);
  //    assertEquals("Should found 3 address'.", 3, result.size());
  //    final HashSet<String> set = new HashSet<String>();
  //    set.add("testa1");
  //    set.add("testa2");
  //    set.add("testa3");
  //    assertTrue("Hit first entry", set.remove(result.get(0).getName()));
  //    assertTrue("Hit second entry", set.remove(result.get(1).getName()));
  //    assertTrue("Hit third entry", set.remove(result.get(2).getName()));
  //    // test_a4 should not be included in result list (no select access)
  //
  //    // Insert
  //    address = new ContactDO();
  //    address.setName("test");
  //    contactDao.setTask(address, getTask("ta_4_xxxx").getId());
  //    try {
  //      contactDao.save(address);
  //      fail("User has no access to insert");
  //    } catch (final AccessException ex) {
  //      assertAccessException(ex, getTask("ta_4_xxxx").getId(), AccessType.TASKS, OperationType.INSERT);
  //    }
  //    contactDao.setTask(address, getTask("ta_1_siud").getId());
  //    contactDao.save(address);
  //    assertEquals("test", address.getName());
  //
  //    // Update
  //    a3.setName("test_a3test");
  //    try {
  //      contactDao.update(a3);
  //      fail("User has no access to update");
  //    } catch (final AccessException ex) {
  //      assertAccessException(ex, getTask("ta_3_sxxx").getId(), AccessType.TASKS, OperationType.UPDATE);
  //    }
  //    a2.setName("testa2test");
  //    contactDao.update(a2);
  //    address = contactDao.getById(a2.getId());
  //    assertEquals("testa2test", address.getName());
  //    a2.setName("testa2");
  //    contactDao.update(a2);
  //    address = contactDao.getById(a2.getId());
  //    assertEquals("testa2", address.getName());
  //
  //    // Update with moving in task hierarchy
  //    a2.setName("testa2test");
  //    contactDao.setTask(a2, getTask("ta_1_siud").getId());
  //    try {
  //      contactDao.update(a2);
  //      fail("User has no access to update");
  //    } catch (final AccessException ex) {
  //      assertAccessException(ex, getTask("ta_2_siux").getId(), AccessType.TASKS, OperationType.DELETE);
  //    }
  //    a2 = contactDao.getById(a2.getId());
  //    a1.setName("testa1test");
  //    contactDao.setTask(a1, getTask("ta_5_sxux").getId());
  //    try {
  //      contactDao.update(a1);
  //      fail("User has no access to update");
  //    } catch (final AccessException ex) {
  //      assertAccessException(ex, getTask("ta_5_sxux").getId(), AccessType.TASKS, OperationType.INSERT);
  //    }
  //    a1 = contactDao.getById(a1.getId());
  //    assertEquals("testa1", a1.getName());
  //
  //    // Delete
  //    try {
  //      contactDao.delete(a1);
  //      fail("Address is historizable and should not be allowed to delete.");
  //    } catch (final RuntimeException ex) {
  //      assertEquals(true, ex.getMessage().startsWith(ContactDao.EXCEPTION_HISTORIZABLE_NOTDELETABLE));
  //    }
  //    try {
  //      contactDao.markAsDeleted(a2);
  //      fail("User has no access to delete");
  //    } catch (final AccessException ex) {
  //      assertAccessException(ex, getTask("ta_2_siux").getId(), AccessType.TASKS, OperationType.DELETE);
  //    }
  //  }

  //  @Test
  //  public void testInstantMessagingField() throws Exception
  //  {
  //    final ContactDO address = new ContactDO();
  //    assertNull(address.getInstantMessaging4DB());
  //    address.setInstantMessaging(SocialMediaType.SKYPE, "skype-name");
  //    assertEquals("SKYPE=skype-name", address.getInstantMessaging4DB());
  //    address.setInstantMessaging(SocialMediaType.AIM, "aim-id");
  //    assertEquals("SKYPE=skype-name\nAIM=aim-id", address.getInstantMessaging4DB());
  //    address.setInstantMessaging(SocialMediaType.YAHOO, "yahoo-name");
  //    assertEquals("SKYPE=skype-name\nAIM=aim-id\nYAHOO=yahoo-name", address.getInstantMessaging4DB());
  //    address.setInstantMessaging(SocialMediaType.YAHOO, "");
  //    assertEquals("SKYPE=skype-name\nAIM=aim-id", address.getInstantMessaging4DB());
  //    address.setInstantMessaging(SocialMediaType.SKYPE, "");
  //    assertEquals("AIM=aim-id", address.getInstantMessaging4DB());
  //    address.setInstantMessaging(SocialMediaType.AIM, "");
  //    assertNull(address.getInstantMessaging4DB());
  //  }
}
