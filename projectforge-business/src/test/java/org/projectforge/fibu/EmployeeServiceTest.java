package org.projectforge.fibu;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.NoResultException;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.Test;

public class EmployeeServiceTest extends AbstractTestBase
{
  @Autowired
  private EmployeeService employeeService;

  @Autowired
  private UserService userService;

  @Test
  public void testInsertDelete()
  {
    logon(TEST_FULL_ACCESS_USER);

    PFUserDO pfUserDO = getPfUserDO();

    EmployeeDO employeeDO = new EmployeeDO();
    employeeDO.setUser(pfUserDO);
    employeeDO.setAccountHolder("Vorname Name");
    Integer id = employeeService.save(employeeDO);
    Assert.assertTrue(id != null && id > 0);
    employeeService.delete(employeeDO);
    EmployeeDO employeeDO1 = null;
    List<Exception> exceptionList = new ArrayList<>();
    try {
      employeeDO1 = employeeService.selectByPkDetached(id);
    } catch (NoResultException e) {
      exceptionList.add(e);
    }

    Assert.assertEquals(exceptionList.size(), 1);
    Assert.assertEquals(employeeDO1, null);
  }

  private PFUserDO getPfUserDO()
  {
    return userService.getUserDao().getUserGroupCache().getAllUsers().iterator().next();
  }

  @Test
  public void testUpdateAttribute()
  {
    logon(TEST_FULL_ACCESS_USER);
    PFUserDO pfUserDO = getPfUserDO();
    EmployeeDO employeeDO = new EmployeeDO();
    employeeDO.setAccountHolder("Vorname Name");
    String abteilung = "Test";
    employeeDO.setAbteilung(abteilung);
    employeeDO.setUser(pfUserDO);
    employeeService.save(employeeDO);
    String expectedNewAccountHolder = "Firstname Lastname";
    employeeService.updateAttribute(pfUserDO.getId(), expectedNewAccountHolder, "accountHolder");
    EmployeeDO employeeByUserId = employeeService.getEmployeeByUserId(pfUserDO.getId());
    Assert.assertEquals(employeeByUserId.getAbteilung(), abteilung);
    Assert.assertEquals(employeeByUserId.getAccountHolder(), expectedNewAccountHolder);
  }
}
