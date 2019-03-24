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

package org.projectforge.business.fibu;

import static org.testng.AssertJUnit.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.UserRightDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.UserRightDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.test.AbstractBase;
import org.projectforge.test.AbstractTestNGBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class AuftragDaoTest extends AbstractTestNGBase
{
  private int dbNumber = AuftragDao.START_NUMBER;

  @Autowired
  private AuftragDao auftragDao;

  @Autowired
  private ProjektDao projektDao;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private UserRightDao userRightDao;

  private final Random random = new Random();

  @Test
  public void getNextNumber()
  {
    logon(AbstractBase.TEST_FINANCE_USER);
    AuftragDO auftrag = new AuftragDO();
    auftrag.setNummer(auftragDao.getNextNumber(auftrag));
    auftrag.addPosition(new AuftragsPositionDO());
    auftragDao.save(auftrag);
    assertEquals(dbNumber++, auftrag.getNummer().intValue());
    auftrag = new AuftragDO();
    auftrag.setNummer(auftragDao.getNextNumber(auftrag));
    auftrag.addPosition(new AuftragsPositionDO());
    auftragDao.save(auftrag);
    assertEquals(dbNumber++, auftrag.getNummer().intValue());
  }

  @Test
  public void checkAccess()
  {
    logon(AbstractBase.TEST_FINANCE_USER);
    AuftragDO auftrag1 = new AuftragDO();
    auftrag1.setNummer(auftragDao.getNextNumber(auftrag1));
    auftragDao.setContactPerson(auftrag1, getUserId(AbstractBase.TEST_FINANCE_USER));
    Serializable id1;
    try {
      id1 = auftragDao.save(auftrag1);
      fail("UserException expected: Order should have positions.");
    } catch (final UserException ex) {
      assertEquals("fibu.auftrag.error.auftragHatKeinePositionen", ex.getI18nKey());
    }
    auftrag1.addPosition(new AuftragsPositionDO());
    id1 = auftragDao.save(auftrag1);
    dbNumber++; // Needed for getNextNumber test;
    auftrag1 = auftragDao.getById(id1);

    AuftragDO auftrag2 = new AuftragDO();
    auftrag2.setNummer(auftragDao.getNextNumber(auftrag2));
    auftragDao.setContactPerson(auftrag2, getUserId(AbstractBase.TEST_PROJECT_MANAGER_USER));
    auftrag2.addPosition(new AuftragsPositionDO());
    final Serializable id2 = auftragDao.save(auftrag2);
    dbNumber++; // Needed for getNextNumber test;

    AuftragDO auftrag3 = new AuftragDO();
    auftrag3.setNummer(auftragDao.getNextNumber(auftrag3));
    auftragDao.setContactPerson(auftrag3, getUserId(AbstractBase.TEST_PROJECT_MANAGER_USER));
    final DateHolder date = new DateHolder();
    date.add(Calendar.YEAR, -6); // 6 years old.
    auftrag3.setAngebotsDatum(date.getSQLDate());
    auftrag3.setAuftragsStatus(AuftragsStatus.ABGESCHLOSSEN);
    final AuftragsPositionDO position = new AuftragsPositionDO();
    position.setVollstaendigFakturiert(true);
    position.setStatus(AuftragsPositionsStatus.ABGESCHLOSSEN);
    auftrag3.addPosition(position);
    final Serializable id3 = auftragDao.save(auftrag3);
    dbNumber++; // Needed for getNextNumber test;

    logon(AbstractBase.TEST_PROJECT_MANAGER_USER);
    try {
      auftragDao.getById(id1);
      fail("AccessException expected: Projectmanager should not have access to foreign orders.");
    } catch (final AccessException ex) {
      // OK
    }
    auftragDao.getById(id2);
    try {
      auftragDao.getById(id3);
      fail("AccessException expected: Projectmanager should not have access to 2 years old orders.");
    } catch (final AccessException ex) {
      // OK
    }

    logon(AbstractBase.TEST_CONTROLLING_USER);
    auftrag1 = auftragDao.getById(id1);
    checkNoWriteAccess(id1, auftrag1, "Controller");

    logon(AbstractBase.TEST_USER);
    checkNoAccess(id1, auftrag1, "Other");

    logon(AbstractBase.TEST_ADMIN_USER);
    checkNoAccess(id1, auftrag1, "Admin ");
  }

  @Test
  public void checkAccess2()
  {
    logon(AbstractBase.TEST_FINANCE_USER);
    final GroupDO group1 = initTestDB.addGroup("AuftragDaoTest.ProjectManagers1", AbstractBase.TEST_PROJECT_ASSISTANT_USER);
    final GroupDO group2 = initTestDB.addGroup("AuftragDaoTest.ProjectManagers2", AbstractBase.TEST_PROJECT_MANAGER_USER);
    ProjektDO projekt1 = new ProjektDO();
    projekt1.setName("ACME - Webportal 1");
    projekt1.setProjektManagerGroup(group1);
    Serializable id = projektDao.save(projekt1);
    projekt1 = projektDao.getById(id);
    AuftragDO auftrag1 = new AuftragDO();
    auftrag1.setNummer(auftragDao.getNextNumber(auftrag1));
    auftrag1.setProjekt(projekt1);
    auftrag1.addPosition(new AuftragsPositionDO());
    id = auftragDao.save(auftrag1);
    dbNumber++; // Needed for getNextNumber test;
    auftrag1 = auftragDao.getById(id);

    ProjektDO projekt2 = new ProjektDO();
    projekt2.setName("ACME - Webportal 2");
    projekt2.setProjektManagerGroup(group2);
    id = projektDao.save(projekt2);
    projekt2 = projektDao.getById(id);
    AuftragDO auftrag2 = new AuftragDO();
    auftrag2.setNummer(auftragDao.getNextNumber(auftrag2));
    auftrag2.setProjekt(projekt2);
    auftrag2.addPosition(new AuftragsPositionDO());
    id = auftragDao.save(auftrag2);
    dbNumber++; // Needed for getNextNumber test;
    auftrag2 = auftragDao.getById(id);

    logon(AbstractBase.TEST_CONTROLLING_USER);
    checkNoWriteAccess(id, auftrag1, "Controlling");

    logon(AbstractBase.TEST_USER);
    checkNoAccess(id, auftrag1, "Other");

    logon(AbstractBase.TEST_PROJECT_MANAGER_USER);
    projektDao.getList(new ProjektFilter());
    checkNoAccess(auftrag1.getId(), "Project manager");
    checkNoWriteAccess(auftrag1.getId(), auftrag1, "Project manager");
    checkHasUpdateAccess(auftrag2.getId());

    logon(AbstractBase.TEST_PROJECT_ASSISTANT_USER);
    projektDao.getList(new ProjektFilter());
    checkHasUpdateAccess(auftrag1.getId());
    checkNoAccess(auftrag2.getId(), "Project assistant");
    checkNoWriteAccess(auftrag2.getId(), auftrag2, "Project assistant");

    logon(AbstractBase.TEST_ADMIN_USER);
    checkNoAccess(id, auftrag1, "Admin ");
  }

  @Test
  public void checkPartlyReadwriteAccess()
  {
    logon(AbstractBase.TEST_ADMIN_USER);
    PFUserDO user = initTestDB.addUser("AuftragDaoCheckPartlyReadWriteAccess");
    GroupDO financeGroup = getGroup(AbstractBase.FINANCE_GROUP);
    financeGroup.getSafeAssignedUsers().add(user);
    groupDao.update(financeGroup);
    final GroupDO projectAssistants = getGroup(AbstractBase.PROJECT_ASSISTANT);
    projectAssistants.getSafeAssignedUsers().add(user);
    groupDao.update(projectAssistants);

    final GroupDO group = initTestDB.addGroup("AuftragDaoTest.checkPartlyReadwriteAccess");
    logon(AbstractBase.TEST_FINANCE_USER);
    ProjektDO projekt = new ProjektDO();
    projekt.setName("ACME - Webportal checkPartlyReadwriteAccess");
    projekt.setProjektManagerGroup(group);
    Serializable id = projektDao.save(projekt);
    projekt = projektDao.getById(id);

    AuftragDO auftrag = new AuftragDO();
    auftrag.setNummer(auftragDao.getNextNumber(auftrag));
    auftrag.setProjekt(projekt);
    auftrag.addPosition(new AuftragsPositionDO());
    id = auftragDao.save(auftrag);
    dbNumber++; // Needed for getNextNumber test;
    auftrag = auftragDao.getById(id);

    logon(user);
    try {
      auftrag = auftragDao.getById(id);
      fail("Access exception expected.");
    } catch (final AccessException ex) {
      assertEquals("access.exception.userHasNotRight", ex.getI18nKey());
    }
    logon(AbstractBase.TEST_ADMIN_USER);
    user.addRight(new UserRightDO(UserRightId.PM_ORDER_BOOK, UserRightValue.PARTLYREADWRITE)); //
    userService.update(user);
    userRightDao.save(new ArrayList<>(user.getRights()));
    user = userService.getById(user.getId());
    logon(user);
    try {
      auftrag = auftragDao.getById(id);
      fail("Access exception expected.");
    } catch (final AccessException ex) {
      assertEquals("access.exception.userHasNotRight", ex.getI18nKey());
    }
    logon(AbstractBase.TEST_ADMIN_USER);
    final UserRightDO right = user.getRight(UserRightId.PM_ORDER_BOOK);
    right.setValue(UserRightValue.READWRITE); // Full access
    userRightDao.update(right);
    logon(user);
    auftrag = auftragDao.getById(id);
    logon(AbstractBase.TEST_ADMIN_USER);
    right.setValue(UserRightValue.PARTLYREADWRITE);
    userRightDao.update(right);
    group.getAssignedUsers().add(user);
    groupDao.update(group); // User is now in project manager group.
    logon(user);
    auftrag = auftragDao.getById(id);
  }

  private void checkHasUpdateAccess(final Serializable auftragsId)
  {
    AuftragDO auftrag = auftragDao.getById(auftragsId);
    final String value = String.valueOf(random.nextLong());
    auftrag.setBemerkung(value);
    auftragDao.update(auftrag);
    auftrag = auftragDao.getById(auftragsId);
    assertEquals(value, auftrag.getBemerkung());
  }

  private void checkNoAccess(final String who)
  {
    try {
      final AuftragFilter filter = new AuftragFilter();
      auftragDao.getList(filter);
      fail("AccessException expected: " + who + " users should not have select list access to orders.");
    } catch (final AccessException ex) {
      // OK
    }
  }

  private void checkNoAccess(final Serializable auftragsId, final String who)
  {
    try {
      auftragDao.getById(auftragsId);
      fail("AccessException expected: " + who + " users should not have select access to orders.");
    } catch (final AccessException ex) {
      // OK
    }
  }

  private void checkNoAccess(final Serializable id, final AuftragDO auftrag, final String who)
  {
    checkNoAccess(who);
    checkNoAccess(id, who);
    checkNoWriteAccess(id, auftrag, who);
  }

  private void checkNoWriteAccess(final Serializable id, final AuftragDO auftrag, final String who)
  {
    try {
      final AuftragDO auf = new AuftragDO();
      final int number = auftragDao.getNextNumber(auf);
      auf.setNummer(number);
      auftragDao.save(auf);
      fail("AccessException expected: " + who + " users should not have save access to orders.");
    } catch (final AccessException ex) {
      // OK
    }
    try {
      auftrag.setBemerkung(who);
      auftragDao.update(auftrag);
      fail("AccessException expected: " + who + " users should not have update access to orders.");
    } catch (final AccessException ex) {
      // OK
    }
  }

  @Test
  public void checkVollstaendigFakturiert()
  {
    logon(AbstractBase.TEST_FINANCE_USER);
    AuftragDO auftrag1 = new AuftragDO();
    auftrag1.setNummer(auftragDao.getNextNumber(auftrag1));
    auftragDao.setContactPerson(auftrag1, getUserId(AbstractBase.TEST_PROJECT_MANAGER_USER));
    auftrag1.addPosition(new AuftragsPositionDO());
    final Serializable id1 = auftragDao.save(auftrag1);
    dbNumber++; // Needed for getNextNumber test;
    auftrag1 = auftragDao.getById(id1);

    AuftragsPositionDO position = auftrag1.getPositionenIncludingDeleted().get(0);
    position.setVollstaendigFakturiert(true);
    try {
      auftragDao.update(auftrag1);
      fail("UserException expected: Only orders with state ABGESCHLOSSEN should be set as fully invoiced.");
    } catch (final UserException ex) {
      assertEquals("fibu.auftrag.error.nurAbgeschlosseneAuftragsPositionenKoennenVollstaendigFakturiertSein",
          ex.getI18nKey());
    }

    auftrag1 = auftragDao.getById(id1);
    auftrag1.setAuftragsStatus(AuftragsStatus.ABGESCHLOSSEN);
    auftragDao.update(auftrag1);
    auftrag1 = auftragDao.getById(id1);

    logon(AbstractBase.TEST_PROJECT_MANAGER_USER);
    position = auftrag1.getPositionenIncludingDeleted().get(0);
    position.setStatus(AuftragsPositionsStatus.ABGESCHLOSSEN);
    position.setVollstaendigFakturiert(true);
    try {
      auftragDao.update(auftrag1);
      fail("AccessException expected: Projectmanager should not able to set order as fully invoiced.");
    } catch (final AccessException ex) {
      // OK
      assertEquals("fibu.auftrag.error.vollstaendigFakturiertProtection", ex.getI18nKey());
    }

    logon(AbstractBase.TEST_FINANCE_USER);
    position = auftrag1.getPositionenIncludingDeleted().get(0);
    position.setStatus(AuftragsPositionsStatus.ABGESCHLOSSEN);
    position.setVollstaendigFakturiert(true);
    auftragDao.update(auftrag1);
  }

  @Test
  public void checkEmptyAuftragsPositionen()
  {
    logon(AbstractBase.TEST_FINANCE_USER);
    AuftragDO auftrag = new AuftragDO();
    auftrag.setNummer(auftragDao.getNextNumber(auftrag));
    auftrag.addPosition(new AuftragsPositionDO());
    auftrag.addPosition(new AuftragsPositionDO());
    auftrag.addPosition(new AuftragsPositionDO());
    auftrag.addPosition(new AuftragsPositionDO());
    Serializable id = auftragDao.save(auftrag);
    dbNumber++; // Needed for getNextNumber test;
    auftrag = auftragDao.getById(id);
    assertEquals(1, auftrag.getPositionenIncludingDeleted().size());
    auftrag = new AuftragDO();
    auftrag.setNummer(auftragDao.getNextNumber(auftrag));
    auftrag.addPosition(new AuftragsPositionDO());
    auftrag.addPosition(new AuftragsPositionDO());
    final AuftragsPositionDO position = new AuftragsPositionDO();
    position.setTitel("Hurzel");
    auftrag.addPosition(position);
    auftrag.addPosition(new AuftragsPositionDO());
    id = auftragDao.save(auftrag);
    dbNumber++; // Needed for getNextNumber test;
    auftrag = auftragDao.getById(id);
    assertEquals(3, auftrag.getPositionenIncludingDeleted().size());
    auftrag.getPositionenIncludingDeleted().get(2).setTitel(null);
    auftragDao.update(auftrag);
    auftrag = auftragDao.getById(id);
    assertEquals(3, auftrag.getPositionenIncludingDeleted().size());
  }

  @Test
  public void validateDatesInPaymentScheduleWithinPeriodOfPerformanceOfPosition()
  {
    final AuftragDO auftrag = new AuftragDO();
    final List<AuftragsPositionDO> auftragsPositions = auftrag.ensureAndGetPositionen();
    final List<PaymentScheduleDO> paymentSchedules = auftrag.ensureAndGetPaymentSchedules();

    auftrag.setPeriodOfPerformanceBegin(java.sql.Date.valueOf(LocalDate.of(2017, 5, 1)));
    auftrag.setPeriodOfPerformanceEnd(java.sql.Date.valueOf(LocalDate.of(2017, 6, 30)));

    auftragsPositions.add(new AuftragsPositionDO().setNumber((short) 1));
    auftragsPositions.add(new AuftragsPositionDO().setNumber((short) 2).setPeriodOfPerformanceType(PeriodOfPerformanceType.OWN)
        .setPeriodOfPerformanceBegin(java.sql.Date.valueOf(LocalDate.of(2017, 5, 24)))
        .setPeriodOfPerformanceEnd(java.sql.Date.valueOf(LocalDate.of(2017, 5, 25))));

    paymentSchedules.add(new PaymentScheduleDO().setPositionNumber((short) 1).setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2017, 5, 1))));
    paymentSchedules.add(new PaymentScheduleDO().setPositionNumber((short) 1).setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2017, 5, 20))));
    paymentSchedules.add(new PaymentScheduleDO().setPositionNumber((short) 1).setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2017, 6, 30))));
    paymentSchedules.add(new PaymentScheduleDO().setPositionNumber((short) 2).setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2017, 5, 24))));
    paymentSchedules.add(new PaymentScheduleDO().setPositionNumber((short) 2).setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2017, 5, 25))));

    boolean exceptionThrown = false;
    try {
      auftragDao.validateDatesInPaymentScheduleWithinPeriodOfPerformanceOfPosition(auftrag);
    } catch (UserException e) {
      exceptionThrown = true;
    }
    assertFalse(exceptionThrown);

    paymentSchedules.add(new PaymentScheduleDO().setPositionNumber((short) 1).setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2017, 4, 30))));
    paymentSchedules.add(new PaymentScheduleDO().setPositionNumber((short) 2).setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2017, 5, 26))));

    try {
      auftragDao.validateDatesInPaymentScheduleWithinPeriodOfPerformanceOfPosition(auftrag);
    } catch (UserException e) {
      exceptionThrown = true;
      assertEquals(e.getParams().length, 1);
      assertEquals(e.getParams()[0], "1, 2");
    }
    assertTrue(exceptionThrown);
  }

  @Test
  public void validateAmountsInPaymentScheduleNotGreaterThanNetSumOfPosition()
  {
    final AuftragDO auftrag = new AuftragDO();
    final List<AuftragsPositionDO> auftragsPositions = auftrag.ensureAndGetPositionen();
    final List<PaymentScheduleDO> paymentSchedules = auftrag.ensureAndGetPaymentSchedules();

    auftragsPositions.add(new AuftragsPositionDO().setNumber((short) 1).setNettoSumme(new BigDecimal(2000)));
    auftragsPositions.add(new AuftragsPositionDO().setNumber((short) 2).setNettoSumme(new BigDecimal(5000)));

    paymentSchedules.add(new PaymentScheduleDO().setPositionNumber((short) 1).setAmount(new BigDecimal(1000)));
    paymentSchedules.add(new PaymentScheduleDO().setPositionNumber((short) 1).setAmount(null)); // should not cause a NPE
    paymentSchedules.add(new PaymentScheduleDO().setPositionNumber((short) 1).setAmount(new BigDecimal(1000)));
    paymentSchedules.add(new PaymentScheduleDO().setPositionNumber((short) 2).setAmount(new BigDecimal(2000)));
    paymentSchedules.add(new PaymentScheduleDO().setPositionNumber((short) 2).setAmount(new BigDecimal(2999)));

    boolean exceptionThrown = false;
    try {
      auftragDao.validateAmountsInPaymentScheduleNotGreaterThanNetSumOfPosition(auftrag);
    } catch (UserException e) {
      exceptionThrown = true;
    }
    assertFalse(exceptionThrown);

    // amounts of position 1 (2001) will now be greater than netto summe (2000) -> should throw exception
    paymentSchedules.add(new PaymentScheduleDO().setPositionNumber((short) 1).setAmount(new BigDecimal(1)));

    try {
      auftragDao.validateAmountsInPaymentScheduleNotGreaterThanNetSumOfPosition(auftrag);
    } catch (UserException e) {
      exceptionThrown = true;
    }
    assertTrue(exceptionThrown);
  }

  @Test
  public void testPeriodOfPerformanceFilter()
  {
    logon(AbstractBase.TEST_FINANCE_USER);

    auftragDao.save(createAuftragWithPeriodOfPerformance(2017, 4, 1, 2017, 4, 30));
    auftragDao.save(createAuftragWithPeriodOfPerformance(2017, 4, 3, 2017, 4, 5));
    auftragDao.save(createAuftragWithPeriodOfPerformance(2017, 3, 31, 2017, 5, 1));
    auftragDao.save(createAuftragWithPeriodOfPerformance(2017, 3, 31, 2017, 4, 5));
    auftragDao.save(createAuftragWithPeriodOfPerformance(2017, 3, 31, 2017, 5, 1));
    auftragDao.save(createAuftragWithPeriodOfPerformance(2010, 1, 1, 2020, 12, 31));

    final AuftragFilter auftragFilter = new AuftragFilter();
    assertEquals(17, auftragDao.getList(auftragFilter).size());

    setPeriodOfPerformanceStartDateAndEndDate(auftragFilter, 2017, 4, 1, 2017, 4, 30);
    assertEquals(6, auftragDao.getList(auftragFilter).size());

    setPeriodOfPerformanceStartDateAndEndDate(auftragFilter, 2017, 4, 1, 2017, 4, 1);
    assertEquals(5, auftragDao.getList(auftragFilter).size());

    auftragFilter.setPeriodOfPerformanceStartDate(null);
    assertEquals(5, auftragDao.getList(auftragFilter).size());

    setPeriodOfPerformanceStartDateAndEndDate(auftragFilter, 2017, 4, 6, 2017, 4, 6);
    assertEquals(4, auftragDao.getList(auftragFilter).size());

    auftragFilter.setPeriodOfPerformanceStartDate(null);
    assertEquals(6, auftragDao.getList(auftragFilter).size());

    setPeriodOfPerformanceStartDateAndEndDate(auftragFilter, 2016, 1, 1, 2016, 1, 1);
    assertEquals(1, auftragDao.getList(auftragFilter).size());

    auftragFilter.setPeriodOfPerformanceEndDate(null);
    assertEquals(6, auftragDao.getList(auftragFilter).size());
  }

  private void setPeriodOfPerformanceStartDateAndEndDate(final AuftragFilter auftragFilter, final int startYear, final int startMonth, final int startDay,
      final int endYear, final int endMonth, final int endDay)
  {
    auftragFilter.setPeriodOfPerformanceStartDate(DateHelper.convertLocalDateTimeToDateInUTC(LocalDate.of(startYear, startMonth, startDay).atStartOfDay()));
    auftragFilter.setPeriodOfPerformanceEndDate(DateHelper.convertLocalDateTimeToDateInUTC(LocalDate.of(endYear, endMonth, endDay).atStartOfDay()));
  }

  private AuftragDO createAuftragWithPeriodOfPerformance(final int beginYear, final int beginMonth, final int beginDay, final int endYear, final int endMonth,
      final int endDay)
  {
    final AuftragDO auftrag = new AuftragDO();
    auftrag.setNummer(auftragDao.getNextNumber(auftrag));
    dbNumber++;
    auftrag.addPosition(new AuftragsPositionDO());
    auftrag.setPeriodOfPerformanceBegin(java.sql.Date.valueOf(LocalDate.of(beginYear, beginMonth, beginDay)));
    auftrag.setPeriodOfPerformanceEnd(java.sql.Date.valueOf(LocalDate.of(endYear, endMonth, endDay)));
    return auftrag;
  }
}
