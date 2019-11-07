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

package org.projectforge.timesheet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.KundeDao;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class TimesheetMassUpdateTest extends AbstractTestBase {
  // private static final Logger log = Logger.getLogger(TaskTest.class);

  @Autowired
  private TimesheetDao timesheetDao;

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private KundeDao kundeDao;

  @Autowired
  private Kost2Dao kost2Dao;

  @Autowired
  private ProjektDao projektDao;

  private DateHolder date;

  @BeforeEach
  public void setUp() {
    date = new DateHolder(new Date(), DatePrecision.MINUTE_15, Locale.GERMAN);
  }

  @Test
  public void massUpdate() {
    final String prefix = "ts-mu1-";
    final List<TimesheetDO> list = new ArrayList<>();
    initTestDB.addTask(prefix + "1", "root");
    initTestDB.addTask(prefix + "1.1", prefix + "1");
    initTestDB.addTask(prefix + "1.2", prefix + "1");
    initTestDB.addTask(prefix + "2", "root");
    initTestDB.addUser(prefix + "user1");
    logon(getUser(AbstractTestBase.TEST_FINANCE_USER));
    list.add(
            createTimesheet(prefix, "1.1", "user1", 2009, 10, 21, 3, 0, 3, 15, "Office", "A lot of stuff done and more."));
    list.add(
            createTimesheet(prefix, "1.2", "user1", 2009, 10, 21, 3, 15, 3, 30, "Office", "A lot of stuff done and more."));
    TimesheetDO master = new TimesheetDO();
    master.setTask(initTestDB.getTask(prefix + "2"));
    master.setLocation("Headquarter");
    timesheetDao.massUpdate(list, master);
    assertAll(list, master);
  }

  @Test
  public void massUpdateWithKost2Transformation() {
    logon(getUser(AbstractTestBase.TEST_FINANCE_USER));
    final String prefix = "ts-mu50-";
    final List<TimesheetDO> list = new ArrayList<>();
    final KundeDO kunde = new KundeDO();
    kunde.setName("ACME");
    kunde.setId(50);
    kundeDao.save(kunde);

    final ProjektDO projekt1 = createProjekt(kunde, 1, "Webportal", 0, 1, 2);
    final ProjektDO projekt2 = createProjekt(kunde, 2, "iPhone App", 0, 1);

    final TaskDO t1 = initTestDB.addTask(prefix + "1", "root");
    projektDao.setTask(projekt1, t1.getId());
    projektDao.update(projekt1);
    initTestDB.addTask(prefix + "1.1", prefix + "1");
    initTestDB.addTask(prefix + "1.2", prefix + "1");

    final TaskDO t2 = initTestDB.addTask(prefix + "2", "root");
    projektDao.setTask(projekt2, t2.getId());
    projektDao.update(projekt2);
    initTestDB.addTask(prefix + "2.1", prefix + "2");
    initTestDB.addUser(prefix + "user1");
    logon(getUser(AbstractTestBase.TEST_ADMIN_USER));

    list.add(createTimesheet(prefix, "1.1", "user1", 2009, 10, 21, 3, 0, 3, 15, "Office",
            "A lot of stuff done and more.", 5, 50, 1, 0));
    list.add(createTimesheet(prefix, "1.2", "user1", 2009, 10, 21, 3, 15, 3, 30, "Office",
            "A lot of stuff done and more.", 5, 50, 1, 1));
    list.add(createTimesheet(prefix, "1.2", "user1", 2009, 10, 21, 3, 30, 3, 45, "Office",
            "A lot of stuff done and more.", 5, 50, 1, 2));
    final TimesheetDO master = new TimesheetDO();
    master.setTask(initTestDB.getTask(prefix + "2"));
    master.setLocation("Headquarter");
    try {
      timesheetDao.massUpdate(list, master);
    } catch (UserException e) {
      // ignore the exception here for testing
    }
    assertSheet(list.get(0), master);
    assertKost2(list.get(0), 5, 50, 2, 0); // Kost2 transformed.
    assertSheet(list.get(1), master);
    assertKost2(list.get(1), 5, 50, 2, 1); // Kost2 transformed.
    assertKost2(list.get(2), 5, 50, 1, 2); // Kost2 not transformed.
    assertEquals(getTask(prefix + "1.2").getId(), list.get(2).getTaskId());
  }

  @Test
  public void massUpdateWithKost2() {
    logon(getUser(AbstractTestBase.TEST_FINANCE_USER));
    final String prefix = "ts-mu51-";
    final List<TimesheetDO> list = new ArrayList<>();
    final KundeDO kunde = new KundeDO();
    kunde.setName("ACME ltd.");
    kunde.setId(51);
    kundeDao.save(kunde);

    final ProjektDO projekt1 = createProjekt(kunde, 1, "Webportal", 0, 1, 2);
    final ProjektDO projekt2 = createProjekt(kunde, 2, "iPhone App", 0, 1);

    final TaskDO t1 = initTestDB.addTask(prefix + "1", "root");
    projektDao.setTask(projekt1, t1.getId());
    projektDao.update(projekt1);
    initTestDB.addTask(prefix + "1.1", prefix + "1");
    initTestDB.addTask(prefix + "1.2", prefix + "1");

    final TaskDO t2 = initTestDB.addTask(prefix + "2", "root");
    projektDao.setTask(projekt2, t2.getId());
    projektDao.update(projekt2);
    initTestDB.addTask(prefix + "2.1", prefix + "2");
    initTestDB.addUser(prefix + "user1");
    logon(getUser(AbstractTestBase.TEST_ADMIN_USER));

    list.add(createTimesheet(prefix, "1.1", "user1", 2009, 10, 21, 3, 0, 3, 15, "Office",
            "A lot of stuff done and more.", 5, 51, 1, 0));
    list.add(createTimesheet(prefix, "1.2", "user1", 2009, 10, 21, 3, 15, 3, 30, "Office",
            "A lot of stuff done and more.", 5, 51, 1, 1));
    list.add(createTimesheet(prefix, "1.2", "user1", 2009, 10, 21, 3, 30, 3, 45, "Office",
            "A lot of stuff done and more.", 5, 51, 1, 2));
    final TimesheetDO master = new TimesheetDO();
    master.setTask(initTestDB.getTask(prefix + "2"));
    master.setLocation("Headquarter");
    Kost2DO kost2 = kost2Dao.getKost2(5, 51, 1, 0); // Kost2 is not supported by destination task.
    assertNotNull(kost2);
    master.setKost2(kost2);
    try {
      timesheetDao.massUpdate(list, master);
    } catch (UserException e) {
      // ignore the exception here for testing
    }
    assertEquals(getTask(prefix + "1.1").getId(), list.get(0).getTaskId()); // Not moved.
    assertEquals(getTask(prefix + "1.2").getId(), list.get(1).getTaskId()); // Not moved.
    assertEquals(getTask(prefix + "1.2").getId(), list.get(2).getTaskId()); // Not moved.
    assertKost2(list.get(0), 5, 51, 1, 0); // Kost2 not transformed.
    assertKost2(list.get(1), 5, 51, 1, 1); // Kost2 not transformed.
    assertKost2(list.get(2), 5, 51, 1, 2); // Kost2 not transformed.

    kost2 = kost2Dao.getKost2(5, 51, 2, 0); // Kost2 supported by destination task.
    assertNotNull(kost2);
    master.setKost2(kost2);
    timesheetDao.massUpdate(list, master);
    assertAll(list, master); // All sheets moved.
    assertKost2(list.get(0), 5, 51, 2, 0); // Kost2 transformed.
    assertKost2(list.get(1), 5, 51, 2, 0); // Kost2 transformed.
    assertKost2(list.get(2), 5, 51, 2, 0); // Kost2 transformed.
  }

  @Test
  public void massUpdateMixedKost2() {
    logon(getUser(AbstractTestBase.TEST_FINANCE_USER));
    final String prefix = "ts-mu52-";
    final List<TimesheetDO> list = new ArrayList<>();
    final KundeDO kunde = new KundeDO();
    kunde.setName("ACME International");
    kunde.setId(52);
    kundeDao.save(kunde);

    final ProjektDO projekt1 = createProjekt(kunde, 1, "Webportal", 0, 1, 2);

    initTestDB.addTask(prefix + "1", "root");
    initTestDB.addTask(prefix + "1.1", prefix + "1");
    initTestDB.addTask(prefix + "1.2", prefix + "1");

    final TaskDO t2 = initTestDB.addTask(prefix + "2", "root");
    projektDao.setTask(projekt1, t2.getId());
    projektDao.update(projekt1);
    initTestDB.addTask(prefix + "2.1", prefix + "2");
    initTestDB.addUser(prefix + "user1");
    logon(getUser(AbstractTestBase.TEST_ADMIN_USER));

    list.add(
            createTimesheet(prefix, "1.1", "user1", 2009, 10, 21, 3, 0, 3, 15, "Office", "A lot of stuff done and more."));
    list.add(
            createTimesheet(prefix, "1.2", "user1", 2009, 10, 21, 3, 15, 3, 30, "Office", "A lot of stuff done and more."));
    list.add(
            createTimesheet(prefix, "1.2", "user1", 2009, 10, 21, 3, 30, 3, 45, "Office", "A lot of stuff done and more."));
    final TimesheetDO master = new TimesheetDO();
    master.setTask(initTestDB.getTask(prefix + "2"));
    master.setLocation("Headquarter");
    try {
      timesheetDao.massUpdate(list, master);
    } catch (UserException e) {
      // ignore the exception here for testing
    }
    assertEquals(getTask(prefix + "1.1").getId(), list.get(0).getTaskId()); // Not moved.
    assertEquals(getTask(prefix + "1.2").getId(), list.get(1).getTaskId()); // Not moved.
    assertEquals(getTask(prefix + "1.2").getId(), list.get(2).getTaskId()); // Not moved.
    assertNull(list.get(0).getKost2Id()); // Kost2 not set.
    assertNull(list.get(1).getKost2Id()); // Kost2 not set.
    assertNull(list.get(2).getKost2Id()); // Kost2 not set.

    final Kost2DO kost2 = kost2Dao.getKost2(5, 52, 1, 0); // Kost2 supported by destination task.
    assertNotNull(kost2);
    master.setKost2(kost2);
    timesheetDao.massUpdate(list, master);
    assertAll(list, master); // All sheets moved.
    assertKost2(list.get(0), 5, 52, 1, 0); // Kost2 set.
    assertKost2(list.get(1), 5, 52, 1, 0); // Kost2 set.
    assertKost2(list.get(2), 5, 52, 1, 0); // Kost2 set.
  }

  @Test
  public void checkMassUpdateWithTimesheetProtection() {
    logon(getUser(AbstractTestBase.TEST_FINANCE_USER));
    final String prefix = "ts-mu53-";
    final List<TimesheetDO> list = new ArrayList<>();
    final KundeDO kunde = new KundeDO();
    kunde.setName("ACME ltd.");
    kunde.setId(53);
    kundeDao.save(kunde);

    final TaskDO t1 = initTestDB.addTask(prefix + "1", "root");
    final ProjektDO projekt1 = createProjekt(kunde, 1, "Webportal", 0, 1, 2);
    projekt1.setTask(t1);
    projektDao.update(projekt1);
    final ProjektDO projekt2 = createProjekt(kunde, 2, "iPhone App", 0, 1);

    initTestDB.addTask(prefix + "1.1", prefix + "1");
    initTestDB.addTask(prefix + "1.2", prefix + "1");

    final TaskDO t2 = initTestDB.addTask(prefix + "2", "root");
    projektDao.setTask(projekt2, t2.getId());
    projektDao.update(projekt2);
    final DateHolder dh = new DateHolder();
    dh.setDate(2009, 11, 31);
    t2.setProtectTimesheetsUntil(dh.getDate());
    taskDao.update(t2);
    initTestDB.addTask(prefix + "2.1", prefix + "2");
    initTestDB.addTask(prefix + "2.2", prefix + "2");
    initTestDB.addUser(prefix + "user");
    final TimesheetDO ts1 = createTimesheet(prefix, "2.1", "user", 2009, 10, 21, 3, 30, 3, 45, "Office",
            "A lot of stuff done and more.",
            5, 53, 2, 0);
    list.add(ts1);
    final TimesheetDO ts2 = createTimesheet(prefix, "1.1", "user", 2009, 10, 21, 3, 0, 3, 15, "Office",
            "A lot of stuff done and more.", 5,
            53, 1, 0);
    list.add(ts2);
    final TimesheetDO ts3 = createTimesheet(prefix, "1.2", "user", 2009, 10, 21, 3, 15, 3, 30, "Office",
            "A lot of stuff done and more.",
            5, 53, 1, 1);
    list.add(ts3);
    logon(getUser(AbstractTestBase.TEST_ADMIN_USER));

    final TimesheetDO master = new TimesheetDO();
    master.setTask(initTestDB.getTask(prefix + "2.2"));
    try {
      timesheetDao.massUpdate(list, master);
    } catch (UserException e) {
      // ignore the exception here for testing
    }
    assertSheet(list.get(0), master);
    assertKost2(list.get(0), 5, 53, 2, 0); // Kost2 unmodified.
    TimesheetDO ts = timesheetDao.getById(ts2.getId());
    assertEquals(getTask(prefix + "1.1").getId(), ts.getTaskId()); // Not moved.
    assertKost2(ts, 5, 53, 1, 0); // Kost2 unmodified.
    ts = timesheetDao.getById(ts3.getId());
    assertEquals(getTask(prefix + "1.2").getId(), ts.getTaskId()); // Not moved.
    assertKost2(ts, 5, 53, 1, 1); // Kost2 unmodified.
  }

  @Test
  public void checkMaxMassUpdateNumber() {
    final List<TimesheetDO> list = new ArrayList<>();
    for (int i = 0; i <= BaseDao.MAX_MASS_UPDATE; i++) {
      list.add(new TimesheetDO());
    }
    try {
      timesheetDao.massUpdate(list, new TimesheetDO());
      fail("Maximum number of allowed mass updates exceeded. Not detected!");
    } catch (UserException ex) {
      assertEquals(BaseDao.MAX_MASS_UPDATE_EXCEEDED_EXCEPTION_I18N, ex.getI18nKey());
      // OK.
    }
  }

  private ProjektDO createProjekt(final KundeDO kunde, final Integer projektNummer, final String projektName,
                                  final Integer... kost2ArtIds) {
    return initTestDB.addProjekt(kunde, projektNummer, projektName, kost2ArtIds);
  }

  private void assertAll(final List<TimesheetDO> list, final TimesheetDO master) {
    for (final TimesheetDO sheet : list) {
      assertSheet(sheet, master);
    }
  }

  private void assertSheet(final TimesheetDO sheet, final TimesheetDO master) {
    if (master.getTaskId() != null) {
      assertEquals(master.getTaskId(), sheet.getTaskId());
    }
    if (master.getLocation() != null) {
      assertEquals(master.getLocation(), sheet.getLocation());
    }
  }

  private void assertKost2(final TimesheetDO sheet, final int nummernkreis, final int bereich, final int teilbereich,
                           final int art) {
    final Kost2DO kost2 = sheet.getKost2();
    assertNotNull(kost2);
    assertEquals(nummernkreis, kost2.getNummernkreis());
    assertEquals(bereich, kost2.getBereich());
    assertEquals(teilbereich, kost2.getTeilbereich());
    assertEquals(art, (int) kost2.getKost2ArtId());
  }

  private TimesheetDO createTimesheet(final String prefix, final String taskName, final String userName, final int year,
                                      final int month,
                                      final int day, final int fromHour, final int fromMinute, final int toHour, final int toMinute,
                                      final String location,
                                      final String description) {
    return createTimesheet(prefix, taskName, userName, year, month, day, fromHour, fromMinute, toHour, toMinute,
            location, description, 0,
            0, 0, 0);
  }

  private TimesheetDO createTimesheet(final String prefix, final String taskName, final String userName, final int year,
                                      final int month,
                                      final int day, final int fromHour, final int fromMinute, final int toHour, final int toMinute,
                                      final String location,
                                      final String description, final int kost2Nummernkreis, final int kost2Bereich, final int kost2Teilbereich,
                                      final int kost2Art) {
    final TimesheetDO ts = new TimesheetDO();
    setTimeperiod(ts, year, month, day, fromHour, fromMinute, day, toHour, toMinute);
    ts.setTask(initTestDB.getTask(prefix + taskName));
    ts.setUser(getUser(prefix + userName));
    ts.setLocation(location);
    ts.setDescription(description);
    if (kost2Nummernkreis > 0) {
      final Kost2DO kost2 = kost2Dao.getKost2(kost2Nummernkreis, kost2Bereich, kost2Teilbereich, kost2Art);
      assertNotNull(kost2);
      ts.setKost2(kost2);
    }
    final Serializable id = timesheetDao.internalSave(ts);
    return timesheetDao.getById(id);
  }

  private void setTimeperiod(TimesheetDO timesheet, int year, int month, int fromDay, int fromHour, int fromMinute,
                             int toDay, int toHour,
                             int toMinute) {
    date.setDate(year, month, fromDay, fromHour, fromMinute, 0);
    timesheet.setStartTime(date.getTimestamp());
    date.setDate(year, month, toDay, toHour, toMinute, 0);
    timesheet.setStopTime(date.getTimestamp());
  }
}
