/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.humanresources;

import org.junit.jupiter.api.Test;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.KundeDao;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.humanresources.*;
import org.projectforge.business.user.*;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.UserRightDO;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.time.PFDay;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class HRPlanningTest extends AbstractTestBase {
  private static ProjektDO projekt1, projekt2;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private HRPlanningDao hrPlanningDao;

  @Autowired
  private KundeDao kundeDao;

  @Autowired
  private UserGroupCache userGroupCache;

  @Autowired
  private UserRightDao userRightDao;

  @Autowired
  UserRightService userRights;

  @Override
  protected void beforeAll() {
    logon(AbstractTestBase.TEST_FINANCE_USER);
    final KundeDO kunde = new KundeDO();
    kunde.setName("ACME ltd.");
    kunde.setId(59);
    kundeDao.save(kunde);
    projekt1 = initTestDB.addProjekt(kunde, 0, "Web portal");
    projekt2 = initTestDB.addProjekt(kunde, 1, "Order management");
  }

  @Test
  public void testUserRights() {
    PFUserDO user1 = initTestDB.addUser("HRPlanningTestUser1");
    final HRPlanningRight right = (HRPlanningRight) userRights.getRight(UserRightId.PM_HR_PLANNING);
    assertFalse(right.isAvailable(user1, userGroupCache.getUserGroupDOs(user1)));
    final HRPlanningDO planning = new HRPlanningDO();
    planning.setUser(getUser(AbstractTestBase.TEST_USER));
    logon(user1);
    assertFalse(hrPlanningDao.hasLoggedInUserAccess(planning, null, OperationType.SELECT, false));
    try {
      hrPlanningDao.hasLoggedInUserAccess(planning, null, OperationType.SELECT, true);
      fail("AccessException excepted.");
    } catch (final AccessException ex) {
      // OK
    }
    logon(AbstractTestBase.TEST_ADMIN_USER);
    final GroupDO group = initTestDB.getGroup(AbstractTestBase.ORGA_GROUP);
    group.getAssignedUsers().add(user1);
    groupDao.update(group);
    assertTrue(right.isAvailable(user1, userGroupCache.getUserGroupDOs(user1)));
    logon(user1);
    assertFalse(hrPlanningDao.hasLoggedInUserAccess(planning, null, OperationType.SELECT, false));
    assertTrue(accessChecker.hasLoggedInUserSelectAccess(UserRightId.PM_HR_PLANNING, false));
    assertFalse(accessChecker.hasLoggedInUserSelectAccess(UserRightId.PM_HR_PLANNING, planning, false));
    assertFalse(accessChecker.hasLoggedInUserHistoryAccess(UserRightId.PM_HR_PLANNING, planning, false));
    assertFalse(accessChecker.hasLoggedInUserInsertAccess(UserRightId.PM_HR_PLANNING, planning, false));
    logon(AbstractTestBase.TEST_ADMIN_USER);
    user1.addRight(new UserRightDO(user1, UserRightId.PM_HR_PLANNING, UserRightValue.READONLY));
    userService.update(user1);
    userRightDao.save(new ArrayList<>(user1.getRights()));
    logon(user1);
    assertTrue(hrPlanningDao.hasLoggedInUserAccess(planning, null, OperationType.SELECT, false));
    assertTrue(accessChecker.hasLoggedInUserSelectAccess(UserRightId.PM_HR_PLANNING, planning, false));
    assertTrue(accessChecker.hasLoggedInUserHistoryAccess(UserRightId.PM_HR_PLANNING, planning, false));
    assertFalse(accessChecker.hasLoggedInUserInsertAccess(UserRightId.PM_HR_PLANNING, planning, false));
    logon(AbstractTestBase.TEST_ADMIN_USER);
    user1 = userService.getById(user1.getId());
    final UserRightDO userRight = user1.getRight(UserRightId.PM_HR_PLANNING);
    userRight.setValue(UserRightValue.READWRITE);
    userRightDao.update(userRight);
    logon(user1);
    assertTrue(hrPlanningDao.hasLoggedInUserAccess(planning, null, OperationType.SELECT, false));
    assertTrue(accessChecker.hasLoggedInUserSelectAccess(UserRightId.PM_HR_PLANNING, planning, false));
    assertTrue(accessChecker.hasLoggedInUserHistoryAccess(UserRightId.PM_HR_PLANNING, planning, false));
    assertTrue(accessChecker.hasLoggedInUserInsertAccess(UserRightId.PM_HR_PLANNING, planning, false));
  }

  @Test
  public void getFirstDayOfWeek() {
    final LocalDate date = LocalDate.of(2010, Month.JANUARY, 9);
    assertEquals("2010-01-04", HRPlanningDO.Companion.getFirstDayOfWeek(date).toString());
  }

  @Test
  public void testBeginOfWeek() {
    logon(AbstractTestBase.TEST_FINANCE_USER);
    HRPlanningDO planning = new HRPlanningDO();
    final LocalDate date = LocalDate.of(2010, Month.JANUARY, 9);
    planning.setFirstDayOfWeek(date);
    assertEquals("2010-01-04", planning.getWeek().toString());
    planning.setWeek(date);
    planning.setUser(getUser(AbstractTestBase.TEST_USER));
    assertEquals("2010-01-09", planning.getWeek().toString());
    final Serializable id = hrPlanningDao.save(planning);
    planning = hrPlanningDao.getById(id);
    assertEquals("2010-01-04", planning.getWeek().toString());
  }

  @Test
  public void overwriteDeletedEntries() {
    logon(AbstractTestBase.TEST_FINANCE_USER);
    // Create planning:
    HRPlanningDO planning = new HRPlanningDO();
    planning.setUser(getUser(AbstractTestBase.TEST_USER));
    planning.setWeek(LocalDate.of(2010, Month.JANUARY, 11));
    assertLocalDate(planning.getWeek(), 2010, Month.JANUARY, 11);
    HRPlanningEntryDO entry = new HRPlanningEntryDO();
    setHours(entry, 1, 2, 3, 4, 5, 6);
    entry.setProjekt(projekt1);
    planning.addEntry(entry);
    entry = new HRPlanningEntryDO();
    setHours(entry, 2, 4, 6, 8, 10, 12);
    entry.setStatus(HRPlanningEntryStatus.OTHER);
    planning.addEntry(entry);
    entry = new HRPlanningEntryDO();
    setHours(entry, 6, 5, 4, 3, 2, 1);
    entry.setProjekt(projekt2);
    planning.addEntry(entry);
    final Serializable id = hrPlanningDao.save(planning);
    // Check saved planning
    planning = hrPlanningDao.getById(id);
    PFDay day = new PFDay(planning.getWeek());
    assertLocalDate(day.getLocalDate(), 2010, Month.JANUARY, 11);
    assertEquals(3, planning.getEntries().size());
    assertHours(planning.getProjectEntry(projekt1), 1, 2, 3, 4, 5, 6);
    assertHours(planning.getProjectEntry(projekt2), 6, 5, 4, 3, 2, 1);
    assertHours(planning.getStatusEntry(HRPlanningEntryStatus.OTHER), 2, 4, 6, 8, 10, 12);
    // Delete entry
    planning.getProjectEntry(projekt1).setDeleted(true);
    hrPlanningDao.update(planning);
    // Check deleted entry and re-adding it
    planning = hrPlanningDao.getById(id);
    assertTrue(planning.getProjectEntry(projekt1).isDeleted());
    entry = new HRPlanningEntryDO();
    setHours(entry, 7, 9, 11, 1, 3, 5);
    entry.setProjekt(projekt1);
    planning.addEntry(entry);
    hrPlanningDao.update(planning);
  }

  private void setHours(final HRPlanningEntryDO entry, final int monday, final int tuesday, final int wednesday,
                        final int thursday,
                        final int friday, final int weekend) {
    entry.setMondayHours(new BigDecimal(monday));
    entry.setTuesdayHours(new BigDecimal(tuesday));
    entry.setWednesdayHours(new BigDecimal(wednesday));
    entry.setThursdayHours(new BigDecimal(thursday));
    entry.setFridayHours(new BigDecimal(friday));
    entry.setWeekendHours(new BigDecimal(weekend));
  }

  private void assertHours(final HRPlanningEntryDO entry, final int monday, final int tuesday, final int wednesday,
                           final int thursday,
                           final int friday, final int weekend) {
    assertBigDecimal(monday, entry.getMondayHours());
    assertBigDecimal(tuesday, entry.getTuesdayHours());
    assertBigDecimal(wednesday, entry.getWednesdayHours());
    assertBigDecimal(thursday, entry.getThursdayHours());
    assertBigDecimal(friday, entry.getFridayHours());
    assertBigDecimal(weekend, entry.getWeekendHours());
  }

  private LocalDate createDate(final int year, final Month month, final int day, final int hour, final int minute,
                                   final int second, final int millisecond) {
    return PFDateTime.withDate(year, month, day, hour, minute, second, millisecond, ZoneId.of("UTC"), Locale.GERMAN).getLocalDate();
  }
}
