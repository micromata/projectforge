package org.projectforge.plugins.eed.service;

import org.projectforge.plugins.eed.EmployeeGeneralValueDO;
import org.projectforge.plugins.eed.EmployeeGeneralValueDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeGeneralValueService
{
  @Autowired
  private EmployeeGeneralValueDao employeeGeneralValueDao;

  public EmployeeGeneralValueDO getValueDO()
  {
    final List<EmployeeGeneralValueDO> employeeGeneralValueDOs = employeeGeneralValueDao.internalLoadAll();
    if(employeeGeneralValueDOs.isEmpty())
    {
      final EmployeeGeneralValueDO employeeGeneralValueDO = employeeGeneralValueDao.newInstance();
      employeeGeneralValueDao.save(employeeGeneralValueDO);
      return employeeGeneralValueDO;
    }
    else if(employeeGeneralValueDOs.size() != 1)
    {
        throw new RuntimeException("Expected One employeeGeneralValueDO's, but found " + employeeGeneralValueDOs.size());
    }
    return employeeGeneralValueDOs.get(0);
  }


}
