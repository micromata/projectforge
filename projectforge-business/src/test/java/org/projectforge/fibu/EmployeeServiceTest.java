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

package org.projectforge.fibu;

import org.junit.jupiter.api.Test;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class EmployeeServiceTest extends AbstractTestBase
{
  @Autowired
  private EmployeeService employeeService;

  @Test
  public void testInsertDelete()
  {
    logon(AbstractTestBase.TEST_FULL_ACCESS_USER);
    PFUserDO pfUserDO = getUser(TEST_FINANCE_USER);
    EmployeeDO employeeDO = new EmployeeDO();
    employeeDO.setAccountHolder("Horst Mustermann");
    employeeDO.setAbteilung("Finance");
    employeeDO.setUser(pfUserDO);
    fail("TODO: Implement employeeService.save");
/*    Integer id = employeeService.save(employeeDO);
    assertTrue(id != null && id > 0);
    employeeService.delete(employeeDO);
    EmployeeDO employeeDO1 = null;
    List<Exception> exceptionList = new ArrayList<>();
    try {
      employeeDO1 = employeeService.selectByPkDetached(id);
    } catch (NoResultException e) {
      exceptionList.add(e);
    }

    assertEquals(1, exceptionList.size());
    assertNull(employeeDO1);*/
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
//    employeeService.save(employeeDO);
    String expectedNewAccountHolder = "Firstname Lastname";
  //  employeeService.updateAttribute(pfUserDO.getId(), expectedNewAccountHolder, "accountHolder");
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
    LocalDate dt = LocalDate.now().plusMonths(1);
    employee.setAustrittsDatum(dt);
    boolean result = employeeService.isEmployeeActive(employee);
    assertTrue(result);
  }

  @Test
  public void isEmployeeActiveWithAustrittsdatumBeforeTest()
  {
    EmployeeDO employee = new EmployeeDO();
    LocalDate dt = LocalDate.now().minusMonths(1);
    employee.setAustrittsDatum(dt);
    boolean result = employeeService.isEmployeeActive(employee);
    assertFalse(result);
  }

  @Test
  public void isEmployeeActiveWithAustrittsdatumNowTest()
  {
    EmployeeDO employee = new EmployeeDO();
    LocalDate dt = LocalDate.now();
    employee.setAustrittsDatum(dt);
    boolean result = employeeService.isEmployeeActive(employee);
    assertFalse(result);
  }
}
