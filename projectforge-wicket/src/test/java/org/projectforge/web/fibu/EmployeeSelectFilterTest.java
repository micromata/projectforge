package org.projectforge.web.fibu;

import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.fibu.EmployeeSalaryDO;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Date;
import java.util.List;

public class EmployeeSelectFilterTest extends AbstractTestBase {

  @Autowired
  EmployeeDao employeeDao;

  @Autowired
  UserDao userDao;

  @Override
  protected void beforeAll() {
    logon(AbstractTestBase.TEST_FULL_ACCESS_USER);
  }

  @Test
  public void test()
          throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
    WicketTester wicketTester = new WicketTester();

    EmployeeSalaryDO data = new EmployeeSalaryDO();
    EmployeeSelectPanel selectPanel = new EmployeeSelectPanel("1", new PropertyModel<EmployeeDO>(data,
            "employee"), null, "employee");
    Field panelDao = selectPanel.getClass().getDeclaredField("employeeDao");
    panelDao.setAccessible(true);
    panelDao.set(selectPanel, employeeDao);

    PFUserDO pfUserDO = new PFUserDO();
    pfUserDO.setUsername("EmployeeSelectFilterTestuser1");
    userDao.save(pfUserDO);
    pfUserDO.setFirstname("1");
    pfUserDO.setLastname("user1");
    userDao.update(pfUserDO);
    EmployeeDO employeeDO = new EmployeeDO();
    employeeDO.setUser(pfUserDO);
    this.employeeDao.save(employeeDO);

    PFUserDO pfUserDO1 = new PFUserDO();
    pfUserDO1.setUsername("EmployeeSelectFilterTestuser2");
    userDao.save(pfUserDO1);
    pfUserDO1.setFirstname("2");
    pfUserDO1.setLastname("user2");
    userDao.update(pfUserDO1);
    EmployeeDO employeeDO1 = new EmployeeDO();
    employeeDO1.setUser(pfUserDO1);
    employeeDO1.setAustrittsDatum(Date.from(new Date().toInstant().minus(Duration.ofDays(2))));
    this.employeeDao.save(employeeDO1);

    PFUserDO pfUserDO2 = new PFUserDO();

    pfUserDO2.setUsername("EmployeeSelectFilterTestuser3");
    pfUserDO2.setFirstname("3");
    pfUserDO2.setLastname("user3");
    userDao.save(pfUserDO2);
    EmployeeDO employeeDO2 = new EmployeeDO();
    employeeDO2.setUser(pfUserDO2);
    employeeDO2.setAustrittsDatum(Date.from(new Date().toInstant().plus(Duration.ofDays(2))));
    this.employeeDao.save(employeeDO2);

    Method getChoices = EmployeeSelectPanel.class.getDeclaredMethod("getFilteredEmployeeDOs", String.class);

    getChoices.setAccessible(true);
    List<EmployeeDO> employeeDOList = (List<EmployeeDO>) getChoices
            .invoke(selectPanel, "user1");
    Assertions.assertTrue(employeeDOList.get(0).getUser().getUsername().equals(pfUserDO.getUsername()));

    employeeDOList = (List<EmployeeDO>) getChoices.invoke(selectPanel, "user2");
    Assertions.assertTrue(employeeDOList.isEmpty());

    employeeDOList = (List<EmployeeDO>) getChoices.invoke(selectPanel, "user3");
    Assertions.assertTrue(employeeDOList.get(0).getUser().getUsername().equals(pfUserDO2.getUsername()));
  }
}
