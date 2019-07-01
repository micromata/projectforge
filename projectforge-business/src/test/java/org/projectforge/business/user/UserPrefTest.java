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

package org.projectforge.business.user;

import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.UserPrefDO;
import org.projectforge.framework.persistence.user.entities.UserPrefEntryDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
public class UserPrefTest extends AbstractTestBase {
  @Autowired
  private UserPrefDao userPrefDao;

  static class User {
    String firstname;
    Locale locale;
    TimeZone timeZone;
    Date lastPasswordChange;
    Timestamp lastLogin;

    static User createTestUser() {
      User user = new User();
      user.firstname = "Kai";
      user.locale = Locale.GERMAN;
      user.timeZone = DateHelper.EUROPE_BERLIN;
      PFDateTime date = PFDateTime.parseUTCDate("2019-06-26 08:33");
      user.lastPasswordChange = date.getUtilDate();
      user.lastLogin = date.getSqlTimestamp();
      return user;
    }
  }

  @Test
  void jsonTest() {
    final PFUserDO loggedInUser = getUser(AbstractTestBase.TEST_USER);
    logon(loggedInUser);
    User user = User.createTestUser();
    UserPrefDO userPref = new UserPrefDO();
    userPref.setUser(loggedInUser);
    userPref.setValueObject(user);
    userPref.setArea("TEST_AREA");
    Integer id = userPrefDao.save(userPref);
    userPref = userPrefDao.internalGetById(id);
    User user2 = (User) userPrefDao.deserizalizeValueObject(userPref);
    assertEquals(User.class.getName(), userPref.getValueTypeString());
    assertEquals(User.class, userPref.getValueType());
    assertEquals(user.firstname, user2.firstname);
    assertEquals(user.locale, user2.locale);
    assertEquals(user.timeZone, user2.timeZone);
    assertEquals(user.lastPasswordChange.getTime(), user2.lastPasswordChange.getTime());
    assertEquals(user.lastLogin.getTime(), user2.lastLogin.getTime());
  }

  @Test
  void saveAndUpdateTest() {
    final PFUserDO loggedInUser = getUser(AbstractTestBase.TEST_USER);
    logon(loggedInUser);
    saveAndUpdateTest(null, loggedInUser);
    saveAndUpdateTest("test", loggedInUser);

    UserPrefDO userPref = new UserPrefDO();
    userPref.setUser(loggedInUser);
    userPref.setArea("TEST_AREA3");
    addEntry(userPref, "param1", "value1");
    addEntry(userPref, "param2", "value2");
    userPrefDao.internalSaveOrUpdate(userPref);
    userPref = userPrefDao.internalQuery(loggedInUser.getId(), "TEST_AREA3", null);
    assertEquals(2, userPref.getUserPrefEntries().size());
    assertEquals("value1", userPref.getUserPrefEntryAsString("param1"));
    assertEquals("value2", userPref.getUserPrefEntryAsString("param2"));

    userPref = new UserPrefDO();
    userPref.setUser(loggedInUser);
    userPref.setArea("TEST_AREA3");
    addEntry(userPref, "param1", "value1b");
    addEntry(userPref, "param3", "value3");
    userPrefDao.internalSaveOrUpdate(userPref);
    userPref = userPrefDao.internalQuery(loggedInUser.getId(), "TEST_AREA3", null);
    assertEquals(2, userPref.getUserPrefEntries().size());
    assertEquals("value1b", userPref.getUserPrefEntryAsString("param1"));
    assertEquals("value3", userPref.getUserPrefEntryAsString("param3"));

    userPref = new UserPrefDO();
    userPref.setUser(loggedInUser);
    userPref.setArea("TEST_AREA3");
    userPrefDao.internalSaveOrUpdate(userPref);
    userPref = userPrefDao.internalQuery(loggedInUser.getId(), "TEST_AREA3", null);
    assertTrue(CollectionUtils.isEmpty(userPref.getUserPrefEntries()));

    userPref = new UserPrefDO();
    userPref.setUser(loggedInUser);
    userPref.setArea("TEST_AREA4");
    userPrefDao.internalSaveOrUpdate(userPref);
    userPref = userPrefDao.internalQuery(loggedInUser.getId(), "TEST_AREA4", null);
    assertTrue(CollectionUtils.isEmpty(userPref.getUserPrefEntries()));

    userPref = new UserPrefDO();
    userPref.setUser(loggedInUser);
    userPref.setArea("TEST_AREA4");
    addEntry(userPref, "param1", "value1");
    addEntry(userPref, "param2", "value2");
    userPrefDao.internalSaveOrUpdate(userPref);
    userPref = userPrefDao.internalQuery(loggedInUser.getId(), "TEST_AREA4", null);
    assertEquals(2, userPref.getUserPrefEntries().size());
    assertEquals("value1", userPref.getUserPrefEntryAsString("param1"));
    assertEquals("value2", userPref.getUserPrefEntryAsString("param2"));

  }

  private void addEntry(UserPrefDO userPref, String parameter, String value) {
    UserPrefEntryDO entry = new UserPrefEntryDO();
    entry.setParameter(parameter);
    entry.setValue(value);
    userPref.addUserPrefEntry(entry);
  }


