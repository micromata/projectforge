package org.projectforge.business.fibu.api;

import java.util.Calendar;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeSalaryDO;

public interface EmployeeSalaryService
{

  EmployeeSalaryDO getLatestSalaryForEmployee(EmployeeDO employee);

  EmployeeSalaryDO getEmployeeSalaryByDate(EmployeeDO employee, Calendar selectedDate);

  void saveOrUpdate(EmployeeSalaryDO employeeSalaryDO);

  EmployeeSalaryDO selectByPk(Integer id);

}
