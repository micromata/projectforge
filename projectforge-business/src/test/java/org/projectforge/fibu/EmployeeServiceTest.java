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

package org.projectforge.fibu;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.user.service.UserService;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.business.vacation.service.VacationServiceImpl;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.NoResultException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class EmployeeServiceTest extends AbstractTestBase
{
  @Autowired
  private EmployeeService employeeService;

  @Autowired
  private UserService userService;

  @InjectMocks
  private VacationService vacationService = new VacationServiceImpl();

  @Test
  public void testInsertDelete()
  {
    logon(AbstractTestBase.TEST_FULL_ACCESS_USER);
    PFUserDO pfUserDO = getUser(TEST_FINANCE_USER);
    EmployeeDO employeeDO = new EmployeeDO();
    employeeDO.setAccountHolder("Horst Mustermann");
    employeeDO.setAbteilung("Finance");
    employeeDO.setUser(pfUserDO);
    Integer id = employeeService.save(employeeDO);
    assertTrue(id != null && id > 0);
    employeeService.delete(employeeDO);
    EmployeeDO employeeDO1 = null;
    List<Exception> exceptionList = new ArrayList<>();
    try {
      employeeDO1 = employeeService.selectByPkDetached(id);
    } catch (NoResultException e) {
      exceptionList.add(e);
    }

    assertEquals(exceptionList.size(), 1);
    assertEquals(employeeDO1, null);
  }

  @Test
  public void testUpdateAttribute()
  {
    logon(AbstractTestBase.TEST_FULL_ACCESS_USER);
    PFUserDO pfUserDO = getUser(TEST_PROJECT_ASSISTANT_USER);
    EmployeeDO employeeDO = new EmployeeDO();
    employeeDO.setAccountHolder("Vorname Name");
    String abteilung = "Test";
    employeeDO.setAbteilung(abteilung);
    employeeDO.setUser(pfUserDO);
    employeeService.save(employeeDO);
    String expectedNewAccountHolder = "Firstname Lastname";
    employeeService.updateAttribute(pfUserDO.getId(), expectedNewAccountHolder, "accountHolder");
    EmployeeDO employeeByUserId = employeeService.getEmployeeByUserId(pfUserDO.getId());
    assertEquals(employeeByUserId.getAbteilung(), abteilung);
    assertEquals(employeeByUserId.getAccountHolder(), expectedNewAccountHolder);
  }

  @Test
  public void isEmployeeActiveWithoutAustrittsdatumTest()
  {
    EmployeeDO employee = new EmployeeDO();
    boolean result = employeeService.isEmployeeActive(employee);
    assertTrue(result);
  }

  @Test
  public void isEmployeeActiveWithAustrittsdatumTest()
  {
    EmployeeDO employee = new EmployeeDO();
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.MONTH, 1);
    employee.setAustrittsDatum(cal.getTime());
    boolean result = employeeService.isEmployeeActive(employee);
    assertTrue(result);
  }

  @Test
  public void isEmployeeActiveWithAustrittsdatumBeforeTest()
  {
    EmployeeDO employee = new EmployeeDO();
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.MONTH, -1);
    employee.setAustrittsDatum(cal.getTime());
    boolean result = employeeService.isEmployeeActive(employee);
    assertFalse(result);
  }

  @Test
  public void isEmployeeActiveWithAustrittsdatumNowTest()
  {
    EmployeeDO employee = new EmployeeDO();
    Calendar cal = Calendar.getInstance();
    employee.setAustrittsDatum(cal.getTime());
    boolean result = employeeService.isEmployeeActive(employee);
    assertFalse(result);
  }

  @Test
  @Disabled
  public void testGetStudentVacationCountPerDay()
  {
    MockitoAnnotations.initMocks(this);
    when(vacationService.getVacationCount(2017, Calendar.MAY, 2017, Calendar.OCTOBER, new PFUserDO())).thenReturn("TestCase 1");
    when(vacationService.getVacationCount(2016, Calendar.JULY, 2017, Calendar.OCTOBER, new PFUserDO())).thenReturn("TestCase 2");
    when(vacationService.getVacationCount(2017, Calendar.JULY, 2017, Calendar.OCTOBER, new PFUserDO())).thenReturn("TestCase 3");

    Calendar testCase1 = new GregorianCalendar(ThreadLocalUserContext.getTimeZone());
    testCase1.set(Calendar.YEAR, 2017);
    testCase1.set(Calendar.MONTH, Calendar.OCTOBER);
    when(new GregorianCalendar(ThreadLocalUserContext.getTimeZone())).thenReturn((GregorianCalendar) testCase1);
    Assertions.assertEquals("TestCase 1", employeeService.getStudentVacationCountPerDay(new EmployeeDO()));

    Calendar testCase2 = new GregorianCalendar(ThreadLocalUserContext.getTimeZone());
    testCase2.set(Calendar.YEAR, 2017);
    testCase2.set(Calendar.MONTH, Calendar.FEBRUARY);
    when(new GregorianCalendar(ThreadLocalUserContext.getTimeZone())).thenReturn((GregorianCalendar) testCase2);

    Date testCase3 = new Date();
    testCase3.setMonth(7);
    testCase3.setYear(2017);
    when(new GregorianCalendar(ThreadLocalUserContext.getTimeZone())).thenReturn((GregorianCalendar) testCase1);
    when(new EmployeeDO().getEintrittsDatum()).thenReturn(testCase3);
  }

}