  private void saveAndUpdateTest(String name, PFUserDO loggedInUser) {
    User user = User.createTestUser();
    UserPrefDO userPref = new UserPrefDO();
    userPref.setUser(loggedInUser);
    userPref.setValueObject(user);
    userPref.setArea("TEST_AREA2");
    userPref.setName(name);
    userPrefDao.internalSaveOrUpdate(userPref);
    Integer id = userPref.getId();
    userPref = new UserPrefDO();
    userPref.setUser(loggedInUser);
    userPref.setValueObject(user);
    userPref.setArea("TEST_AREA2");
    userPref.setName(name);
    userPrefDao.internalSaveOrUpdate(userPref);
    assertEquals(id, userPref.getId(), "Object should be updated not inserted.");
  }

  @Test
  public void convertPrefParameters() {
    final PFUserDO user = getUser(AbstractTestBase.TEST_USER);
    logon(user);
    final PFUserDO user2 = getUser(AbstractTestBase.TEST_USER2);
    final TaskDO task = initTestDB.addTask("UserPrefTest", "root");
    UserPrefDO userPref = createUserPref(user, UserPrefArea.TIMESHEET_TEMPLATE, "test");
    TimesheetDO timesheet = createTimesheet(user2, task, "Micromata", "Wrote a test case...");
    userPrefDao.addUserPrefParameters(userPref, timesheet);
    assertFalse(userPrefDao.doesParameterNameAlreadyExist(null, user, UserPrefArea.TIMESHEET_TEMPLATE, "test"));
    final Serializable id = userPrefDao.save(userPref);
    assertTrue(userPrefDao.doesParameterNameAlreadyExist(null, user, UserPrefArea.TIMESHEET_TEMPLATE, "test"));
    assertFalse(userPrefDao.doesParameterNameAlreadyExist((Integer) id, user, UserPrefArea.TIMESHEET_TEMPLATE, "test"));
    userPref = userPrefDao.getById(id);
    assertEquals(5, userPref.getUserPrefEntries().size()); // user, task, kost2, location, description.
    {
      final Iterator<UserPrefEntryDO> it = userPref.getSortedUserPrefEntries().iterator();
      UserPrefEntryDO entry = it.next();
      assertUserPrefEntry(entry, "user", PFUserDO.class, user2.getId().toString(), "user", null, "1");
      userPrefDao.updateParameterValueObject(entry);
      assertEquals(user2.getId(), ((PFUserDO) entry.getValueAsObject()).getId());
      entry = it.next();
      assertUserPrefEntry(entry, "task", TaskDO.class, task.getId().toString(), "task", null, "2");
      userPrefDao.updateParameterValueObject(entry);
      assertEquals(task.getId(), ((TaskDO) entry.getValueAsObject()).getId());
      entry = it.next();
      assertUserPrefEntry(entry, "kost2", Kost2DO.class, null, "fibu.kost2", null, "3");
      entry = it.next();
      assertUserPrefEntry(entry, "location", String.class, "Micromata", "timesheet.location", 100, "ZZZ00");
      entry = it.next();
      assertUserPrefEntry(entry, "description", String.class, "Wrote a test case...", "description", 4000, "ZZZ01");
    }
    timesheet = new TimesheetDO();
    userPrefDao.fillFromUserPrefParameters(userPref, timesheet);
    assertEquals(user2.getId(), timesheet.getUserId());
    assertEquals(task.getId(), timesheet.getTaskId());
    assertNull(timesheet.getKost2Id());
    assertEquals("Micromata", timesheet.getLocation());
    assertEquals("Wrote a test case...", timesheet.getDescription());
    userPref.getUserPrefEntry("location").setValue("At home");
    userPrefDao.update(userPref);
    String[] names = userPrefDao.getPrefNames(UserPrefArea.TIMESHEET_TEMPLATE);
    assertEquals(1, names.length);
    assertEquals("test", names[0]);
    List<UserPrefEntryDO> dependents = userPref
            .getDependentUserPrefEntries(userPref.getUserPrefEntry("user").getParameter());
    assertNull(dependents);
    dependents = userPref.getDependentUserPrefEntries(userPref.getUserPrefEntry("task").getParameter());
    assertEquals(1, dependents.size());
    assertEquals("kost2", dependents.get(0).getParameter());
  }

  private void assertUserPrefEntry(final UserPrefEntryDO userPrefEntry, final String parameter, final Class<?> type,
                                   final String valueAsString, final String i18nKey, final Integer maxLength, final String orderString) {
    assertEquals(parameter, userPrefEntry.getParameter());
    assertEquals(type, userPrefEntry.getType());
    assertEquals(i18nKey, userPrefEntry.getI18nKey());
    assertEquals(maxLength, userPrefEntry.getMaxLength());
    assertEquals(orderString, userPrefEntry.getOrderString());
    assertEquals(valueAsString, userPrefEntry.getValue());
  }

  private TimesheetDO createTimesheet(final PFUserDO user, final TaskDO task, final String location,
                                      final String description) {
    TimesheetDO timesheet = new TimesheetDO();
    timesheet.setUser(user);
    timesheet.setTask(task);
    timesheet.setLocation(location);
    timesheet.setDescription(description);
    return timesheet;
  }

  private UserPrefDO createUserPref(final PFUserDO user, final UserPrefArea area, final String name) {
    UserPrefDO userPref = new UserPrefDO();
    userPref.setUser(user);
    userPref.setAreaObject(area);
    userPref.setName(name);
    return userPref;
  }
}
