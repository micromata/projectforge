package org.projectforge.business.fibu.api;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeSalaryDO;

public interface EmployeeSalaryService
{

  EmployeeSalaryDO getLatestSalaryForEmployee(EmployeeDO employee);

}
