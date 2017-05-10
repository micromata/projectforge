package org.projectforge.business.fibu.impl;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeSalaryDO;
import org.projectforge.business.fibu.EmployeeSalaryDao;
import org.projectforge.business.fibu.api.EmployeeSalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmployeeSalaryServiceImpl implements EmployeeSalaryService
{

  @Autowired
  private EmployeeSalaryDao employeeSalaryDao;

  @Override
  public EmployeeSalaryDO getLatestSalaryForEmployee(EmployeeDO employee)
  {
    List<EmployeeSalaryDO> findByEmployee = employeeSalaryDao.findByEmployee(employee);
    return findByEmployee
        .stream()
        .sorted((sal1, sal2) -> {
          Calendar c1 = new GregorianCalendar(sal1.getYear(), sal1.getMonth(), 1);
          Calendar c2 = new GregorianCalendar(sal2.getYear(), sal2.getMonth(), 1);
          return c2.compareTo(c1);
        })
        .findFirst()
        .orElse(null);
  }

  @Override
  public EmployeeSalaryDO getEmployeeSalaryByDate(EmployeeDO employee, Calendar selectedDate)
  {
    List<EmployeeSalaryDO> findByEmployee = employeeSalaryDao.findByEmployee(employee);
    for (EmployeeSalaryDO sal : findByEmployee) {
      if (sal.isDeleted() == false && sal.getYear().equals(selectedDate.get(Calendar.YEAR)) && sal.getMonth().equals(selectedDate.get(Calendar.MONTH))) {
        return sal;
      }
    }
    return null;
  }

  @Override
  public void saveOrUpdate(EmployeeSalaryDO employeeSalaryDO)
  {
    employeeSalaryDao.saveOrUpdate(employeeSalaryDO);
  }

  @Override
  public EmployeeSalaryDO selectByPk(Integer id)
  {
    return employeeSalaryDao.getById(id);
  }

}
