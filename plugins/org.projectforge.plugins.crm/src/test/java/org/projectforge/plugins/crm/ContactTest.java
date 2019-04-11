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

import org.hibernate.criterion.Order;
import org.projectforge.business.address.PhoneType;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.test.AbstractTestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ContactTest extends AbstractTestBase {
  private final static Logger log = LoggerFactory.getLogger(ContactTest.class);

  @Autowired
  private ContactDao contactDao;

  //  @Test
  public void testSaveAndUpdate() {
    logon(AbstractTestBase.ADMIN);

    final ContactDO a1 = new ContactDO();
    a1.setName("Kai Reinhard");
    a1.setTask(getTask("1.1"));

    final SocialMediaValue value1 = new SocialMediaValue()
            .setContactType(ContactType.BUSINESS)
            .setSocialMediaType(SocialMediaType.JABBER)
            .setUser("Hurzel");
    final SocialMediaValue value2 = new SocialMediaValue()
            .setContactType(ContactType.PRIVATE)
            .setSocialMediaType(SocialMediaType.TWITTER)
            .setUser("Hurzeli");
    a1.setSocialMediaValues(contactDao.getSocialMediaValuesAsXml(value1, value2));

    final EmailValue email1 = new EmailValue().setContactType(ContactType.BUSINESS).setEmail("theo.test@acme.com");
    final EmailValue email2 = new EmailValue().setContactType(ContactType.PRIVATE).setEmail("theo.test@t-offline.de");
    a1.setEmailValues(contactDao.getEmailValuesAsXml(email1, email2));

    final PhoneValue phone1 = new PhoneValue().setPhoneType(PhoneType.BUSINESS).setNumber("1234567");
    final PhoneValue phone2 = new PhoneValue().setPhoneType(PhoneType.PRIVATE).setNumber("7654321");
    a1.setPhoneValues(contactDao.getPhoneValuesAsXml(phone1, phone2));

    contactDao.save(a1);
    log.debug(a1.toString());

    final ContactDO a1a = contactDao.getById(a1.getId());

    ArrayList<SocialMediaValue> list = new ArrayList<SocialMediaValue>();
    list = (ArrayList<SocialMediaValue>) contactDao.readSocialMediaValues(a1a.getSocialMediaValues());
    assertEquals(value1.getUser(), list.get(0).getUser());
    assertEquals(value2.getUser(), list.get(1).getUser());

    ArrayList<EmailValue> emailList = new ArrayList<EmailValue>();
    emailList = (ArrayList<EmailValue>) contactDao.readEmailValues(a1a.getEmailValues());
    assertEquals(email1.getEmail(), emailList.get(0).getEmail());
    assertEquals(email2.getEmail(), emailList.get(1).getEmail());

    ArrayList<PhoneValue> phoneList = new ArrayList<PhoneValue>();
    phoneList = (ArrayList<PhoneValue>) contactDao.readPhoneValues(a1a.getPhoneValues());
    assertEquals(phone1.getNumber(), phoneList.get(0).getNumber());
    assertEquals(phone2.getNumber(), phoneList.get(1).getNumber());

    a1.setName("Hurzel");
    contactDao.setTask(a1, getTask("1.2").getId());
    contactDao.update(a1);
    assertEquals("Hurzel", a1.getName());

    final ContactDO a2 = contactDao.getById(a1.getId());
    assertEquals("Hurzel", a2.getName());
    assertEquals(getTask("1.2").getId(), a2.getTaskId());
    a2.setName("Micromata GmbH");
    contactDao.setTask(a2, getTask("1").getId());
    contactDao.update(a2);
    log.debug(a2.toString());

    final ContactDO a3 = contactDao.getById(a1.getId());
    assertEquals("Micromata GmbH", a3.getName());
    assertEquals(getTask("1").getId(), a3.getTaskId());
    log.debug(a3.toString());
  }

  //@Test
  public void testDeleteAndUndelete() {
    logon(AbstractTestBase.ADMIN);
    ContactDO a1 = new ContactDO();
    a1.setName("Test");
    a1.setTask(getTask("1.1"));
    contactDao.save(a1);

    final Integer id = a1.getId();
    a1 = contactDao.getById(id);
    contactDao.markAsDeleted(a1);
    a1 = contactDao.getById(id);
    assertEquals(true, a1.isDeleted(), "Should be marked as deleted.");

    contactDao.undelete(a1);
    a1 = contactDao.getById(id);
    assertEquals(false, a1.isDeleted(), "Should be undeleted.");
  }

  //@Test(expected = RuntimeException.class)
  public void testDelete() {
    ContactDO a1 = new ContactDO();
    a1.setName("Not deletable");
    a1.setTask(getTask("1.1"));
    contactDao.save(a1);
    final Integer id = a1.getId();
    a1 = contactDao.getById(id);
    contactDao.delete(a1);
  }

  // TODO HISTORY
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
  //    assertHistoryEntry(entry, id, user, HistoryEntryType.UPDATE, "task", TaskDO.class, getTask("1.1").getId(),
  //        getTask("1.2").getId());
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

  //@Test
  public void checkStandardAccess() {
    ContactDO a1 = new ContactDO();
    a1.setName("testa1");
    a1.setTask(getTask("ta_1_siud"));
    contactDao.internalSave(a1);
    ContactDO a2 = new ContactDO();
    a2.setName("testa2");
    a2.setTask(getTask("ta_2_siux"));
    contactDao.internalSave(a2);
    final ContactDO a3 = new ContactDO();
    a3.setName("testa3");
    a3.setTask(getTask("ta_3_sxxx"));
    contactDao.internalSave(a3);
    final ContactDO a4 = new ContactDO();
    a4.setName("testa4");
    a4.setTask(getTask("ta_4_xxxx"));
    contactDao.internalSave(a4);
    logon(AbstractTestBase.TEST_USER);

    // Select
    try {
      contactDao.getById(a4.getId());
      fail("User has no access to select");
    } catch (final AccessException ex) {
      assertAccessException(ex, getTask("ta_4_xxxx").getId(), AccessType.TASKS, OperationType.SELECT);
    }
    ContactDO address = contactDao.getById(a3.getId());
    assertEquals("testa3", address.getName());

    // Select filter
    final BaseSearchFilter searchFilter = new BaseSearchFilter();
    searchFilter.setSearchString("testa*");
    final QueryFilter filter = new QueryFilter(searchFilter);
    filter.addOrder(Order.asc("name"));
    final List<ContactDO> result = contactDao.getList(filter);
    assertEquals(3, result.size(), "Should found 3 address'.");
    final HashSet<String> set = new HashSet<String>();
    set.add("testa1");
    set.add("testa2");
    set.add("testa3");
    assertTrue(set.remove(result.get(0).getName()), "Hit first entry");
    assertTrue(set.remove(result.get(1).getName()), "Hit second entry");
    assertTrue(set.remove(result.get(2).getName()), "Hit third entry");
    // test_a4 should not be included in result list (no select access)

    // Insert
    address = new ContactDO();
    address.setName("test");
    contactDao.setTask(address, getTask("ta_4_xxxx").getId());
    try {
      contactDao.save(address);
      fail("User has no access to insert");
    } catch (final AccessException ex) {
      assertAccessException(ex, getTask("ta_4_xxxx").getId(), AccessType.TASKS, OperationType.INSERT);
    }
    contactDao.setTask(address, getTask("ta_1_siud").getId());
    contactDao.save(address);
    assertEquals("test", address.getName());

    // Update
    a3.setName("test_a3test");
    try {
      contactDao.update(a3);
      fail("User has no access to update");
    } catch (final AccessException ex) {
      assertAccessException(ex, getTask("ta_3_sxxx").getId(), AccessType.TASKS, OperationType.UPDATE);
    }
    a2.setName("testa2test");
    contactDao.update(a2);
    address = contactDao.getById(a2.getId());
    assertEquals("testa2test", address.getName());
    a2.setName("testa2");
    contactDao.update(a2);
    address = contactDao.getById(a2.getId());
    assertEquals("testa2", address.getName());

    // Update with moving in task hierarchy
    a2.setName("testa2test");
    contactDao.setTask(a2, getTask("ta_1_siud").getId());
    try {
      contactDao.update(a2);
      fail("User has no access to update");
    } catch (final AccessException ex) {
      assertAccessException(ex, getTask("ta_2_siux").getId(), AccessType.TASKS, OperationType.DELETE);
    }
    a2 = contactDao.getById(a2.getId());
    a1.setName("testa1test");
    contactDao.setTask(a1, getTask("ta_5_sxux").getId());
    try {
      contactDao.update(a1);
      fail("User has no access to update");
    } catch (final AccessException ex) {
      assertAccessException(ex, getTask("ta_5_sxux").getId(), AccessType.TASKS, OperationType.INSERT);
    }
    a1 = contactDao.getById(a1.getId());
    assertEquals("testa1", a1.getName());

    // Delete
    try {
      contactDao.delete(a1);
      fail("Address is historizable and should not be allowed to delete.");
    } catch (final RuntimeException ex) {
      assertEquals(true, ex.getMessage().startsWith(ContactDao.EXCEPTION_HISTORIZABLE_NOTDELETABLE));
    }
    try {
      contactDao.markAsDeleted(a2);
      fail("User has no access to delete");
    } catch (final AccessException ex) {
      assertAccessException(ex, getTask("ta_2_siux").getId(), AccessType.TASKS, OperationType.DELETE);
    }
  }

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
